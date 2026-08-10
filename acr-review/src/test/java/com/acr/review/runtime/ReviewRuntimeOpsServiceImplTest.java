package com.acr.review.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import com.acr.review.mapper.ReviewRuntimeStatsMapper;
import com.acr.review.scheduling.IReviewRuntimeStatusService;
import com.acr.review.scheduling.ReviewResourceBudgetStatus;
import com.acr.review.scheduling.ReviewRuntimeStatus;

class ReviewRuntimeOpsServiceImplTest
{
    @Test
    void overviewAggregatesTaskResourceDeliveryCounts()
    {
        ReviewRuntimeStatsMapper statsMapper = mock(ReviewRuntimeStatsMapper.class);
        IReviewRuntimeStatusService runtimeStatusService = mock(IReviewRuntimeStatusService.class);
        ReviewRuntimeAlertService alertService = mock(ReviewRuntimeAlertService.class);
        ReviewRuntimeAlertSettings settings = mock(ReviewRuntimeAlertSettings.class);
        when(settings.pendingAgeMinutes()).thenReturn(30);
        when(settings.deliveryPendingAgeMinutes()).thenReturn(20);
        when(settings.budgetSaturatedMinutes()).thenReturn(10);
        when(settings.failureRateWindowMinutes()).thenReturn(60);
        when(settings.failureRatePercent()).thenReturn(40);
        when(settings.drainTimeoutSeconds()).thenReturn(60);

        when(statsMapper.countTasksByStatus(org.mockito.ArgumentMatchers.any())).thenReturn(List.of(
            Map.of("status", "PENDING", "cnt", 3L),
            Map.of("status", "RETRYING", "cnt", 2L),
            Map.of("status", "RUNNING", "cnt", 1L),
            Map.of("status", "SUPERSEDED", "cnt", 4L)));
        when(statsMapper.selectOldestPendingTask()).thenReturn(Map.of("taskId", 11L, "ageSeconds", 120L));
        when(statsMapper.countTerminalTasksSinceHours(24)).thenReturn(List.of(
            Map.of("status", "SUCCESS", "cnt", 8L),
            Map.of("status", "FAILED", "cnt", 2L)));
        when(statsMapper.countRetryEventsSinceHours(24)).thenReturn(5L);
        when(statsMapper.countTimeoutEventsSinceHours(24)).thenReturn(1L);
        when(statsMapper.countDeliveriesByStatus("PENDING")).thenReturn(6L);
        when(statsMapper.countDeliveriesByStatus("MANUAL")).thenReturn(2L);
        when(statsMapper.selectOldestPendingDelivery()).thenReturn(Map.of("deliveryId", 21L, "ageSeconds", 90L));
        when(runtimeStatusService.snapshot()).thenReturn(new ReviewRuntimeStatus(
            7, 2, 4, 64, 3, 1, 1, 2, 64, 0,
            new ReviewResourceBudgetStatus(1, 4, 0, 10, 10240, 1, 2, 0, 2, 4, 0, 1, 2, 0)));
        when(alertService.evaluateNow()).thenReturn(List.of());

        ReviewRuntimeOpsServiceImpl service = new ReviewRuntimeOpsServiceImpl(
            statsMapper, runtimeStatusService, alertService, settings);
        ReviewRuntimeOverview overview = service.getOverview();

        assertEquals(3L, overview.getTask().getPendingCount());
        assertEquals(2L, overview.getTask().getRetryingCount());
        assertEquals(1L, overview.getTask().getRunningCount());
        assertEquals(4L, overview.getTask().getSupersededCount());
        assertEquals(11L, overview.getTask().getOldestPendingTaskId());
        assertEquals(120L, overview.getTask().getOldestPendingAgeSeconds());
        assertEquals(8L, overview.getTask().getTerminalRatio24h().get("成功"));
        assertEquals(5L, overview.getTask().getRetryCount24h());
        assertEquals(1L, overview.getTask().getTimeoutCount24h());
        assertEquals(7, overview.getResource().getReviewQueueDepth());
        assertEquals(3L, overview.getResource().getReviewRejectedCount());
        assertEquals(6L, overview.getDelivery().getPendingCount());
        assertEquals(2L, overview.getDelivery().getManualCount());
        assertEquals(21L, overview.getDelivery().getOldestPendingDeliveryId());
    }
}
