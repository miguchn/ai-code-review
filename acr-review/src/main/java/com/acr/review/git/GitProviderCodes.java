package com.acr.review.git;

/** 稳定的 Git Provider 编码。 */
public final class GitProviderCodes
{
    public static final String GITHUB = "GITHUB";
    public static final String GITLAB = "GITLAB";
    public static final String GITEE = "GITEE";
    public static final String GITEA = "GITEA";

    public static final String DEFAULT_GITHUB_SERVER = "https://github.com";
    public static final String DEFAULT_GITEE_SERVER = "https://gitee.com";

    private GitProviderCodes()
    {
    }

    public static boolean requiresServerUrl(String provider)
    {
        return GITLAB.equalsIgnoreCase(provider) || GITEA.equalsIgnoreCase(provider);
    }

    public static boolean forbidsServerUrl(String provider)
    {
        return GITHUB.equalsIgnoreCase(provider) || GITEE.equalsIgnoreCase(provider);
    }

    public static String defaultServerUrl(String provider)
    {
        if (GITHUB.equalsIgnoreCase(provider))
        {
            return DEFAULT_GITHUB_SERVER;
        }
        if (GITEE.equalsIgnoreCase(provider))
        {
            return DEFAULT_GITEE_SERVER;
        }
        return null;
    }

    public static boolean isSupported(String provider)
    {
        return GITHUB.equalsIgnoreCase(provider)
            || GITLAB.equalsIgnoreCase(provider)
            || GITEE.equalsIgnoreCase(provider)
            || GITEA.equalsIgnoreCase(provider);
    }
}
