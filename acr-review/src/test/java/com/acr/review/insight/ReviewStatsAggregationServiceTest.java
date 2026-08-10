package com.acr.review.insight;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.sql.Date;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.acr.review.mapper.ReviewCommitFactMapper;
import com.acr.review.mapper.ReviewMemberStatsDailyMapper;
import com.acr.review.mapper.ReviewStatsDailyMapper;
import com.acr.review.mapper.ReviewStatsSourceMapper;

@ExtendWith(MockitoExtension.class)
class ReviewStatsAggregationServiceTest
{
    @Mock
    private ReviewStatsSourceMapper sourceMapper;
    @Mock
    private ReviewStatsDailyMapper dailyMapper;
    @Mock
    private ReviewCommitFactMapper commitFactMapper;
    @Mock
    private ReviewMemberStatsDailyMapper memberStatsMapper;

    private ReviewStatsAggregationService service;

    @BeforeEach
    void setUp()
    {
        service = new ReviewStatsAggregationService(sourceMapper, dailyMapper, commitFactMapper, memberStatsMapper);
    }

    @Test
    void percentile95_and_ratios_matchDictionary()
    {
        List<Long> durations = List.of(10L, 20L, 30L, 40L, 50L, 60L, 70L, 80L, 90L, 100L,
            110L, 120L, 130L, 140L, 150L, 160L, 170L, 180L, 190L, 200L);
        assertEquals(190L, InsightMetrics.percentile95(durations));
        assertEquals(0d, InsightMetrics.percentile95(List.of()));
        assertEquals(0.708, InsightMetrics.ratio(17, 24), 0.001);
        assertEquals(0d, InsightMetrics.ratio(1, 0));
    }

    @Test
    void buildRow_computesSuccessRateCoverageAndIssueCounts()
    {
        ReviewStatsDaily row = ReviewStatsAggregationService.buildRowForTest(
            1L, LocalDate.of(2026, 8, 1),
            List.of(1000L, 2000L, 3000L, 4000L),
            17, 7, 2, 17,
            54, 6, 20, 18, 10,
            5, 3, 1,
            17, 17,
            24, 3);
        assertEquals(24, row.getTaskTotal());
        assertEquals(17, row.getTaskSuccess());
        assertEquals(17, row.getTaskCovered());
        assertEquals(4000L, row.getDurationP95Ms());
        assertEquals(54, row.getIssueNew());
        assertEquals(6, row.getIssueCritical());
        assertEquals(0.708, InsightMetrics.ratio(row.getTaskSuccess(), row.getTaskTotal()), 0.001);
        assertEquals(17d / 24d, InsightMetrics.ratio(row.getTaskCovered(), row.getEventAccepted()), 0.001);
    }

    @Test
    void recalculateRange_isIdempotent_sameUpsertPayload()
    {
        LocalDate day = LocalDate.of(2026, 8, 9);
        Date sqlDay = Date.valueOf(day);
        when(sourceMapper.selectActiveProjectIds()).thenReturn(List.of(9L));
        when(sourceMapper.selectTaskAggByDay(eq(9L), any(), any())).thenReturn(List.of(map(
            "stat_date", sqlDay, "task_total", 10, "task_success", 7, "task_failed", 3, "task_push", 1)));
        when(sourceMapper.selectTaskCoveredAggByDay(eq(9L), any(), any())).thenReturn(List.of(map(
            "stat_date", sqlDay, "task_covered", 6)));
        when(sourceMapper.selectIssueNewAggByDay(eq(9L), any(), any())).thenReturn(List.of(map(
            "stat_date", sqlDay, "issue_new", 8, "issue_critical", 1, "issue_high", 2,
            "issue_medium", 3, "issue_low", 2)));
        when(sourceMapper.selectIssueClosedAggByDay(eq(9L), any(), any())).thenReturn(List.of());
        when(sourceMapper.selectIssueConfirmedAggByDay(eq(9L), any(), any())).thenReturn(List.of());
        when(sourceMapper.selectDeliveryAggByDay(eq(9L), any(), any())).thenReturn(List.of(map(
            "stat_date", sqlDay, "delivery_total", 6, "delivery_success", 6)));
        when(sourceMapper.selectEventAggByDay(eq(9L), any(), any())).thenReturn(List.of(map(
            "stat_date", sqlDay, "event_accepted", 8, "event_ignored", 1)));
        when(sourceMapper.selectSuccessDurations(eq(9L), any())).thenReturn(List.of(100L, 200L, 300L, 400L));
        when(commitFactMapper.selectCommitCountByAuthorDay(eq(9L), any(), any())).thenReturn(List.of());
        when(sourceMapper.selectTasksReviewedByAuthorDay(eq(9L), any(), any())).thenReturn(List.of());
        when(sourceMapper.selectIssuesNewByAuthorDay(eq(9L), any(), any())).thenReturn(List.of());
        when(sourceMapper.selectIssuesOpenByAuthorAsOf(eq(9L), any())).thenReturn(List.of());

        service.recalculateRange(day, day);
        service.recalculateRange(day, day);

        ArgumentCaptor<ReviewStatsDaily> captor = ArgumentCaptor.forClass(ReviewStatsDaily.class);
        verify(dailyMapper, times(2)).upsert(captor.capture());
        List<ReviewStatsDaily> values = captor.getAllValues();
        assertTrue(ReviewStatsAggregationService.sameMetrics(values.get(0), values.get(1)));
        assertEquals(7, values.get(0).getTaskSuccess());
        assertEquals(6, values.get(0).getTaskCovered());
        assertEquals(400L, values.get(0).getDurationP95Ms());
        assertEquals(8, values.get(0).getIssueNew());
        assertEquals(1, values.get(0).getIssueCritical());
    }

    @Test
    void recalculateRange_rebuildsMemberDailyWithAuthorKeyAndWeakMatch()
    {
        LocalDate day = LocalDate.of(2026, 8, 9);
        Date sqlDay = Date.valueOf(day);
        when(sourceMapper.selectActiveProjectIds()).thenReturn(List.of(9L));
        when(sourceMapper.selectTaskAggByDay(eq(9L), any(), any())).thenReturn(List.of());
        when(sourceMapper.selectTaskCoveredAggByDay(eq(9L), any(), any())).thenReturn(List.of());
        when(sourceMapper.selectIssueNewAggByDay(eq(9L), any(), any())).thenReturn(List.of());
        when(sourceMapper.selectIssueClosedAggByDay(eq(9L), any(), any())).thenReturn(List.of());
        when(sourceMapper.selectIssueConfirmedAggByDay(eq(9L), any(), any())).thenReturn(List.of());
        when(sourceMapper.selectDeliveryAggByDay(eq(9L), any(), any())).thenReturn(List.of());
        when(sourceMapper.selectEventAggByDay(eq(9L), any(), any())).thenReturn(List.of());
        when(sourceMapper.selectSuccessDurations(eq(9L), any())).thenReturn(List.of());

        when(commitFactMapper.selectCommitCountByAuthorDay(eq(9L), any(), any())).thenReturn(List.of(map(
            "stat_date", sqlDay,
            "author_key", "alice@example.com",
            "author_name", "Alice",
            "commit_count", 3)));
        // 弱匹配：pr_author 与 author_key 字符串相等（邮箱键）
        when(sourceMapper.selectTasksReviewedByAuthorDay(eq(9L), any(), any())).thenReturn(List.of(map(
            "stat_date", sqlDay, "author_key", "alice@example.com", "tasks_reviewed", 2)));
        when(sourceMapper.selectIssuesNewByAuthorDay(eq(9L), any(), any())).thenReturn(List.of(map(
            "stat_date", sqlDay, "author_key", "alice@example.com", "issues_new", 5)));
        when(sourceMapper.selectIssuesOpenByAuthorAsOf(eq(9L), any())).thenReturn(List.of(map(
            "author_key", "alice@example.com", "issues_open", 4)));

        service.recalculateRange(day, day);

        ArgumentCaptor<ReviewMemberStatsDaily> captor = ArgumentCaptor.forClass(ReviewMemberStatsDaily.class);
        verify(memberStatsMapper).upsert(captor.capture());
        ReviewMemberStatsDaily row = captor.getValue();
        assertEquals("alice@example.com", row.getAuthorKey());
        assertEquals("Alice", row.getAuthorName());
        assertEquals(3, row.getCommitCount());
        assertEquals(2, row.getTasksReviewed());
        assertEquals(5, row.getIssuesNew());
        assertEquals(4, row.getIssuesOpen());
    }

    private static Map<String, Object> map(Object... kv)
    {
        Map<String, Object> map = new HashMap<>();
        for (int i = 0; i < kv.length; i += 2)
        {
            map.put(String.valueOf(kv[i]), kv[i + 1]);
        }
        return map;
    }
}
