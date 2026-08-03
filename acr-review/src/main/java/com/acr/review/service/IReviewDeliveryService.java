package com.acr.review.service;

import com.acr.review.domain.ReviewDeliveryRecord;
import com.acr.review.domain.ReviewTask;
import com.acr.review.domain.ReviewTaskRun;

/** 审查结果外部投递（GitHub PR 总结评论）。 */
public interface IReviewDeliveryService
{
    /**
     * 审查成功后投递总结评论。失败只记投递记录，不抛出到调用方影响任务状态。
     */
    void deliverAfterSuccess(ReviewTask task, ReviewTaskRun run);

    /**
     * 人工重试投递：以 taskId 定位项目/PR，按该 PR 最近一次 SUCCESS 任务结论渲染。
     */
    void retryDelivery(Long taskId);

    /** 按项目+PR 查询总结评论投递记录（无则 null）。 */
    ReviewDeliveryRecord selectSummaryDelivery(Long projectId, Integer prNumber);
}
