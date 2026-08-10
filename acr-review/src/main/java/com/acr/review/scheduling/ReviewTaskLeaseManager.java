package com.acr.review.scheduling;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import com.acr.review.domain.ReviewTask;
import com.acr.review.mapper.ReviewTaskMapper;

/** 在外部阻塞调用期间续租；租约比较和延长均由数据库时钟完成。 */
@Component
public class ReviewTaskLeaseManager
{
    private static final Logger log = LoggerFactory.getLogger(ReviewTaskLeaseManager.class);

    private final ReviewTaskMapper taskMapper;
    private final ReviewTaskRuntimeSettings settings;
    private final ScheduledExecutorService scheduler;

    public ReviewTaskLeaseManager(ReviewTaskMapper taskMapper,
                                  ReviewTaskRuntimeSettings settings,
                                  @Qualifier("reviewTaskControlScheduler") ScheduledExecutorService scheduler)
    {
        this.taskMapper = taskMapper;
        this.settings = settings;
        this.scheduler = scheduler;
    }

    public LeaseHandle start(ReviewTask task)
    {
        int heartbeatSeconds = settings.heartbeatSeconds();
        int leaseSeconds = settings.leaseSeconds();
        AtomicBoolean lost = new AtomicBoolean(false);
        ScheduledFuture<?> future = scheduler.scheduleAtFixedRate(() -> {
            try
            {
                int renewed = taskMapper.renewTaskLease(task.getTaskId(), task.getExecutionEpoch(),
                    task.getLeaseOwner(), leaseSeconds);
                if (renewed != 1 && lost.compareAndSet(false, true))
                {
                    log.warn("审查任务租约续期被拒绝，后续写入将由 epoch fencing 拦截, taskId={}, epoch={}",
                        task.getTaskId(), task.getExecutionEpoch());
                }
            }
            catch (RuntimeException ex)
            {
                log.warn("审查任务租约续期异常，等待下一次心跳或恢复扫描, taskId={}", task.getTaskId(), ex);
            }
        }, heartbeatSeconds, heartbeatSeconds, TimeUnit.SECONDS);
        return new LeaseHandle(future, lost);
    }

    public static final class LeaseHandle implements AutoCloseable
    {
        private final ScheduledFuture<?> future;
        private final AtomicBoolean lost;

        private LeaseHandle(ScheduledFuture<?> future, AtomicBoolean lost)
        {
            this.future = future;
            this.lost = lost;
        }

        public boolean isLost()
        {
            return lost.get();
        }

        @Override
        public void close()
        {
            future.cancel(false);
        }
    }
}
