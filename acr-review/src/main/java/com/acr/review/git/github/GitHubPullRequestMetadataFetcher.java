package com.acr.review.git.github;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.acr.review.git.GitPullRequestMetadata;
import com.acr.review.git.GitPullRequestMetadataFetcher;
import com.acr.review.git.GitRepositoryCoordinates;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/** 通过 GitHub REST API 拉取 PR 描述与提交说明。 */
@Component
public class GitHubPullRequestMetadataFetcher implements GitPullRequestMetadataFetcher
{
    private final HttpUrl apiBaseUrl;
    private final OkHttpClient client;

    public GitHubPullRequestMetadataFetcher(
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
    public GitPullRequestMetadata fetch(GitRepositoryCoordinates repository, String token, int prNumber)
    {
        if (repository == null)
        {
            return GitPullRequestMetadata.unavailable("仓库信息不完整");
        }
        if (token == null || token.isBlank())
        {
            return GitPullRequestMetadata.unavailable("GitHub 凭据不可用");
        }
        if (prNumber <= 0)
        {
            return GitPullRequestMetadata.unavailable("PR 编号无效");
        }

        FetchOutcome descriptionOutcome = fetchPullDescription(repository, token, prNumber);
        if (!descriptionOutcome.success())
        {
            return GitPullRequestMetadata.unavailable(descriptionOutcome.message());
        }

        FetchOutcome commitsOutcome = fetchCommitMessages(repository, token, prNumber);
        if (!commitsOutcome.success())
        {
            return GitPullRequestMetadata.unavailable(commitsOutcome.message());
        }

        return GitPullRequestMetadata.ok(descriptionOutcome.content(), commitsOutcome.content());
    }

    private FetchOutcome fetchPullDescription(GitRepositoryCoordinates repository, String token, int prNumber)
    {
        HttpUrl url = pullRequestUrl(repository, prNumber);
        Request request = authorizedGet(token, url)
            .header("Accept", "application/vnd.github+json")
            .build();

        try (Response response = client.newCall(request).execute())
        {
            int status = response.code();
            String body = response.body() == null ? "" : response.body().string();
            if (status == 200)
            {
                JSONObject json;
                try
                {
                    json = JSON.parseObject(body);
                }
                catch (RuntimeException ex)
                {
                    return FetchOutcome.fail("PR 描述响应不是合法 JSON");
                }
                String description = json == null ? "" : json.getString("body");
                return FetchOutcome.ok(description == null ? "" : description);
            }
            return FetchOutcome.fail(failureMessage("PR 描述", status, response.header("X-RateLimit-Remaining")));
        }
        catch (InterruptedIOException ex)
        {
            Thread.currentThread().interrupt();
            return FetchOutcome.fail("获取 PR 描述超时");
        }
        catch (IOException ex)
        {
            return FetchOutcome.fail("无法连接 GitHub 获取 PR 描述，请检查网络");
        }
    }

    private FetchOutcome fetchCommitMessages(GitRepositoryCoordinates repository, String token, int prNumber)
    {
        HttpUrl url = pullRequestUrl(repository, prNumber).newBuilder()
            .addPathSegment("commits")
            .build();
        Request request = authorizedGet(token, url)
            .header("Accept", "application/vnd.github+json")
            .build();

        try (Response response = client.newCall(request).execute())
        {
            int status = response.code();
            String body = response.body() == null ? "" : response.body().string();
            if (status == 200)
            {
                JSONArray commits;
                try
                {
                    commits = JSON.parseArray(body);
                }
                catch (RuntimeException ex)
                {
                    return FetchOutcome.fail("PR 提交说明响应不是合法 JSON");
                }
                return FetchOutcome.ok(GitHubPullRequestCommitMessagesFormatter.format(commits));
            }
            return FetchOutcome.fail(failureMessage("PR 提交说明", status, response.header("X-RateLimit-Remaining")));
        }
        catch (InterruptedIOException ex)
        {
            Thread.currentThread().interrupt();
            return FetchOutcome.fail("获取 PR 提交说明超时");
        }
        catch (IOException ex)
        {
            return FetchOutcome.fail("无法连接 GitHub 获取 PR 提交说明，请检查网络");
        }
    }

    private HttpUrl pullRequestUrl(GitRepositoryCoordinates repository, int prNumber)
    {
        return apiBaseUrl.newBuilder()
            .addPathSegment("repos")
            .addPathSegment(repository.owner())
            .addPathSegment(repository.repository())
            .addPathSegment("pulls")
            .addPathSegment(String.valueOf(prNumber))
            .build();
    }

    private Request.Builder authorizedGet(String token, HttpUrl url)
    {
        return new Request.Builder()
            .url(url)
            .get()
            .header("Authorization", "Bearer " + token)
            .header("User-Agent", "ai-code-review");
    }

    private String failureMessage(String resource, int status, String rateLimitRemaining)
    {
        if (status == 429 || (status == 403 && "0".equals(rateLimitRemaining)))
        {
            return "GitHub API 已限流，稍后重试，无法获取" + resource;
        }
        if (status == 401)
        {
            return "GitHub 凭据无效或已过期，无法获取" + resource;
        }
        if (status == 403)
        {
            return "当前 Token 权限不足，无法获取" + resource;
        }
        if (status == 404)
        {
            return "未找到对应 PR，无法获取" + resource;
        }
        return "获取" + resource + "失败，GitHub 返回状态：" + status;
    }

    private record FetchOutcome(boolean success, String content, String message)
    {
        static FetchOutcome ok(String content)
        {
            return new FetchOutcome(true, content, null);
        }

        static FetchOutcome fail(String message)
        {
            return new FetchOutcome(false, "", message);
        }
    }
}
