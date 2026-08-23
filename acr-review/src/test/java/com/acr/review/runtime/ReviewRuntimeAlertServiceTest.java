package com.acr.review.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.acr.review.mapper.ReviewRuntimeStatsMapper;
import com.acr.review.mapper.ReviewProjectMapper;
import com.acr.review.engine.OcrEngineAvailability;
import com.acr.review.engine.OcrEngineAvailabilityService;
import com.acr.review.scheduling.IReviewRuntimeStatusService;
import com.acr.review.scheduling.ReviewResourceBudgetStatus;
import com.acr.review.scheduling.ReviewRuntimeStatus;

class ReviewRuntimeAlertServiceTest
{
    private ReviewRuntimeAlertSettings settings;
    private ReviewRuntimeStatsMapper statsMapper;
    private IReviewRuntimeStatusService runtimeStatusService;
    private ReviewRuntimeAlertService service;
    private OcrEngineAvailabilityService ocrAvailabilityService;
    private ReviewProjectMapper projectMapper;

    @BeforeEach
    void setUp()
    {
        settings = mock(ReviewRuntimeAlertSettings.class);
        statsMapper = mock(ReviewRuntimeStatsMapper.class);
        runtimeStatusService = mock(IReviewRuntimeStatusService.class);
        ocrAvailabilityService = mock(OcrEngineAvailabilityService.class);
        projectMapper = mock(ReviewProjectMapper.class);
        when(settings.pendingAgeMinutes()).thenReturn(30);
        when(settings.deliveryPendingAgeMinutes()).thenReturn(20);
        when(settings.budgetSaturatedMinutes()).thenReturn(10);
        when(settings.failureRateWindowMinutes()).thenReturn(60);
        when(settings.failureRatePercent()).thenReturn(40);
        when(settings.alertScanIntervalSeconds()).thenReturn(30);
        when(ocrAvailabilityService.probe()).thenReturn(
            new OcrEngineAvailability(true, "ocr", "open-code-review v1", "OCR 引擎可用", new Date()));
        service = new ReviewRuntimeAlertService(settings, statsMapper, runtimeStatusService,
            mock(ScheduledExecutorService.class), ocrAvailabilityService, projectMapper);
    }

    @Test
    void pendingOverageProducesActionableAlertWithTaskId()
    {
        when(statsMapper.selectOldestPendingTask()).thenReturn(Map.of("taskId", 88L, "ageSeconds", 2400L));
        when(statsMapper.selectOldestPendingDelivery()).thenReturn(null);
        when(statsMapper.countTerminalOutcomesSinceMinutes(60)).thenReturn(Map.of(
            "successCount", 10, "failedCount", 1, "cancelledCount", 0));

        List<ReviewRuntimeAlert> alerts = service.evaluate(emptyStatus(), new Date());

        assertEquals(1, alerts.size());
        ReviewRuntimeAlert alert = alerts.get(0);
        assertEquals(ReviewRuntimeConstants.ALERT_PENDING_OVERAGE, alert.getCode());
        assertEquals(88L, alert.getTaskId());
        assertTrue(alert.getMessage().contains("88"));
        assertTrue(alert.getAction().contains("任务"));
    }

    @Test
    void deliveryOverageProducesAlertWithDeliveryId()
    {
        when(statsMapper.selectOldestPendingTask()).thenReturn(null);
        when(statsMapper.selectOldestPendingDelivery()).thenReturn(
            Map.of("deliveryId", 55L, "taskId", 9L, "ageSeconds", 1800L));
        when(statsMapper.countTerminalOutcomesSinceMinutes(60)).thenReturn(Map.of(
            "successCount", 10, "failedCount", 1, "cancelledCount", 0));

        List<ReviewRuntimeAlert> alerts = service.evaluate(emptyStatus(), new Date());

        assertEquals(1, alerts.size());
        assertEquals(55L, alerts.get(0).getDeliveryId());
        assertTrue(alerts.get(0).getMessage().contains("55"));
        assertTrue(alerts.get(0).getAction().contains("投递"));
    }

    @Test
    void budgetSaturationRequiresContinuousWindow()
    {
        ReviewRuntimeStatus saturated = new ReviewRuntimeStatus(
            0, 0, 4, 64, 0, 0, 0, 2, 64, 0,
            new ReviewResourceBudgetStatus(4, 4, 0, 100, 10240, 2, 2, 0, 4, 4, 0, 1, 2, 0));
        Date t0 = new Date(1_000_000L);
        assertTrue(service.evaluate(saturated, t0).isEmpty());

        Date t1 = new Date(t0.getTime() + 11 * 60_000L);
        when(statsMapper.selectOldestPendingTask()).thenReturn(null);
        when(statsMapper.selectOldestPendingDelivery()).thenReturn(null);
        when(statsMapper.countTerminalOutcomesSinceMinutes(60)).thenReturn(Map.of(
            "successCount", 0, "failedCount", 0, "cancelledCount", 0));
        List<ReviewRuntimeAlert> alerts = service.evaluate(saturated, t1);
        assertEquals(1, alerts.size());
        assertEquals(ReviewRuntimeConstants.ALERT_BUDGET_SATURATED, alerts.get(0).getCode());
        assertFalse(alerts.get(0).getAction().isBlank());
    }

    @Test
    void unavailableOcrWithEnabledProjectsProducesWarning()
    {
        when(ocrAvailabilityService.probe()).thenReturn(new OcrEngineAvailability(
            false, "ocr", null,
            "未检测到 OCR 引擎（命令：ocr），请安装 open-code-review 或检查 ACR_OCR_EXECUTABLE 配置",
            new Date()));
        when(projectMapper.countEnabledOcrProjects()).thenReturn(2);

        List<ReviewRuntimeAlert> alerts = service.evaluate(emptyStatus(), new Date());

        assertEquals(1, alerts.size());
        assertEquals(ReviewRuntimeConstants.ALERT_OCR_ENGINE_UNAVAILABLE, alerts.get(0).getCode());
        assertEquals("warning", alerts.get(0).getSeverity());
        assertTrue(alerts.get(0).getMessage().contains("2 个"));
    }

    @Test
    void unavailableOcrWithoutEnabledProjectsDoesNotAlert()
    {
        when(ocrAvailabilityService.probe()).thenReturn(new OcrEngineAvailability(
            false, "ocr", null, "未检测到 OCR 引擎", new Date()));
        when(projectMapper.countEnabledOcrProjects()).thenReturn(0);

        assertTrue(service.evaluate(emptyStatus(), new Date()).isEmpty());
    }

    private static ReviewRuntimeStatus emptyStatus()
    {
        return new ReviewRuntimeStatus(
            0, 0, 4, 64, 0, 0, 0, 2, 64, 0,
            new ReviewResourceBudgetStatus(0, 4, 0, 0, 10240, 0, 2, 0, 0, 4, 0, 0, 2, 0));
    }
}
