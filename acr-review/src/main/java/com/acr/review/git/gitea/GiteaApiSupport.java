package com.acr.review.git.gitea;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import com.acr.review.git.GitAccessContext;
import com.acr.review.git.GitRepositoryCoordinates;
import okhttp3.HttpUrl;

/** Gitea API 地址与路径拼装（serverUrl + /api/v1）。 */
final class GiteaApiSupport
{
    private GiteaApiSupport()
    {
    }

    static HttpUrl apiBaseUrl(GitAccessContext access)
    {
        HttpUrl parsed = HttpUrl.parse(GitAccessContext.normalizeServerUrl(access.serverUrl()) + "/api/v1");
        if (parsed == null)
        {
            throw new IllegalArgumentException("Gitea API 地址无效");
        }
        return parsed;
    }

    static HttpUrl.Builder reposBuilder(HttpUrl apiBase, GitRepositoryCoordinates repository)
    {
        HttpUrl.Builder builder = apiBase.newBuilder().addPathSegment("repos");
        for (String segment : repository.fullPath().split("/"))
        {
            if (!segment.isBlank())
            {
                builder.addPathSegment(segment);
            }
        }
        return builder;
    }

    static String authorizationHeader(String token)
    {
        return "token " + token;
    }

    static URI toUri(String serverUrl)
    {
        try
        {
            return new URI(GitAccessContext.normalizeServerUrl(serverUrl));
        }
        catch (URISyntaxException e)
        {
            throw new IllegalArgumentException("Gitea 服务地址无效");
        }
    }

    static boolean hostMatches(URI uri, String expectedHost, int expectedPort)
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

    static String extractRepoPath(String path)
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

    static String stripGitSuffix(String repository)
    {
        return repository.toLowerCase(Locale.ROOT).endsWith(".git")
            ? repository.substring(0, repository.length() - 4)
            : repository;
    }

    static IllegalArgumentException invalidRepositoryUrl()
    {
        return new IllegalArgumentException("Gitea 仓库地址格式错误，需与服务地址 host 一致，支持 HTTPS 或 git@host SSH 地址");
    }
}
