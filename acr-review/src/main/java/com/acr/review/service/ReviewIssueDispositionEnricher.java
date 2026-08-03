package com.acr.review.service;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.acr.review.domain.ReviewIssue;
import com.acr.review.domain.result.ReviewTopIssue;

/** 按指纹把台账处置态挂到 Top3（评论/详情共用）。 */
public final class ReviewIssueDispositionEnricher
{
    private ReviewIssueDispositionEnricher()
    {
    }

    public static void enrich(List<ReviewTopIssue> topIssues, Map<String, ReviewIssue> byFingerprint)
    {
        if (topIssues == null || topIssues.isEmpty() || byFingerprint == null || byFingerprint.isEmpty())
        {
            return;
        }
        Set<String> used = new HashSet<>();
        int batchIndex = 0;
        for (ReviewTopIssue top : topIssues)
        {
            batchIndex++;
            String baseFp = ReviewIssueFingerprint.of(top);
            String fp = baseFp;
            if (used.contains(fp))
            {
                fp = ReviewIssueFingerprint.withBatchSuffix(baseFp, batchIndex);
            }
            used.add(fp);
            ReviewIssue matched = byFingerprint.get(fp);
            if (matched == null && !fp.equals(baseFp))
            {
                matched = byFingerprint.get(baseFp);
            }
            if (matched != null)
            {
                top.setIssueId(matched.getIssueId());
                top.setDispositionStatus(matched.getStatus());
                top.setDispositionNote(matched.getResolveNote());
            }
        }
    }
}
