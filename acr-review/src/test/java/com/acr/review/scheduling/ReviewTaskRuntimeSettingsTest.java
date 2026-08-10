package com.acr.review.scheduling;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.Test;
import com.acr.system.service.ISysConfigService;

class ReviewTaskRuntimeSettingsTest
{
    private final ISysConfigService configService = mock(ISysConfigService.class);
    private final ReviewTaskRuntimeSettings settings = new ReviewTaskRuntimeSettings(configService);

    @Test
    void boundsUnsafeRuntimeValues()
    {
        when(configService.selectConfigByKey(ReviewTaskRuntimeSettings.CONFIG_SCAN_INTERVAL_SECONDS))
            .thenReturn("0");
        when(configService.selectConfigByKey(ReviewTaskRuntimeSettings.CONFIG_LEASE_SECONDS))
            .thenReturn("999999");

        assertEquals(1, settings.scanIntervalSeconds());
        assertEquals(7200, settings.leaseSeconds());
    }

    @Test
    void usesDefaultsForInvalidValuesAndCapsExponentialBackoff()
    {
        when(configService.selectConfigByKey(ReviewTaskRuntimeSettings.CONFIG_SCAN_BATCH_SIZE))
            .thenReturn("not-a-number");
        when(configService.selectConfigByKey(ReviewTaskRuntimeSettings.CONFIG_RETRY_BASE_DELAY_SECONDS))
            .thenReturn("30");
        when(configService.selectConfigByKey(ReviewTaskRuntimeSettings.CONFIG_RETRY_MAX_DELAY_SECONDS))
            .thenReturn("100");

        assertEquals(64, settings.scanBatchSize());
        assertEquals(100, settings.retryDelaySeconds(10));
        assertEquals(4, settings.executorPoolSize());
        assertEquals(64, settings.executorQueueCapacity());
        assertEquals(2, settings.projectMaxConcurrency());
        assertEquals(2, settings.ocrMaxConcurrency());
        assertEquals(4, settings.llmMaxConcurrency());
    }
}
