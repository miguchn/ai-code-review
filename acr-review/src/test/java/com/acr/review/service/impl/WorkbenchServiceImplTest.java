package com.acr.review.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
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
import com.acr.review.domain.WorkbenchSummary;
import com.acr.review.service.IReviewDeliveryService;
import com.acr.review.service.IReviewIssueService;
import com.acr.review.service.IReviewProjectService;
import com.acr.review.service.IReviewRecordService;
import com.acr.review.service.IReviewTaskService;

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

    private WorkbenchServiceImpl service;

    @BeforeEach
    void setUp()
    {
        service = new WorkbenchServiceImpl(projectService, issueService, taskService, recordService, deliveryService);
    }

    @Test
    void getSummary_allPermissions_buildsFiveCardsAndToday()
    {
        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class))
        {
            security.when(() -> SecurityUtils.hasPermi(any())).thenReturn(true);
            when(projectService.countReviewProjectList(any())).thenReturn(3);
            when(taskService.selectLatestTaskTime(any())).thenReturn(DateUtils.parseDate("2026-08-03 15:20:00"));
            when(issueService.countIssueList(any())).thenReturn(4, 2);
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
            assertEquals(5, summary.getCards().size());
            assertEquals(WorkbenchConstants.CARD_ISSUE_AWAITING_CONFIRM, summary.getCards().get(0).getType());
            assertEquals(4, summary.getCards().get(0).getCount());
            assertEquals(ReviewIssueConstants.ORIGIN_NEW, summary.getCards().get(0).getQuery().get("origin"));
            assertEquals(WorkbenchConstants.CARD_DELIVERY_FAILED, summary.getCards().get(4).getType());
            assertEquals(0, summary.getCards().get(4).getCount());
            assertEquals(5, summary.getToday().getNewTasks());
            assertEquals(4, summary.getToday().getSuccessTasks());
            assertEquals(1, summary.getToday().getFailedTasks());
            assertEquals(2, summary.getToday().getClosedIssues());
            assertEquals(1, summary.getRecent().size());
            assertTrue(summary.getRecent().get(0).getTitle().contains("高风险"));
            assertTrue(summary.getRecent().get(0).getLink().contains("/review/task-detail/index/99"));
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
    void getSummary_issueCardFiltersMatchList_originNewAndStatus()
    {
        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class))
        {
            security.when(() -> SecurityUtils.hasPermi(any())).thenAnswer(inv -> {
                String perm = inv.getArgument(0);
                return WorkbenchConstants.PERM_ISSUE_LIST.equals(perm);
            });
            when(issueService.countIssueList(any())).thenReturn(3, 1);
            when(issueService.countClosedToday(any())).thenReturn(0);

            WorkbenchSummary summary = service.getSummary();

            ArgumentCaptor<ReviewIssue> captor = ArgumentCaptor.forClass(ReviewIssue.class);
            verify(issueService, org.mockito.Mockito.times(2)).countIssueList(captor.capture());
            List<ReviewIssue> queries = captor.getAllValues();
            assertEquals(ReviewIssueConstants.STATUS_AWAITING_CONFIRM, queries.get(0).getStatus());
            assertEquals(ReviewIssueConstants.ORIGIN_NEW, queries.get(0).getOrigin());
            assertEquals(ReviewIssueConstants.STATUS_AWAITING_FIX, queries.get(1).getStatus());
            assertEquals(ReviewIssueConstants.ORIGIN_NEW, queries.get(1).getOrigin());

            WorkbenchCard confirm = summary.getCards().get(0);
            assertEquals(3, confirm.getCount());
            assertEquals("AWAITING_CONFIRM", confirm.getQuery().get("status"));
            assertEquals("NEW", confirm.getQuery().get("origin"));
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
            assertEquals(ReviewDeliveryConstants.STATUS_FAILED, deliveryCaptor.getValue().getDeliveryStatus());

            assertEquals(2, summary.getCards().size());
            assertEquals("FAILED", summary.getCards().get(0).getQuery().get("taskStatus"));
            assertEquals("FAILED", summary.getCards().get(1).getQuery().get("deliveryStatus"));
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
