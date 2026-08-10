package com.acr.review.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.acr.common.exception.ServiceException;
import com.acr.review.delivery.ReviewDeliveryConstants;
import com.acr.review.delivery.ReviewDeliveryIntentService;
import com.acr.review.delivery.ReviewDeliveryRuntimeSettings;
import com.acr.review.delivery.ReviewSummaryContent;
import com.acr.review.delivery.ReviewSummaryContentFactory;
import com.acr.review.domain.GitCredential;
import com.acr.review.domain.ReviewDeliveryRecord;
import com.acr.review.domain.ReviewPipelineConstants;
import com.acr.review.domain.ReviewProject;
import com.acr.review.domain.ReviewTask;
import com.acr.review.domain.ReviewTaskRun;
import com.acr.review.git.GitAdapterRegistry;
import com.acr.review.git.GitPullRequestComment;
import com.acr.review.git.GitPullRequestCommentClient;
import com.acr.review.git.GitPullRequestCommentException;
import com.acr.review.mapper.GitCredentialMapper;
import com.acr.review.mapper.ReviewDeliveryRecordMapper;
import com.acr.review.mapper.ReviewIssueMapper;
import com.acr.review.mapper.ReviewProjectMapper;
import com.acr.review.mapper.ReviewTaskMapper;
import com.acr.review.mapper.ReviewTaskRunMapper;
import com.acr.review.notify.NotifyRobotClients;
import com.acr.review.service.IGitCredentialService;
import com.acr.review.service.IReviewIssueService;
import com.acr.review.service.IReviewNotifyChannelService;
import com.acr.system.service.ISysDeptService;

@ExtendWith(MockitoExtension.class)
class ReviewDeliveryServiceImplTest
{
    @Mock private ReviewDeliveryRecordMapper deliveryMapper;
    @Mock private ReviewTaskMapper taskMapper;
    @Mock private ReviewTaskRunMapper runMapper;
    @Mock private ReviewProjectMapper projectMapper;
    @Mock private ReviewIssueMapper issueMapper;
    @Mock private IGitCredentialService credentialService;
    @Mock private IReviewNotifyChannelService notifyChannelService;
    @Mock private NotifyRobotClients robotClients;
    @Mock private ReviewSummaryContentFactory contentFactory;
    @Mock private ISysDeptService deptService;
    @Mock private GitAdapterRegistry adapterRegistry;
    @Mock private GitCredentialMapper credentialMapper;
    @Mock private GitPullRequestCommentClient commentClient;
    @Mock private IReviewIssueService issueService;
    @Mock private ReviewDeliveryIntentService intentService;
    @Mock private ReviewDeliveryRuntimeSettings settings;

    private ReviewDeliveryServiceImpl service;

    @BeforeEach
    void setUp()
    {
        service = new ReviewDeliveryServiceImpl(deliveryMapper, taskMapper, runMapper, projectMapper,
            issueMapper, credentialService, notifyChannelService, robotClients, contentFactory,
            deptService, adapterRegistry, credentialMapper, issueService, intentService, settings);
    }

    @Test
    void terminalCallbacksOnlyPersistIntent()
    {
        ReviewTask task = successTask(10L);
        ReviewTaskRun run = successRun(100L);

        service.deliverAfterSuccess(task, run, null);
        service.deliverNotifyAfterTerminal(task, run, null);

        verify(intentService).enqueueSummary(task, run, ReviewDeliveryConstants.TRIGGER_TASK_SUCCESS, "system");
        verify(intentService).enqueueInlineComments(task, run, ReviewDeliveryConstants.TRIGGER_TASK_SUCCESS, "system");
        verify(intentService).enqueueTerminalNotification(task, run, "system");
        verify(adapterRegistry, never()).requireCommentClient(anyString());
        verify(robotClients, never()).require(anyString());
    }

    @Test
    void inlineMarkerHitCommitsWithoutCreatingAgain()
    {
        ReviewDeliveryRecord record = claimedRecord(60L, ReviewDeliveryConstants.CHANNEL_GITHUB_PR_INLINE);
        record.setIssueId(52L);
        ReviewTask task = successTask(10L);
        task.setHeadSha("abcdef0123456789");
        ReviewTaskRun run = successRun(100L);
        run.setTopIssuesJson("""
            [{"issueId":52,"severity":"CRITICAL","category":"SECURITY","title":"注入",
              "filePath":"a.java","startLine":10,"endLine":12,"description":"d","suggestion":"s"}]
            """);
        when(deliveryMapper.selectDeliveryById(60L)).thenReturn(record);
        when(taskMapper.selectReviewTaskById(10L)).thenReturn(task);
        when(runMapper.selectReviewTaskRunById(100L)).thenReturn(run);
        when(projectMapper.selectReviewProjectById(3L)).thenReturn(project());
        when(credentialMapper.selectGitCredentialById(5L)).thenReturn(credential());
        when(credentialService.getPlainToken(5L, true)).thenReturn("pat");
        when(adapterRegistry.requireCommentClient("GITHUB")).thenReturn(commentClient);
        when(commentClient.findInlineCommentWithMarker(any(), any(), eq(8), anyString()))
            .thenReturn(Optional.of(new GitPullRequestComment("9001", "exists")));
        when(deliveryMapper.completeDelivery(eq(60L), eq("worker:delivery"), eq(null), eq("9001"), anyString()))
            .thenReturn(1);

        service.executeClaimedDelivery(60L, "worker:delivery");

        verify(commentClient, never()).createInlineComment(any(), any(), anyInt(), any());
        verify(deliveryMapper).completeDelivery(eq(60L), eq("worker:delivery"), eq(null), eq("9001"), anyString());
    }

    @Test
    void giteeUnsupportedInlineBecomesSkipped()
    {
        ReviewDeliveryRecord record = claimedRecord(61L, ReviewDeliveryConstants.CHANNEL_GITEE_PR_INLINE);
        record.setIssueId(52L);
        ReviewTask task = successTask(10L);
        task.setProvider("GITEE");
        task.setHeadSha("abcdef0123456789");
        ReviewTaskRun run = successRun(100L);
        run.setTopIssuesJson("""
            [{"issueId":52,"severity":"HIGH","category":"SECURITY","title":"注入",
              "filePath":"a.java","startLine":10,"endLine":10}]
            """);
        ReviewProject giteeProject = project();
        giteeProject.setProvider("GITEE");
        when(deliveryMapper.selectDeliveryById(61L)).thenReturn(record);
        when(taskMapper.selectReviewTaskById(10L)).thenReturn(task);
        when(runMapper.selectReviewTaskRunById(100L)).thenReturn(run);
        when(projectMapper.selectReviewProjectById(3L)).thenReturn(giteeProject);
        when(credentialMapper.selectGitCredentialById(5L)).thenReturn(credential());
        when(credentialService.getPlainToken(5L, true)).thenReturn("pat");
        when(adapterRegistry.requireCommentClient("GITEE")).thenReturn(commentClient);
        when(commentClient.findInlineCommentWithMarker(any(), any(), anyInt(), anyString()))
            .thenReturn(Optional.empty());
        when(commentClient.createInlineComment(any(), any(), anyInt(), any()))
            .thenThrow(new com.acr.review.git.GitInlineCommentUnsupportedException("Gitee 不支持行内评论"));
        when(deliveryMapper.failDelivery(eq(61L), eq("worker:delivery"),
            eq(ReviewDeliveryConstants.STATUS_SKIPPED), eq(ReviewDeliveryConstants.ERROR_INLINE_UNSUPPORTED),
            anyString(), eq(null), any())).thenReturn(1);

        service.executeClaimedDelivery(61L, "worker:delivery");

        verify(deliveryMapper).failDelivery(eq(61L), eq("worker:delivery"),
            eq(ReviewDeliveryConstants.STATUS_SKIPPED), eq(ReviewDeliveryConstants.ERROR_INLINE_UNSUPPORTED),
            anyString(), eq(null), any());
        verify(deliveryMapper, never()).completeDelivery(anyLong(), anyString(), any(), any(), any());
    }

    @Test
    void inlineFailureDoesNotTouchSummaryIntent()
    {
        ReviewDeliveryRecord record = claimedRecord(62L, ReviewDeliveryConstants.CHANNEL_GITHUB_PR_INLINE);
        record.setIssueId(52L);
        record.setAttemptCount(0);
        ReviewTask task = successTask(10L);
        task.setHeadSha("abcdef");
        ReviewTaskRun run = successRun(100L);
        run.setTopIssuesJson("""
            [{"issueId":52,"severity":"CRITICAL","filePath":"a.java","endLine":1,"title":"t"}]
            """);
        when(deliveryMapper.selectDeliveryById(62L)).thenReturn(record);
        when(taskMapper.selectReviewTaskById(10L)).thenReturn(task);
        when(runMapper.selectReviewTaskRunById(100L)).thenReturn(run);
        when(projectMapper.selectReviewProjectById(3L)).thenReturn(project());
        when(credentialMapper.selectGitCredentialById(5L)).thenReturn(credential());
        when(credentialService.getPlainToken(5L, true)).thenReturn("pat");
        when(adapterRegistry.requireCommentClient("GITHUB")).thenReturn(commentClient);
        when(commentClient.findInlineCommentWithMarker(any(), any(), anyInt(), anyString()))
            .thenReturn(Optional.empty());
        when(commentClient.createInlineComment(any(), any(), anyInt(), any()))
            .thenThrow(new GitPullRequestCommentException("行号越界"));
        when(settings.maxAttempts()).thenReturn(5);
        when(settings.retryDelaySeconds(0)).thenReturn(30);
        when(deliveryMapper.failDelivery(eq(62L), eq("worker:delivery"),
            eq(ReviewDeliveryConstants.STATUS_FAILED), eq(ReviewDeliveryConstants.ERROR_EXTERNAL_CALL),
            anyString(), eq(30), any())).thenReturn(1);

        service.executeClaimedDelivery(62L, "worker:delivery");

        verify(intentService, never()).enqueueSummary(any(), any(), anyString(), anyString());
        verify(deliveryMapper).failDelivery(eq(62L), eq("worker:delivery"),
            eq(ReviewDeliveryConstants.STATUS_FAILED), eq(ReviewDeliveryConstants.ERROR_EXTERNAL_CALL),
            anyString(), eq(30), any());
    }

    @Test
    void manualSummaryRetryUsesLatestSuccessAndQueuesSameStateMachine()
    {
        ReviewTask anchor = successTask(20L);
        ReviewTask latest = successTask(21L);
        ReviewTaskRun run = successRun(201L);
        when(taskMapper.selectReviewTaskById(20L)).thenReturn(anchor);
        when(projectMapper.selectReviewProjectById(3L)).thenReturn(project());
        when(taskMapper.selectLatestSuccessByProjectAndPr(3L, 8)).thenReturn(latest);
        when(runMapper.selectRunsByTaskId(21L)).thenReturn(List.of(run));

        service.retryDelivery(20L);

        verify(intentService).enqueueSummary(latest, run,
            ReviewDeliveryConstants.TRIGGER_MANUAL_RETRY, "system");
        verify(commentClient, never()).createIssueComment(any(), any(), anyInt(), anyString());
    }

    @Test
    void manualRetryRejectsPrWithoutSuccessfulReview()
    {
        ReviewTask anchor = successTask(22L);
        when(taskMapper.selectReviewTaskById(22L)).thenReturn(anchor);
        when(projectMapper.selectReviewProjectById(3L)).thenReturn(project());

        assertThrows(ServiceException.class, () -> service.retryDelivery(22L));
        verify(intentService, never()).enqueueSummary(any(), any(), anyString(), anyString());
    }

    @Test
    void manualImRetryOnlyRequeuesLedgerRecord()
    {
        ReviewDeliveryRecord record = claimedRecord(30L, ReviewDeliveryConstants.CHANNEL_FEISHU_BOT);
        when(deliveryMapper.selectDeliveryById(30L)).thenReturn(record);
        when(projectMapper.selectReviewProjectById(3L)).thenReturn(project());

        service.retryDeliveryById(30L);

        verify(intentService).requeue(30L, "system");
        verify(robotClients, never()).require(anyString());
    }

    @Test
    void claimedSummaryDeliveryUpdatesMarkerAndCommitsWithinLeaseFence()
    {
        ReviewDeliveryRecord record = claimedRecord(40L, ReviewDeliveryConstants.CHANNEL_GITHUB_PR_SUMMARY);
        ReviewTask task = successTask(10L);
        ReviewTaskRun run = successRun(100L);
        stubSummaryExecution(record, task, run);
        when(commentClient.findCommentWithMarker(any(), any(), eq(8), anyString()))
            .thenReturn(Optional.of(new GitPullRequestComment("501", "old")));
        when(commentClient.updateIssueComment(any(), any(), eq("501"), anyString()))
            .thenReturn(new GitPullRequestComment("501", "new"));
        when(deliveryMapper.completeDelivery(eq(40L), eq("worker:delivery"), eq(null), eq("501"), anyString()))
            .thenReturn(1);

        service.executeClaimedDelivery(40L, "worker:delivery");

        verify(commentClient).updateIssueComment(any(), any(), eq("501"), anyString());
        verify(deliveryMapper).completeDelivery(eq(40L), eq("worker:delivery"), eq(null), eq("501"), anyString());
        verify(deliveryMapper, never()).failDelivery(anyLong(), anyString(), anyString(), anyString(),
            anyString(), any(), any());
    }

    @Test
    void staleHeadSummaryDeliveryRecordsSkippedStaleWithoutExternalWrite()
    {
        ReviewDeliveryRecord record = claimedRecord(43L, ReviewDeliveryConstants.CHANNEL_GITHUB_PR_SUMMARY);
        ReviewTask task = successTask(10L);
        task.setChangeKey("PR#8");
        ReviewTaskRun run = successRun(100L);
        when(deliveryMapper.selectDeliveryById(43L)).thenReturn(record);
        when(taskMapper.selectReviewTaskById(10L)).thenReturn(task);
        when(runMapper.selectReviewTaskRunById(100L)).thenReturn(run);
        when(taskMapper.countNewerTasksByChangeKey(3L, "PR#8", 10L)).thenReturn(1);
        when(deliveryMapper.failDelivery(eq(43L), eq("worker:delivery"),
            eq(ReviewDeliveryConstants.STATUS_SKIPPED), eq(ReviewDeliveryConstants.ERROR_SKIPPED_STALE),
            anyString(), eq(null), any())).thenReturn(1);

        service.executeClaimedDelivery(43L, "worker:delivery");

        verify(deliveryMapper).failDelivery(eq(43L), eq("worker:delivery"),
            eq(ReviewDeliveryConstants.STATUS_SKIPPED), eq(ReviewDeliveryConstants.ERROR_SKIPPED_STALE),
            anyString(), eq(null), any());
        verify(adapterRegistry, never()).requireCommentClient(anyString());
        verify(commentClient, never()).createIssueComment(any(), any(), anyInt(), anyString());
        verify(commentClient, never()).updateIssueComment(any(), any(), anyString(), anyString());
    }

    @Test
    void transientExternalFailureEntersAutomaticBackoffWithoutRerunningReview()
    {
        ReviewDeliveryRecord record = claimedRecord(41L, ReviewDeliveryConstants.CHANNEL_GITHUB_PR_SUMMARY);
        record.setAttemptCount(1);
        ReviewTask task = successTask(10L);
        ReviewTaskRun run = successRun(100L);
        stubSummaryExecution(record, task, run);
        when(commentClient.findCommentWithMarker(any(), any(), anyInt(), anyString()))
            .thenThrow(new GitPullRequestCommentException("Git API 超时"));
        when(settings.maxAttempts()).thenReturn(5);
        when(settings.retryDelaySeconds(1)).thenReturn(60);
        when(deliveryMapper.failDelivery(eq(41L), eq("worker:delivery"),
            eq(ReviewDeliveryConstants.STATUS_FAILED), eq(ReviewDeliveryConstants.ERROR_EXTERNAL_CALL),
            anyString(), eq(60), any())).thenReturn(1);

        service.executeClaimedDelivery(41L, "worker:delivery");

        verify(deliveryMapper).failDelivery(eq(41L), eq("worker:delivery"),
            eq(ReviewDeliveryConstants.STATUS_FAILED), eq(ReviewDeliveryConstants.ERROR_EXTERNAL_CALL),
            anyString(), eq(60), any());
        verify(taskMapper, never()).updateTaskExecution(any());
    }

    @Test
    void configurationFailureMovesDeliveryToManualState()
    {
        ReviewDeliveryRecord record = claimedRecord(42L, ReviewDeliveryConstants.CHANNEL_FEISHU_BOT);
        when(deliveryMapper.selectDeliveryById(42L)).thenReturn(record);
        when(taskMapper.selectReviewTaskById(10L)).thenReturn(successTask(10L));
        when(runMapper.selectReviewTaskRunById(100L)).thenReturn(successRun(100L));
        when(projectMapper.selectReviewProjectById(3L)).thenReturn(project());
        when(deliveryMapper.failDelivery(eq(42L), eq("worker:delivery"),
            eq(ReviewDeliveryConstants.STATUS_MANUAL), eq(ReviewDeliveryConstants.ERROR_CONFIGURATION),
            anyString(), eq(null), any())).thenReturn(1);

        service.executeClaimedDelivery(42L, "worker:delivery");

        verify(deliveryMapper).failDelivery(eq(42L), eq("worker:delivery"),
            eq(ReviewDeliveryConstants.STATUS_MANUAL), eq(ReviewDeliveryConstants.ERROR_CONFIGURATION),
            anyString(), eq(null), any());
    }

    @Test
    void issueDispositionReturnsPendingInsteadOfPretendingExternalSuccess()
    {
        ReviewTask latest = successTask(50L);
        ReviewTaskRun run = successRun(500L);
        ReviewDeliveryRecord pending = new ReviewDeliveryRecord();
        pending.setDeliveryId(900L);
        when(taskMapper.selectLatestSuccessByProjectAndPr(3L, 8)).thenReturn(latest);
        when(runMapper.selectRunsByTaskId(50L)).thenReturn(List.of(run));
        when(intentService.enqueueSummary(latest, run,
            ReviewDeliveryConstants.TRIGGER_ISSUE_DISPOSITION, "system")).thenReturn(pending);

        var result = service.rerenderSummaryComment(3L, 8);

        assertEquals(ReviewDeliveryConstants.STATUS_PENDING, result.getStatus());
        assertEquals(900L, result.getDeliveryId());
    }

    @Test
    void contentSnapshotReadStillEnforcesProjectDataScope()
    {
        ReviewDeliveryRecord record = new ReviewDeliveryRecord();
        record.setDeliveryId(77L);
        record.setProjectId(3L);
        record.setProjectName("示例项目");
        record.setBusinessSystemName("核心系统");
        when(deliveryMapper.selectDeliveryById(77L)).thenReturn(record);
        when(projectMapper.selectReviewProjectById(3L)).thenReturn(project());
        when(deliveryMapper.selectContentSnapshotById(77L)).thenReturn(
            "{\"kind\":\"IM\",\"channelType\":\"FEISHU_BOT\",\"title\":\"t\",\"body\":\"b\"}");

        var content = service.selectDeliveryContent(77L);

        assertEquals("IM", content.get("kind"));
        assertEquals("核心系统", content.get("businessSystemName"));
        verify(deptService).checkDeptDataScope(1L);
    }

    private void stubSummaryExecution(ReviewDeliveryRecord record, ReviewTask task, ReviewTaskRun run)
    {
        when(deliveryMapper.selectDeliveryById(record.getDeliveryId())).thenReturn(record);
        when(taskMapper.selectReviewTaskById(10L)).thenReturn(task);
        when(runMapper.selectReviewTaskRunById(100L)).thenReturn(run);
        when(projectMapper.selectReviewProjectById(3L)).thenReturn(project());
        when(credentialMapper.selectGitCredentialById(5L)).thenReturn(credential());
        when(credentialService.getPlainToken(5L, true)).thenReturn("pat");
        when(adapterRegistry.requireCommentClient("GITHUB")).thenReturn(commentClient);
        when(contentFactory.build(any(), any(), any(), any())).thenReturn(ReviewSummaryContent.builder()
            .conclusion(ReviewPipelineConstants.CONCLUSION_PASS)
            .conclusionLabel("通过")
            .totalScore(90)
            .build());
        when(issueService.listRecheckingTitles(anyLong(), anyInt(), any())).thenReturn(List.of());
    }

    private static ReviewDeliveryRecord claimedRecord(Long deliveryId, String channel)
    {
        ReviewDeliveryRecord record = new ReviewDeliveryRecord();
        record.setDeliveryId(deliveryId);
        record.setTaskId(10L);
        record.setRunId(100L);
        record.setProjectId(3L);
        record.setChannel(channel);
        record.setLeaseOwner("worker:delivery");
        record.setAttemptCount(0);
        return record;
    }

    private static ReviewTask successTask(Long taskId)
    {
        ReviewTask task = new ReviewTask();
        task.setTaskId(taskId);
        task.setProjectId(3L);
        task.setProvider("GITHUB");
        task.setPrNumber(8);
        task.setTaskStatus(ReviewPipelineConstants.TASK_SUCCESS);
        task.setReviewConclusion(ReviewPipelineConstants.CONCLUSION_PASS);
        return task;
    }

    private static ReviewTaskRun successRun(Long runId)
    {
        ReviewTaskRun run = new ReviewTaskRun();
        run.setRunId(runId);
        run.setRunStatus(ReviewPipelineConstants.RUN_SUCCESS);
        run.setAttemptNo(1);
        return run;
    }

    private static ReviewProject project()
    {
        ReviewProject project = new ReviewProject();
        project.setProjectId(3L);
        project.setDeptId(1L);
        project.setProvider("GITHUB");
        project.setStatus("0");
        project.setCredentialId(5L);
        project.setRepositoryOwner("acme");
        project.setRepositoryName("repo");
        project.setRepositoryUrl("https://github.com/acme/repo.git");
        return project;
    }

    private static GitCredential credential()
    {
        GitCredential credential = new GitCredential();
        credential.setCredentialId(5L);
        credential.setStatus("0");
        return credential;
    }
}
