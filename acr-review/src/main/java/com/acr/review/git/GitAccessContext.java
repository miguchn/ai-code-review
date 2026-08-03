package com.acr.review.git;

/** Provider 调用所需的最小访问上下文：明文 Token（可空，仅解析地址时）与 Web 根地址。 */
public record GitAccessContext(String token, String serverUrl)
{
    public GitAccessContext
    {
        if (serverUrl == null || serverUrl.isBlank())
        {
            throw new IllegalArgumentException("服务地址不能为空");
        }
    }

    public static GitAccessContext of(String token, String serverUrl)
    {
        return new GitAccessContext(token, normalizeServerUrl(serverUrl));
    }

    /** 仅用于仓库地址解析，不发起需要鉴权的 API。 */
    public static GitAccessContext forParse(String serverUrl)
    {
        return of(null, serverUrl);
    }

    public String requireToken()
    {
        if (token == null || token.isBlank())
        {
            throw new IllegalArgumentException("访问 Token 不能为空");
        }
        return token;
    }

    /** 规范化 Web 根：去尾斜杠；仅允许 http/https；拒绝 userinfo/query/fragment。 */
    public static String normalizeServerUrl(String serverUrl)
    {
        if (serverUrl == null || serverUrl.isBlank())
        {
            throw new IllegalArgumentException("服务地址不能为空");
        }
        String value = serverUrl.trim();
        while (value.endsWith("/"))
        {
            value = value.substring(0, value.length() - 1);
        }
        java.net.URI uri;
        try
        {
            uri = java.net.URI.create(value);
        }
        catch (IllegalArgumentException ex)
        {
            throw new IllegalArgumentException("服务地址无效");
        }
        String scheme = uri.getScheme();
        if (scheme == null
            || (!"https".equalsIgnoreCase(scheme) && !"http".equalsIgnoreCase(scheme))
            || uri.getHost() == null
            || uri.getHost().isBlank()
            || uri.getUserInfo() != null
            || uri.getRawQuery() != null
            || uri.getRawFragment() != null)
        {
            throw new IllegalArgumentException("服务地址无效，仅支持 http/https 且不含账号、查询或片段");
        }
        StringBuilder normalized = new StringBuilder();
        normalized.append(scheme.toLowerCase()).append("://").append(uri.getHost().toLowerCase());
        if (uri.getPort() > 0)
        {
            normalized.append(':').append(uri.getPort());
        }
        if (uri.getPath() != null && !uri.getPath().isBlank() && !"/".equals(uri.getPath()))
        {
            String path = uri.getPath();
            while (path.endsWith("/"))
            {
                path = path.substring(0, path.length() - 1);
            }
            normalized.append(path);
        }
        return normalized.toString();
    }
}
