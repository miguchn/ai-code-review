package com.acr.review.scheduling;

/** 审查调度与资源预算的可查询运行态（S6 指标/页面复用）。 */
public record ReviewRuntimeStatus(
    int reviewQueueDepth,
    int reviewActiveCount,
    int reviewPoolSize,
    int reviewQueueCapacity,
    long reviewRejectedCount,
    int deliveryQueueDepth,
    int deliveryActiveCount,
    int deliveryPoolSize,
    int deliveryQueueCapacity,
    long deliveryRejectedCount,
    ReviewResourceBudgetStatus budgets)
{
}
