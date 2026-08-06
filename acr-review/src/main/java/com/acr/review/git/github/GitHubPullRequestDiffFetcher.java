package com.acr.review.git.github;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.acr.review.domain.ReviewPipelineConstants;
import com.acr.review.git.GitAccessContext;
import com.acr.review.git.GitPullRequestDiffFetcher;
import com.acr.review.git.GitPullRequestDiffResult;
import com.acr.review.git.GitRepositoryCoordinates;
import com.acr.review.git.HttpResponseBodies;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/** 通过 GitHub Compare Diff API 拉取 PR 变更。响应体按上限截断读取，避免超大 Diff 压垮内存。 */
@Component
public class GitHubPullRequestDiffFetcher implements GitPullRequestDiffFetcher
{
    /** Diff 原始字节读取上限（按 UTF-8 双字节预留，下游渲染再按字符截断）。 */
    private static final long MAX_DIFF_BYTES = ReviewPipelineConstants.MAX_DIFF_CHARS * 2L;
    private static final java.util.regex.Pattern SHA_PATTERN = java.util.regex.Pattern.compile("^[0-9a-fA-F]{4,64}$");

    private final HttpUrl apiBaseUrl;
    private final OkHttpClient client;

    public GitHubPullRequestDiffFetcher(
        @Value("${review.github.api-url:https://api.github.com}") String apiBaseUrl,
        @Value("${review.github.connect-timeout-ms:5000}") int connectTimeoutMs,
        @Value("${review.github.read-timeout-ms:30000}") int readTimeoutMs)
    {
        HttpUrl parsed = HttpUrl.parse(apiBaseUrl);
        if (parsed == null)
        {
            throw new IllegalArgumentException("GitHub API 地址配置无效");
        }
        this.apiBaseUrl = parsed;
        this.client = new OkHttpClient.Builder()
            .connectTimeout(connectTimeoutMs, TimeUnit.MILLISECONDS)
            .readTimeout(readTimeoutMs, TimeUnit.MILLISECONDS)
            .callTimeout(readTimeoutMs, TimeUnit.MILLISECONDS)
            .build();
    }

    @Override
    public String providerCode()
    {
        return "GITHUB";
    }

    @Override
    public GitPullRequestDiffResult fetchDiff(GitRepositoryCoordinates repository, GitAccessContext access,
                                              String baseSha, String headSha)
    {
        if (repository == null)
        {
            return GitPullRequestDiffResult.fail(ReviewPipelineConstants.FAILURE_WORKSPACE_PREPARE, "仓库信息不完整");
        }
        String token;
        try
        {
            token = access.requireToken();
        }
        catch (IllegalArgumentException ex)
        {
            return GitPullRequestDiffResult.fail(ReviewPipelineConstants.FAILURE_CREDENTIAL_ERROR, "GitHub 凭据不可用");
        }
        if (!isValidSha(baseSha) || !isValidSha(headSha))
        {
            return GitPullRequestDiffResult.fail(ReviewPipelineConstants.FAILURE_WORKSPACE_PREPARE, "base/head SHA 格式非法，无法获取 Diff");
        }

        HttpUrl url = apiBaseUrl.newBuilder()
            .addPathSegment("repos")
            .addPathSegment(repository.owner())
            .addPathSegment(repository.repository())
            .addPathSegment("compare")
            .addPathSegment(baseSha + "..." + headSha)
            .build();
        Request request = new Request.Builder()
            .url(url)
            .get()
            .header("Authorization", "Bearer " + token)
            .header("Accept", "application/vnd.github.v3.diff")
            .header("User-Agent", "ai-code-review")
            .build();

        try (Response response = client.newCall(request).execute())
        {
            int status = response.code();
            if (status == 200)
            {
                return GitPullRequestDiffResult.ok(readBodyCapped(response));
            }
            if (status == 401)
            {
                return GitPullRequestDiffResult.fail(ReviewPipelineConstants.FAILURE_CREDENTIAL_ERROR, "GitHub 凭据无效或已过期");
            }
            if (isRateLimited(response))
            {
                return GitPullRequestDiffResult.fail(ReviewPipelineConstants.FAILURE_RATE_LIMIT, "GitHub API 已限流，请稍后重试");
            }
            if (status == 403)
            {
                return GitPullRequestDiffResult.fail(ReviewPipelineConstants.FAILURE_CREDENTIAL_ERROR, "当前 Token 权限不足，无法读取 PR Diff");
            }
            if (status == 404)
            {
                return GitPullRequestDiffResult.fail(ReviewPipelineConstants.FAILURE_WORKSPACE_PREPARE, "未找到对应提交范围，请确认 base/head SHA");
            }
            return GitPullRequestDiffResult.fail(ReviewPipelineConstants.FAILURE_WORKSPACE_PREPARE,
                "获取 PR Diff 失败，GitHub 返回状态：" + status);
        }
        catch (InterruptedIOException ex)
        {
            Thread.currentThread().interrupt();
            return GitPullRequestDiffResult.fail(ReviewPipelineConstants.FAILURE_TIMEOUT, "获取 PR Diff 超时");
        }
        catch (IOException ex)
        {
            return GitPullRequestDiffResult.fail(ReviewPipelineConstants.FAILURE_WORKSPACE_PREPARE,
                "无法连接 GitHub 获取 Diff，请检查网络");
        }
    }

    private static boolean isValidSha(String sha)
    {
        return sha != null && SHA_PATTERN.matcher(sha).matches();
    }

    private static boolean isRateLimited(Response response)
    {
        if (response.code() == 429)
        {
            return true;
        }
        return response.code() == 403 && "0".equals(response.header("X-RateLimit-Remaining"));
    }

    /** 有界读取响应体：超出上限的部分直接丢弃，防止超大 Diff 耗尽堆内存。 */
    private static String readBodyCapped(Response response) throws IOException
    {
        return HttpResponseBodies.readCapped(response, MAX_DIFF_BYTES);
    }
}
