package com.acr.review.delivery;

import org.springframework.stereotype.Component;
import com.acr.system.service.ISysConfigService;

/** 投递调度参数：使用安全默认值，并对管理端配置做边界收敛。 */
@Component
public class ReviewDeliveryRuntimeSettings
{
    public static final String CONFIG_SCAN_INTERVAL_SECONDS = "review.delivery.dispatch.scanIntervalSeconds";
    public static final String CONFIG_SCAN_BATCH_SIZE = "review.delivery.dispatch.batchSize";
    public static final String CONFIG_LEASE_SECONDS = "review.delivery.lease.seconds";
    public static final String CONFIG_MAX_ATTEMPTS = "review.delivery.retry.maxAttempts";
    public static final String CONFIG_RETRY_BASE_DELAY_SECONDS = "review.delivery.retry.baseDelaySeconds";
    public static final String CONFIG_RETRY_MAX_DELAY_SECONDS = "review.delivery.retry.maxDelaySeconds";
    public static final String CONFIG_EXECUTOR_POOL_SIZE = "review.delivery.executor.poolSize";
    public static final String CONFIG_EXECUTOR_QUEUE_CAPACITY = "review.delivery.executor.queueCapacity";

    private final ISysConfigService configService;

    public ReviewDeliveryRuntimeSettings(ISysConfigService configService)
    {
        this.configService = configService;
    }

    public int scanIntervalSeconds()
    {
        return integer(CONFIG_SCAN_INTERVAL_SECONDS, 10, 1, 300);
    }

    public int scanBatchSize()
    {
        return integer(CONFIG_SCAN_BATCH_SIZE, 32, 1, 500);
    }

    public int leaseSeconds()
    {
        return integer(CONFIG_LEASE_SECONDS, 120, 30, 1800);
    }

    public int maxAttempts()
    {
        return integer(CONFIG_MAX_ATTEMPTS, 5, 1, 20);
    }

    /** 投递专用执行池线程数（启动时生效，与审查主池隔离）。 */
    public int executorPoolSize()
    {
        return integer(CONFIG_EXECUTOR_POOL_SIZE, 2, 1, 32);
    }

    /** 投递专用执行池有界队列容量（启动时生效）。 */
    public int executorQueueCapacity()
    {
        return integer(CONFIG_EXECUTOR_QUEUE_CAPACITY, 64, 1, 5000);
    }

    public int retryDelaySeconds(int completedAttempts)
    {
        int base = integer(CONFIG_RETRY_BASE_DELAY_SECONDS, 30, 1, 3600);
        int maximum = integer(CONFIG_RETRY_MAX_DELAY_SECONDS, 1800, 1, 86400);
        int exponent = Math.max(0, Math.min(completedAttempts, 20));
        return (int) Math.min((long) base << exponent, maximum);
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
            return Math.max(minimum, Math.min(Integer.parseInt(raw.trim()), maximum));
        }
        catch (RuntimeException ex)
        {
            return defaultValue;
        }
    }
}
