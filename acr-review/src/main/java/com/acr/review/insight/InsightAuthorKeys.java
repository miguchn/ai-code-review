package com.acr.review.insight;

import com.acr.common.utils.StringUtils;

/** 提交身份键归一：邮箱非空取小写邮箱，否则取名称。 */
public final class InsightAuthorKeys
{
    private InsightAuthorKeys()
    {
    }

    public static String of(String authorEmail, String authorName)
    {
        if (StringUtils.isNotEmpty(authorEmail))
        {
            return authorEmail.trim().toLowerCase();
        }
        if (StringUtils.isNotEmpty(authorName))
        {
            return authorName.trim();
        }
        return null;
    }
}
