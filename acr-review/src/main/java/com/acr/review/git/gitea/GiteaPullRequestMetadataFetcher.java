package com.acr.review.git.gitea;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.acr.review.git.GitAccessContext;
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

/** 通过 Gitea REST API 拉取 PR 描述、提交者、增删行与提交说明。 */
@Component
public class GiteaPullRequestMetadataFetcher implements GitPullRequestMetadataFetcher
{
    private final OkHttpClient client;

    public GiteaPullRequestMetadataFetcher(
        @Value("${review.gitea.connect-timeout-ms:5000}") int connectTimeoutMs,
        @Value("${review.gitea.read-timeout-ms:30000}") int readTimeoutMs)
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
        return "GITEA";
    }

    @Override
    public GitPullRequestMetadata fetch(GitRepositoryCoordinates repository, GitAccessContext access, int prNumber)
    {
        if (repository == null)
        {
            return GitPullRequestMetadata.unavailable("仓库信息不完整");
        }
        String token;
        try
        {
            token = access.requireToken();
        }
        catch (IllegalArgumentException ex)
        {
            return GitPullRequestMetadata.unavailable("Gitea 凭据不可用");
        }
        if (prNumber <= 0)
        {
            return GitPullRequestMetadata.unavailable("PR 编号无效");
        }

        PullSummary pullSummary = fetchPullSummary(repository, access, token, prNumber);
        if (!pullSummary.success())
        {
            return GitPullRequestMetadata.unavailable(pullSummary.message());
        }

        FetchOutcome commitsOutcome = fetchCommitMessages(repository, access, token, prNumber);
        String commitMessages = commitsOutcome.success() ? commitsOutcome.content() : "";

        return GitPullRequestMetadata.ok(
            pullSummary.description(),
            commitMessages,
            pullSummary.prAuthor(),
            pullSummary.additions(),
            pullSummary.deletions(),
            pullSummary.changedFiles());
    }

    private PullSummary fetchPullSummary(GitRepositoryCoordinates repository, GitAccessContext access,
                                         String token, int prNumber)
    {
        HttpUrl url = pullRequestUrl(access, repository, prNumber);
        Request request = authorizedGet(token, url).build();

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
                    return PullSummary.fail("PR 描述响应不是合法 JSON");
                }
                if (json == null)
                {
                    return PullSummary.ok("", null, null, null, null);
                }
                String description = json.getString("body");
                JSONObject user = json.getJSONObject("user");
                String prAuthor = user == null ? null : user.getString("login");
                return PullSummary.ok(
                    description == null ? "" : description,
                    prAuthor,
                    json.getInteger("additions"),
                    json.getInteger("deletions"),
                    json.getInteger("changed_files"));
            }
            return PullSummary.fail(failureMessage("PR 元数据", status));
        }
        catch (InterruptedIOException ex)
        {
            Thread.currentThread().interrupt();
            return PullSummary.fail("获取 PR 元数据超时");
        }
        catch (IOException ex)
        {
            return PullSummary.fail("无法连接 Gitea 获取 PR 元数据，请检查网络");
        }
    }

    private FetchOutcome fetchCommitMessages(GitRepositoryCoordinates repository, GitAccessContext access,
                                             String token, int prNumber)
    {
        HttpUrl url = pullRequestUrl(access, repository, prNumber).newBuilder()
            .addPathSegment("commits")
            .build();
        Request request = authorizedGet(token, url).build();

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
                return FetchOutcome.ok(GiteaPullRequestCommitMessagesFormatter.format(commits));
            }
            return FetchOutcome.fail(failureMessage("PR 提交说明", status));
        }
        catch (InterruptedIOException ex)
        {
            Thread.currentThread().interrupt();
            return FetchOutcome.fail("获取 PR 提交说明超时");
        }
        catch (IOException ex)
        {
            return FetchOutcome.fail("无法连接 Gitea 获取 PR 提交说明，请检查网络");
        }
    }

    private HttpUrl pullRequestUrl(GitAccessContext access, GitRepositoryCoordinates repository, int prNumber)
    {
        return GiteaApiSupport.reposBuilder(GiteaApiSupport.apiBaseUrl(access), repository)
            .addPathSegment("pulls")
            .addPathSegment(String.valueOf(prNumber))
            .build();
    }

    private Request.Builder authorizedGet(String token, HttpUrl url)
    {
        return new Request.Builder()
            .url(url)
            .get()
            .header("Authorization", GiteaApiSupport.authorizationHeader(token))
            .header("User-Agent", "ai-code-review");
    }

    private String failureMessage(String resource, int status)
    {
        if (status == 429)
        {
            return "Gitea API 已限流，稍后重试，无法获取" + resource;
        }
        if (status == 401)
        {
            return "Gitea 凭据无效或已过期，无法获取" + resource;
        }
        if (status == 403)
        {
            return "当前 Token 权限不足，无法获取" + resource;
        }
        if (status == 404)
        {
            return "未找到对应 PR，无法获取" + resource;
        }
        return "获取" + resource + "失败，Gitea 返回状态：" + status;
    }

    private record PullSummary(boolean success, String description, String prAuthor,
                               Integer additions, Integer deletions, Integer changedFiles, String message)
    {
        static PullSummary ok(String description, String prAuthor, Integer additions, Integer deletions,
                              Integer changedFiles)
        {
            return new PullSummary(true, description, prAuthor, additions, deletions, changedFiles, null);
        }

        static PullSummary fail(String message)
        {
            return new PullSummary(false, "", null, null, null, null, message);
        }
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
