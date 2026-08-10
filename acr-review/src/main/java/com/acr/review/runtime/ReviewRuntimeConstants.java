package com.acr.review.runtime;

/** 运行可观测与运营面稳定常量。 */
public final class ReviewRuntimeConstants
{
    public static final String PERM_RUNTIME_VIEW = "review:runtime:view";
    public static final String PERM_TASK_CANCEL = "review:task:cancel";
    public static final String PERM_TASK_HANDLE = "review:task:handle";

    public static final String CONFIG_PENDING_AGE_MINUTES = "review.runtime.alert.pendingAgeMinutes";
    public static final String CONFIG_DELIVERY_PENDING_AGE_MINUTES = "review.runtime.alert.deliveryPendingAgeMinutes";
    public static final String CONFIG_BUDGET_SATURATED_MINUTES = "review.runtime.alert.budgetSaturatedMinutes";
    public static final String CONFIG_FAILURE_RATE_WINDOW_MINUTES = "review.runtime.alert.failureRateWindowMinutes";
    public static final String CONFIG_FAILURE_RATE_PERCENT = "review.runtime.alert.failureRatePercent";
    public static final String CONFIG_ALERT_SCAN_INTERVAL_SECONDS = "review.runtime.alert.scanIntervalSeconds";
    public static final String CONFIG_DRAIN_TIMEOUT_SECONDS = "review.runtime.drain.timeoutSeconds";

    public static final String ALERT_PENDING_OVERAGE = "PENDING_OVERAGE";
    public static final String ALERT_DELIVERY_OVERAGE = "DELIVERY_OVERAGE";
    public static final String ALERT_BUDGET_SATURATED = "BUDGET_SATURATED";
    public static final String ALERT_FAILURE_RATE = "FAILURE_RATE";

    public static final String TARGET_TASK = "TASK";
    public static final String TARGET_DELIVERY = "DELIVERY";
    public static final String TARGET_RUNTIME = "RUNTIME";

    private ReviewRuntimeConstants()
    {
    }
}
