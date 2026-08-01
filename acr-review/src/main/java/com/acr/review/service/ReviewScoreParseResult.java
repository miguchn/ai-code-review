package com.acr.review.service;

import com.acr.review.domain.result.ReviewScoreResult;

/** 大模型审查结果解析结果。 */
public final class ReviewScoreParseResult
{
    private final boolean success;
    private final ReviewScoreResult result;
    private final String errorMessage;
    private final String rawExcerpt;

    private ReviewScoreParseResult(boolean success, ReviewScoreResult result, String errorMessage, String rawExcerpt)
    {
        this.success = success;
        this.result = result;
        this.errorMessage = errorMessage;
        this.rawExcerpt = rawExcerpt;
    }

    public static ReviewScoreParseResult ok(ReviewScoreResult result, String rawExcerpt)
    {
        return new ReviewScoreParseResult(true, result, null, rawExcerpt);
    }

    public static ReviewScoreParseResult fail(String errorMessage, String rawExcerpt)
    {
        return new ReviewScoreParseResult(false, null, errorMessage, rawExcerpt);
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
}
