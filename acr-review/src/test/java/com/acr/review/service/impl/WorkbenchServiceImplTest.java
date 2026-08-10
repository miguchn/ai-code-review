package com.acr.review.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import com.acr.common.utils.DateUtils;
import com.acr.common.utils.SecurityUtils;
import com.acr.review.delivery.ReviewDeliveryConstants;
import com.acr.review.domain.ReviewDeliveryRecord;
import com.acr.review.domain.ReviewIssue;
import com.acr.review.domain.ReviewIssueConstants;
import com.acr.review.domain.ReviewPipelineConstants;
import com.acr.review.domain.ReviewProject;
import com.acr.review.domain.ReviewTask;
import com.acr.review.domain.WorkbenchCard;
import com.acr.review.domain.WorkbenchConstants;
import com.acr.review.domain.WorkbenchModels;
import com.acr.review.domain.WorkbenchSummary;
import com.acr.review.domain.WorkbenchTrend;
import com.acr.review.domain.result.ReviewConclusionDailyStat;
import com.acr.review.service.IReviewDeliveryService;
import com.acr.review.service.IReviewIssueService;
import com.acr.review.service.IReviewProjectService;
import com.acr.review.service.IReviewRecordService;
import com.acr.review.service.IReviewTaskService;
import com.acr.system.domain.SysAiModelConfig;
import com.acr.system.service.ISysAiModelConfigService;

@ExtendWith(MockitoExtension.class)
class WorkbenchServiceImplTest
{
    @Mock
    private IReviewProjectService projectService;
    @Mock
    private IReviewIssueService issueService;
    @Mock
    private IReviewTaskService taskService;
    @Mock
    private IReviewRecordService recordService;
    @Mock
    private IReviewDeliveryService deliveryService;
    @Mock
    private ISysAiModelConfigService aiModelConfigService;

    private WorkbenchServiceImpl service;

    @BeforeEach
    void setUp()
    {
        service = new WorkbenchServiceImpl(projectService, issueService, taskService, recordService, deliveryService,
            aiModelConfigService);
    }

    @Test
    void getSummary_allPermissions_buildsSixCardsAndToday()
    {
        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class))
        {
            security.when(() -> SecurityUtils.hasPermi(any())).thenReturn(true);
            when(projectService.countReviewProjectList(any())).thenReturn(3);
            when(taskService.selectLatestTaskTime(any())).thenReturn(DateUtils.parseDate("2026-08-03 15:20:00"));
            when(issueService.countIssueList(any())).thenReturn(4, 1, 2, 3);
            when(recordService.countReviewRecordList(any())).thenReturn(1);
            when(taskService.countReviewTaskList(any())).thenReturn(5);
            when(deliveryService.countDeliveryList(any())).thenReturn(0);
            when(taskService.countTodayNewTasks(any())).thenReturn(5);
            when(taskService.countTodaySuccessTasks(any())).thenReturn(4);
            when(taskService.countTodayFailedTasks(any())).thenReturn(1);
            when(issueService.countClosedToday(any())).thenReturn(2);
            when(taskService.selectRecentTasks(any())).thenReturn(List.of(sampleTask()));

            WorkbenchSummary summary = service.getSummary();

            assertEquals(3, summary.getScope().getProjectCount());
            assertEquals("2026-08-03 15:20", summary.getScope().getLatestTaskTime());
            assertEquals(7, summary.getCards().size());
            assertEquals(WorkbenchConstants.CARD_ISSUE_AWAITING_CONFIRM, summary.getCards().get(0).getType());
            assertEquals(4, summary.getCards().get(0).getCount());
            assertEquals(ReviewIssueConstants.ORIGIN_NEW, summary.getCards().get(0).getQuery().get("origin"));
            assertEquals("本次变更", summary.getCards().get(0).getSubtitle());
            assertEquals(WorkbenchConstants.CARD_ISSUE_EXISTING_CONFIRM, summary.getCards().get(1).getType());
            assertEquals(1, summary.getCards().get(1).getCount());
            assertEquals("EXISTING", summary.getCards().get(1).getQuery().get("origin"));
            assertEquals(WorkbenchConstants.CARD_ISSUE_AWAITING_FIX, summary.getCards().get(2).getType());
            assertEquals(2, summary.getCards().get(2).getCount());
            assertFalse(summary.getCards().get(2).getQuery().containsKey("origin"));
            assertEquals(WorkbenchConstants.CARD_ISSUE_RECHECKING, summary.getCards().get(3).getType());
            assertEquals(3, summary.getCards().get(3).getCount());
            assertEquals("修复待验证", summary.getCards().get(3).getSubtitle());
            assertEquals(ReviewIssueConstants.STATUS_RECHECKING, summary.getCards().get(3).getQuery().get("status"));
            assertEquals(WorkbenchConstants.CARD_HIGH_RISK_CONCLUSION, summary.getCards().get(4).getType());
            assertEquals(WorkbenchConstants.CARD_DELIVERY_FAILED, summary.getCards().get(6).getType());
            assertEquals(0, summary.getCards().get(6).getCount());
            assertEquals(5, summary.getToday().getNewTasks());
            assertEquals(4, summary.getToday().getSuccessTasks());
            assertEquals(1, summary.getToday().getFailedTasks());
            assertEquals(2, summary.getToday().getClosedIssues());
            assertEquals(1, summary.getRecent().size());
            assertTrue(summary.getRecent().get(0).getTitle().contains("高风险"));
            assertTrue(summary.getRecent().get(0).getLink().contains("/review/task-detail/index/99"));
            assertEquals(ReviewPipelineConstants.CONCLUSION_BLOCK, summary.getRecent().get(0).getConclusion());
        }
    }

    @Test
    void getSummary_noBusinessListPermissions_emptyCardsAndNullTodayFields()
    {
        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class))
        {
            security.when(() -> SecurityUtils.hasPermi(any())).thenReturn(false);

            WorkbenchSummary summary = service.getSummary();

            assertNull(summary.getScope().getProjectCount());
            assertNull(summary.getScope().getLatestTaskTime());
            assertTrue(summary.getCards().isEmpty());
            assertNull(summary.getToday().getNewTasks());
            assertNull(summary.getToday().getClosedIssues());
            assertTrue(summary.getRecent().isEmpty());
        }
    }

    @Test
    void getSummary_withoutProjectList_projectCountNull_doesNotTriggerZeroGuide()
    {
        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class))
        {
            security.when(() -> SecurityUtils.hasPermi(WorkbenchConstants.PERM_PROJECT_LIST)).thenReturn(false);
            security.when(() -> SecurityUtils.hasPermi(WorkbenchConstants.PERM_TASK_LIST)).thenReturn(true);
            security.when(() -> SecurityUtils.hasPermi(WorkbenchConstants.PERM_ISSUE_LIST)).thenReturn(false);
            security.when(() -> SecurityUtils.hasPermi(WorkbenchConstants.PERM_RECORD_LIST)).thenReturn(false);
            security.when(() -> SecurityUtils.hasPermi(WorkbenchConstants.PERM_DELIVERY_LIST)).thenReturn(false);
            when(taskService.selectLatestTaskTime(any())).thenReturn(null);
            when(taskService.countReviewTaskList(any())).thenReturn(0);
            when(taskService.countTodayNewTasks(any())).thenReturn(0);
            when(taskService.countTodaySuccessTasks(any())).thenReturn(0);
            when(taskService.countTodayFailedTasks(any())).thenReturn(0);
            when(taskService.selectRecentTasks(any())).thenReturn(List.of());

            WorkbenchSummary summary = service.getSummary();

            assertNull(summary.getScope().getProjectCount());
            assertEquals(1, summary.getCards().size());
            assertEquals(WorkbenchConstants.CARD_TASK_FAILED, summary.getCards().get(0).getType());
            assertEquals(0, summary.getToday().getNewTasks());
            assertNull(summary.getToday().getClosedIssues());
        }
    }

    @Test
    void getSummary_issueCards_originSplitAndFixCoversAllOrigins()
    {
        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class))
        {
            security.when(() -> SecurityUtils.hasPermi(any())).thenAnswer(inv -> {
                String perm = inv.getArgument(0);
                return WorkbenchConstants.PERM_ISSUE_LIST.equals(perm);
            });
            when(issueService.countIssueList(any())).thenReturn(3, 1, 2, 4);
            when(issueService.countClosedToday(any())).thenReturn(0);

            WorkbenchSummary summary = service.getSummary();

            ArgumentCaptor<ReviewIssue> captor = ArgumentCaptor.forClass(ReviewIssue.class);
            verify(issueService, org.mockito.Mockito.times(4)).countIssueList(captor.capture());
            List<ReviewIssue> queries = captor.getAllValues();
            assertEquals(ReviewIssueConstants.STATUS_AWAITING_CONFIRM, queries.get(0).getStatus());
            assertEquals(ReviewIssueConstants.ORIGIN_NEW, queries.get(0).getOrigin());
            assertEquals(ReviewIssueConstants.STATUS_AWAITING_CONFIRM, queries.get(1).getStatus());
            assertEquals(ReviewIssueConstants.ORIGIN_EXISTING, queries.get(1).getOrigin());
            assertEquals(ReviewIssueConstants.STATUS_AWAITING_FIX, queries.get(2).getStatus());
            assertNull(queries.get(2).getOrigin());
            assertEquals(ReviewIssueConstants.STATUS_RECHECKING, queries.get(3).getStatus());

            assertEquals(4, summary.getCards().size());
            WorkbenchCard confirm = summary.getCards().get(0);
            assertEquals(3, confirm.getCount());
            assertEquals("AWAITING_CONFIRM", confirm.getQuery().get("status"));
            assertEquals("NEW", confirm.getQuery().get("origin"));

            WorkbenchCard existing = summary.getCards().get(1);
            assertEquals("AWAITING_CONFIRM", existing.getQuery().get("status"));
            assertEquals("EXISTING", existing.getQuery().get("origin"));
            assertEquals(WorkbenchConstants.CARD_ISSUE_RECHECKING, summary.getCards().get(3).getType());
            assertEquals(4, summary.getCards().get(3).getCount());

            WorkbenchCard fix = summary.getCards().get(2);
            assertEquals(2, fix.getCount());
            assertEquals("AWAITING_FIX", fix.getQuery().get("status"));
            assertFalse(fix.getQuery().containsKey("origin"));
        }
    }

    @Test
    void getSummary_highRiskCard_usesSevenDayWindowAndBlockConclusion()
    {
        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class))
        {
            security.when(() -> SecurityUtils.hasPermi(any())).thenAnswer(inv ->
                WorkbenchConstants.PERM_RECORD_LIST.equals(inv.getArgument(0)));
            when(recordService.countReviewRecordList(any())).thenReturn(2);

            WorkbenchSummary summary = service.getSummary();

            ArgumentCaptor<ReviewTask> captor = ArgumentCaptor.forClass(ReviewTask.class);
            verify(recordService).countReviewRecordList(captor.capture());
            ReviewTask query = captor.getValue();
            assertEquals(ReviewPipelineConstants.CONCLUSION_BLOCK, query.getReviewConclusion());

            LocalDate end = LocalDate.now(ZoneId.of("Asia/Shanghai"));
            LocalDate begin = end.minusDays(6);
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            assertEquals(begin.format(fmt), query.getParams().get("beginTime"));
            assertEquals(end.format(fmt), query.getParams().get("endTime"));

            WorkbenchCard card = summary.getCards().get(0);
            assertEquals(WorkbenchConstants.CARD_HIGH_RISK_CONCLUSION, card.getType());
            assertEquals(2, card.getCount());
            assertEquals("BLOCK", card.getQuery().get("reviewConclusion"));
            assertEquals(begin.format(fmt), card.getQuery().get("beginTime"));
            assertEquals(end.format(fmt), card.getQuery().get("endTime"));
        }
    }

    @Test
    void getSummary_failedTaskAndDeliveryFilters()
    {
        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class))
        {
            security.when(() -> SecurityUtils.hasPermi(any())).thenAnswer(inv -> {
                String p = inv.getArgument(0);
                return Set.of(WorkbenchConstants.PERM_TASK_LIST, WorkbenchConstants.PERM_DELIVERY_LIST).contains(p);
            });
            when(taskService.selectLatestTaskTime(any())).thenReturn(null);
            when(taskService.countReviewTaskList(any())).thenReturn(7);
            when(deliveryService.countDeliveryList(any())).thenReturn(3);
            when(taskService.countTodayNewTasks(any())).thenReturn(0);
            when(taskService.countTodaySuccessTasks(any())).thenReturn(0);
            when(taskService.countTodayFailedTasks(any())).thenReturn(0);
            when(taskService.selectRecentTasks(any())).thenReturn(List.of());

            WorkbenchSummary summary = service.getSummary();

            ArgumentCaptor<ReviewTask> taskCaptor = ArgumentCaptor.forClass(ReviewTask.class);
            verify(taskService).countReviewTaskList(taskCaptor.capture());
            assertEquals(ReviewPipelineConstants.TASK_FAILED, taskCaptor.getValue().getTaskStatus());

            ArgumentCaptor<ReviewDeliveryRecord> deliveryCaptor = ArgumentCaptor.forClass(ReviewDeliveryRecord.class);
            verify(deliveryService).countDeliveryList(deliveryCaptor.capture());
            assertEquals(ReviewDeliveryConstants.STATUS_MANUAL, deliveryCaptor.getValue().getDeliveryStatus());

            assertEquals(2, summary.getCards().size());
            assertEquals("FAILED", summary.getCards().get(0).getQuery().get("taskStatus"));
            assertEquals("MANUAL", summary.getCards().get(1).getQuery().get("deliveryStatus"));
        }
    }

    @Test
    void buildRecentTitle_usesChineseConclusion()
    {
        ReviewTask task = sampleTask();
        assertEquals("demo-repo · 合并请求 #12 · 高风险", WorkbenchServiceImpl.buildRecentTitle(task));

        task.setTaskStatus(ReviewPipelineConstants.TASK_FAILED);
        assertEquals("demo-repo · 合并请求 #12 · 已失败", WorkbenchServiceImpl.buildRecentTitle(task));
    }

    @Test
    void buildRecentTitle_usesPushSourceInsteadOfPullRequestZero()
    {
        ReviewTask task = sampleTask();
        task.setEventSource(ReviewPipelineConstants.EVENT_SOURCE_PUSH);
        task.setPrNumber(0);
        task.setTargetBranch("main");

        assertEquals("demo-repo · 推送 main · 高风险", WorkbenchServiceImpl.buildRecentTitle(task));
    }

    @Test
    void deriveRecentConclusion_mapsTaskOutcome()
    {
        ReviewTask task = sampleTask();
        assertEquals(ReviewPipelineConstants.CONCLUSION_BLOCK, WorkbenchServiceImpl.deriveRecentConclusion(task));

        task.setTaskStatus(ReviewPipelineConstants.TASK_FAILED);
        task.setReviewConclusion(null);
        assertEquals(WorkbenchConstants.RECENT_CONCLUSION_FAILED, WorkbenchServiceImpl.deriveRecentConclusion(task));

        task.setTaskStatus(ReviewPipelineConstants.TASK_RUNNING);
        assertNull(WorkbenchServiceImpl.deriveRecentConclusion(task));
    }

    @Test
    void getSummary_zeroProjectCount_stillReturnedWhenHasProjectList()
    {
        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class))
        {
            security.when(() -> SecurityUtils.hasPermi(any())).thenAnswer(inv ->
                WorkbenchConstants.PERM_PROJECT_LIST.equals(inv.getArgument(0)));
            when(projectService.countReviewProjectList(any(ReviewProject.class))).thenReturn(0);

            WorkbenchSummary summary = service.getSummary();

            assertEquals(0, summary.getScope().getProjectCount());
            assertTrue(summary.getCards().isEmpty());
        }
    }

    @Test
    void getTrend_fillsMissingDaysWithZeros()
    {
        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class))
        {
            security.when(() -> SecurityUtils.hasPermi(any())).thenReturn(true);
            LocalDate end = LocalDate.now(ZoneId.of("Asia/Shanghai"));
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            when(recordService.selectReviewConclusionTrend(any())).thenReturn(List.of(
                stat(end.minusDays(13).format(fmt), 2, 1, 0, 1),
                stat(end.format(fmt), 1, 0, 1, 0)));

            WorkbenchTrend trend = service.getTrend(14);

            assertEquals(14, trend.getDays());
            assertEquals(end.minusDays(13).format(fmt), trend.getBeginDate());
            assertEquals(end.format(fmt), trend.getEndDate());
            assertEquals(14, trend.getPoints().size());
            assertEquals(2, trend.getPoints().get(0).getPass());
            assertEquals(1, trend.getPoints().get(0).getWarn());
            assertEquals(1, trend.getPoints().get(0).getFailed());
            assertEquals(0, trend.getPoints().get(1).getPass());
            assertEquals(0, trend.getPoints().get(1).getBlock());
            assertEquals(1, trend.getPoints().get(13).getPass());
            assertEquals(1, trend.getPoints().get(13).getBlock());

            ArgumentCaptor<ReviewTask> captor = ArgumentCaptor.forClass(ReviewTask.class);
            verify(recordService).selectReviewConclusionTrend(captor.capture());
            assertEquals(end.minusDays(13).format(fmt), captor.getValue().getParams().get("beginTime"));
            assertEquals(end.format(fmt), captor.getValue().getParams().get("endTime"));
        }
    }

    @Test
    void getTrend_withoutRecordPermission_returnsNull()
    {
        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class))
        {
            security.when(() -> SecurityUtils.hasPermi(any())).thenReturn(false);

            assertNull(service.getTrend(14));
            verify(recordService, never()).selectReviewConclusionTrend(any());
        }
    }

    @Test
    void getTrend_daysOutOfRange_clamped()
    {
        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class))
        {
            security.when(() -> SecurityUtils.hasPermi(any())).thenReturn(true);
            when(recordService.selectReviewConclusionTrend(any())).thenReturn(List.of());

            WorkbenchTrend min = service.getTrend(0);
            assertEquals(1, min.getDays());
            assertEquals(1, min.getPoints().size());

            WorkbenchTrend max = service.getTrend(999);
            assertEquals(WorkbenchConstants.TREND_MAX_DAYS, max.getDays());
            assertEquals(WorkbenchConstants.TREND_MAX_DAYS, max.getPoints().size());
        }
    }

    @Test
    void getModelHealth_buildsSummaryAndDerivedCheckStatus()
    {
        when(aiModelConfigService.selectSysAiModelConfigList(any())).thenReturn(List.of(
            model("deepseek-main", "deepseek", null, "1", "成功 (123ms)", DateUtils.parseDate("2026-08-03 10:00:00")),
            model("kimi-backup", "kimi", null, "0", "超时: connect timed out", DateUtils.parseDate("2026-08-04 09:30:00")),
            model("internal-llm", "custom", "内部网关", "0", null, null)));

        WorkbenchModels models = service.getModelHealth();

        assertEquals(3, models.getEnabledCount());
        assertEquals(1, models.getOnlineCount());
        assertEquals("2026-08-04 09:30", models.getLastCheckTime());
        assertEquals(3, models.getItems().size());
        assertEquals(WorkbenchConstants.CHECK_STATUS_SUCCESS, models.getItems().get(0).getCheckStatus());
        assertTrue(models.getItems().get(0).getIsDefault());
        assertEquals("DeepSeek", models.getItems().get(0).getProviderLabel());
        assertEquals(WorkbenchConstants.CHECK_STATUS_FAILED, models.getItems().get(1).getCheckStatus());
        assertFalse(models.getItems().get(1).getIsDefault());
        assertEquals(WorkbenchConstants.CHECK_STATUS_NEVER, models.getItems().get(2).getCheckStatus());
        assertNull(models.getItems().get(2).getLastCheckTime());
        assertEquals("内部网关", models.getItems().get(2).getProviderLabel());
    }

    @Test
    void getModelHealth_noEnabledModels_returnsZeros()
    {
        when(aiModelConfigService.selectSysAiModelConfigList(any())).thenReturn(List.of());

        WorkbenchModels models = service.getModelHealth();

        assertEquals(0, models.getEnabledCount());
        assertEquals(0, models.getOnlineCount());
        assertNull(models.getLastCheckTime());
        assertTrue(models.getItems().isEmpty());
    }

    private static ReviewConclusionDailyStat stat(String day, int pass, int warn, int block, int failed)
    {
        ReviewConclusionDailyStat stat = new ReviewConclusionDailyStat();
        stat.setStatDate(day);
        stat.setPassCount(pass);
        stat.setWarnCount(warn);
        stat.setBlockCount(block);
        stat.setFailedCount(failed);
        return stat;
    }

    private static SysAiModelConfig model(String name, String provider, String customName, String isDefault,
                                          String checkResult, Date checkTime)
    {
        SysAiModelConfig config = new SysAiModelConfig();
        config.setModelName(name);
        config.setProvider(provider);
        config.setCustomProviderName(customName);
        config.setModel(name + "-v1");
        config.setEnabled("1");
        config.setIsDefault(isDefault);
        config.setLastCheckResult(checkResult);
        config.setLastCheckTime(checkTime);
        return config;
    }

    private static ReviewTask sampleTask()
    {
        ReviewTask task = new ReviewTask();
        task.setTaskId(99L);
        task.setProjectName("demo-repo");
        task.setPrNumber(12);
        task.setTaskStatus(ReviewPipelineConstants.TASK_SUCCESS);
        task.setReviewConclusion(ReviewPipelineConstants.CONCLUSION_BLOCK);
        task.setFinishedTime(new Date());
        return task;
    }
}
