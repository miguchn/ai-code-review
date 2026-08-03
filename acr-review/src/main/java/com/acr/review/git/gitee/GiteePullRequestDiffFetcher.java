package com.acr.review.git.gitee;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.acr.review.domain.ReviewPipelineConstants;
import com.acr.review.git.GitAccessContext;
import com.acr.review.git.GitProviderCodes;
import com.acr.review.git.GitPullRequestDiffFetcher;
import com.acr.review.git.GitPullRequestDiffResult;
import com.acr.review.git.GitRepositoryCoordinates;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/** 通过 Gitee Compare API 拉取 PR base...head Diff。 */
@Component
public class GiteePullRequestDiffFetcher implements GitPullRequestDiffFetcher
{
    private static final long MAX_DIFF_BYTES = ReviewPipelineConstants.MAX_DIFF_CHARS * 2L;
    private static final java.util.regex.Pattern SHA_PATTERN = java.util.regex.Pattern.compile("^[0-9a-fA-F]{4,64}$");

    private final HttpUrl apiBaseUrl;
    private final OkHttpClient client;

    public GiteePullRequestDiffFetcher(
        @Value("${review.gitee.server-url:https://gitee.com}") String serverUrl,
        @Value("${review.gitee.connect-timeout-ms:5000}") int connectTimeoutMs,
        @Value("${review.gitee.read-timeout-ms:30000}") int readTimeoutMs)
    {
        this(GiteeApiUrls.apiBaseFromServer(serverUrl), connectTimeoutMs, readTimeoutMs);
    }

    GiteePullRequestDiffFetcher(HttpUrl apiBaseUrl, int connectTimeoutMs, int readTimeoutMs)
    {
        this.apiBaseUrl = apiBaseUrl;
        this.client = new OkHttpClient.Builder()
            .connectTimeout(connectTimeoutMs, TimeUnit.MILLISECONDS)
            .readTimeout(readTimeoutMs, TimeUnit.MILLISECONDS)
            .callTimeout(readTimeoutMs, TimeUnit.MILLISECONDS)
            .build();
    }

    @Override
    public String providerCode()
    {
        return GitProviderCodes.GITEE;
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
            return GitPullRequestDiffResult.fail(ReviewPipelineConstants.FAILURE_CREDENTIAL_ERROR, "Gitee 凭据不可用");
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
            .addEncodedPathSegment(baseSha + "..." + headSha)
            .addQueryParameter("access_token", token)
            .build();
        Request request = new Request.Builder()
            .url(url)
            .get()
            .header("User-Agent", "ai-code-review")
            .build();

        try (Response response = client.newCall(request).execute())
        {
            int status = response.code();
            if (status == 200)
            {
                return GitPullRequestDiffResult.ok(extractDiff(readBodyCapped(response)));
            }
            if (status == 401)
            {
                return GitPullRequestDiffResult.fail(ReviewPipelineConstants.FAILURE_CREDENTIAL_ERROR, "Gitee 凭据无效或已过期");
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
                "获取 PR Diff 失败，Gitee 返回状态：" + status);
        }
        catch (InterruptedIOException ex)
        {
            Thread.currentThread().interrupt();
            return GitPullRequestDiffResult.fail(ReviewPipelineConstants.FAILURE_TIMEOUT, "获取 PR Diff 超时");
        }
        catch (IOException ex)
        {
            return GitPullRequestDiffResult.fail(ReviewPipelineConstants.FAILURE_WORKSPACE_PREPARE,
                "无法连接 Gitee 获取 Diff，请检查网络");
        }
    }

    static String extractDiff(String body)
    {
        if (body == null || body.isBlank())
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
            JSONArray files = json.getJSONArray("files");
            if (files == null || files.isEmpty())
            {
                return "";
            }
            StringBuilder diff = new StringBuilder();
            for (int index = 0; index < files.size(); index++)
            {
                JSONObject file = files.getJSONObject(index);
                if (file == null)
                {
                    continue;
                }
                String patch = file.getString("patch");
                if (patch != null && !patch.isBlank())
                {
                    if (diff.length() > 0)
                    {
                        diff.append('\n');
                    }
                    diff.append(patch);
                }
            }
            return diff.toString();
        }
        catch (RuntimeException ex)
        {
            return body;
        }
    }

    private static boolean isValidSha(String sha)
    {
        return sha != null && SHA_PATTERN.matcher(sha).matches();
    }

    private static String readBodyCapped(Response response) throws IOException
    {
        okhttp3.ResponseBody body = response.body();
        if (body == null)
        {
            return "";
        }
        try (okio.BufferedSource source = body.source())
        {
            okio.Buffer buffer = new okio.Buffer();
            source.read(buffer, MAX_DIFF_BYTES);
            return buffer.readString(java.nio.charset.StandardCharsets.UTF_8);
        }
    }
}
