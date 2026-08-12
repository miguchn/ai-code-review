package com.acr.review.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.acr.review.domain.ReviewDeliveryRecord;

/** 审查结果投递记录数据访问。 */
public interface ReviewDeliveryRecordMapper
{
    ReviewDeliveryRecord selectDeliveryById(Long deliveryId);

    /** 仅拉取正文快照（列表/详情主查询不带此大字段）。 */
    String selectContentSnapshotById(@Param("deliveryId") Long deliveryId);

    ReviewDeliveryRecord selectByIdempotencyKey(@Param("idempotencyKey") String idempotencyKey);

    ReviewDeliveryRecord selectByProjectAndPr(@Param("projectId") Long projectId,
                                              @Param("prNumber") Integer prNumber,
                                              @Param("channel") String channel);

    ReviewDeliveryRecord selectLatestImByTaskId(@Param("taskId") Long taskId);

    /** 同项目同渠道冷却窗口内是否已有待处理或已成功发送通知；失败不应抑制后续消息。 */
    int countRecentImDeliveries(@Param("projectId") Long projectId,
                                @Param("channel") String channel,
                                @Param("excludeTaskId") Long excludeTaskId,
                                @Param("cooldownMinutes") int cooldownMinutes);

    /** 按问题 ID 反查最近一条行内投递记录。 */
    ReviewDeliveryRecord selectByIssueId(@Param("issueId") Long issueId);

    /** 任务下全部行内投递意图（任务详情展示）。 */
    List<ReviewDeliveryRecord> selectInlineByTaskId(@Param("taskId") Long taskId);

    List<ReviewDeliveryRecord> selectDeliveryList(ReviewDeliveryRecord query);

    /** 与 selectDeliveryList 同筛选、同 DataScope 的计数。 */
    int countDeliveryList(ReviewDeliveryRecord query);

    /** 首次插入投递结果（成功或失败）。 */
    int insertDelivery(ReviewDeliveryRecord record);

    /** 按幂等键更新最近一次投递结果（attempt_count + 1）。 */
    int updateDeliveryResult(ReviewDeliveryRecord record);

    /** 新建或刷新待投递意图；唯一键冲突时保留累计尝试次数。 */
    int upsertDeliveryIntent(ReviewDeliveryRecord record);

    /** 查询到期且当前无有效租约的投递记录。 */
    List<Long> selectDispatchableDeliveryIds(@Param("limit") int limit);

    /** 使用数据库时钟原子领取投递租约。 */
    int claimDelivery(@Param("deliveryId") Long deliveryId,
                      @Param("leaseOwner") String leaseOwner,
                      @Param("leaseSeconds") int leaseSeconds);

    /** 外部副作用成功后，在租约围栏内提交投递事实。 */
    int completeDelivery(@Param("deliveryId") Long deliveryId,
                         @Param("leaseOwner") String leaseOwner,
                         @Param("resolvedChannel") String resolvedChannel,
                         @Param("externalId") String externalId,
                         @Param("contentSnapshot") String contentSnapshot);

    /** 外部副作用失败后，在租约围栏内进入自动退避或人工处置。 */
    int failDelivery(@Param("deliveryId") Long deliveryId,
                     @Param("leaseOwner") String leaseOwner,
                     @Param("deliveryStatus") String deliveryStatus,
                     @Param("lastErrorCode") String lastErrorCode,
                     @Param("failureMessage") String failureMessage,
                     @Param("retryDelaySeconds") Integer retryDelaySeconds,
                     @Param("contentSnapshot") String contentSnapshot);

    /** 人工补发只改变投递状态机，不直接执行外部调用。 */
    int requeueDelivery(@Param("deliveryId") Long deliveryId,
                        @Param("triggerSource") String triggerSource,
                        @Param("operator") String operator);

    int releaseDeliveryLease(@Param("deliveryId") Long deliveryId,
                             @Param("leaseOwner") String leaseOwner);

    int releaseWorkerLeases(@Param("leaseOwner") String leaseOwner);

    /** 优雅停机超时：将本实例持有的投递租约置过期。 */
    int expireWorkerLeases(@Param("leaseOwner") String leaseOwner);

    /** 人工标记已处理：MANUAL → SKIPPED，不再自动投递。 */
    int markManualHandled(@Param("deliveryId") Long deliveryId,
                          @Param("operator") String operator,
                          @Param("lastErrorCode") String lastErrorCode,
                          @Param("failureMessage") String failureMessage);
}
