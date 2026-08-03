package com.acr.review.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.acr.common.exception.ServiceException;
import com.acr.review.domain.ReviewIssue;
import com.acr.review.domain.ReviewIssueAction;
import com.acr.review.domain.ReviewIssueConstants;
import com.acr.review.domain.ReviewPipelineConstants;
import com.acr.review.domain.ReviewProject;
import com.acr.review.domain.ReviewTask;
import com.acr.review.domain.ReviewTaskRun;
import com.acr.review.domain.result.ReviewTopIssue;
import com.acr.review.mapper.ReviewIssueActionMapper;
import com.acr.review.mapper.ReviewIssueMapper;
import com.acr.review.mapper.ReviewProjectMapper;
import com.acr.review.mapper.ReviewTaskMapper;
import com.acr.review.service.IReviewDeliveryService;
import com.acr.review.service.ReviewIssueFingerprint;
import com.acr.system.service.ISysDeptService;
import com.alibaba.fastjson2.JSON;

@ExtendWith(MockitoExtension.class)
class ReviewIssueServiceImplTest
{
    @Mock private ReviewIssueMapper issueMapper;
    @Mock private ReviewIssueActionMapper actionMapper;
    @Mock private ReviewProjectMapper projectMapper;
    @Mock private ReviewTaskMapper taskMapper;
    @Mock private ISysDeptService deptService;
    @Mock private IReviewDeliveryService deliveryService;

    private ReviewIssueServiceImpl service;

    @BeforeEach
    void setUp()
    {
        service = new ReviewIssueServiceImpl(issueMapper, actionMapper, projectMapper, taskMapper,
            deptService, deliveryService);
    }

    @Test
    void materializeInsertsNewIssues()
    {
        ReviewTask task = successTask(1L, 10L, 8);
        ReviewTaskRun run = runWithIssues(top("SEC", "a.java", "leak", 1));
        when(issueMapper.selectByProjectPrFingerprint(eq(10L), eq(8), any())).thenReturn(null);

        service.materializeAfterSuccess(task, run);

        ArgumentCaptor<ReviewIssue> captor = ArgumentCaptor.forClass(ReviewIssue.class);
        verify(issueMapper).insertIssue(captor.capture());
        assertEquals(ReviewIssueConstants.STATUS_AWAITING_CONFIRM, captor.getValue().getStatus());
        assertEquals(ReviewIssueFingerprint.of("SEC", "a.java", "leak"), captor.getValue().getFingerprint());
    }

    @Test
    void materializeRefreshesOpenIssueAcrossTasks()
    {
        ReviewTask task = successTask(2L, 10L, 8);
        ReviewTaskRun run = runWithIssues(top("SEC", "a.java", "leak", 1));
        ReviewIssue existing = openIssue(99L, "SEC", "a.java", "leak");
        when(issueMapper.selectByProjectPrFingerprint(eq(10L), eq(8), any())).thenReturn(existing);

        service.materializeAfterSuccess(task, run);

        verify(issueMapper, never()).insertIssue(any());
        verify(issueMapper).updateIssueSnapshot(existing);
        assertEquals(2L, existing.getLastTaskId());
        assertEquals(ReviewIssueConstants.STATUS_AWAITING_CONFIRM, existing.getStatus());
    }

    @Test
    void materializeSkipsTerminalIssue()
    {
        ReviewTask task = successTask(3L, 10L, 8);
        ReviewTaskRun run = runWithIssues(top("SEC", "a.java", "leak", 1));
        ReviewIssue existing = openIssue(99L, "SEC", "a.java", "leak");
        existing.setStatus(ReviewIssueConstants.STATUS_CLOSED);
        when(issueMapper.selectByProjectPrFingerprint(eq(10L), eq(8), any())).thenReturn(existing);

        service.materializeAfterSuccess(task, run);

        verify(issueMapper, never()).insertIssue(any());
        verify(issueMapper, never()).updateIssueSnapshot(any());
    }

    @Test
    void materializeHandlesBatchFingerprintCollision()
    {
        ReviewTask task = successTask(4L, 10L, 8);
        ReviewTopIssue a = top("SEC", "a.java", "same", 1);
        ReviewTopIssue b = top("SEC", "a.java", "same", 2);
        ReviewTaskRun run = runWithIssues(a, b);
        when(issueMapper.selectByProjectPrFingerprint(eq(10L), eq(8), any())).thenReturn(null);

        service.materializeAfterSuccess(task, run);

        ArgumentCaptor<ReviewIssue> captor = ArgumentCaptor.forClass(ReviewIssue.class);
        verify(issueMapper, org.mockito.Mockito.times(2)).insertIssue(captor.capture());
        List<ReviewIssue> inserted = captor.getAllValues();
        assertEquals(2, inserted.size());
        assertTrue(!inserted.get(0).getFingerprint().equals(inserted.get(1).getFingerprint())
            || inserted.get(1).getFingerprint().contains(":"));
    }

    @Test
    void confirmThenCloseAndDismissTerminal()
    {
        ReviewIssue issue = openIssue(7L, "SEC", "a.java", "x");
        issue.setStatus(ReviewIssueConstants.STATUS_AWAITING_CONFIRM);
        stubProjectScope(issue);
        when(issueMapper.selectIssueById(7L)).thenReturn(issue);
        when(deliveryService.rerenderSummaryComment(10L, 8)).thenReturn("SUCCESS");

        assertEquals("SUCCESS", service.confirm(7L));
        assertEquals(ReviewIssueConstants.STATUS_AWAITING_FIX, issue.getStatus());

        when(issueMapper.selectIssueById(7L)).thenReturn(issue);
        service.close(7L, "fixed");
        assertEquals(ReviewIssueConstants.STATUS_CLOSED, issue.getStatus());

        assertThrows(ServiceException.class, () -> service.dismiss(7L, "IGNORED", "nope"));
    }

    @Test
    void dismissRequiresNote()
    {
        ReviewIssue issue = openIssue(8L, "SEC", "a.java", "x");
        stubProjectScope(issue);
        when(issueMapper.selectIssueById(8L)).thenReturn(issue);

        assertThrows(ServiceException.class, () -> service.dismiss(8L, "FALSE_POSITIVE", " "));
    }

    @Test
    void dismissFalsePositiveWritesAction()
    {
        ReviewIssue issue = openIssue(9L, "SEC", "a.java", "x");
        stubProjectScope(issue);
        when(issueMapper.selectIssueById(9L)).thenReturn(issue);
        when(deliveryService.rerenderSummaryComment(10L, 8)).thenReturn("FAILED");

        assertEquals("FAILED", service.dismiss(9L, "FALSE_POSITIVE", "历史噪声"));
        assertEquals(ReviewIssueConstants.STATUS_FALSE_POSITIVE, issue.getStatus());
        ArgumentCaptor<ReviewIssueAction> actionCaptor = ArgumentCaptor.forClass(ReviewIssueAction.class);
        verify(actionMapper).insertAction(actionCaptor.capture());
        assertEquals(ReviewIssueConstants.ACTION_DISMISS, actionCaptor.getValue().getActionType());
        assertNotNull(issue.getClosedTime());
    }

    @Test
    void countOpenNewExcludesExistingViaMapper()
    {
        when(issueMapper.countOpenNewByProject(10L)).thenReturn(2);
        assertEquals(2, service.countOpenNewByProject(10L));
    }

    private void stubProjectScope(ReviewIssue issue)
    {
        ReviewProject project = new ReviewProject();
        project.setProjectId(issue.getProjectId());
        project.setDeptId(1L);
        when(projectMapper.selectReviewProjectById(issue.getProjectId())).thenReturn(project);
    }

    private static ReviewIssue openIssue(Long id, String category, String path, String title)
    {
        ReviewIssue issue = new ReviewIssue();
        issue.setIssueId(id);
        issue.setProjectId(10L);
        issue.setPrNumber(8);
        issue.setFingerprint(ReviewIssueFingerprint.of(category, path, title));
        issue.setStatus(ReviewIssueConstants.STATUS_AWAITING_CONFIRM);
        issue.setTitle(title);
        issue.setOrigin(ReviewIssueConstants.ORIGIN_NEW);
        return issue;
    }

    private static ReviewTask successTask(Long taskId, Long projectId, int pr)
    {
        ReviewTask task = new ReviewTask();
        task.setTaskId(taskId);
        task.setProjectId(projectId);
        task.setPrNumber(pr);
        task.setTaskStatus(ReviewPipelineConstants.TASK_SUCCESS);
        task.setProvider("GITHUB");
        return task;
    }

    private static ReviewTaskRun runWithIssues(ReviewTopIssue... issues)
    {
        ReviewTaskRun run = new ReviewTaskRun();
        run.setRunId(100L);
        run.setTopIssuesJson(JSON.toJSONString(List.of(issues)));
        return run;
    }

    private static ReviewTopIssue top(String category, String path, String title, int rank)
    {
        ReviewTopIssue issue = new ReviewTopIssue();
        issue.setCategory(category);
        issue.setFilePath(path);
        issue.setTitle(title);
        issue.setRank(rank);
        issue.setSeverity("HIGH");
        issue.setOrigin("NEW");
        return issue;
    }
}
