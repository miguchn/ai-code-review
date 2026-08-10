package com.acr.review.scheduling;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.acr.review.delivery.ReviewDeliveryRuntimeSettings;

/**
 * acr-review 自有执行面：
 * - controlScheduler：扫描/心跳（轻量）
 * - reviewTaskWorkerExecutor：有界审查执行池（禁止 CallerRuns）
 * - reviewDeliveryWorkerExecutor：有界投递执行池（与审查主池隔离）
 */
@Configuration
public class ReviewTaskSchedulingConfiguration
{
    @Bean(name = "reviewTaskControlScheduler", destroyMethod = "shutdown")
    public ScheduledExecutorService reviewTaskControlScheduler()
    {
        AtomicInteger sequence = new AtomicInteger();
        return Executors.newScheduledThreadPool(2, runnable -> {
            Thread thread = new Thread(runnable, "review-task-control-" + sequence.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        });
    }

    @Bean(name = "reviewTaskRejectedCount")
    public AtomicLong reviewTaskRejectedCount()
    {
        return new AtomicLong();
    }

    @Bean(name = "reviewDeliveryRejectedCount")
    public AtomicLong reviewDeliveryRejectedCount()
    {
        return new AtomicLong();
    }

    @Bean(name = "reviewTaskWorkerExecutor", destroyMethod = "shutdown")
    public ThreadPoolExecutor reviewTaskWorkerExecutor(ReviewTaskRuntimeSettings settings,
                                                       @org.springframework.beans.factory.annotation.Qualifier("reviewTaskRejectedCount")
                                                       AtomicLong rejectedCount)
    {
        int poolSize = settings.executorPoolSize();
        int queueCapacity = settings.executorQueueCapacity();
        AtomicInteger sequence = new AtomicInteger();
        return new ThreadPoolExecutor(
            poolSize,
            poolSize,
            60L,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(queueCapacity),
            runnable -> {
                Thread thread = new Thread(runnable, "review-task-worker-" + sequence.incrementAndGet());
                thread.setDaemon(true);
                return thread;
            },
            (runnable, executor) -> {
                rejectedCount.incrementAndGet();
                throw new RejectedExecutionException("审查执行池队列已满");
            });
    }

    @Bean(name = "reviewDeliveryWorkerExecutor", destroyMethod = "shutdown")
    public ThreadPoolExecutor reviewDeliveryWorkerExecutor(ReviewDeliveryRuntimeSettings settings,
                                                           @org.springframework.beans.factory.annotation.Qualifier("reviewDeliveryRejectedCount")
                                                           AtomicLong rejectedCount)
    {
        int poolSize = settings.executorPoolSize();
        int queueCapacity = settings.executorQueueCapacity();
        AtomicInteger sequence = new AtomicInteger();
        return new ThreadPoolExecutor(
            poolSize,
            poolSize,
            60L,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(queueCapacity),
            runnable -> {
                Thread thread = new Thread(runnable, "review-delivery-worker-" + sequence.incrementAndGet());
                thread.setDaemon(true);
                return thread;
            },
            (runnable, executor) -> {
                rejectedCount.incrementAndGet();
                throw new RejectedExecutionException("投递执行池队列已满");
            });
    }
}
