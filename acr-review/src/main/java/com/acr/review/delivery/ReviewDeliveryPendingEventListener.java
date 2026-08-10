package com.acr.review.delivery;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** 事务提交后唤醒投递，避免读取到尚未提交的意图。 */
@Component
public class ReviewDeliveryPendingEventListener
{
    private final ReviewDeliveryDispatcher dispatcher;

    public ReviewDeliveryPendingEventListener(ReviewDeliveryDispatcher dispatcher)
    {
        this.dispatcher = dispatcher;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onPending(ReviewDeliveryPendingEvent event)
    {
        dispatcher.wake(event.getDeliveryId());
    }
}
