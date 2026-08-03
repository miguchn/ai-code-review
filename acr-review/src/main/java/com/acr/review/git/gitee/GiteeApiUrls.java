package com.acr.review.git.gitee;

import com.acr.review.git.GitAccessContext;
import okhttp3.HttpUrl;

/** Gitee API 根地址推导（serverUrl + /api/v5）。 */
final class GiteeApiUrls
{
    private GiteeApiUrls()
    {
    }

    static HttpUrl apiBaseFromServer(String serverUrl)
    {
        HttpUrl parsed = HttpUrl.parse(GitAccessContext.normalizeServerUrl(serverUrl) + "/api/v5");
        if (parsed == null)
        {
            throw new IllegalArgumentException("Gitee API 地址配置无效");
        }
        return parsed;
    }

    static HttpUrl parseApiBase(String apiBaseUrl)
    {
        HttpUrl parsed = HttpUrl.parse(apiBaseUrl);
        if (parsed == null)
        {
            throw new IllegalArgumentException("Gitee API 地址配置无效");
        }
        return parsed;
    }
}
