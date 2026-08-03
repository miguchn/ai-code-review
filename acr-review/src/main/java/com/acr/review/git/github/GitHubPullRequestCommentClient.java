package com.acr.review.git.github;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.acr.review.delivery.ReviewDeliveryConstants;
import com.acr.review.git.GitAccessContext;
import com.acr.review.git.GitPullRequestComment;
import com.acr.review.git.GitPullRequestCommentClient;
import com.acr.review.git.GitPullRequestCommentException;
import com.acr.review.git.GitRepositoryCoordinates;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/** GitHub Issue/PR 总结评论适配（list / create / update）。 */
@Component
public class GitHubPullRequestCommentClient implements GitPullRequestCommentClient
{
    private static final MediaType JSON_MEDIA = MediaType.parse("application/json; charset=utf-8");
    private static final Pattern TOKEN_PATTERN = Pattern.compile(
        "(?i)(ghp_|gho_|ghu_|ghs_|ghr_|github_pat_)[A-Za-z0-9_]{10,}");

    private final HttpUrl apiBaseUrl;
    private final OkHttpClient client;

    public GitHubPullRequestCommentClient(
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
        return ReviewDeliveryConstants.PROVIDER_GITHUB;
    }

    @Override
    public Optional<GitPullRequestComment> findCommentWithMarker(GitRepositoryCoordinates repository,
                                                                 GitAccessContext access,
                                                                 int prNumber,
                                                                 String marker)
    {
        String token = access.requireToken();
        validate(repository, token, prNumber);
        if (marker == null || marker.isBlank())
        {
            return Optional.empty();
        }
        for (int page = 1; page <= ReviewDeliveryConstants.COMMENT_MAX_PAGES; page++)
        {
            HttpUrl url = issueCommentsUrl(repository, prNumber)
                .newBuilder()
                .addQueryParameter("per_page", String.valueOf(ReviewDeliveryConstants.COMMENT_PAGE_SIZE))
                .addQueryParameter("page", String.valueOf(page))
                .build();
            String body = execute(token, requestBuilder(token, url).get().build(), "列出 PR 评论");
            JSONArray array = parseArray(body, token);
            if (array.isEmpty())
            {
                return Optional.empty();
            }
            for (int i = 0; i < array.size(); i++)
            {
                JSONObject item = array.getJSONObject(i);
                if (item == null)
                {
                    continue;
                }
                String commentBody = item.getString("body");
                if (commentBody != null && commentBody.contains(marker))
                {
                    return Optional.of(new GitPullRequestComment(String.valueOf(item.get("id")), commentBody));
                }
            }
            if (array.size() < ReviewDeliveryConstants.COMMENT_PAGE_SIZE)
            {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    @Override
    public GitPullRequestComment createIssueComment(GitRepositoryCoordinates repository,
                                                    GitAccessContext access,
                                                    int prNumber,
                                                    String body)
    {
        String token = access.requireToken();
        validate(repository, token, prNumber);
        HttpUrl url = issueCommentsUrl(repository, prNumber);
        Request request = requestBuilder(token, url)
            .post(jsonBody(body))
            .build();
        String responseBody = execute(token, request, "创建 PR 评论");
        return parseComment(responseBody, token);
    }

    @Override
    public GitPullRequestComment updateIssueComment(GitRepositoryCoordinates repository,
                                                    GitAccessContext access,
                                                    String commentId,
                                                    String body)
    {
        String token = access.requireToken();
        validate(repository, token, 1);
        if (commentId == null || commentId.isBlank())
        {
            throw new GitPullRequestCommentException("评论 ID 无效，无法更新");
        }
        HttpUrl url = apiBaseUrl.newBuilder()
            .addPathSegment("repos")
            .addPathSegment(repository.owner())
            .addPathSegment(repository.repository())
            .addPathSegment("issues")
            .addPathSegment("comments")
            .addPathSegment(commentId)
            .build();
        Request request = requestBuilder(token, url)
            .patch(jsonBody(body))
            .build();
        String responseBody = execute(token, request, "更新 PR 评论");
        return parseComment(responseBody, token);
    }

    private HttpUrl issueCommentsUrl(GitRepositoryCoordinates repository, int prNumber)
    {
        return apiBaseUrl.newBuilder()
            .addPathSegment("repos")
            .addPathSegment(repository.owner())
            .addPathSegment(repository.repository())
            .addPathSegment("issues")
            .addPathSegment(String.valueOf(prNumber))
            .addPathSegment("comments")
            .build();
    }

    private Request.Builder requestBuilder(String token, HttpUrl url)
    {
        return new Request.Builder()
            .url(url)
            .header("Authorization", "Bearer " + token)
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "ai-code-review");
    }

    private RequestBody jsonBody(String body)
    {
        JSONObject payload = new JSONObject();
        payload.put("body", body == null ? "" : body);
        return RequestBody.create(payload.toJSONString(), JSON_MEDIA);
    }

    private String execute(String token, Request request, String action)
    {
        try (Response response = client.newCall(request).execute())
        {
            int status = response.code();
            String body = response.body() == null ? "" : response.body().string();
            if (status >= 200 && status < 300)
            {
                return body;
            }
            if (status == 401)
            {
                throw new GitPullRequestCommentException("GitHub 凭据无效或已过期，无法" + action);
            }
            if (isRateLimited(response))
            {
                throw new GitPullRequestCommentException("GitHub API 限流，请稍后重试（" + action + "）");
            }
            if (status == 403)
            {
                throw new GitPullRequestCommentException("当前 Token 权限不足，无法" + action);
            }
            if (status == 404)
            {
                throw new GitPullRequestCommentException("未找到 PR 或评论，无法" + action);
            }
            throw new GitPullRequestCommentException(
                sanitize(action + "失败，GitHub 返回状态：" + status, token));
        }
        catch (InterruptedIOException ex)
        {
            Thread.currentThread().interrupt();
            throw new GitPullRequestCommentException(action + "超时", ex);
        }
        catch (IOException ex)
        {
            throw new GitPullRequestCommentException(
                sanitize("无法连接 GitHub：" + ex.getMessage(), token), ex);
        }
    }

    private static boolean isRateLimited(Response response)
    {
        if (response.code() == 429)
        {
            return true;
        }
        return response.code() == 403 && "0".equals(response.header("X-RateLimit-Remaining"));
    }

    private static void validate(GitRepositoryCoordinates repository, String token, int prNumber)
    {
        if (repository == null || repository.owner() == null || repository.repository() == null)
        {
            throw new GitPullRequestCommentException("仓库信息不完整，无法投递评论");
        }
        if (token == null || token.isBlank())
        {
            throw new GitPullRequestCommentException("GitHub 凭据不可用，无法投递评论");
        }
        if (prNumber <= 0)
        {
            throw new GitPullRequestCommentException("PR 编号无效，无法投递评论");
        }
    }

    private static JSONArray parseArray(String body, String token)
    {
        try
        {
            Object parsed = JSON.parse(body);
            if (parsed instanceof JSONArray array)
            {
                return array;
            }
            throw new GitPullRequestCommentException("列出 PR 评论响应格式异常");
        }
        catch (GitPullRequestCommentException ex)
        {
            throw ex;
        }
        catch (Exception ex)
        {
            throw new GitPullRequestCommentException(sanitize("列出 PR 评论响应解析失败", token), ex);
        }
    }

    private static GitPullRequestComment parseComment(String body, String token)
    {
        try
        {
            JSONObject json = JSON.parseObject(body);
            if (json == null || json.get("id") == null)
            {
                throw new GitPullRequestCommentException("评论响应缺少 id");
            }
            return new GitPullRequestComment(String.valueOf(json.get("id")), json.getString("body"));
        }
        catch (GitPullRequestCommentException ex)
        {
            throw ex;
        }
        catch (Exception ex)
        {
            throw new GitPullRequestCommentException(sanitize("评论响应解析失败", token), ex);
        }
    }

    public static String sanitize(String message, String token)
    {
        if (message == null)
        {
            return "GitHub 评论操作失败";
        }
        String sanitized = message;
        if (token != null && !token.isBlank())
        {
            sanitized = sanitized.replace(token, "***");
        }
        sanitized = TOKEN_PATTERN.matcher(sanitized).replaceAll("***");
        return sanitized.length() > ReviewDeliveryConstants.MAX_FAILURE_MESSAGE_CHARS
            ? sanitized.substring(0, ReviewDeliveryConstants.MAX_FAILURE_MESSAGE_CHARS)
            : sanitized;
    }
}
