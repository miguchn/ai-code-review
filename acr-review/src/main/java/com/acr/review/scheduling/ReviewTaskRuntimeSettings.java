package com.acr.review.scheduling;

import org.springframework.stereotype.Component;
import com.acr.system.service.ISysConfigService;

/** 审查任务调度参数。参数由管理端运行期维护，代码只提供安全默认值和边界。 */
@Component
public class ReviewTaskRuntimeSettings
{
    public static final String CONFIG_SCAN_INTERVAL_SECONDS = "review.task.dispatch.scanIntervalSeconds";
    public static final String CONFIG_SCAN_BATCH_SIZE = "review.task.dispatch.batchSize";
    public static final String CONFIG_LEASE_SECONDS = "review.task.lease.seconds";
    public static final String CONFIG_HEARTBEAT_SECONDS = "review.task.heartbeat.seconds";
    public static final String CONFIG_MAX_RETRIES = "review.task.retry.maxAttempts";
    public static final String CONFIG_RETRY_BASE_DELAY_SECONDS = "review.task.retry.baseDelaySeconds";
    public static final String CONFIG_RETRY_MAX_DELAY_SECONDS = "review.task.retry.maxDelaySeconds";
    public static final String CONFIG_EXECUTOR_POOL_SIZE = "review.task.executor.poolSize";
    public static final String CONFIG_EXECUTOR_QUEUE_CAPACITY = "review.task.executor.queueCapacity";
    public static final String CONFIG_PROJECT_MAX_CONCURRENCY = "review.task.project.maxConcurrency";
    public static final String CONFIG_WORKSPACE_MAX_COUNT = "review.task.budget.workspace.maxCount";
    public static final String CONFIG_WORKSPACE_MAX_DISK_MB = "review.task.budget.workspace.maxDiskMb";
    public static final String CONFIG_OCR_MAX_CONCURRENCY = "review.task.budget.ocr.maxConcurrency";
    public static final String CONFIG_LLM_MAX_CONCURRENCY = "review.task.budget.llm.maxConcurrency";

    private final ISysConfigService configService;

    public ReviewTaskRuntimeSettings(ISysConfigService configService)
    {
        this.configService = configService;
    }

    public int scanIntervalSeconds()
    {
        return integer(CONFIG_SCAN_INTERVAL_SECONDS, 10, 1, 300);
    }

    public int scanBatchSize()
    {
        return integer(CONFIG_SCAN_BATCH_SIZE, 64, 1, 1000);
    }

    public int leaseSeconds()
    {
        return integer(CONFIG_LEASE_SECONDS, 900, 60, 7200);
    }

    public int heartbeatSeconds()
    {
        int configured = integer(CONFIG_HEARTBEAT_SECONDS, 30, 5, 600);
        return Math.min(configured, Math.max(5, leaseSeconds() / 3));
    }

    public int maxRetries()
    {
        return integer(CONFIG_MAX_RETRIES, 3, 0, 20);
    }

    public int retryBaseDelaySeconds()
    {
        return integer(CONFIG_RETRY_BASE_DELAY_SECONDS, 30, 1, 3600);
    }

    public int retryMaxDelaySeconds()
    {
        return integer(CONFIG_RETRY_MAX_DELAY_SECONDS, 900, 1, 86400);
    }

    /** 专用审查执行池线程数（启动时生效）。 */
    public int executorPoolSize()
    {
        return integer(CONFIG_EXECUTOR_POOL_SIZE, 4, 1, 64);
    }

    /** 专用审查执行池有界队列容量（启动时生效）。 */
    public int executorQueueCapacity()
    {
        return integer(CONFIG_EXECUTOR_QUEUE_CAPACITY, 64, 1, 10000);
    }

    /** 单项目同时进入执行池的任务上限。 */
    public int projectMaxConcurrency()
    {
        return integer(CONFIG_PROJECT_MAX_CONCURRENCY, 2, 1, 32);
    }

    /** 同时存活的工作区数量上限。 */
    public int workspaceMaxCount()
    {
        return integer(CONFIG_WORKSPACE_MAX_COUNT, 4, 1, 128);
    }

    /** 工作区根目录磁盘占用上限（MB）。 */
    public int workspaceMaxDiskMb()
    {
        return integer(CONFIG_WORKSPACE_MAX_DISK_MB, 10240, 64, 1_048_576);
    }

    /** OCR 外部进程全局并发上限。 */
    public int ocrMaxConcurrency()
    {
        return integer(CONFIG_OCR_MAX_CONCURRENCY, 2, 1, 64);
    }

    /** LLM 调用全局并发上限。 */
    public int llmMaxConcurrency()
    {
        return integer(CONFIG_LLM_MAX_CONCURRENCY, 4, 1, 64);
    }

    /** 队列拒绝或预算不足时的退避秒数（复用重试基数）。 */
    public int budgetBackoffSeconds()
    {
        return retryBaseDelaySeconds();
    }

    public int retryDelaySeconds(int completedRetries)
    {
        int exponent = Math.max(0, Math.min(completedRetries, 20));
        long delay = (long) retryBaseDelaySeconds() << exponent;
        return (int) Math.min(delay, retryMaxDelaySeconds());
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
