package com.acr.review.service;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import com.acr.review.scheduling.ReviewTaskDispatcher;

/** 事务提交后调度审查执行，避免领取时任务尚未可见。 */
@Component
public class ReviewTaskExecuteEventListener
{
    private final ReviewTaskDispatcher dispatcher;

    public ReviewTaskExecuteEventListener(ReviewTaskDispatcher dispatcher)
    {
        this.dispatcher = dispatcher;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onExecute(ReviewTaskExecuteEvent event)
    {
        dispatcher.wake(event.getTaskId());
    }
}
