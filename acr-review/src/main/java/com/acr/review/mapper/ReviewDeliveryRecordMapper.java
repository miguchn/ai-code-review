package com.acr.review.mapper;

import org.apache.ibatis.annotations.Param;
import com.acr.review.domain.ReviewDeliveryRecord;

/** 审查结果投递记录数据访问。 */
public interface ReviewDeliveryRecordMapper
{
    ReviewDeliveryRecord selectByIdempotencyKey(@Param("idempotencyKey") String idempotencyKey);

    ReviewDeliveryRecord selectByProjectAndPr(@Param("projectId") Long projectId,
                                              @Param("prNumber") Integer prNumber,
                                              @Param("channel") String channel);

    /** 首次插入投递结果（成功或失败）。 */
    int insertDelivery(ReviewDeliveryRecord record);

    /** 按幂等键更新最近一次投递结果（attempt_count + 1）。 */
    int updateDeliveryResult(ReviewDeliveryRecord record);
}
