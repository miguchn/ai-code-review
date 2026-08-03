package com.acr.review.git.gitlab;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.acr.review.git.GitAccessContext;
import com.acr.review.git.GitConnectionFailure;
import com.acr.review.git.GitConnectionResult;
import com.acr.review.git.GitProvider;
import com.acr.review.git.GitRepositoryCoordinates;
import com.acr.review.git.GitRepositoryInfoResult;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/** GitLab REST API 适配（自建实例 serverUrl + /api/v4）。 */
@Component
public class GitLabProvider implements GitProvider
{
    private static final Pattern SSH_URL = Pattern.compile("^git@([^:]+):(.+)$", Pattern.CASE_INSENSITIVE);
    private static final int BRANCH_PAGE_SIZE = 100;
    private static final int MAX_BRANCH_PAGES = 1000;

    private final OkHttpClient client;

    public GitLabProvider(
            @Value("${review.gitlab.connect-timeout-ms:5000}") int connectTimeoutMs,
            @Value("${review.gitlab.read-timeout-ms:10000}") int readTimeoutMs)
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
    public GitRepositoryCoordinates parseRepository(String repositoryUrl, GitAccessContext access)
    {
        if (repositoryUrl == null || repositoryUrl.isBlank())
        {
            throw new IllegalArgumentException("GitLab 仓库地址不能为空");
        }

        URI serverUri = toUri(access.serverUrl());
        String expectedHost = serverUri.getHost().toLowerCase(Locale.ROOT);
        int expectedPort = serverUri.getPort();

        String value = repositoryUrl.trim();
        String fullPath;

        Matcher sshMatcher = SSH_URL.matcher(value);
        if (sshMatcher.matches())
        {
            String host = sshMatcher.group(1).toLowerCase(Locale.ROOT);
            if (!host.equals(expectedHost))
            {
                throw invalidRepositoryUrl();
            }
            fullPath = stripGitSuffix(sshMatcher.group(2));
        }
        else
        {
            try
            {
                URI uri = new URI(value);
                if (!"https".equalsIgnoreCase(uri.getScheme()) && !"http".equalsIgnoreCase(uri.getScheme()))
                {
                    throw invalidRepositoryUrl();
                }
                if (!hostMatches(uri, expectedHost, expectedPort))
                {
                    throw invalidRepositoryUrl();
                }
                if (uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null)
                {
                    throw invalidRepositoryUrl();
                }
                fullPath = extractPathWithNamespace(uri.getPath());
            }
            catch (URISyntaxException e)
            {
                throw invalidRepositoryUrl();
            }
        }

        int lastSlash = fullPath.lastIndexOf('/');
        if (lastSlash <= 0 || lastSlash >= fullPath.length() - 1)
        {
            throw invalidRepositoryUrl();
        }

        String owner = fullPath.substring(0, lastSlash);
        String repository = fullPath.substring(lastSlash + 1);
        String canonicalUrl = access.serverUrl() + "/" + fullPath;
        return new GitRepositoryCoordinates(owner, repository, fullPath, canonicalUrl);
    }

    @Override
    public GitConnectionResult testCredential(GitAccessContext access)
    {
        ApiResponse response = getObject(access, apiBaseUrl(access).newBuilder().addPathSegment("user").build());
        if (!response.success())
        {
            return response.result();
        }
        String username = response.body().getString("username");
        String name = response.body().getString("name");
        String display = username != null && !username.isBlank() ? username
            : (name != null && !name.isBlank() ? name : null);
        String suffix = display == null ? "" : "，GitLab 用户：" + display;
        return GitConnectionResult.success("凭据有效" + suffix);
    }

    @Override
    public GitConnectionResult testRepository(GitRepositoryCoordinates repository, GitAccessContext access)
    {
        GitConnectionResult credentialResult = testCredential(access);
        if (!credentialResult.isSuccess())
        {
            return credentialResult;
        }

        ApiResponse response = getProject(access, repository.fullPath());
        if (!response.success())
        {
            return response.result();
        }
        String defaultBranch = response.body().getString("default_branch");
        String webUrl = response.body().getString("web_url");
        if (webUrl == null || webUrl.isBlank())
        {
            webUrl = repository.canonicalUrl();
        }
        return GitConnectionResult.success("连接成功，可访问 GitLab 仓库", defaultBranch, webUrl);
    }

    @Override
    public GitRepositoryInfoResult readRepository(GitRepositoryCoordinates repository, GitAccessContext access)
    {
        GitConnectionResult credentialResult = testCredential(access);
        if (!credentialResult.isSuccess())
        {
            return GitRepositoryInfoResult.failure(credentialResult.getFailure(), credentialResult.getMessage());
        }

        ApiResponse projectResponse = getProject(access, repository.fullPath());
        if (!projectResponse.success())
        {
            return GitRepositoryInfoResult.failure(
                projectResponse.result().getFailure(), projectResponse.result().getMessage());
        }

        String defaultBranch = projectResponse.body().getString("default_branch");
        String webUrl = projectResponse.body().getString("web_url");
        if (webUrl == null || webUrl.isBlank())
        {
            webUrl = repository.canonicalUrl();
        }

        Set<String> branchNames = new LinkedHashSet<>();
        for (int page = 1; page <= MAX_BRANCH_PAGES; page++)
        {
            ApiListResponse branchResponse = getArray(access,
                projectUrl(access, repository.fullPath(), "repository/branches"), page, BRANCH_PAGE_SIZE);
            if (!branchResponse.success())
            {
                return GitRepositoryInfoResult.failure(
                    branchResponse.result().getFailure(), branchResponse.result().getMessage());
            }
            for (Object value : branchResponse.body())
            {
                if (value instanceof JSONObject branch)
                {
                    String name = branch.getString("name");
                    if (name != null && !name.isBlank())
                    {
                        branchNames.add(name);
                    }
                }
            }
            if (branchResponse.body().size() < BRANCH_PAGE_SIZE)
            {
                return GitRepositoryInfoResult.success(repository, webUrl, defaultBranch, new ArrayList<>(branchNames));
            }
        }
        return GitRepositoryInfoResult.failure(GitConnectionFailure.API_ERROR, "GitLab 分支数量超过单次同步上限");
    }

    private ApiResponse getProject(GitAccessContext access, String fullPath)
    {
        return getObject(access, projectUrl(access, fullPath, null));
    }

    private ApiResponse getObject(GitAccessContext access, HttpUrl url)
    {
        RawApiResponse response = execute(access, url);
        if (!response.success())
        {
            return ApiResponse.failure(response.result());
        }
        try
        {
            JSONObject json = JSON.parseObject(response.body());
            return ApiResponse.success(json == null ? new JSONObject() : json);
        }
        catch (RuntimeException e)
        {
            return ApiResponse.failure(GitConnectionResult.failure(GitConnectionFailure.API_ERROR, "GitLab API 响应异常"));
        }
    }

    private ApiListResponse getArray(GitAccessContext access, HttpUrl url, int page, int perPage)
    {
        HttpUrl paged = url.newBuilder()
            .addQueryParameter("page", String.valueOf(page))
            .addQueryParameter("per_page", String.valueOf(perPage))
            .build();
        RawApiResponse response = execute(access, paged);
        if (!response.success())
        {
            return ApiListResponse.failure(response.result());
        }
        try
        {
            JSONArray json = JSON.parseArray(response.body());
            return ApiListResponse.success(json == null ? new JSONArray() : json);
        }
        catch (RuntimeException e)
        {
            return ApiListResponse.failure(GitConnectionResult.failure(GitConnectionFailure.API_ERROR, "GitLab API 响应异常"));
        }
    }

    static HttpUrl projectUrl(GitAccessContext access, String fullPath, String suffix)
    {
        HttpUrl.Builder builder = apiBaseUrl(access).newBuilder()
            .addPathSegment("projects")
            .addEncodedPathSegment(encodeProjectId(fullPath));
        if (suffix != null && !suffix.isBlank())
        {
            for (String segment : suffix.split("/"))
            {
                if (!segment.isBlank())
                {
                    builder.addPathSegment(segment);
                }
            }
        }
        return builder.build();
    }

    static HttpUrl apiBaseUrl(GitAccessContext access)
    {
        HttpUrl parsed = HttpUrl.parse(GitAccessContext.normalizeServerUrl(access.serverUrl()) + "/api/v4");
        if (parsed == null)
        {
            throw new IllegalArgumentException("GitLab API 地址无效");
        }
        return parsed;
    }

    static String encodeProjectId(String fullPath)
    {
        return URLEncoder.encode(fullPath, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private RawApiResponse execute(GitAccessContext access, HttpUrl url)
    {
        String token;
        try
        {
            token = access.requireToken();
        }
        catch (IllegalArgumentException ex)
        {
            return RawApiResponse.failure(
                GitConnectionResult.failure(GitConnectionFailure.INVALID_CREDENTIAL, "GitLab 凭据无效"));
        }

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
                String body = response.body() == null ? "{}" : response.body().string();
                return RawApiResponse.success(body);
            }
            if (status == 401)
            {
                return RawApiResponse.failure(
                    GitConnectionResult.failure(GitConnectionFailure.INVALID_CREDENTIAL, "GitLab 凭据无效或已过期"));
            }
            if (status == 403)
            {
                return RawApiResponse.failure(
                    GitConnectionResult.failure(GitConnectionFailure.PERMISSION_DENIED, "当前 Token 权限不足"));
            }
            if (status == 404)
            {
                return RawApiResponse.failure(
                    GitConnectionResult.failure(GitConnectionFailure.REPOSITORY_NOT_FOUND, "仓库不存在或当前 Token 不可见"));
            }
            return RawApiResponse.failure(
                GitConnectionResult.failure(GitConnectionFailure.API_ERROR, "GitLab API 返回异常状态：" + status));
        }
        catch (SocketTimeoutException e)
        {
            return RawApiResponse.failure(GitConnectionResult.failure(GitConnectionFailure.TIMEOUT, "GitLab 请求超时"));
        }
        catch (InterruptedIOException e)
        {
            Thread.currentThread().interrupt();
            return RawApiResponse.failure(GitConnectionResult.failure(GitConnectionFailure.TIMEOUT, "GitLab 请求超时"));
        }
        catch (IOException e)
        {
            return RawApiResponse.failure(
                GitConnectionResult.failure(GitConnectionFailure.NETWORK_ERROR, "无法连接 GitLab API，请检查网络"));
        }
    }

    private static URI toUri(String serverUrl)
    {
        try
        {
            return new URI(GitAccessContext.normalizeServerUrl(serverUrl));
        }
        catch (URISyntaxException e)
        {
            throw new IllegalArgumentException("GitLab 服务地址无效");
        }
    }

    private static boolean hostMatches(URI uri, String expectedHost, int expectedPort)
    {
        if (uri.getHost() == null || !uri.getHost().equalsIgnoreCase(expectedHost))
        {
            return false;
        }
        int actualPort = uri.getPort();
        if (expectedPort > 0)
        {
            return actualPort == expectedPort || (actualPort == -1 && isDefaultPort(uri.getScheme(), expectedPort));
        }
        return actualPort == -1 || isDefaultPort(uri.getScheme(), actualPort);
    }

    private static boolean isDefaultPort(String scheme, int port)
    {
        return ("https".equalsIgnoreCase(scheme) && port == 443)
            || ("http".equalsIgnoreCase(scheme) && port == 80);
    }

    private static String extractPathWithNamespace(String path)
    {
        if (path == null || path.isBlank() || "/".equals(path))
        {
            throw invalidRepositoryUrl();
        }
        String value = path.startsWith("/") ? path.substring(1) : path;
        while (value.endsWith("/"))
        {
            value = value.substring(0, value.length() - 1);
        }
        return stripGitSuffix(value);
    }

    private static String stripGitSuffix(String repository)
    {
        return repository.toLowerCase(Locale.ROOT).endsWith(".git")
            ? repository.substring(0, repository.length() - 4)
            : repository;
    }

    private static IllegalArgumentException invalidRepositoryUrl()
    {
        return new IllegalArgumentException("GitLab 仓库地址格式错误，需与服务地址 host 一致，支持 HTTPS 或 git@host SSH 地址");
    }

    private record ApiResponse(boolean success, JSONObject body, GitConnectionResult result)
    {
        static ApiResponse success(JSONObject body)
        {
            return new ApiResponse(true, body, null);
        }

        static ApiResponse failure(GitConnectionResult result)
        {
            return new ApiResponse(false, null, result);
        }
    }

    private record ApiListResponse(boolean success, JSONArray body, GitConnectionResult result)
    {
        static ApiListResponse success(JSONArray body)
        {
            return new ApiListResponse(true, body, null);
        }

        static ApiListResponse failure(GitConnectionResult result)
        {
            return new ApiListResponse(false, null, result);
        }
    }

    private record RawApiResponse(boolean success, String body, GitConnectionResult result)
    {
        static RawApiResponse success(String body)
        {
            return new RawApiResponse(true, body, null);
        }

        static RawApiResponse failure(GitConnectionResult result)
        {
            return new RawApiResponse(false, null, result);
        }
    }
}
