package com.acr.review.delivery;

import java.util.List;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;
import com.acr.review.mapper.ReviewDeliveryRecordMapper;
import com.acr.review.runtime.ReviewRuntimeAlertSettings;
import com.acr.review.scheduling.ReviewDrainSupport;
import com.acr.review.scheduling.ReviewTaskWorkerIdentity;
import com.acr.review.service.IReviewDeliveryService;

/** 数据库驱动的投递调度器；扫描用于恢复，事件唤醒只用于降低延迟。 */
@Component
public class ReviewDeliveryDispatcher implements SmartLifecycle
{
    private static final Logger log = LoggerFactory.getLogger(ReviewDeliveryDispatcher.class);

    private final ReviewDeliveryRecordMapper deliveryMapper;
    private final ReviewDeliveryRuntimeSettings settings;
    private final ReviewRuntimeAlertSettings alertSettings;
    private final IReviewDeliveryService deliveryService;
    private final ScheduledExecutorService controlScheduler;
    private final ThreadPoolExecutor workerExecutor;
    private final String leaseOwner;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private volatile ScheduledFuture<?> nextScan;

    public ReviewDeliveryDispatcher(ReviewDeliveryRecordMapper deliveryMapper,
                                    ReviewDeliveryRuntimeSettings settings,
                                    ReviewRuntimeAlertSettings alertSettings,
                                    IReviewDeliveryService deliveryService,
                                    ReviewTaskWorkerIdentity workerIdentity,
                                    @Qualifier("reviewTaskControlScheduler") ScheduledExecutorService controlScheduler,
                                    @Qualifier("reviewDeliveryWorkerExecutor") ThreadPoolExecutor workerExecutor)
    {
        this.deliveryMapper = deliveryMapper;
        this.settings = settings;
        this.alertSettings = alertSettings;
        this.deliveryService = deliveryService;
        this.controlScheduler = controlScheduler;
        this.workerExecutor = workerExecutor;
        this.leaseOwner = workerIdentity.owner() + ":delivery";
    }

    public void wake(Long deliveryId)
    {
        if (deliveryId != null && running.get())
        {
            dispatch(deliveryId);
        }
    }

    void scanOnce()
    {
        List<Long> candidates = deliveryMapper.selectDispatchableDeliveryIds(settings.scanBatchSize());
        if (candidates == null)
        {
            return;
        }
        for (Long deliveryId : candidates)
        {
            dispatch(deliveryId);
        }
    }

    private void dispatch(Long deliveryId)
    {
        if (deliveryMapper.claimDelivery(deliveryId, leaseOwner, settings.leaseSeconds()) != 1)
        {
            return;
        }
        try
        {
            workerExecutor.execute(() -> {
                try
                {
                    deliveryService.executeClaimedDelivery(deliveryId, leaseOwner);
                }
                catch (RuntimeException ex)
                {
                    log.error("投递工作节点执行异常，租约到期后将由扫描恢复, deliveryId={}", deliveryId);
                }
            });
        }
        catch (RejectedExecutionException ex)
        {
            deliveryMapper.releaseDeliveryLease(deliveryId, leaseOwner);
            log.warn("投递执行器已饱和，记录保留在数据库等待下次扫描, deliveryId={}", deliveryId);
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
                log.error("投递恢复扫描异常", ex);
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
        ReviewDrainSupport.drain("投递调度", workerExecutor, alertSettings, () -> {
            int expired = deliveryMapper.expireWorkerLeases(leaseOwner);
            log.info("投递调度停机超时，已将本实例租约置过期, worker={}, expired={}", leaseOwner, expired);
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
        return Integer.MAX_VALUE - 90;
    }
}
