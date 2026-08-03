package com.acr.review.git.github;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URISyntaxException;
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

/** GitHub REST API 适配。 */
@Component
public class GitHubProvider implements GitProvider
{
    private static final Pattern SSH_URL = Pattern.compile("^git@github\\.com:([^/]+)/([^/]+?)/?$", Pattern.CASE_INSENSITIVE);
    private static final Pattern OWNER = Pattern.compile("^[A-Za-z0-9](?:[A-Za-z0-9-]{0,38})$");
    private static final Pattern REPOSITORY = Pattern.compile("^[A-Za-z0-9_.-]{1,100}$");
    private static final int BRANCH_PAGE_SIZE = 100;
    private static final int MAX_BRANCH_PAGES = 1000;

    private final HttpUrl apiBaseUrl;
    private final OkHttpClient client;

    public GitHubProvider(
            @Value("${review.github.api-url:https://api.github.com}") String apiBaseUrl,
            @Value("${review.github.connect-timeout-ms:5000}") int connectTimeoutMs,
            @Value("${review.github.read-timeout-ms:10000}") int readTimeoutMs)
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
    public GitRepositoryCoordinates parseRepository(String repositoryUrl, GitAccessContext access)
    {
        if (repositoryUrl == null || repositoryUrl.isBlank())
        {
            throw new IllegalArgumentException("GitHub 仓库地址不能为空");
        }

        String value = repositoryUrl.trim();
        String owner;
        String repository;

        Matcher sshMatcher = SSH_URL.matcher(value);
        if (sshMatcher.matches())
        {
            owner = sshMatcher.group(1);
            repository = stripGitSuffix(sshMatcher.group(2));
        }
        else
        {
            try
            {
                URI uri = new URI(value);
                if (!"https".equalsIgnoreCase(uri.getScheme())
                    || !"github.com".equalsIgnoreCase(uri.getHost())
                    || uri.getUserInfo() != null
                    || uri.getQuery() != null
                    || uri.getFragment() != null)
                {
                    throw invalidRepositoryUrl();
                }
                String[] parts = uri.getPath().split("/");
                if (parts.length != 3 || parts[1].isBlank() || parts[2].isBlank())
                {
                    throw invalidRepositoryUrl();
                }
                owner = parts[1];
                repository = stripGitSuffix(parts[2]);
            }
            catch (URISyntaxException e)
            {
                throw invalidRepositoryUrl();
            }
        }

        if (!OWNER.matcher(owner).matches() || !REPOSITORY.matcher(repository).matches())
        {
            throw invalidRepositoryUrl();
        }
        return new GitRepositoryCoordinates(owner, repository, "https://github.com/" + owner + "/" + repository);
    }

    @Override
    public GitConnectionResult testCredential(GitAccessContext access)
    {
        ApiResponse response = getObject("user", access.requireToken());
        if (!response.success())
        {
            return response.result();
        }
        String login = response.body().getString("login");
        String suffix = login == null || login.isBlank() ? "" : "，GitHub 用户：" + login;
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

        String token = access.requireToken();
        ApiResponse response = getObject("repos/" + repository.owner() + "/" + repository.repository(), token);
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
        return GitConnectionResult.success("连接成功，可访问 GitHub 仓库", defaultBranch, htmlUrl);
    }

    @Override
    public GitRepositoryInfoResult readRepository(GitRepositoryCoordinates repository, GitAccessContext access)
    {
        GitConnectionResult credentialResult = testCredential(access);
        if (!credentialResult.isSuccess())
        {
            return GitRepositoryInfoResult.failure(credentialResult.getFailure(), credentialResult.getMessage());
        }

        String token = access.requireToken();
        String repositoryPath = "repos/" + repository.owner() + "/" + repository.repository();
        ApiResponse repositoryResponse = getObject(repositoryPath, token);
        if (!repositoryResponse.success())
        {
            return GitRepositoryInfoResult.failure(repositoryResponse.result().getFailure(), repositoryResponse.result().getMessage());
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
            ApiListResponse branchResponse = getArray(repositoryPath + "/branches", token, page, BRANCH_PAGE_SIZE);
            if (!branchResponse.success())
            {
                return GitRepositoryInfoResult.failure(branchResponse.result().getFailure(), branchResponse.result().getMessage());
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
        return GitRepositoryInfoResult.failure(GitConnectionFailure.API_ERROR, "GitHub 分支数量超过单次同步上限");
    }

    private ApiResponse getObject(String relativePath, String token)
    {
        RawApiResponse response = execute(buildUrl(relativePath, null, null), token);
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
            return ApiResponse.failure(GitConnectionResult.failure(GitConnectionFailure.API_ERROR, "GitHub API 响应异常"));
        }
    }

    private ApiListResponse getArray(String relativePath, String token, int page, int perPage)
    {
        RawApiResponse response = execute(buildUrl(relativePath, page, perPage), token);
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
            return ApiListResponse.failure(GitConnectionResult.failure(GitConnectionFailure.API_ERROR, "GitHub API 响应异常"));
        }
    }

    private HttpUrl buildUrl(String relativePath, Integer page, Integer perPage)
    {
        HttpUrl.Builder builder = apiBaseUrl.newBuilder().addPathSegments(relativePath);
        if (page != null && perPage != null)
        {
            builder.addQueryParameter("page", String.valueOf(page));
            builder.addQueryParameter("per_page", String.valueOf(perPage));
        }
        return builder.build();
    }

    private RawApiResponse execute(HttpUrl url, String token)
    {
        if (token == null || token.isBlank())
        {
            return RawApiResponse.failure(GitConnectionResult.failure(GitConnectionFailure.INVALID_CREDENTIAL, "GitHub 凭据无效"));
        }

        Request request = new Request.Builder()
            .url(url)
            .get()
            .header("Authorization", "Bearer " + token)
            .header("Accept", "application/vnd.github+json")
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
                return RawApiResponse.failure(GitConnectionResult.failure(GitConnectionFailure.INVALID_CREDENTIAL, "GitHub 凭据无效或已过期"));
            }
            if (status == 403)
            {
                if ("0".equals(response.header("X-RateLimit-Remaining")))
                {
                    return RawApiResponse.failure(GitConnectionResult.failure(GitConnectionFailure.API_ERROR, "GitHub API 请求频率受限，请稍后重试"));
                }
                return RawApiResponse.failure(GitConnectionResult.failure(GitConnectionFailure.PERMISSION_DENIED, "当前 Token 权限不足"));
            }
            if (status == 404)
            {
                return RawApiResponse.failure(GitConnectionResult.failure(GitConnectionFailure.REPOSITORY_NOT_FOUND, "仓库不存在或当前 Token 不可见"));
            }
            return RawApiResponse.failure(GitConnectionResult.failure(GitConnectionFailure.API_ERROR, "GitHub API 返回异常状态：" + status));
        }
        catch (SocketTimeoutException e)
        {
            return RawApiResponse.failure(GitConnectionResult.failure(GitConnectionFailure.TIMEOUT, "GitHub 请求超时"));
        }
        catch (InterruptedIOException e)
        {
            Thread.currentThread().interrupt();
            return RawApiResponse.failure(GitConnectionResult.failure(GitConnectionFailure.TIMEOUT, "GitHub 请求超时"));
        }
        catch (IOException e)
        {
            return RawApiResponse.failure(GitConnectionResult.failure(GitConnectionFailure.NETWORK_ERROR, "无法连接 GitHub API，请检查网络"));
        }
    }

    private static String stripGitSuffix(String repository)
    {
        return repository.toLowerCase(Locale.ROOT).endsWith(".git")
            ? repository.substring(0, repository.length() - 4)
            : repository;
    }

    private static IllegalArgumentException invalidRepositoryUrl()
    {
        return new IllegalArgumentException("GitHub 仓库地址格式错误，仅支持 HTTPS 或 git@github.com SSH 地址");
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
