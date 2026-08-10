package com.acr.review.service;

import java.util.List;
import java.util.Map;
import com.acr.review.domain.ReviewCommentSyncResult;
import com.acr.review.domain.ReviewDeliveryRecord;
import com.acr.review.domain.ReviewRoundReconcileResult;
import com.acr.review.domain.ReviewTask;
import com.acr.review.domain.ReviewTaskRun;

/** 审查结果外部投递（GitHub PR 总结评论 + IM 群机器人）。 */
public interface IReviewDeliveryService
{
    /** 审查成功后登记总结评论投递意图；外部调用由投递工作节点异步执行。 */
    void deliverAfterSuccess(ReviewTask task, ReviewTaskRun run, ReviewRoundReconcileResult reconcile);

    /** 审查结束后登记 IM 投递意图；外部调用由投递工作节点异步执行。 */
    void deliverNotifyAfterTerminal(ReviewTask task, ReviewTaskRun run, ReviewRoundReconcileResult reconcile);

    /** 人工重试：按 PR 最近成功结论刷新同一投递意图，不直接调用外部平台。 */
    void retryDelivery(Long taskId);

    /** 按投递记录补发：复用自动投递状态机、尝试次数和幂等键。 */
    void retryDeliveryById(Long deliveryId);

    /** 标记人工已处理：MANUAL → SKIPPED，不再自动投递。 */
    void markManualHandled(Long deliveryId);

    /**
     * 按项目+PR 用最近 SUCCESS 结论重渲染总结评论（含问题处置态）。
     * 返回排队结果（status + deliveryId），不在问题处置事务中调用外部平台。
     */
    ReviewCommentSyncResult rerenderSummaryComment(Long projectId, Integer prNumber);

    /** 按项目+PR 查询总结评论投递记录（无则 null）。 */
    ReviewDeliveryRecord selectSummaryDelivery(Long projectId, Integer prNumber);

    /** 按任务查询最近一条 IM 投递记录（无则 null）。 */
    ReviewDeliveryRecord selectLatestImDelivery(Long taskId);

    /** 按问题 ID 反查行内评论投递记录（无则 null）。 */
    ReviewDeliveryRecord selectInlineDeliveryByIssueId(Long issueId);

    /** 任务下全部行内投递记录（任务/记录详情展示）。 */
    List<ReviewDeliveryRecord> selectInlineDeliveriesByTaskId(Long taskId);

    ReviewDeliveryRecord selectDeliveryById(Long deliveryId);

    /**
     * 查看投递正文快照（kind/channelType/title/body）。
     * 权限与数据范围与列表一致；无快照时返回空 Map。
     */
    Map<String, Object> selectDeliveryContent(Long deliveryId);

    List<ReviewDeliveryRecord> selectDeliveryList(ReviewDeliveryRecord query);

    /** 与投递列表同口径计数。 */
    int countDeliveryList(ReviewDeliveryRecord query);

    /** 调度器已取得数据库租约后执行单条投递；仅供平台内部工作节点调用。 */
    void executeClaimedDelivery(Long deliveryId, String leaseOwner);
}
