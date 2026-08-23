package com.acr.review.engine;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.io.IOException;
import java.time.Clock;
import org.junit.jupiter.api.Test;
import com.acr.review.engine.config.ReviewEngineProperties;

class OcrEngineAvailabilityServiceTest
{
    @Test
    void cachesAvailableProbeForSixtySecondsAndRefreshesAfterExpiry() throws Exception
    {
        ExternalProcessRunner processRunner = mock(ExternalProcessRunner.class);
        Clock clock = mock(Clock.class);
        when(clock.millis()).thenReturn(1_000L, 30_000L, 61_001L);
        when(processRunner.execute(anyList(), isNull(), isNull(),
            eq(OcrEngineAvailabilityService.PROBE_TIMEOUT_SECONDS),
            eq(OcrEngineAvailabilityService.PROBE_MAX_OUTPUT_BYTES)))
            .thenReturn(new ExternalProcessRunner.ProcessExecution(
                0, 12, "open-code-review v1.2.3", "", false));

        OcrEngineAvailabilityService service = new OcrEngineAvailabilityService(
            processRunner, new ReviewEngineProperties(), clock);

        OcrEngineAvailability first = service.probe();
        OcrEngineAvailability cached = service.probe();
        OcrEngineAvailability refreshed = service.probe();

        assertTrue(first.available());
        assertTrue(first.version().contains("1.2.3"));
        assertSame(first, cached);
        assertTrue(refreshed.available());
        verify(processRunner, times(2)).execute(anyList(), isNull(), isNull(),
            eq(5), eq(16_384));
    }

    @Test
    void cachesMissingExecutableWithoutThrowingToCaller() throws Exception
    {
        ExternalProcessRunner processRunner = mock(ExternalProcessRunner.class);
        Clock clock = mock(Clock.class);
        when(clock.millis()).thenReturn(1_000L, 10_000L);
        when(processRunner.execute(anyList(), isNull(), isNull(), eq(5), eq(16_384)))
            .thenThrow(new IOException("Cannot run program \"ocr\": error=2, No such file or directory"));

        OcrEngineAvailabilityService service = new OcrEngineAvailabilityService(
            processRunner, new ReviewEngineProperties(), clock);

        OcrEngineAvailability first = service.probe();
        OcrEngineAvailability cached = service.probe();

        assertFalse(first.available());
        assertTrue(first.message().contains("未检测到 OCR 引擎（命令：ocr）"));
        assertSame(first, cached);
        verify(processRunner).execute(anyList(), isNull(), isNull(), eq(5), eq(16_384));
    }
}
