package com.acr.review.service;

import com.acr.review.domain.result.ReviewScoreResult;

/** 大模型审查结果解析结果。 */
public final class ReviewScoreParseResult
{
    private final boolean success;
    private final ReviewScoreResult result;
    private final String errorMessage;
    private final String rawExcerpt;
    /** 归属打标统计（未启用打标时全为 0）。 */
    private final int newCount;
    private final int existingCount;
    private final int originUnverifiableCount;
    /** 是否因超过 maxIssues 发生截断。 */
    private final boolean issuesTruncated;

    private ReviewScoreParseResult(boolean success, ReviewScoreResult result, String errorMessage, String rawExcerpt,
        int newCount, int existingCount, int originUnverifiableCount, boolean issuesTruncated)
    {
        this.success = success;
        this.result = result;
        this.errorMessage = errorMessage;
        this.rawExcerpt = rawExcerpt;
        this.newCount = newCount;
        this.existingCount = existingCount;
        this.originUnverifiableCount = originUnverifiableCount;
        this.issuesTruncated = issuesTruncated;
    }

    public static ReviewScoreParseResult ok(ReviewScoreResult result, String rawExcerpt)
    {
        return new ReviewScoreParseResult(true, result, null, rawExcerpt, 0, 0, 0, false);
    }

    public static ReviewScoreParseResult ok(ReviewScoreResult result, String rawExcerpt,
        int newCount, int existingCount, int originUnverifiableCount)
    {
        return ok(result, rawExcerpt, newCount, existingCount, originUnverifiableCount, false);
    }

    public static ReviewScoreParseResult ok(ReviewScoreResult result, String rawExcerpt,
        int newCount, int existingCount, int originUnverifiableCount, boolean issuesTruncated)
    {
        return new ReviewScoreParseResult(true, result, null, rawExcerpt,
            newCount, existingCount, originUnverifiableCount, issuesTruncated);
    }

    public static ReviewScoreParseResult fail(String errorMessage, String rawExcerpt)
    {
        return new ReviewScoreParseResult(false, null, errorMessage, rawExcerpt, 0, 0, 0, false);
    }

    public boolean isSuccess()
    {
        return success;
    }

    public ReviewScoreResult getResult()
    {
        return result;
    }

    public String getErrorMessage()
    {
        return errorMessage;
    }

    public String getRawExcerpt()
    {
        return rawExcerpt;
    }

    public int getNewCount()
    {
        return newCount;
    }

    public int getExistingCount()
    {
        return existingCount;
    }

    public int getOriginUnverifiableCount()
    {
        return originUnverifiableCount;
    }

    public boolean isIssuesTruncated()
    {
        return issuesTruncated;
    }
}
