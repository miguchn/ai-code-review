package com.acr.framework.config;

import java.util.concurrent.ThreadPoolExecutor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;

/**
 * 异步线程池队列深度等指标，供 Prometheus 告警（堆积、延迟）使用。
 */
@Configuration
public class AcrExecutorMetricsConfiguration
{
    @Bean
    public MeterBinder acrThreadPoolMetricsBinder(ObjectProvider<ThreadPoolTaskExecutor> threadPoolProvider)
    {
        return registry -> {
            ThreadPoolTaskExecutor exec = threadPoolProvider.getIfAvailable();
            if (exec == null || exec.getThreadPoolExecutor() == null)
            {
                return;
            }
            ThreadPoolExecutor tpe = exec.getThreadPoolExecutor();
            Gauge.builder("acr.executor.queue.depth", tpe, e -> e.getQueue().size())
                .tag("name", "threadPoolTaskExecutor")
                .register(registry);
            Gauge.builder("acr.executor.active.threads", tpe, e -> (double) e.getActiveCount())
                .tag("name", "threadPoolTaskExecutor")
                .register(registry);
            Gauge.builder("acr.executor.pool.size", tpe, e -> (double) e.getPoolSize())
                .tag("name", "threadPoolTaskExecutor")
                .register(registry);
        };
    }
}
