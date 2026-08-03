package com.acr.review.service;

import java.util.List;
import com.acr.review.domain.ReviewCommentSyncResult;
import com.acr.review.domain.ReviewDeliveryRecord;
import com.acr.review.domain.ReviewTask;
import com.acr.review.domain.ReviewTaskRun;

/** 审查结果外部投递（GitHub PR 总结评论 + IM 群机器人）。 */
public interface IReviewDeliveryService
{
    /**
     * 审查成功后投递 GitHub 总结评论。失败只记投递记录，不抛出到调用方影响任务状态。
     */
    void deliverAfterSuccess(ReviewTask task, ReviewTaskRun run);

    /**
     * 审查结束后投递 IM（SUCCESS 摘要 / FAILED 简讯）。失败只记投递记录，不影响任务状态。
     */
    void deliverNotifyAfterTerminal(ReviewTask task, ReviewTaskRun run);

    /**
     * 人工重试投递：以 taskId 定位项目/PR，按该 PR 最近一次 SUCCESS 任务结论渲染（GitHub）。
     */
    void retryDelivery(Long taskId);

    /**
     * 按投递记录重试：GitHub 行走 PR 最近 SUCCESS；IM 行用原 task 结论重渲染。
     */
    void retryDeliveryById(Long deliveryId);

    /**
     * 按项目+PR 用最近 SUCCESS 结论重渲染总结评论（含问题处置态）。
     * 返回同步结果（status + 失败原因 + deliveryId），不抛出评论失败。
     */
    ReviewCommentSyncResult rerenderSummaryComment(Long projectId, Integer prNumber);

    /** 按项目+PR 查询总结评论投递记录（无则 null）。 */
    ReviewDeliveryRecord selectSummaryDelivery(Long projectId, Integer prNumber);

    /** 按任务查询最近一条 IM 投递记录（无则 null）。 */
    ReviewDeliveryRecord selectLatestImDelivery(Long taskId);

    ReviewDeliveryRecord selectDeliveryById(Long deliveryId);

    List<ReviewDeliveryRecord> selectDeliveryList(ReviewDeliveryRecord query);

    /** 与投递列表同口径计数。 */
    int countDeliveryList(ReviewDeliveryRecord query);
}
