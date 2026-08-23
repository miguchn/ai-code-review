package com.acr.review.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.acr.review.delivery.ReviewDeliveryConstants;
import com.acr.review.delivery.ReviewDeliveryIntentService;
import com.acr.review.domain.ReviewIssue;
import com.acr.review.domain.ReviewIssueConstants;
import com.acr.review.domain.ReviewProject;
import com.acr.review.domain.ReviewTask;
import com.acr.review.mapper.ReviewIssueMapper;
import com.acr.review.mapper.ReviewProjectMapper;
import com.acr.review.mapper.ReviewTaskMapper;
import com.acr.system.service.ISysConfigService;

@ExtendWith(MockitoExtension.class)
class ReviewIssueOverdueServiceTest
{
    @Mock private ReviewIssueMapper issueMapper;
    @Mock private ReviewProjectMapper projectMapper;
    @Mock private ReviewTaskMapper taskMapper;
    @Mock private ReviewDeliveryIntentService deliveryIntentService;
    @Mock private ISysConfigService configService;

    private ReviewIssueOverdueService service;

    @BeforeEach
    void setUp()
    {
        service = new ReviewIssueOverdueService(
            issueMapper, projectMapper, taskMapper, deliveryIntentService, configService);
    }

    private void stubThresholds()
    {
        when(configService.selectConfigByKey(ReviewIssueConstants.CONFIG_OVERDUE_HIGH_DAYS)).thenReturn("3");
        when(configService.selectConfigByKey(ReviewIssueConstants.CONFIG_OVERDUE_NORMAL_DAYS)).thenReturn("7");
    }

    @Test
    void highSeverityUsesThreeDays()
    {
        Date now = new Date();
        ReviewIssue high = issue(1L, 9L, "HIGH", "N", new Date(now.getTime() - 3L * 24 * 3600_000));
        ReviewIssue medium = issue(2L, 9L, "MEDIUM", "N", new Date(now.getTime() - 3L * 24 * 3600_000));
        assertTrue(ReviewIssueOverdueService.isOverdue(high, now, 3, 7));
        assertFalse(ReviewIssueOverdueService.isOverdue(medium, now, 3, 7));
    }

    @Test
    void scan_newOverdueEnqueuesOncePerProject()
    {
        stubThresholds();
        Date now = new Date();
        ReviewIssue issue = issue(11L, 9L, "CRITICAL", "N", new Date(now.getTime() - 4L * 24 * 3600_000));
        when(issueMapper.selectActiveIssuesForOverdueScan()).thenReturn(List.of(issue));
        ReviewProject project = new ReviewProject();
        project.setProjectId(9L);
        project.setNotifyEnabled("Y");
        when(projectMapper.selectReviewProjectById(9L)).thenReturn(project);
        ReviewTask task = new ReviewTask();
        task.setTaskId(88L);
        task.setProjectId(9L);
        task.setPrNumber(1);
        when(taskMapper.selectLatestByProjectId(9L)).thenReturn(task);

        service.scan();

        verify(issueMapper).updateOverdueFlag(11L, "Y");
        verify(deliveryIntentService).enqueueOverdueRemind(project, task, ReviewIssueConstants.OPERATOR_SYSTEM);
    }

    @Test
    void scan_alreadyOverdueDoesNotReenqueue()
    {
        stubThresholds();
        Date now = new Date();
        ReviewIssue issue = issue(11L, 9L, "CRITICAL", "Y", new Date(now.getTime() - 4L * 24 * 3600_000));
        when(issueMapper.selectActiveIssuesForOverdueScan()).thenReturn(List.of(issue));

        service.scan();

        verify(issueMapper, never()).updateOverdueFlag(any(), any());
        verify(deliveryIntentService, never()).enqueueOverdueRemind(any(), any(), any());
    }

    @Test
    void scan_notifyDisabledOnlyUpdatesFlag()
    {
        stubThresholds();
        Date now = new Date();
        ReviewIssue issue = issue(11L, 9L, "CRITICAL", "N", new Date(now.getTime() - 4L * 24 * 3600_000));
        when(issueMapper.selectActiveIssuesForOverdueScan()).thenReturn(List.of(issue));
        ReviewProject project = new ReviewProject();
        project.setProjectId(9L);
        project.setNotifyEnabled("N");
        when(projectMapper.selectReviewProjectById(9L)).thenReturn(project);

        service.scan();

        verify(issueMapper).updateOverdueFlag(11L, "Y");
        verify(deliveryIntentService, never()).enqueueOverdueRemind(any(), any(), any());
    }

    @Test
    void scan_resetsFlagWhenNoLongerOverdue()
    {
        stubThresholds();
        Date now = new Date();
        ReviewIssue issue = issue(11L, 9L, "LOW", "Y", new Date(now.getTime() - 2L * 24 * 3600_000));
        when(issueMapper.selectActiveIssuesForOverdueScan()).thenReturn(List.of(issue));

        service.scan();

        verify(issueMapper).updateOverdueFlag(11L, "N");
        verify(deliveryIntentService, never()).enqueueOverdueRemind(any(), any(), any());
    }

    private static ReviewIssue issue(Long issueId, Long projectId, String severity, String flag, Date stage)
    {
        ReviewIssue issue = new ReviewIssue();
        issue.setIssueId(issueId);
        issue.setProjectId(projectId);
        issue.setSeverity(severity);
        issue.setOverdueFlag(flag);
        issue.setStageEnteredTime(stage);
        return issue;
    }
}
