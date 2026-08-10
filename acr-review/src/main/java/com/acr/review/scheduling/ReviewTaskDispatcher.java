package com.acr.review.scheduling;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;
import com.acr.review.domain.ReviewTask;
import com.acr.review.mapper.ReviewTaskMapper;
import com.acr.review.runtime.ReviewRuntimeAlertSettings;
import com.acr.review.service.IReviewTaskExecutionService;

/**
 * 数据库驱动的审查任务调度入口。内存唤醒只降低延迟，周期扫描负责丢事件和宕机恢复。
 * 执行提交到专用有界池；队列拒绝时延迟 next_run_at，禁止 CallerRuns，禁止静默丢弃。
 */
@Component
public class ReviewTaskDispatcher implements SmartLifecycle
{
    private static final Logger log = LoggerFactory.getLogger(ReviewTaskDispatcher.class);

    private final ReviewTaskMapper taskMapper;
    private final ReviewTaskRuntimeSettings settings;
    private final ReviewTaskWorkerIdentity workerIdentity;
    private final ReviewResourceBudgetService budgetService;
    private final ReviewRuntimeAlertSettings alertSettings;
    private final ObjectProvider<IReviewTaskExecutionService> executionServiceProvider;
    private final ScheduledExecutorService controlScheduler;
    private final ThreadPoolExecutor workerExecutor;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private volatile ScheduledFuture<?> nextScan;

    public ReviewTaskDispatcher(ReviewTaskMapper taskMapper,
                                ReviewTaskRuntimeSettings settings,
                                ReviewTaskWorkerIdentity workerIdentity,
                                ReviewResourceBudgetService budgetService,
                                ReviewRuntimeAlertSettings alertSettings,
                                ObjectProvider<IReviewTaskExecutionService> executionServiceProvider,
                                @Qualifier("reviewTaskControlScheduler") ScheduledExecutorService controlScheduler,
                                @Qualifier("reviewTaskWorkerExecutor") ThreadPoolExecutor workerExecutor)
    {
        this.taskMapper = taskMapper;
        this.settings = settings;
        this.workerIdentity = workerIdentity;
        this.budgetService = budgetService;
        this.alertSettings = alertSettings;
        this.executionServiceProvider = executionServiceProvider;
        this.controlScheduler = controlScheduler;
        this.workerExecutor = workerExecutor;
    }

    /** 事务提交后的低延迟唤醒；拒绝时任务仍保留在数据库等待扫描。 */
    public void wake(Long taskId)
    {
        if (taskId == null || !running.get())
        {
            return;
        }
        ReviewTask task = taskMapper.selectReviewTaskById(taskId);
        if (task == null)
        {
            return;
        }
        dispatch(task.getTaskId(), task.getProjectId());
    }

    void scanOnce()
    {
        int maxRetries = settings.maxRetries();
        int terminalized = taskMapper.terminalizeExpiredTasks(maxRetries);
        int recovered = taskMapper.requeueExpiredTasks(maxRetries, settings.retryBaseDelaySeconds());
        if (terminalized > 0 || recovered > 0)
        {
            log.info("审查任务租约恢复扫描完成, recovered={}, terminalized={}", recovered, terminalized);
        }

        int batchSize = settings.scanBatchSize();
        List<ReviewTask> candidates = taskMapper.selectDispatchableTasks(batchSize * 4);
        // P0：按项目公平轮询；优先级字段语义预留到 P1，此处不做优先级排序。
        for (ReviewTask candidate : fairOrderTasks(candidates, batchSize))
        {
            dispatch(candidate.getTaskId(), candidate.getProjectId());
        }
    }

    /** @deprecated 保留给既有测试；新代码使用 {@link #fairOrderTasks}。 */
    static List<Long> fairOrder(List<ReviewTask> candidates, int limit)
    {
        return fairOrderTasks(candidates, limit).stream().map(ReviewTask::getTaskId).toList();
    }

    static List<ReviewTask> fairOrderTasks(List<ReviewTask> candidates, int limit)
    {
        if (candidates == null || candidates.isEmpty() || limit <= 0)
        {
            return List.of();
        }
        Map<Long, ArrayDeque<ReviewTask>> byProject = new LinkedHashMap<>();
        for (ReviewTask candidate : candidates)
        {
            if (candidate == null || candidate.getTaskId() == null || candidate.getProjectId() == null)
            {
                continue;
            }
            byProject.computeIfAbsent(candidate.getProjectId(), ignored -> new ArrayDeque<>())
                .add(candidate);
        }
        List<ReviewTask> ordered = new ArrayList<>(Math.min(limit, candidates.size()));
        while (ordered.size() < limit)
        {
            boolean added = false;
            for (ArrayDeque<ReviewTask> projectQueue : byProject.values())
            {
                ReviewTask task = projectQueue.poll();
                if (task != null)
                {
                    ordered.add(task);
                    added = true;
                    if (ordered.size() == limit)
                    {
                        break;
                    }
                }
            }
            if (!added)
            {
                break;
            }
        }
        return ordered;
    }

    private void dispatch(Long taskId, Long projectId)
    {
        if (!budgetService.tryAcquireProject(projectId))
        {
            deferDispatch(taskId, "单项目并发已达上限");
            return;
        }
        try
        {
            workerExecutor.execute(() -> {
                try
                {
                    executionServiceProvider.getObject().executeTask(taskId);
                }
                catch (RuntimeException ex)
                {
                    log.error("审查任务调度执行异常，数据库恢复扫描将继续接管, taskId={}", taskId, ex);
                }
                finally
                {
                    budgetService.releaseProject(projectId);
                }
            });
        }
        catch (RejectedExecutionException ex)
        {
            budgetService.releaseProject(projectId);
            deferDispatch(taskId, "审查执行池队列已满");
            log.warn("审查任务执行器已饱和，任务保留并延迟下次调度, taskId={}, delaySeconds={}",
                taskId, settings.budgetBackoffSeconds());
        }
    }

    private void deferDispatch(Long taskId, String reason)
    {
        int delayed = taskMapper.deferDispatchableTask(taskId, settings.budgetBackoffSeconds(), reason);
        if (delayed == 1)
        {
            log.info("审查任务调度延迟, taskId={}, reason={}, delaySeconds={}",
                taskId, reason, settings.budgetBackoffSeconds());
        }
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
                scanOnce();
            }
            catch (RuntimeException ex)
            {
                log.error("审查任务恢复扫描异常", ex);
            }
            finally
            {
                scheduleNext(settings.scanIntervalSeconds());
            }
        }, delaySeconds, TimeUnit.SECONDS);
    }

    @Override
    public void stop()
    {
        drainAndStop();
    }

    @Override
    public void stop(Runnable callback)
    {
        try
        {
            drainAndStop();
        }
        finally
        {
            callback.run();
        }
    }

    private void drainAndStop()
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
        ReviewDrainSupport.drain("审查任务调度", workerExecutor, alertSettings, () -> {
            int expired = taskMapper.expireWorkerLeases(workerIdentity.owner());
            log.info("审查任务调度停机超时，已将本实例租约置过期, worker={}, expired={}",
                workerIdentity.owner(), expired);
        });
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
        return Integer.MAX_VALUE - 100;
    }
}
