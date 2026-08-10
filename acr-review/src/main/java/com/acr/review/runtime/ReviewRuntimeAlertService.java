package com.acr.review.runtime;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Service;
import com.acr.review.delivery.ReviewDeliveryConstants;
import com.acr.review.domain.ReviewPipelineConstants;
import com.acr.review.mapper.ReviewRuntimeStatsMapper;
import com.acr.review.scheduling.IReviewRuntimeStatusService;
import com.acr.review.scheduling.ReviewResourceBudgetStatus;
import com.acr.review.scheduling.ReviewRuntimeStatus;

/**
 * 内置告警判定：阈值来自 sys_config，条目仅存内存并写中文日志，不落业务表。
 */
@Service
public class ReviewRuntimeAlertService implements SmartLifecycle
{
    private static final Logger log = LoggerFactory.getLogger(ReviewRuntimeAlertService.class);

    private final ReviewRuntimeAlertSettings settings;
    private final ReviewRuntimeStatsMapper statsMapper;
    private final IReviewRuntimeStatusService runtimeStatusService;
    private final ScheduledExecutorService controlScheduler;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicReference<List<ReviewRuntimeAlert>> latestAlerts = new AtomicReference<>(List.of());
    private final AtomicReference<Long> budgetSaturatedSinceMs = new AtomicReference<>(null);
    private volatile ScheduledFuture<?> nextScan;

    public ReviewRuntimeAlertService(ReviewRuntimeAlertSettings settings,
                                     ReviewRuntimeStatsMapper statsMapper,
                                     IReviewRuntimeStatusService runtimeStatusService,
                                     @Qualifier("reviewTaskControlScheduler") ScheduledExecutorService controlScheduler)
    {
        this.settings = settings;
        this.statsMapper = statsMapper;
        this.runtimeStatusService = runtimeStatusService;
        this.controlScheduler = controlScheduler;
    }

    public List<ReviewRuntimeAlert> currentAlerts()
    {
        return latestAlerts.get();
    }

    /** 立即判定一次；供概览接口与单测调用。 */
    public List<ReviewRuntimeAlert> evaluateNow()
    {
        List<ReviewRuntimeAlert> alerts = evaluate(runtimeStatusService.snapshot(), new Date());
        latestAlerts.set(List.copyOf(alerts));
        for (ReviewRuntimeAlert alert : alerts)
        {
            log.warn("审查运行告警 [{}] {}；看哪里：{}；怎么办：{}",
                alert.getCode(), alert.getMessage(), describeTarget(alert), alert.getAction());
        }
        return latestAlerts.get();
    }

    List<ReviewRuntimeAlert> evaluate(ReviewRuntimeStatus status, Date now)
    {
        List<ReviewRuntimeAlert> alerts = new ArrayList<>();
        appendPendingOverage(alerts, now);
        appendDeliveryOverage(alerts, now);
        appendBudgetSaturation(alerts, status, now);
        appendFailureRate(alerts, now);
        return alerts;
    }

    private void appendPendingOverage(List<ReviewRuntimeAlert> alerts, Date now)
    {
        Map<String, Object> oldest = statsMapper.selectOldestPendingTask();
        if (oldest == null)
        {
            return;
        }
        long ageSeconds = toLong(oldest.get("ageSeconds"));
        long thresholdSeconds = settings.pendingAgeMinutes() * 60L;
        if (ageSeconds < thresholdSeconds)
        {
            return;
        }
        Long taskId = toLongObject(oldest.get("taskId"));
        alerts.add(new ReviewRuntimeAlert(
            ReviewRuntimeConstants.ALERT_PENDING_OVERAGE,
            "warning",
            "待执行任务超龄",
            "存在待执行任务已等待 " + (ageSeconds / 60) + " 分钟（阈值 " + settings.pendingAgeMinutes() + " 分钟），任务 #"
                + taskId,
            "打开该任务详情确认是否卡住；可手动执行或终止后由恢复扫描清理积压",
            ReviewRuntimeConstants.TARGET_TASK,
            taskId,
            null,
            now));
    }

    private void appendDeliveryOverage(List<ReviewRuntimeAlert> alerts, Date now)
    {
        Map<String, Object> oldest = statsMapper.selectOldestPendingDelivery();
        if (oldest == null)
        {
            return;
        }
        long ageSeconds = toLong(oldest.get("ageSeconds"));
        long thresholdSeconds = settings.deliveryPendingAgeMinutes() * 60L;
        if (ageSeconds < thresholdSeconds)
        {
            return;
        }
        Long deliveryId = toLongObject(oldest.get("deliveryId"));
        Long taskId = toLongObject(oldest.get("taskId"));
        alerts.add(new ReviewRuntimeAlert(
            ReviewRuntimeConstants.ALERT_DELIVERY_OVERAGE,
            "warning",
            "待投递记录超龄",
            "存在待投递记录已等待 " + (ageSeconds / 60) + " 分钟（阈值 " + settings.deliveryPendingAgeMinutes()
                + " 分钟），投递 #" + deliveryId,
            "打开投递记录核对渠道与凭证；可补发或标记人工已处理",
            ReviewRuntimeConstants.TARGET_DELIVERY,
            taskId,
            deliveryId,
            now));
    }

    private void appendBudgetSaturation(List<ReviewRuntimeAlert> alerts, ReviewRuntimeStatus status, Date now)
    {
        if (status == null || status.budgets() == null)
        {
            budgetSaturatedSinceMs.set(null);
            return;
        }
        ReviewResourceBudgetStatus budgets = status.budgets();
        boolean saturated = isSaturated(budgets.workspaceHeld(), budgets.workspaceLimit())
            || isSaturated(budgets.ocrHeld(), budgets.ocrLimit())
            || isSaturated(budgets.llmHeld(), budgets.llmLimit());
        if (!saturated)
        {
            budgetSaturatedSinceMs.set(null);
            return;
        }
        Long since = budgetSaturatedSinceMs.updateAndGet(prev -> prev == null ? now.getTime() : prev);
        long saturatedMinutes = Math.max(0L, (now.getTime() - since) / 60_000L);
        if (saturatedMinutes < settings.budgetSaturatedMinutes())
        {
            return;
        }
        alerts.add(new ReviewRuntimeAlert(
            ReviewRuntimeConstants.ALERT_BUDGET_SATURATED,
            "critical",
            "资源预算持续饱和",
            "工作区/OCR/LLM 预算已持续打满约 " + saturatedMinutes + " 分钟（阈值 "
                + settings.budgetSaturatedMinutes() + " 分钟）",
            "查看运行概览资源占用；调高预算参数或降低并发，避免任务长期停留在待重试",
            ReviewRuntimeConstants.TARGET_RUNTIME,
            null,
            null,
            now));
    }

    private void appendFailureRate(List<ReviewRuntimeAlert> alerts, Date now)
    {
        Map<String, Object> outcomes = statsMapper.countTerminalOutcomesSinceMinutes(settings.failureRateWindowMinutes());
        if (outcomes == null)
        {
            return;
        }
        long success = toLong(outcomes.get("successCount"));
        long failed = toLong(outcomes.get("failedCount"));
        long cancelled = toLong(outcomes.get("cancelledCount"));
        long total = success + failed + cancelled;
        if (total < 5)
        {
            return;
        }
        int rate = (int) Math.round(failed * 100.0 / total);
        if (rate < settings.failureRatePercent())
        {
            return;
        }
        alerts.add(new ReviewRuntimeAlert(
            ReviewRuntimeConstants.ALERT_FAILURE_RATE,
            "critical",
            "近窗失败率过高",
            "近 " + settings.failureRateWindowMinutes() + " 分钟终态失败率约 " + rate + "%（阈值 "
                + settings.failureRatePercent() + "%），失败 " + failed + " / 合计 " + total,
            "打开审查任务列表筛选「已失败」，按失败分类排查依赖与配置后重试",
            ReviewRuntimeConstants.TARGET_RUNTIME,
            null,
            null,
            now));
    }

    private static boolean isSaturated(int held, int limit)
    {
        return limit > 0 && held >= limit;
    }

    private static String describeTarget(ReviewRuntimeAlert alert)
    {
        if (alert.getDeliveryId() != null)
        {
            return "投递记录 #" + alert.getDeliveryId()
                + (alert.getTaskId() != null ? "（任务 #" + alert.getTaskId() + "）" : "");
        }
        if (alert.getTaskId() != null)
        {
            return "审查任务 #" + alert.getTaskId();
        }
        return "运行概览资源面";
    }

    private static long toLong(Object value)
    {
        if (value == null)
        {
            return 0L;
        }
        if (value instanceof Number number)
        {
            return number.longValue();
        }
        return Long.parseLong(value.toString());
    }

    private static Long toLongObject(Object value)
    {
        if (value == null)
        {
            return null;
        }
        return toLong(value);
    }

    @Override
    public void start()
    {
        if (running.compareAndSet(false, true))
        {
            scheduleNext(0);
        }
    }

    private void scheduleNext(long delaySeconds)
    {
        if (!running.get())
        {
            return;
        }
        nextScan = controlScheduler.schedule(() -> {
            try
            {
                evaluateNow();
            }
            catch (RuntimeException ex)
            {
                log.error("运行告警周期判定异常", ex);
            }
            finally
            {
                scheduleNext(settings.alertScanIntervalSeconds());
            }
        }, delaySeconds, TimeUnit.SECONDS);
    }

    @Override
    public void stop()
    {
        if (!running.compareAndSet(true, false))
        {
            return;
        }
        ScheduledFuture<?> scan = nextScan;
        if (scan != null)
        {
            scan.cancel(false);
        }
    }

    @Override
    public boolean isRunning()
    {
        return running.get();
    }

    @Override
    public boolean isAutoStartup()
    {
        return true;
    }

    @Override
    public int getPhase()
    {
        return Integer.MAX_VALUE - 80;
    }

    /** 测试用：重置预算饱和计时。 */
    void resetBudgetSaturationClock()
    {
        budgetSaturatedSinceMs.set(null);
    }

    /** 暴露给测试的状态常量引用，避免告警规则与任务状态漂移。 */
    static String pendingStatus()
    {
        return ReviewPipelineConstants.TASK_PENDING;
    }

    static String deliveryPendingStatus()
    {
        return ReviewDeliveryConstants.STATUS_PENDING;
    }
}
