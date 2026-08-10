package com.acr.review.scheduling;

import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import com.acr.review.delivery.ReviewDeliveryRuntimeSettings;

@Service
public class ReviewRuntimeStatusService implements IReviewRuntimeStatusService
{
    private final ThreadPoolExecutor reviewWorkerExecutor;
    private final ThreadPoolExecutor deliveryWorkerExecutor;
    private final AtomicLong reviewRejectedCount;
    private final AtomicLong deliveryRejectedCount;
    private final ReviewTaskRuntimeSettings taskSettings;
    private final ReviewDeliveryRuntimeSettings deliverySettings;
    private final ReviewResourceBudgetService budgetService;

    public ReviewRuntimeStatusService(
        @Qualifier("reviewTaskWorkerExecutor") ThreadPoolExecutor reviewWorkerExecutor,
        @Qualifier("reviewDeliveryWorkerExecutor") ThreadPoolExecutor deliveryWorkerExecutor,
        @Qualifier("reviewTaskRejectedCount") AtomicLong reviewRejectedCount,
        @Qualifier("reviewDeliveryRejectedCount") AtomicLong deliveryRejectedCount,
        ReviewTaskRuntimeSettings taskSettings,
        ReviewDeliveryRuntimeSettings deliverySettings,
        ReviewResourceBudgetService budgetService)
    {
        this.reviewWorkerExecutor = reviewWorkerExecutor;
        this.deliveryWorkerExecutor = deliveryWorkerExecutor;
        this.reviewRejectedCount = reviewRejectedCount;
        this.deliveryRejectedCount = deliveryRejectedCount;
        this.taskSettings = taskSettings;
        this.deliverySettings = deliverySettings;
        this.budgetService = budgetService;
    }

    @Override
    public ReviewRuntimeStatus snapshot()
    {
        return new ReviewRuntimeStatus(
            reviewWorkerExecutor.getQueue().size(),
            reviewWorkerExecutor.getActiveCount(),
            reviewWorkerExecutor.getMaximumPoolSize(),
            taskSettings.executorQueueCapacity(),
            reviewRejectedCount.get(),
            deliveryWorkerExecutor.getQueue().size(),
            deliveryWorkerExecutor.getActiveCount(),
            deliveryWorkerExecutor.getMaximumPoolSize(),
            deliverySettings.executorQueueCapacity(),
            deliveryRejectedCount.get(),
            budgetService.snapshot());
    }
}
