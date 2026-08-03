package com.acr.review.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import com.acr.common.exception.ServiceException;
import com.acr.review.delivery.ReviewDeliveryConstants;
import com.acr.review.domain.ReviewDeliveryRecord;
import com.acr.review.domain.ReviewPipelineConstants;
import com.acr.review.domain.ReviewProject;
import com.acr.review.domain.ReviewTask;
import com.acr.review.domain.ReviewTaskRun;
import com.acr.review.git.GitPullRequestComment;
import com.acr.review.git.GitPullRequestCommentClient;
import com.acr.review.git.GitPullRequestCommentException;
import com.acr.review.mapper.ReviewDeliveryRecordMapper;
import com.acr.review.mapper.ReviewProjectMapper;
import com.acr.review.mapper.ReviewTaskMapper;
import com.acr.review.mapper.ReviewTaskRunMapper;
import com.acr.review.service.IGitCredentialService;
import com.acr.system.service.ISysDeptService;

@ExtendWith(MockitoExtension.class)
class ReviewDeliveryServiceImplTest
{
    @Mock private ReviewDeliveryRecordMapper deliveryMapper;
    @Mock private ReviewTaskMapper taskMapper;
    @Mock private ReviewTaskRunMapper runMapper;
    @Mock private ReviewProjectMapper projectMapper;
    @Mock private IGitCredentialService credentialService;
    @Mock private ISysDeptService deptService;
    @Mock private GitPullRequestCommentClient commentClient;

    private ReviewDeliveryServiceImpl service;

    @BeforeEach
    void setUp()
    {
        service = new ReviewDeliveryServiceImpl(deliveryMapper, taskMapper, runMapper, projectMapper,
            credentialService, deptService, commentClient);
    }

    @Test
    void createsCommentWhenNoMarkerExists()
    {
        ReviewTask task = successTask(10L, 3L, 8);
        ReviewTaskRun run = successRun(100L, 1);
        stubProjectAndToken();
        when(commentClient.findCommentWithMarker(any(), anyString(), anyInt(), anyString()))
            .thenReturn(Optional.empty());
        when(commentClient.createIssueComment(any(), anyString(), anyInt(), anyString()))
            .thenReturn(new GitPullRequestComment("501", "body"));
        when(deliveryMapper.selectByIdempotencyKey(anyString())).thenReturn(null);

        service.deliverAfterSuccess(task, run);

        verify(commentClient).createIssueComment(any(), eq("pat"), eq(8), anyString());
        ArgumentCaptor<ReviewDeliveryRecord> captor = ArgumentCaptor.forClass(ReviewDeliveryRecord.class);
        verify(deliveryMapper).insertDelivery(captor.capture());
        assertEquals(ReviewDeliveryConstants.STATUS_SUCCESS, captor.getValue().getDeliveryStatus());
        assertEquals("501", captor.getValue().getExternalId());
        assertEquals(ReviewDeliveryConstants.idempotencyKey(3L, 8), captor.getValue().getIdempotencyKey());
    }

    @Test
    void updatesExistingMarkerCommentAndIncrementsAttempt()
    {
        ReviewTask task = successTask(11L, 3L, 8);
        ReviewTaskRun run = successRun(101L, 2);
        stubProjectAndToken();
        when(commentClient.findCommentWithMarker(any(), anyString(), anyInt(), anyString()))
            .thenReturn(Optional.of(new GitPullRequestComment("77", "old")));
        when(commentClient.updateIssueComment(any(), anyString(), eq("77"), anyString()))
            .thenReturn(new GitPullRequestComment("77", "new"));
        ReviewDeliveryRecord existing = new ReviewDeliveryRecord();
        existing.setExternalId("77");
        existing.setAttemptCount(1);
        when(deliveryMapper.selectByIdempotencyKey(anyString())).thenReturn(existing);

        service.deliverAfterSuccess(task, run);

        verify(commentClient).updateIssueComment(any(), eq("pat"), eq("77"), anyString());
        verify(commentClient, never()).createIssueComment(any(), anyString(), anyInt(), anyString());
        verify(deliveryMapper).updateDeliveryResult(any());
    }

    @Test
    void githubFailurePersistsFailedWithoutRethrow()
    {
        ReviewTask task = successTask(12L, 3L, 8);
        ReviewTaskRun run = successRun(102L, 1);
        stubProjectAndToken();
        when(commentClient.findCommentWithMarker(any(), anyString(), anyInt(), anyString()))
            .thenThrow(new GitPullRequestCommentException("GitHub API 超时"));
        when(deliveryMapper.selectByIdempotencyKey(anyString())).thenReturn(null);

        service.deliverAfterSuccess(task, run);

        ArgumentCaptor<ReviewDeliveryRecord> captor = ArgumentCaptor.forClass(ReviewDeliveryRecord.class);
        verify(deliveryMapper).insertDelivery(captor.capture());
        assertEquals(ReviewDeliveryConstants.STATUS_FAILED, captor.getValue().getDeliveryStatus());
        assertTrue(captor.getValue().getFailureMessage().contains("超时"));
        assertNull(captor.getValue().getExternalId());
    }

    @Test
    void skipsNonSuccessTask()
    {
        ReviewTask task = successTask(13L, 3L, 8);
        task.setTaskStatus(ReviewPipelineConstants.TASK_FAILED);

        service.deliverAfterSuccess(task, successRun(1L, 1));

        verify(commentClient, never()).findCommentWithMarker(any(), anyString(), anyInt(), anyString());
        verify(deliveryMapper, never()).insertDelivery(any());
    }

    @Test
    void retryUsesLatestSuccessTaskForRendering()
    {
        ReviewTask anchor = successTask(20L, 3L, 8);
        anchor.setTaskStatus(ReviewPipelineConstants.TASK_SUCCESS);
        ReviewTask latest = successTask(21L, 3L, 8);
        latest.setReviewConclusion(ReviewPipelineConstants.CONCLUSION_BLOCK);
        latest.setTotalScore(40);
        ReviewTaskRun run = successRun(200L, 1);
        run.setTopIssuesJson("[]");

        when(taskMapper.selectReviewTaskById(20L)).thenReturn(anchor);
        when(projectMapper.selectReviewProjectById(3L)).thenReturn(project());
        when(taskMapper.selectLatestSuccessByProjectAndPr(3L, 8)).thenReturn(latest);
        when(runMapper.selectRunsByTaskId(21L)).thenReturn(List.of(run));
        when(credentialService.getPlainToken(5L, true)).thenReturn("pat");
        when(commentClient.findCommentWithMarker(any(), anyString(), anyInt(), anyString()))
            .thenReturn(Optional.empty());
        when(commentClient.createIssueComment(any(), anyString(), anyInt(), anyString()))
            .thenAnswer(inv -> {
                String body = inv.getArgument(3);
                assertTrue(body.contains("高风险"));
                assertTrue(body.contains("40 / 100"));
                return new GitPullRequestComment("9", body);
            });
        when(deliveryMapper.selectByIdempotencyKey(anyString())).thenReturn(null);

        service.retryDelivery(20L);

        verify(taskMapper).selectLatestSuccessByProjectAndPr(3L, 8);
        verify(commentClient).createIssueComment(any(), eq("pat"), eq(8), anyString());
    }

    @Test
    void uniqueKeyConflictFallsBackToUpdateInsteadOfFailed()
    {
        ReviewTask task = successTask(14L, 3L, 8);
        ReviewTaskRun run = successRun(103L, 1);
        stubProjectAndToken();
        when(commentClient.findCommentWithMarker(any(), anyString(), anyInt(), anyString()))
            .thenReturn(Optional.empty());
        when(commentClient.createIssueComment(any(), anyString(), anyInt(), anyString()))
            .thenReturn(new GitPullRequestComment("601", "body"));
        when(deliveryMapper.selectByIdempotencyKey(anyString())).thenReturn(null);
        when(deliveryMapper.insertDelivery(any()))
            .thenThrow(new DuplicateKeyException("Duplicate entry"));

        service.deliverAfterSuccess(task, run);

        ArgumentCaptor<ReviewDeliveryRecord> captor = ArgumentCaptor.forClass(ReviewDeliveryRecord.class);
        verify(deliveryMapper).updateDeliveryResult(captor.capture());
        assertEquals(ReviewDeliveryConstants.STATUS_SUCCESS, captor.getValue().getDeliveryStatus());
        assertEquals("601", captor.getValue().getExternalId());
    }

    @Test
    void retryWithoutSuccessTaskFails()
    {
        ReviewTask anchor = successTask(22L, 3L, 8);
        when(taskMapper.selectReviewTaskById(22L)).thenReturn(anchor);
        when(projectMapper.selectReviewProjectById(3L)).thenReturn(project());
        when(taskMapper.selectLatestSuccessByProjectAndPr(3L, 8)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class, () -> service.retryDelivery(22L));
        assertTrue(ex.getMessage().contains("尚无成功审查结果"));
    }

    private void stubProjectAndToken()
    {
        when(projectMapper.selectReviewProjectById(3L)).thenReturn(project());
        when(credentialService.getPlainToken(5L, true)).thenReturn("pat");
    }

    private static ReviewProject project()
    {
        ReviewProject project = new ReviewProject();
        project.setProjectId(3L);
        project.setStatus("0");
        project.setProvider("GITHUB");
        project.setCredentialId(5L);
        project.setDeptId(1L);
        project.setRepositoryOwner("acme");
        project.setRepositoryName("demo");
        project.setRepositoryUrl("https://github.com/acme/demo");
        return project;
    }

    private static ReviewTask successTask(Long taskId, Long projectId, int prNumber)
    {
        ReviewTask task = new ReviewTask();
        task.setTaskId(taskId);
        task.setProjectId(projectId);
        task.setPrNumber(prNumber);
        task.setTaskStatus(ReviewPipelineConstants.TASK_SUCCESS);
        task.setReviewConclusion(ReviewPipelineConstants.CONCLUSION_PASS);
        task.setTotalScore(90);
        task.setHeadSha("abc1234567890");
        return task;
    }

    private static ReviewTaskRun successRun(Long runId, int attempt)
    {
        ReviewTaskRun run = new ReviewTaskRun();
        run.setRunId(runId);
        run.setAttemptNo(attempt);
        run.setRunStatus(ReviewPipelineConstants.RUN_SUCCESS);
        return run;
    }
}
