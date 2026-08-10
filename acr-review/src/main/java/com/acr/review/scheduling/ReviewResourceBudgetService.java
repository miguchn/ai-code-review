package com.acr.review.scheduling;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import com.acr.review.engine.ReviewEngineWorkspaceManager;

/**
 * 审查资源预算中心：Git/工作区、OCR、LLM、项目并发。
 * 抢不到名额一律由调用方回退 RETRYING，本服务不写任务终态。
 */
@Component
public class ReviewResourceBudgetService
{
    private static final Logger log = LoggerFactory.getLogger(ReviewResourceBudgetService.class);

    private final ReviewTaskRuntimeSettings settings;
    private final ReviewEngineWorkspaceManager workspaceManager;
    private final AdjustableBudget workspaceBudget = new AdjustableBudget();
    private final AdjustableBudget ocrBudget = new AdjustableBudget();
    private final AdjustableBudget llmBudget = new AdjustableBudget();
    private final Map<Long, AtomicInteger> projectActive = new ConcurrentHashMap<>();
    private final AtomicLong projectRejected = new AtomicLong();

    public ReviewResourceBudgetService(ReviewTaskRuntimeSettings settings,
                                       ReviewEngineWorkspaceManager workspaceManager)
    {
        this.settings = settings;
        this.workspaceManager = workspaceManager;
    }

    /**
     * OCR 审查：在准备工作区之前同时占用工作区与 OCR 名额。
     * 任一预算不足则全部不占用。
     */
    public ReviewBudgetLease tryAcquireOcrExecution()
    {
        if (!tryAcquireWorkspaceInternal())
        {
            log.info("Git/工作区预算不足，拒绝 OCR 执行进入准备阶段, held={}, limit={}, diskUsedMb={}, diskLimitMb={}",
                workspaceBudget.held(), settings.workspaceMaxCount(),
                workspaceUsedMb(), settings.workspaceMaxDiskMb());
            return null;
        }
        if (!ocrBudget.tryAcquire(settings.ocrMaxConcurrency()))
        {
            workspaceBudget.release();
            log.info("OCR 引擎预算不足，拒绝进入外部进程, held={}, limit={}",
                ocrBudget.held(), settings.ocrMaxConcurrency());
            return null;
        }
        return new ReviewBudgetLease(this, true, true, false);
    }

    /** LLM 审查：仅在真正发起模型调用前占用全局 LLM 并发名额。 */
    public ReviewBudgetLease tryAcquireLlmCall()
    {
        if (!llmBudget.tryAcquire(settings.llmMaxConcurrency()))
        {
            log.info("LLM 全局并发预算不足，拒绝发起模型调用, held={}, limit={}",
                llmBudget.held(), settings.llmMaxConcurrency());
            return null;
        }
        return new ReviewBudgetLease(this, false, false, true);
    }

    /** 管理端引擎探测/样例调用与审查任务共享 OCR/工作区总量上限。 */
    public ReviewBudgetLease tryAcquireOcrProbe()
    {
        return tryAcquireOcrExecution();
    }

    public boolean tryAcquireProject(Long projectId)
    {
        if (projectId == null)
        {
            return true;
        }
        int limit = settings.projectMaxConcurrency();
        AtomicInteger counter = projectActive.computeIfAbsent(projectId, ignored -> new AtomicInteger());
        while (true)
        {
            int current = counter.get();
            if (current >= limit)
            {
                projectRejected.incrementAndGet();
                log.info("单项目并发已达上限，延后派发, projectId={}, held={}, limit={}",
                    projectId, current, limit);
                return false;
            }
            if (counter.compareAndSet(current, current + 1))
            {
                return true;
            }
        }
    }

    public void releaseProject(Long projectId)
    {
        if (projectId == null)
        {
            return;
        }
        AtomicInteger counter = projectActive.get(projectId);
        if (counter == null)
        {
            return;
        }
        counter.updateAndGet(value -> Math.max(0, value - 1));
    }

    void release(boolean workspace, boolean ocr, boolean llm)
    {
        if (workspace)
        {
            workspaceBudget.release();
        }
        if (ocr)
        {
            ocrBudget.release();
        }
        if (llm)
        {
            llmBudget.release();
        }
    }

    public ReviewResourceBudgetStatus snapshot()
    {
        int projectHeld = projectActive.values().stream().mapToInt(AtomicInteger::get).sum();
        return new ReviewResourceBudgetStatus(
            workspaceBudget.held(),
            settings.workspaceMaxCount(),
            workspaceBudget.rejected(),
            workspaceUsedMb(),
            settings.workspaceMaxDiskMb(),
            ocrBudget.held(),
            settings.ocrMaxConcurrency(),
            ocrBudget.rejected(),
            llmBudget.held(),
            settings.llmMaxConcurrency(),
            llmBudget.rejected(),
            projectHeld,
            settings.projectMaxConcurrency(),
            projectRejected.get());
    }

    private boolean tryAcquireWorkspaceInternal()
    {
        long usedMb = workspaceUsedMb();
        int diskLimit = settings.workspaceMaxDiskMb();
        if (usedMb >= diskLimit)
        {
            workspaceBudget.recordReject();
            log.info("工作区磁盘占用已达上限, usedMb={}, limitMb={}", usedMb, diskLimit);
            return false;
        }
        return workspaceBudget.tryAcquire(settings.workspaceMaxCount());
    }

    long workspaceUsedMb()
    {
        Path root = workspaceManager.getWorkspaceRoot();
        if (root == null || !Files.isDirectory(root))
        {
            return 0L;
        }
        try
        {
            final AtomicLong total = new AtomicLong();
            Files.walkFileTree(root, new SimpleFileVisitor<>()
            {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                {
                    total.addAndGet(attrs.size());
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc)
                {
                    return FileVisitResult.CONTINUE;
                }
            });
            return Math.max(0L, total.get() / (1024L * 1024L));
        }
        catch (IOException ex)
        {
            log.warn("统计工作区磁盘占用失败，按 0 处理: {}", ex.toString());
            return 0L;
        }
    }
}
