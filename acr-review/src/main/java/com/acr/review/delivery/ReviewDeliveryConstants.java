package com.acr.review.delivery;

/** 审查结果投递稳定常量。 */
public final class ReviewDeliveryConstants
{
    public static final String CHANNEL_GITHUB_PR_SUMMARY = "GITHUB_PR_SUMMARY_COMMENT";

    /** 评论正文固定标记：用于查找并更新同一 PR 上的 ACR 总结评论。 */
    public static final String COMMENT_MARKER = "<!-- acr-review-summary -->";

    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_FAILED = "FAILED";

    public static final String PROVIDER_GITHUB = "GITHUB";

    /** Top3 单条描述最大字符数，避免评论过长。 */
    public static final int MAX_ISSUE_DESCRIPTION_CHARS = 500;

    /** 失败原因落库上限。 */
    public static final int MAX_FAILURE_MESSAGE_CHARS = 500;

    /** 查找标记时每页评论数。 */
    public static final int COMMENT_PAGE_SIZE = 100;

    /** 最多翻页数（合计最多 300 条）。 */
    public static final int COMMENT_MAX_PAGES = 3;

    private ReviewDeliveryConstants()
    {
    }

    /** 幂等键：GITHUB:{projectId}:{prNumber}:SUMMARY_COMMENT */
    public static String idempotencyKey(Long projectId, Integer prNumber)
    {
        return PROVIDER_GITHUB + ":" + projectId + ":" + prNumber + ":SUMMARY_COMMENT";
    }
}
