package com.acr.review.runtime;

import org.springframework.stereotype.Component;
import com.acr.system.service.ISysConfigService;

/** 运行告警与优雅停机参数，运行期可调。 */
@Component
public class ReviewRuntimeAlertSettings
{
    private final ISysConfigService configService;

    public ReviewRuntimeAlertSettings(ISysConfigService configService)
    {
        this.configService = configService;
    }

    public int pendingAgeMinutes()
    {
        return integer(ReviewRuntimeConstants.CONFIG_PENDING_AGE_MINUTES, 30, 1, 10080);
    }

    public int deliveryPendingAgeMinutes()
    {
        return integer(ReviewRuntimeConstants.CONFIG_DELIVERY_PENDING_AGE_MINUTES, 20, 1, 10080);
    }

    public int budgetSaturatedMinutes()
    {
        return integer(ReviewRuntimeConstants.CONFIG_BUDGET_SATURATED_MINUTES, 10, 1, 1440);
    }

    public int failureRateWindowMinutes()
    {
        return integer(ReviewRuntimeConstants.CONFIG_FAILURE_RATE_WINDOW_MINUTES, 60, 5, 10080);
    }

    public int failureRatePercent()
    {
        return integer(ReviewRuntimeConstants.CONFIG_FAILURE_RATE_PERCENT, 40, 1, 100);
    }

    public int alertScanIntervalSeconds()
    {
        return integer(ReviewRuntimeConstants.CONFIG_ALERT_SCAN_INTERVAL_SECONDS, 30, 5, 600);
    }

    public int drainTimeoutSeconds()
    {
        return integer(ReviewRuntimeConstants.CONFIG_DRAIN_TIMEOUT_SECONDS, 60, 5, 1800);
    }

    private int integer(String key, int defaultValue, int minimum, int maximum)
    {
        try
        {
            String raw = configService.selectConfigByKey(key);
            if (raw == null || raw.isBlank())
            {
                return defaultValue;
            }
            int value = Integer.parseInt(raw.trim());
            return Math.max(minimum, Math.min(value, maximum));
        }
        catch (RuntimeException ex)
        {
            return defaultValue;
        }
    }
}
