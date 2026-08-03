package com.acr.review.git.gitea;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
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

/** Gitea REST API 适配（自建实例 serverUrl + /api/v1）。 */
@Component
public class GiteaProvider implements GitProvider
{
    private static final Pattern SSH_URL = Pattern.compile("^git@([^:]+):(.+)$", Pattern.CASE_INSENSITIVE);
    private static final int BRANCH_PAGE_SIZE = 100;
    private static final int MAX_BRANCH_PAGES = 1000;

    private final OkHttpClient client;

    public GiteaProvider(
            @Value("${review.gitea.connect-timeout-ms:5000}") int connectTimeoutMs,
            @Value("${review.gitea.read-timeout-ms:10000}") int readTimeoutMs)
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
    public GitRepositoryCoordinates parseRepository(String repositoryUrl, GitAccessContext access)
    {
        if (repositoryUrl == null || repositoryUrl.isBlank())
        {
            throw new IllegalArgumentException("Gitea 仓库地址不能为空");
        }

        URI serverUri = GiteaApiSupport.toUri(access.serverUrl());
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
                throw GiteaApiSupport.invalidRepositoryUrl();
            }
            fullPath = GiteaApiSupport.extractRepoPath(sshMatcher.group(2));
        }
        else
        {
            try
            {
                URI uri = new URI(value);
                if (!"https".equalsIgnoreCase(uri.getScheme()) && !"http".equalsIgnoreCase(uri.getScheme()))
                {
                    throw GiteaApiSupport.invalidRepositoryUrl();
                }
                if (!GiteaApiSupport.hostMatches(uri, expectedHost, expectedPort))
                {
                    throw GiteaApiSupport.invalidRepositoryUrl();
                }
                if (uri.getUserInfo() != null || uri.getQuery() != null || uri.getFragment() != null)
                {
                    throw GiteaApiSupport.invalidRepositoryUrl();
                }
                fullPath = GiteaApiSupport.extractRepoPath(uri.getPath());
            }
            catch (URISyntaxException e)
            {
                throw GiteaApiSupport.invalidRepositoryUrl();
            }
        }

        int lastSlash = fullPath.lastIndexOf('/');
        if (lastSlash <= 0 || lastSlash >= fullPath.length() - 1)
        {
            throw GiteaApiSupport.invalidRepositoryUrl();
        }

        String owner = fullPath.substring(0, lastSlash);
        String repository = fullPath.substring(lastSlash + 1);
        String canonicalUrl = access.serverUrl() + "/" + fullPath;
        return new GitRepositoryCoordinates(owner, repository, fullPath, canonicalUrl);
    }

    @Override
    public GitConnectionResult testCredential(GitAccessContext access)
    {
        ApiResponse response = getObject(access, "user");
        if (!response.success())
        {
            return response.result();
        }
        String login = response.body().getString("login");
        String suffix = login == null || login.isBlank() ? "" : "，Gitea 用户：" + login;
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

        ApiResponse response = getRepository(access, repository);
        if (!response.success())
        {
            return response.result();
        }
        String defaultBranch = response.body().getString("default_branch");
        String htmlUrl = response.body().getString("html_url");
        if (htmlUrl == null || htmlUrl.isBlank())
        {
            htmlUrl = repository.canonicalUrl();
        }
        return GitConnectionResult.success("连接成功，可访问 Gitea 仓库", defaultBranch, htmlUrl);
    }

    @Override
    public GitRepositoryInfoResult readRepository(GitRepositoryCoordinates repository, GitAccessContext access)
    {
        GitConnectionResult credentialResult = testCredential(access);
        if (!credentialResult.isSuccess())
        {
            return GitRepositoryInfoResult.failure(credentialResult.getFailure(), credentialResult.getMessage());
        }

        ApiResponse repositoryResponse = getRepository(access, repository);
        if (!repositoryResponse.success())
        {
            return GitRepositoryInfoResult.failure(
                repositoryResponse.result().getFailure(), repositoryResponse.result().getMessage());
        }

        String defaultBranch = repositoryResponse.body().getString("default_branch");
        String htmlUrl = repositoryResponse.body().getString("html_url");
        if (htmlUrl == null || htmlUrl.isBlank())
        {
            htmlUrl = repository.canonicalUrl();
        }

        Set<String> branchNames = new LinkedHashSet<>();
        for (int page = 1; page <= MAX_BRANCH_PAGES; page++)
        {
            ApiListResponse branchResponse = getArray(access, repository, "branches", page, BRANCH_PAGE_SIZE);
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
                return GitRepositoryInfoResult.success(repository, htmlUrl, defaultBranch, new ArrayList<>(branchNames));
            }
        }
        return GitRepositoryInfoResult.failure(GitConnectionFailure.API_ERROR, "Gitea 分支数量超过单次同步上限");
    }

    private ApiResponse getRepository(GitAccessContext access, GitRepositoryCoordinates repository)
    {
        HttpUrl url = GiteaApiSupport.reposBuilder(GiteaApiSupport.apiBaseUrl(access), repository).build();
        return getObject(access, url);
    }

    private ApiResponse getObject(GitAccessContext access, String relativePath)
    {
        HttpUrl url = GiteaApiSupport.apiBaseUrl(access).newBuilder().addPathSegments(relativePath).build();
        return getObject(access, url);
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
            return ApiResponse.failure(GitConnectionResult.failure(GitConnectionFailure.API_ERROR, "Gitea API 响应异常"));
        }
    }

    private ApiListResponse getArray(GitAccessContext access, GitRepositoryCoordinates repository,
                                     String suffix, int page, int perPage)
    {
        HttpUrl url = GiteaApiSupport.reposBuilder(GiteaApiSupport.apiBaseUrl(access), repository)
            .addPathSegment(suffix)
            .addQueryParameter("page", String.valueOf(page))
            .addQueryParameter("limit", String.valueOf(perPage))
            .build();
        RawApiResponse response = execute(access, url);
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
            return ApiListResponse.failure(GitConnectionResult.failure(GitConnectionFailure.API_ERROR, "Gitea API 响应异常"));
        }
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
                GitConnectionResult.failure(GitConnectionFailure.INVALID_CREDENTIAL, "Gitea 凭据无效"));
        }

        Request request = new Request.Builder()
            .url(url)
            .get()
            .header("Authorization", GiteaApiSupport.authorizationHeader(token))
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
                    GitConnectionResult.failure(GitConnectionFailure.INVALID_CREDENTIAL, "Gitea 凭据无效或已过期"));
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
                GitConnectionResult.failure(GitConnectionFailure.API_ERROR, "Gitea API 返回异常状态：" + status));
        }
        catch (SocketTimeoutException e)
        {
            return RawApiResponse.failure(GitConnectionResult.failure(GitConnectionFailure.TIMEOUT, "Gitea 请求超时"));
        }
        catch (InterruptedIOException e)
        {
            Thread.currentThread().interrupt();
            return RawApiResponse.failure(GitConnectionResult.failure(GitConnectionFailure.TIMEOUT, "Gitea 请求超时"));
        }
        catch (IOException e)
        {
            return RawApiResponse.failure(
                GitConnectionResult.failure(GitConnectionFailure.NETWORK_ERROR, "无法连接 Gitea API，请检查网络"));
        }
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
