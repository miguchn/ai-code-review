package com.acr.review.service;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** 事务提交后调度审查执行，避免领取时任务尚未可见。 */
@Component
public class ReviewTaskExecuteEventListener
{
    private static final Logger log = LoggerFactory.getLogger(ReviewTaskExecuteEventListener.class);

    private final IReviewTaskExecutionService executionService;
    private final Executor executor;

    public ReviewTaskExecuteEventListener(IReviewTaskExecutionService executionService,
                                          @Qualifier("scheduledExecutorService") ObjectProvider<ScheduledExecutorService> executorProvider)
    {
        this.executionService = executionService;
        ScheduledExecutorService shared = executorProvider.getIfAvailable();
        this.executor = shared != null ? shared : Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "review-task-exec");
            thread.setDaemon(true);
            return thread;
        });
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onExecute(ReviewTaskExecuteEvent event)
    {
        Long taskId = event.getTaskId();
        executor.execute(() -> {
            try
            {
                executionService.executeTask(taskId);
            }
            catch (RuntimeException ex)
            {
                log.error("审查任务异步执行异常, taskId={}", taskId, ex);
            }
        });
    }
}
