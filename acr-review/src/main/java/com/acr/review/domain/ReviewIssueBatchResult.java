package com.acr.review.domain;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/** 问题批量处置结果：预校验失败返回 failures；成功返回 successCount 与评论同步结果。 */
public class ReviewIssueBatchResult
{
    private final List<Map<String, Object>> failures;
    private final int successCount;
    private final ReviewCommentSyncResult commentSync;

    private ReviewIssueBatchResult(List<Map<String, Object>> failures, int successCount,
                                   ReviewCommentSyncResult commentSync)
    {
        this.failures = failures == null ? List.of() : failures;
        this.successCount = successCount;
        this.commentSync = commentSync;
    }

    public static ReviewIssueBatchResult rejected(List<Map<String, Object>> failures)
    {
        return new ReviewIssueBatchResult(failures, 0, null);
    }

    public static ReviewIssueBatchResult success(int successCount, ReviewCommentSyncResult commentSync)
    {
        return new ReviewIssueBatchResult(Collections.emptyList(), successCount, commentSync);
    }

    public boolean hasFailures()
    {
        return failures != null && !failures.isEmpty();
    }

    public List<Map<String, Object>> getFailures()
    {
        return failures;
    }

    public int getSuccessCount()
    {
        return successCount;
    }

    public ReviewCommentSyncResult getCommentSync()
    {
        return commentSync;
    }
}
