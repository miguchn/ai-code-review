package com.acr.review.git.gitlab;

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

/** 通过 GitLab REST API 拉取 MR 描述、提交者、增删行与提交说明。 */
@Component
public class GitLabPullRequestMetadataFetcher implements GitPullRequestMetadataFetcher
{
    private final OkHttpClient client;

    public GitLabPullRequestMetadataFetcher(
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
            return GitPullRequestMetadata.unavailable("GitLab 凭据不可用");
        }
        if (prNumber <= 0)
        {
            return GitPullRequestMetadata.unavailable("MR 编号无效");
        }

        PullSummary pullSummary = fetchMergeRequestSummary(repository, access, token, prNumber);
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

    private PullSummary fetchMergeRequestSummary(GitRepositoryCoordinates repository, GitAccessContext access,
                                                 String token, int iid)
    {
        HttpUrl url = mergeRequestUrl(repository, access, iid);
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
                    return PullSummary.fail("MR 描述响应不是合法 JSON");
                }
                if (json == null)
                {
                    return PullSummary.ok("", null, null, null, null);
                }
                String description = json.getString("description");
                JSONObject author = json.getJSONObject("author");
                String prAuthor = author == null ? null : author.getString("username");
                Integer changesCount = json.getInteger("changes_count");
                JSONObject stats = json.getJSONObject("stats");
                Integer additions = stats == null ? null : stats.getInteger("additions");
                Integer deletions = stats == null ? null : stats.getInteger("deletions");
                return PullSummary.ok(
                    description == null ? "" : description,
                    prAuthor,
                    additions,
                    deletions,
                    changesCount);
            }
            return PullSummary.fail(failureMessage("MR 元数据", status));
        }
        catch (InterruptedIOException ex)
        {
            Thread.currentThread().interrupt();
            return PullSummary.fail("获取 MR 元数据超时");
        }
        catch (IOException ex)
        {
            return PullSummary.fail("无法连接 GitLab 获取 MR 元数据，请检查网络");
        }
    }

    private FetchOutcome fetchCommitMessages(GitRepositoryCoordinates repository, GitAccessContext access,
                                             String token, int iid)
    {
        HttpUrl url = mergeRequestUrl(repository, access, iid).newBuilder()
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
                    return FetchOutcome.fail("MR 提交说明响应不是合法 JSON");
                }
                return FetchOutcome.ok(GitLabPullRequestCommitMessagesFormatter.format(commits));
            }
            return FetchOutcome.fail(failureMessage("MR 提交说明", status));
        }
        catch (InterruptedIOException ex)
        {
            Thread.currentThread().interrupt();
            return FetchOutcome.fail("获取 MR 提交说明超时");
        }
        catch (IOException ex)
        {
            return FetchOutcome.fail("无法连接 GitLab 获取 MR 提交说明，请检查网络");
        }
    }

    private HttpUrl mergeRequestUrl(GitRepositoryCoordinates repository, GitAccessContext access, int iid)
    {
        return GitLabProvider.projectUrl(access, repository.fullPath(), "merge_requests/" + iid);
    }

    private Request.Builder authorizedGet(String token, HttpUrl url)
    {
        return new Request.Builder()
            .url(url)
            .get()
            .header("PRIVATE-TOKEN", token)
            .header("User-Agent", "ai-code-review");
    }

    private String failureMessage(String resource, int status)
    {
        if (status == 429)
        {
            return "GitLab API 已限流，稍后重试，无法获取" + resource;
        }
        if (status == 401)
        {
            return "GitLab 凭据无效或已过期，无法获取" + resource;
        }
        if (status == 403)
        {
            return "当前 Token 权限不足，无法获取" + resource;
        }
        if (status == 404)
        {
            return "未找到对应 MR，无法获取" + resource;
        }
        return "获取" + resource + "失败，GitLab 返回状态：" + status;
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
