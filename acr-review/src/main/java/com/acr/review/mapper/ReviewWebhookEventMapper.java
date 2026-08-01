package com.acr.review.mapper;

import com.acr.review.domain.ReviewWebhookEvent;
import org.apache.ibatis.annotations.Param;

/** Webhook 事件数据访问。 */
public interface ReviewWebhookEventMapper
{
    /** 插入事件占位（RECEIVED），delivery 唯一键冲突时抛出 DuplicateKeyException。 */
    int insertEvent(ReviewWebhookEvent event);

    /** 按幂等键查询事件。 */
    ReviewWebhookEvent selectByDelivery(@Param("provider") String provider, @Param("deliveryId") String deliveryId);

    /** 更新处理结果（状态、说明、任务关联、业务字段、处理时间）。 */
    int updateProcessResult(ReviewWebhookEvent event);

    /** 重复投递：累加计数并刷新处理时间。 */
    int incrementDuplicate(@Param("provider") String provider, @Param("deliveryId") String deliveryId);
}
