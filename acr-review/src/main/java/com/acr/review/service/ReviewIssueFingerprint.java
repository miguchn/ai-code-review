package com.acr.review.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import com.acr.common.utils.StringUtils;
import com.acr.review.domain.ReviewIssueConstants;
import com.acr.review.domain.result.ReviewTopIssue;

/**
 * 问题指纹：hash(category + filePath + normalize(title))，不含行号。
 */
public final class ReviewIssueFingerprint
{
    private ReviewIssueFingerprint()
    {
    }

    public static String of(ReviewTopIssue issue)
    {
        if (issue == null)
        {
            return hash(rawKey("", "", ReviewIssueConstants.DEFAULT_TITLE));
        }
        String title = StringUtils.isEmpty(issue.getTitle())
            ? ReviewIssueConstants.DEFAULT_TITLE
            : issue.getTitle();
        return hash(rawKey(issue.getCategory(), issue.getFilePath(), title));
    }

    public static String of(String category, String filePath, String title)
    {
        String safeTitle = StringUtils.isEmpty(title) ? ReviewIssueConstants.DEFAULT_TITLE : title;
        return hash(rawKey(category, filePath, safeTitle));
    }

    /** 同批碰撞：在基础指纹后追加 :batchIndex（从 1 起）。 */
    public static String withBatchSuffix(String baseFingerprint, int batchIndex)
    {
        if (batchIndex <= 0)
        {
            return baseFingerprint;
        }
        return baseFingerprint + ":" + batchIndex;
    }

    static String normalizeTitle(String title)
    {
        if (title == null)
        {
            return "";
        }
        return title.trim().replaceAll("\\s+", " ").toLowerCase();
    }

    static String nullToEmpty(String value)
    {
        return value == null ? "" : value;
    }

    static String rawKey(String category, String filePath, String title)
    {
        return nullToEmpty(category) + "\0" + nullToEmpty(filePath) + "\0" + normalizeTitle(title);
    }

    static String hash(String raw)
    {
        try
        {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        }
        catch (NoSuchAlgorithmException ex)
        {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }
}
