package com.acr.review.git.gitlab;

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
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/** 通过 GitLab Compare API 拉取 MR 变更 diff。 */
@Component
public class GitLabPullRequestDiffFetcher implements GitPullRequestDiffFetcher
{
    private static final long MAX_DIFF_BYTES = ReviewPipelineConstants.MAX_DIFF_CHARS * 2L;
    private static final java.util.regex.Pattern SHA_PATTERN = java.util.regex.Pattern.compile("^[0-9a-fA-F]{4,64}$");

    private final OkHttpClient client;

    public GitLabPullRequestDiffFetcher(
        @Value("${review.gitlab.connect-timeout-ms:5000}") int connectTimeoutMs,
        @Value("${review.gitlab.read-timeout-ms:30000}") int readTimeoutMs)
    {
        this.client = new OkHttpClient.Builder()
            .connectTimeout(connectTimeoutMs, TimeUnit.MILLISECONDS)
            .readTimeout(readTimeoutMs, TimeUnit.MILLISECONDS)
            .callTimeout(readTimeoutMs, TimeUnit.MILLISECONDS)
            .build();
    }

    @Override
    public String providerCode()
    {
        return "GITLAB";
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
            return GitPullRequestDiffResult.fail(ReviewPipelineConstants.FAILURE_CREDENTIAL_ERROR, "GitLab 凭据不可用");
        }
        if (!isValidSha(baseSha) || !isValidSha(headSha))
        {
            return GitPullRequestDiffResult.fail(
                ReviewPipelineConstants.FAILURE_WORKSPACE_PREPARE, "base/head SHA 格式非法，无法获取 Diff");
        }

        HttpUrl url = GitLabProvider.projectUrl(access, repository.fullPath(), "repository/compare")
            .newBuilder()
            .addQueryParameter("from", baseSha)
            .addQueryParameter("to", headSha)
            .build();
        Request request = new Request.Builder()
            .url(url)
            .get()
            .header("PRIVATE-TOKEN", token)
            .header("User-Agent", "ai-code-review")
            .build();

        try (Response response = client.newCall(request).execute())
        {
            int status = response.code();
            if (status == 200)
            {
                return GitPullRequestDiffResult.ok(readDiffFromCompare(response));
            }
            if (status == 401)
            {
                return GitPullRequestDiffResult.fail(
                    ReviewPipelineConstants.FAILURE_CREDENTIAL_ERROR, "GitLab 凭据无效或已过期");
            }
            if (status == 429)
            {
                return GitPullRequestDiffResult.fail(
                    ReviewPipelineConstants.FAILURE_RATE_LIMIT, "GitLab API 已限流，请稍后重试");
            }
            if (status == 403)
            {
                return GitPullRequestDiffResult.fail(
                    ReviewPipelineConstants.FAILURE_CREDENTIAL_ERROR, "当前 Token 权限不足，无法读取 MR Diff");
            }
            if (status == 404)
            {
                return GitPullRequestDiffResult.fail(
                    ReviewPipelineConstants.FAILURE_WORKSPACE_PREPARE, "未找到对应提交范围，请确认 base/head SHA");
            }
            return GitPullRequestDiffResult.fail(status >= 500
                ? ReviewPipelineConstants.FAILURE_DEPENDENCY_UNAVAILABLE
                : ReviewPipelineConstants.FAILURE_WORKSPACE_PREPARE,
                "获取 MR Diff 失败，GitLab 返回状态：" + status);
        }
        catch (InterruptedIOException ex)
        {
            Thread.currentThread().interrupt();
            return GitPullRequestDiffResult.fail(ReviewPipelineConstants.FAILURE_TIMEOUT, "获取 MR Diff 超时");
        }
        catch (IOException ex)
        {
            return GitPullRequestDiffResult.fail(ReviewPipelineConstants.FAILURE_DEPENDENCY_UNAVAILABLE,
                "无法连接 GitLab 获取 Diff，请检查网络");
        }
    }

    private static boolean isValidSha(String sha)
    {
        return sha != null && SHA_PATTERN.matcher(sha).matches();
    }

    private String readDiffFromCompare(Response response) throws IOException
    {
        String body = readBodyCapped(response);
        if (body.isBlank())
        {
            return "";
        }
        try
        {
            JSONObject json = JSON.parseObject(body);
            if (json == null)
            {
                return body;
            }
            JSONArray diffs = json.getJSONArray("diffs");
            if (diffs == null || diffs.isEmpty())
            {
                return "";
            }
            StringBuilder builder = new StringBuilder();
            for (int index = 0; index < diffs.size(); index++)
            {
                JSONObject item = diffs.getJSONObject(index);
                if (item == null)
                {
                    continue;
                }
                String diff = item.getString("diff");
                if (diff == null || diff.isBlank())
                {
                    continue;
                }
                if (builder.length() > 0)
                {
                    builder.append('\n');
                }
                builder.append(diff);
            }
            return builder.toString();
        }
        catch (RuntimeException ex)
        {
            return body;
        }
    }

    private static String readBodyCapped(Response response) throws IOException
    {
        return HttpResponseBodies.readCapped(response, MAX_DIFF_BYTES);
    }
}
