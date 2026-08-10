package com.acr.review.delivery;

import org.springframework.context.ApplicationEvent;

/** 事务提交后唤醒投递扫描；数据库记录仍是唯一事实源。 */
public class ReviewDeliveryPendingEvent extends ApplicationEvent
{
    private static final long serialVersionUID = 1L;

    private final Long deliveryId;

    public ReviewDeliveryPendingEvent(Object source, Long deliveryId)
    {
        super(source);
        this.deliveryId = deliveryId;
    }

    public Long getDeliveryId()
    {
        return deliveryId;
    }
}
