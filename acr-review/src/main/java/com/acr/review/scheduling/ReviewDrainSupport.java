package com.acr.review.scheduling;

import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.acr.review.runtime.ReviewRuntimeAlertSettings;

/** 优雅停机排空：停止取新任务，等待活跃执行至截止，超时后由回调释租。 */
public final class ReviewDrainSupport
{
    private static final Logger log = LoggerFactory.getLogger(ReviewDrainSupport.class);

    private ReviewDrainSupport()
    {
    }

    public static void drain(String componentName,
                      ThreadPoolExecutor workerExecutor,
                      ReviewRuntimeAlertSettings alertSettings,
                      Runnable onTimeoutExpireLeases)
    {
        int timeoutSeconds = alertSettings == null ? 60 : alertSettings.drainTimeoutSeconds();
        log.info("{}进入优雅停机：停止取新任务，最多等待 {} 秒", componentName, timeoutSeconds);
        long deadlineNanos = System.nanoTime() + TimeUnit.SECONDS.toNanos(timeoutSeconds);
        while (workerExecutor != null && workerExecutor.getActiveCount() > 0 && System.nanoTime() < deadlineNanos)
        {
            try
            {
                Thread.sleep(200L);
            }
            catch (InterruptedException ex)
            {
                Thread.currentThread().interrupt();
                break;
            }
        }
        int active = workerExecutor == null ? 0 : workerExecutor.getActiveCount();
        if (active > 0)
        {
            log.warn("{}优雅停机等待超时，仍有 {} 个活跃任务，开始释放本实例租约", componentName, active);
            onTimeoutExpireLeases.run();
        }
        else
        {
            log.info("{}优雅停机完成：租约内任务已排空", componentName);
        }
    }
}
