package com.acr.review.delivery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.Test;
import com.acr.system.service.ISysConfigService;

class ReviewDeliveryRuntimeSettingsTest
{
    @Test
    void appliesDefaultsBoundsAndExponentialBackoff()
    {
        ISysConfigService config = mock(ISysConfigService.class);
        when(config.selectConfigByKey(ReviewDeliveryRuntimeSettings.CONFIG_SCAN_BATCH_SIZE)).thenReturn("9999");
        when(config.selectConfigByKey(ReviewDeliveryRuntimeSettings.CONFIG_RETRY_BASE_DELAY_SECONDS)).thenReturn("10");
        when(config.selectConfigByKey(ReviewDeliveryRuntimeSettings.CONFIG_RETRY_MAX_DELAY_SECONDS)).thenReturn("25");
        ReviewDeliveryRuntimeSettings settings = new ReviewDeliveryRuntimeSettings(config);

        assertEquals(10, settings.scanIntervalSeconds());
        assertEquals(500, settings.scanBatchSize());
        assertEquals(10, settings.retryDelaySeconds(0));
        assertEquals(20, settings.retryDelaySeconds(1));
        assertEquals(25, settings.retryDelaySeconds(2));
    }
}
