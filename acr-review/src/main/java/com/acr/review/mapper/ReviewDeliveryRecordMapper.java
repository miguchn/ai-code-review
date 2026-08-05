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

    List<ReviewDeliveryRecord> selectDeliveryList(ReviewDeliveryRecord query);

    /** 与 selectDeliveryList 同筛选、同 DataScope 的计数。 */
    int countDeliveryList(ReviewDeliveryRecord query);

    /** 首次插入投递结果（成功或失败）。 */
    int insertDelivery(ReviewDeliveryRecord record);

    /** 按幂等键更新最近一次投递结果（attempt_count + 1）。 */
    int updateDeliveryResult(ReviewDeliveryRecord record);
}
