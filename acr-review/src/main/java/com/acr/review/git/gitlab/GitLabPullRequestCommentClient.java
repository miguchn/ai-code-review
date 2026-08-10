package com.acr.review.git.gitlab;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.acr.review.delivery.ReviewDeliveryConstants;
import com.acr.review.git.GitAccessContext;
import com.acr.review.git.GitInlineCommentRequest;
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

/** GitLab MR Notes API 适配（list / create / update）。 */
@Component
public class GitLabPullRequestCommentClient implements GitPullRequestCommentClient
{
    private static final MediaType JSON_MEDIA = MediaType.parse("application/json; charset=utf-8");
    private static final Pattern TOKEN_PATTERN = Pattern.compile("glpat-[A-Za-z0-9_-]{10,}");

    private final OkHttpClient client;

    public GitLabPullRequestCommentClient(
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
            HttpUrl url = notesUrl(repository, access, prNumber)
                .newBuilder()
                .addQueryParameter("per_page", String.valueOf(ReviewDeliveryConstants.COMMENT_PAGE_SIZE))
                .addQueryParameter("page", String.valueOf(page))
                .build();
            String body = execute(token, requestBuilder(token, url).get().build(), "列出 MR 评论");
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
                String noteBody = item.getString("body");
                if (noteBody != null && noteBody.contains(marker))
                {
                    return Optional.of(new GitPullRequestComment(compositeId(prNumber, item.get("id")), noteBody));
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
        HttpUrl url = notesUrl(repository, access, prNumber);
        Request request = requestBuilder(token, url)
            .post(jsonBody(body))
            .build();
        String responseBody = execute(token, request, "创建 MR 评论");
        return parseComment(responseBody, token, prNumber);
    }

    @Override
    public boolean supportsInlineComments()
    {
        return true;
    }

    @Override
    public GitPullRequestComment createInlineComment(GitRepositoryCoordinates repository,
                                                     GitAccessContext access,
                                                     int prNumber,
                                                     GitInlineCommentRequest request)
    {
        String token = access.requireToken();
        validate(repository, token, prNumber);
        validateInlineRequest(request);
        DiffRefs diffRefs = fetchDiffRefs(repository, access, token, prNumber, request.headSha());
        JSONObject position = new JSONObject();
        position.put("base_sha", diffRefs.baseSha());
        position.put("start_sha", diffRefs.startSha());
        position.put("head_sha", diffRefs.headSha());
        position.put("position_type", "text");
        position.put("new_path", request.path());
        position.put("new_line", resolveLine(request));
        JSONObject payload = new JSONObject();
        payload.put("body", request.body() == null ? "" : request.body());
        payload.put("position", position);
        HttpUrl url = GitLabProvider.projectUrl(access, repository.fullPath(),
            "merge_requests/" + prNumber + "/discussions");
        Request httpRequest = requestBuilder(token, url)
            .post(RequestBody.create(payload.toJSONString(), JSON_MEDIA))
            .build();
        String responseBody = execute(token, httpRequest, "创建 MR 行内评论");
        return parseDiscussionComment(responseBody, token, prNumber, request.body());
    }

    @Override
    public Optional<GitPullRequestComment> findInlineCommentWithMarker(GitRepositoryCoordinates repository,
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
            HttpUrl url = GitLabProvider.projectUrl(access, repository.fullPath(),
                    "merge_requests/" + prNumber + "/discussions")
                .newBuilder()
                .addQueryParameter("per_page", String.valueOf(ReviewDeliveryConstants.COMMENT_PAGE_SIZE))
                .addQueryParameter("page", String.valueOf(page))
                .build();
            String body = execute(token, requestBuilder(token, url).get().build(), "列出 MR 讨论");
            JSONArray array = parseArray(body, token);
            if (array.isEmpty())
            {
                return Optional.empty();
            }
            for (int i = 0; i < array.size(); i++)
            {
                JSONObject discussion = array.getJSONObject(i);
                if (discussion == null)
                {
                    continue;
                }
                JSONArray notes = discussion.getJSONArray("notes");
                if (notes == null)
                {
                    continue;
                }
                for (int j = 0; j < notes.size(); j++)
                {
                    JSONObject note = notes.getJSONObject(j);
                    if (note == null)
                    {
                        continue;
                    }
                    String noteBody = note.getString("body");
                    if (noteBody != null && noteBody.contains(marker))
                    {
                        return Optional.of(new GitPullRequestComment(
                            compositeId(prNumber, note.get("id")), noteBody));
                    }
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
        ParsedCommentId parsed = parseCommentId(commentId);
        HttpUrl url = GitLabProvider.projectUrl(access, repository.fullPath(),
                "merge_requests/" + parsed.iid() + "/notes/" + parsed.noteId());
        Request request = requestBuilder(token, url)
            .put(jsonBody(body))
            .build();
        String responseBody = execute(token, request, "更新 MR 评论");
        return parseComment(responseBody, token, parsed.iid());
    }

    HttpUrl notesUrl(GitRepositoryCoordinates repository, GitAccessContext access, int iid)
    {
        return GitLabProvider.projectUrl(access, repository.fullPath(), "merge_requests/" + iid + "/notes");
    }

    private Request.Builder requestBuilder(String token, HttpUrl url)
    {
        return new Request.Builder()
            .url(url)
            .header("PRIVATE-TOKEN", token)
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
                throw new GitPullRequestCommentException("GitLab 凭据无效或已过期，无法" + action);
            }
            if (status == 429)
            {
                throw new GitPullRequestCommentException("GitLab API 限流，请稍后重试（" + action + "）");
            }
            if (status == 403)
            {
                throw new GitPullRequestCommentException("当前 Token 权限不足，无法" + action);
            }
            if (status == 404)
            {
                throw new GitPullRequestCommentException("未找到 MR 或评论，无法" + action);
            }
            throw new GitPullRequestCommentException(
                sanitize(action + "失败，GitLab 返回状态：" + status, token));
        }
        catch (InterruptedIOException ex)
        {
            Thread.currentThread().interrupt();
            throw new GitPullRequestCommentException(action + "超时", ex);
        }
        catch (IOException ex)
        {
            throw new GitPullRequestCommentException(
                sanitize("无法连接 GitLab：" + ex.getMessage(), token), ex);
        }
    }

    private DiffRefs fetchDiffRefs(GitRepositoryCoordinates repository,
                                   GitAccessContext access,
                                   String token,
                                   int prNumber,
                                   String fallbackHeadSha)
    {
        HttpUrl url = GitLabProvider.projectUrl(access, repository.fullPath(), "merge_requests/" + prNumber);
        String body = execute(token, requestBuilder(token, url).get().build(), "读取 MR diff_refs");
        try
        {
            JSONObject json = JSON.parseObject(body);
            JSONObject diffRefs = json == null ? null : json.getJSONObject("diff_refs");
            if (diffRefs == null)
            {
                throw new GitPullRequestCommentException("MR 缺少 diff_refs，无法创建行内评论");
            }
            String baseSha = diffRefs.getString("base_sha");
            String startSha = diffRefs.getString("start_sha");
            String headSha = diffRefs.getString("head_sha");
            if (headSha == null || headSha.isBlank())
            {
                headSha = fallbackHeadSha;
            }
            if (baseSha == null || baseSha.isBlank() || startSha == null || startSha.isBlank()
                || headSha == null || headSha.isBlank())
            {
                throw new GitPullRequestCommentException("MR diff_refs 不完整，无法创建行内评论");
            }
            return new DiffRefs(baseSha, startSha, headSha);
        }
        catch (GitPullRequestCommentException ex)
        {
            throw ex;
        }
        catch (Exception ex)
        {
            throw new GitPullRequestCommentException(sanitize("读取 MR diff_refs 响应解析失败", token), ex);
        }
    }

    private static GitPullRequestComment parseDiscussionComment(String body,
                                                                String token,
                                                                int iid,
                                                                String fallbackBody)
    {
        try
        {
            JSONObject json = JSON.parseObject(body);
            if (json == null)
            {
                throw new GitPullRequestCommentException("行内评论响应格式异常");
            }
            JSONArray notes = json.getJSONArray("notes");
            if (notes != null && !notes.isEmpty())
            {
                JSONObject note = notes.getJSONObject(0);
                if (note != null && note.get("id") != null)
                {
                    return new GitPullRequestComment(
                        compositeId(iid, note.get("id")),
                        note.getString("body") == null ? fallbackBody : note.getString("body"));
                }
            }
            if (json.get("id") != null)
            {
                return new GitPullRequestComment(
                    compositeId(iid, json.get("id")),
                    json.getString("body") == null ? fallbackBody : json.getString("body"));
            }
            throw new GitPullRequestCommentException("行内评论响应缺少 id");
        }
        catch (GitPullRequestCommentException ex)
        {
            throw ex;
        }
        catch (Exception ex)
        {
            throw new GitPullRequestCommentException(sanitize("行内评论响应解析失败", token), ex);
        }
    }

    private static void validateInlineRequest(GitInlineCommentRequest request)
    {
        if (request == null || request.path() == null || request.path().isBlank())
        {
            throw new GitPullRequestCommentException("行内评论缺少文件路径");
        }
        if (request.endLine() == null && request.startLine() == null)
        {
            throw new GitPullRequestCommentException("行内评论缺少行号");
        }
    }

    private static int resolveLine(GitInlineCommentRequest request)
    {
        if (request.endLine() != null)
        {
            return request.endLine();
        }
        return request.startLine();
    }

    private record DiffRefs(String baseSha, String startSha, String headSha)
    {
    }

    private static void validate(GitRepositoryCoordinates repository, String token, int prNumber)
    {
        if (repository == null || repository.fullPath() == null || repository.fullPath().isBlank())
        {
            throw new GitPullRequestCommentException("仓库信息不完整，无法投递评论");
        }
        if (token == null || token.isBlank())
        {
            throw new GitPullRequestCommentException("GitLab 凭据不可用，无法投递评论");
        }
        if (prNumber <= 0)
        {
            throw new GitPullRequestCommentException("MR 编号无效，无法投递评论");
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
            throw new GitPullRequestCommentException("列出 MR 评论响应格式异常");
        }
        catch (GitPullRequestCommentException ex)
        {
            throw ex;
        }
        catch (Exception ex)
        {
            throw new GitPullRequestCommentException(sanitize("列出 MR 评论响应解析失败", token), ex);
        }
    }

    private static GitPullRequestComment parseComment(String body, String token, int iid)
    {
        try
        {
            JSONObject json = JSON.parseObject(body);
            if (json == null || json.get("id") == null)
            {
                throw new GitPullRequestCommentException("评论响应缺少 id");
            }
            return new GitPullRequestComment(compositeId(iid, json.get("id")), json.getString("body"));
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

    private static String compositeId(int iid, Object noteId)
    {
        return iid + ":" + noteId;
    }

    private static ParsedCommentId parseCommentId(String commentId)
    {
        int colon = commentId.indexOf(':');
        if (colon <= 0 || colon >= commentId.length() - 1)
        {
            throw new GitPullRequestCommentException("评论 ID 无效，无法更新");
        }
        try
        {
            int iid = Integer.parseInt(commentId.substring(0, colon));
            String noteId = commentId.substring(colon + 1);
            if (iid <= 0 || noteId.isBlank())
            {
                throw new GitPullRequestCommentException("评论 ID 无效，无法更新");
            }
            return new ParsedCommentId(iid, noteId);
        }
        catch (NumberFormatException ex)
        {
            throw new GitPullRequestCommentException("评论 ID 无效，无法更新");
        }
    }

    private record ParsedCommentId(int iid, String noteId)
    {
    }

    public static String sanitize(String message, String token)
    {
        if (message == null)
        {
            return "GitLab 评论操作失败";
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
