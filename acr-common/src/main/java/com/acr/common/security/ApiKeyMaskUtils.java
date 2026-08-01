package com.acr.common.security;

import com.acr.common.utils.StringUtils;

/** API Key 展示脱敏。 */
public final class ApiKeyMaskUtils
{
    private ApiKeyMaskUtils()
    {
    }

    public static String mask(String apiKey)
    {
        if (StringUtils.isEmpty(apiKey))
        {
            return apiKey;
        }
        if (AesGcmStringCrypto.looksEncrypted(apiKey))
        {
            return "****";
        }
        if (apiKey.length() <= 8)
        {
            return "****";
        }
        String suffix = apiKey.substring(apiKey.length() - 4);
        if (apiKey.startsWith("sk-"))
        {
            return "sk-****" + suffix;
        }
        return apiKey.substring(0, 4) + "****" + suffix;
    }

    public static boolean isMaskedOrBlank(String apiKey)
    {
        return StringUtils.isEmpty(apiKey) || apiKey.contains("****");
    }
}
