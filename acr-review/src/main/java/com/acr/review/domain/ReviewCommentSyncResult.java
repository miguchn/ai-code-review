package com.acr.review.domain;

import com.acr.review.delivery.ReviewDeliveryConstants;

/** 问题处置后 PR 总结评论同步结果。 */
public class ReviewCommentSyncResult
{
    private final String status;
    private final String failureMessage;
    private final Long deliveryId;

    public ReviewCommentSyncResult(String status, String failureMessage, Long deliveryId)
    {
        this.status = status;
        this.failureMessage = failureMessage;
        this.deliveryId = deliveryId;
    }

    public static ReviewCommentSyncResult skipped()
    {
        return new ReviewCommentSyncResult(ReviewDeliveryConstants.STATUS_SKIPPED, null, null);
    }

    public static ReviewCommentSyncResult of(String status, String failureMessage, Long deliveryId)
    {
        return new ReviewCommentSyncResult(status, failureMessage, deliveryId);
    }

    public String getStatus()
    {
        return status;
    }

    public String getFailureMessage()
    {
        return failureMessage;
    }

    public Long getDeliveryId()
    {
        return deliveryId;
    }
}
