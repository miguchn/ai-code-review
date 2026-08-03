package com.acr.review.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
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
import com.acr.review.delivery.ReviewSummaryContent;
import com.acr.review.delivery.ReviewSummaryContentFactory;
import com.acr.review.domain.ReviewDeliveryRecord;
import com.acr.review.domain.ReviewNotifyChannel;
import com.acr.review.domain.ReviewPipelineConstants;
import com.acr.review.domain.ReviewProject;
import com.acr.review.domain.ReviewTask;
import com.acr.review.domain.ReviewTaskRun;
import com.acr.review.git.GitPullRequestComment;
import com.acr.review.git.GitPullRequestCommentClient;
import com.acr.review.git.GitPullRequestCommentException;
import com.acr.review.mapper.ReviewDeliveryRecordMapper;
import com.acr.review.mapper.ReviewIssueMapper;
import com.acr.review.mapper.ReviewProjectMapper;
import com.acr.review.mapper.ReviewTaskMapper;
import com.acr.review.mapper.ReviewTaskRunMapper;
import com.acr.review.notify.NotifyRobotClient;
import com.acr.review.notify.NotifyRobotClients;
import com.acr.review.notify.NotifyRobotException;
import com.acr.review.service.IGitCredentialService;
import com.acr.review.service.IReviewNotifyChannelService;
import com.acr.review.service.IReviewNotifyChannelService.DecryptedNotifyChannel;
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
    @Mock private GitPullRequestCommentClient commentClient;

    private ReviewDeliveryServiceImpl service;

    @BeforeEach
    void setUp()
    {
        service = new ReviewDeliveryServiceImpl(deliveryMapper, taskMapper, runMapper, projectMapper,
            issueMapper, credentialService, notifyChannelService, robotClients, contentFactory,
            deptService, commentClient);
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
        when(contentFactory.build(any(), any(), any())).thenAnswer(inv -> {
            ReviewTask task = inv.getArgument(0);
            return ReviewSummaryContent.builder()
                .conclusion(task.getReviewConclusion())
                .conclusionLabel(ReviewPipelineConstants.CONCLUSION_BLOCK.equals(task.getReviewConclusion())
                    ? "高风险" : "通过")
                .totalScore(task.getTotalScore())
                .build();
        });
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

    @Test
    void imDeliversSuccessMessageAndUpsertsTaskLevelKey()
    {
        ReviewTask task = successTask(30L, 3L, 8);
        when(projectMapper.selectReviewProjectById(3L)).thenReturn(notifyProject("Y", "Y", 7L));
        when(notifyChannelService.getDecryptedChannel(7L, true))
            .thenReturn(decryptedChannel(ReviewDeliveryConstants.CHANNEL_DINGTALK_ROBOT));
        when(contentFactory.build(any(), any(), any()))
            .thenReturn(ReviewSummaryContent.builder().conclusionLabel("通过").build());
        NotifyRobotClient robot = mock(NotifyRobotClient.class);
        when(robotClients.require(ReviewDeliveryConstants.CHANNEL_DINGTALK_ROBOT)).thenReturn(robot);
        when(deliveryMapper.selectByIdempotencyKey(anyString())).thenReturn(null);

        service.deliverNotifyAfterTerminal(task, successRun(300L, 1));

        verify(robot).send(eq("https://robot.example/send"), eq(null), anyString(), anyString());
        ArgumentCaptor<ReviewDeliveryRecord> captor = ArgumentCaptor.forClass(ReviewDeliveryRecord.class);
        verify(deliveryMapper).insertDelivery(captor.capture());
        assertEquals(ReviewDeliveryConstants.STATUS_SUCCESS, captor.getValue().getDeliveryStatus());
        assertEquals(ReviewDeliveryConstants.CHANNEL_DINGTALK_ROBOT, captor.getValue().getChannel());
        assertEquals(ReviewDeliveryConstants.imIdempotencyKey(
            ReviewDeliveryConstants.CHANNEL_DINGTALK_ROBOT, 30L), captor.getValue().getIdempotencyKey());
        assertNull(captor.getValue().getExternalId());
    }

    @Test
    void imSkipsWhenNotifyDisabled()
    {
        ReviewTask task = successTask(31L, 3L, 8);
        when(projectMapper.selectReviewProjectById(3L)).thenReturn(notifyProject("N", "Y", 7L));

        service.deliverNotifyAfterTerminal(task, successRun(301L, 1));

        verify(notifyChannelService, never()).getDecryptedChannel(anyLong(), anyBoolean());
        verify(deliveryMapper, never()).insertDelivery(any());
    }

    @Test
    void imSkipsFailedTaskWhenFailureNotifyOff()
    {
        ReviewTask task = successTask(32L, 3L, 8);
        task.setTaskStatus(ReviewPipelineConstants.TASK_FAILED);
        when(projectMapper.selectReviewProjectById(3L)).thenReturn(notifyProject("Y", "N", 7L));

        service.deliverNotifyAfterTerminal(task, successRun(302L, 1));

        verify(notifyChannelService, never()).getDecryptedChannel(anyLong(), anyBoolean());
        verify(deliveryMapper, never()).insertDelivery(any());
    }

    @Test
    void imDisabledChannelPersistsFailedWithRealChannelType()
    {
        ReviewTask task = successTask(33L, 3L, 8);
        when(projectMapper.selectReviewProjectById(3L)).thenReturn(notifyProject("Y", "Y", 7L));
        when(notifyChannelService.getDecryptedChannel(7L, true)).thenThrow(new ServiceException("通知渠道已停用"));
        ReviewNotifyChannel channel = new ReviewNotifyChannel();
        channel.setChannelId(7L);
        channel.setChannelType(ReviewDeliveryConstants.CHANNEL_DINGTALK_ROBOT);
        when(notifyChannelService.selectReviewNotifyChannelById(7L)).thenReturn(channel);
        when(deliveryMapper.selectByIdempotencyKey(anyString())).thenReturn(null);

        service.deliverNotifyAfterTerminal(task, successRun(303L, 1));

        ArgumentCaptor<ReviewDeliveryRecord> captor = ArgumentCaptor.forClass(ReviewDeliveryRecord.class);
        verify(deliveryMapper).insertDelivery(captor.capture());
        assertEquals(ReviewDeliveryConstants.STATUS_FAILED, captor.getValue().getDeliveryStatus());
        assertEquals(ReviewDeliveryConstants.CHANNEL_DINGTALK_ROBOT, captor.getValue().getChannel());
        assertTrue(captor.getValue().getFailureMessage().contains("停用"));
    }

    @Test
    void imRobotFailurePersistsFailedWithoutRethrow()
    {
        ReviewTask task = successTask(34L, 3L, 8);
        when(projectMapper.selectReviewProjectById(3L)).thenReturn(notifyProject("Y", "Y", 7L));
        when(notifyChannelService.getDecryptedChannel(7L, true))
            .thenReturn(decryptedChannel(ReviewDeliveryConstants.CHANNEL_WECOM_ROBOT));
        when(contentFactory.build(any(), any(), any()))
            .thenReturn(ReviewSummaryContent.builder().conclusionLabel("通过").build());
        NotifyRobotClient robot = mock(NotifyRobotClient.class);
        when(robotClients.require(ReviewDeliveryConstants.CHANNEL_WECOM_ROBOT)).thenReturn(robot);
        doThrow(new NotifyRobotException("企微机器人发送失败：HTTP 400"))
            .when(robot).send(anyString(), any(), anyString(), anyString());
        ReviewNotifyChannel channel = new ReviewNotifyChannel();
        channel.setChannelId(7L);
        channel.setChannelType(ReviewDeliveryConstants.CHANNEL_WECOM_ROBOT);
        when(notifyChannelService.selectReviewNotifyChannelById(7L)).thenReturn(channel);
        when(deliveryMapper.selectByIdempotencyKey(anyString())).thenReturn(null);

        service.deliverNotifyAfterTerminal(task, successRun(304L, 1));

        ArgumentCaptor<ReviewDeliveryRecord> captor = ArgumentCaptor.forClass(ReviewDeliveryRecord.class);
        verify(deliveryMapper).insertDelivery(captor.capture());
        assertEquals(ReviewDeliveryConstants.STATUS_FAILED, captor.getValue().getDeliveryStatus());
        assertEquals(ReviewDeliveryConstants.CHANNEL_WECOM_ROBOT, captor.getValue().getChannel());
        assertTrue(captor.getValue().getFailureMessage().contains("HTTP 400"));
    }

    @Test
    void retryByIdForImRowRendersOriginalTaskAndUpsertsSameKey()
    {
        ReviewDeliveryRecord record = new ReviewDeliveryRecord();
        record.setDeliveryId(900L);
        record.setTaskId(40L);
        record.setProjectId(3L);
        record.setPrNumber(8);
        record.setChannel(ReviewDeliveryConstants.CHANNEL_DINGTALK_ROBOT);
        when(deliveryMapper.selectDeliveryById(900L)).thenReturn(record);
        when(projectMapper.selectReviewProjectById(3L)).thenReturn(notifyProject("Y", "Y", 7L));
        when(taskMapper.selectReviewTaskById(40L)).thenReturn(successTask(40L, 3L, 8));
        when(runMapper.selectRunsByTaskId(40L)).thenReturn(List.of(successRun(400L, 1)));
        when(notifyChannelService.getDecryptedChannel(7L, true))
            .thenReturn(decryptedChannel(ReviewDeliveryConstants.CHANNEL_DINGTALK_ROBOT));
        when(contentFactory.build(any(), any(), any()))
            .thenReturn(ReviewSummaryContent.builder().conclusionLabel("通过").build());
        NotifyRobotClient robot = mock(NotifyRobotClient.class);
        when(robotClients.require(ReviewDeliveryConstants.CHANNEL_DINGTALK_ROBOT)).thenReturn(robot);
        when(deliveryMapper.selectByIdempotencyKey(anyString())).thenReturn(null);

        service.retryDeliveryById(900L);

        verify(robot).send(anyString(), any(), anyString(), anyString());
        ArgumentCaptor<ReviewDeliveryRecord> captor = ArgumentCaptor.forClass(ReviewDeliveryRecord.class);
        verify(deliveryMapper).insertDelivery(captor.capture());
        assertEquals(ReviewDeliveryConstants.STATUS_SUCCESS, captor.getValue().getDeliveryStatus());
        assertEquals(ReviewDeliveryConstants.imIdempotencyKey(
            ReviewDeliveryConstants.CHANNEL_DINGTALK_ROBOT, 40L), captor.getValue().getIdempotencyKey());
    }

    @Test
    void selectLatestImDeliveryChecksDeptScopeAndReturnsRecord()
    {
        ReviewDeliveryRecord record = new ReviewDeliveryRecord();
        record.setDeliveryId(77L);
        record.setTaskId(50L);
        record.setChannel(ReviewDeliveryConstants.CHANNEL_DINGTALK_ROBOT);
        when(taskMapper.selectReviewTaskById(50L)).thenReturn(successTask(50L, 3L, 8));
        when(projectMapper.selectReviewProjectById(3L)).thenReturn(project());
        when(deliveryMapper.selectLatestImByTaskId(50L)).thenReturn(record);

        ReviewDeliveryRecord result = service.selectLatestImDelivery(50L);

        assertEquals(77L, result.getDeliveryId());
        verify(deptService).checkDeptDataScope(1L);
        verify(deliveryMapper).selectLatestImByTaskId(50L);
    }

    @Test
    void selectLatestImDeliveryThrowsWhenTaskMissing()
    {
        when(taskMapper.selectReviewTaskById(51L)).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class, () -> service.selectLatestImDelivery(51L));
        assertTrue(ex.getMessage().contains("审查任务不存在"));
        verify(deliveryMapper, never()).selectLatestImByTaskId(anyLong());
    }

    @Test
    void retryByIdForGithubRowDelegatesToLatestSuccessTask()
    {
        ReviewDeliveryRecord record = new ReviewDeliveryRecord();
        record.setDeliveryId(901L);
        record.setTaskId(20L);
        record.setProjectId(3L);
        record.setPrNumber(8);
        record.setChannel(ReviewDeliveryConstants.CHANNEL_GITHUB_PR_SUMMARY);
        when(deliveryMapper.selectDeliveryById(901L)).thenReturn(record);
        when(taskMapper.selectReviewTaskById(20L)).thenReturn(successTask(20L, 3L, 8));
        when(projectMapper.selectReviewProjectById(3L)).thenReturn(project());
        ReviewTask latest = successTask(21L, 3L, 8);
        when(taskMapper.selectLatestSuccessByProjectAndPr(3L, 8)).thenReturn(latest);
        when(runMapper.selectRunsByTaskId(21L)).thenReturn(List.of(successRun(401L, 1)));
        when(credentialService.getPlainToken(5L, true)).thenReturn("pat");
        when(contentFactory.build(any(), any(), any()))
            .thenReturn(ReviewSummaryContent.builder().conclusionLabel("通过").build());
        when(commentClient.findCommentWithMarker(any(), anyString(), anyInt(), anyString()))
            .thenReturn(Optional.empty());
        when(commentClient.createIssueComment(any(), anyString(), anyInt(), anyString()))
            .thenReturn(new GitPullRequestComment("55", "body"));
        when(deliveryMapper.selectByIdempotencyKey(anyString())).thenReturn(null);

        service.retryDeliveryById(901L);

        verify(taskMapper).selectLatestSuccessByProjectAndPr(3L, 8);
        verify(commentClient).createIssueComment(any(), eq("pat"), eq(8), anyString());
    }

    @Test
    void rerenderSummaryCommentReturnsSkippedWhenNoSuccessTask()
    {
        when(taskMapper.selectLatestSuccessByProjectAndPr(3L, 8)).thenReturn(null);

        String status = service.rerenderSummaryComment(3L, 8);

        assertEquals("SKIPPED", status);
        verify(commentClient, never()).findCommentWithMarker(any(), anyString(), anyInt(), anyString());
        verify(commentClient, never()).createIssueComment(any(), anyString(), anyInt(), anyString());
        verify(commentClient, never()).updateIssueComment(any(), anyString(), anyString(), anyString());
        verify(deliveryMapper, never()).insertDelivery(any());
        verify(deliveryMapper, never()).updateDeliveryResult(any());
    }

    @Test
    void rerenderSummaryCommentReturnsSuccessAndUpserts()
    {
        ReviewTask latest = successTask(21L, 3L, 8);
        when(taskMapper.selectLatestSuccessByProjectAndPr(3L, 8)).thenReturn(latest);
        when(runMapper.selectRunsByTaskId(21L)).thenReturn(List.of(successRun(401L, 1)));
        stubProjectAndToken();
        when(contentFactory.build(any(), any(), any()))
            .thenReturn(ReviewSummaryContent.builder().conclusionLabel("通过").build());
        when(commentClient.findCommentWithMarker(any(), anyString(), anyInt(), anyString()))
            .thenReturn(Optional.empty());
        when(commentClient.createIssueComment(any(), anyString(), anyInt(), anyString()))
            .thenReturn(new GitPullRequestComment("88", "body"));
        when(deliveryMapper.selectByIdempotencyKey(anyString())).thenReturn(null);

        String status = service.rerenderSummaryComment(3L, 8);

        assertEquals(ReviewDeliveryConstants.STATUS_SUCCESS, status);
        ArgumentCaptor<ReviewDeliveryRecord> captor = ArgumentCaptor.forClass(ReviewDeliveryRecord.class);
        verify(deliveryMapper).insertDelivery(captor.capture());
        assertEquals(ReviewDeliveryConstants.STATUS_SUCCESS, captor.getValue().getDeliveryStatus());
        assertEquals("88", captor.getValue().getExternalId());
    }

    @Test
    void rerenderSummaryCommentReturnsFailedWithoutRethrow()
    {
        ReviewTask latest = successTask(22L, 3L, 8);
        when(taskMapper.selectLatestSuccessByProjectAndPr(3L, 8)).thenReturn(latest);
        when(runMapper.selectRunsByTaskId(22L)).thenReturn(List.of(successRun(402L, 1)));
        stubProjectAndToken();
        when(contentFactory.build(any(), any(), any()))
            .thenReturn(ReviewSummaryContent.builder().conclusionLabel("通过").build());
        when(commentClient.findCommentWithMarker(any(), anyString(), anyInt(), anyString()))
            .thenThrow(new GitPullRequestCommentException("GitHub API 超时"));
        when(deliveryMapper.selectByIdempotencyKey(anyString())).thenReturn(null);

        String status = service.rerenderSummaryComment(3L, 8);

        assertEquals(ReviewDeliveryConstants.STATUS_FAILED, status);
        ArgumentCaptor<ReviewDeliveryRecord> captor = ArgumentCaptor.forClass(ReviewDeliveryRecord.class);
        verify(deliveryMapper).insertDelivery(captor.capture());
        assertEquals(ReviewDeliveryConstants.STATUS_FAILED, captor.getValue().getDeliveryStatus());
        assertTrue(captor.getValue().getFailureMessage().contains("超时"));
        assertNull(captor.getValue().getExternalId());
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

    private static ReviewProject notifyProject(String notifyEnabled, String notifyOnFailure, Long channelId)
    {
        ReviewProject project = project();
        project.setNotifyEnabled(notifyEnabled);
        project.setNotifyOnFailure(notifyOnFailure);
        project.setNotifyChannelId(channelId);
        return project;
    }

    private static DecryptedNotifyChannel decryptedChannel(String channelType)
    {
        return new DecryptedNotifyChannel(7L, "研发群", channelType, "https://robot.example/send", null);
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
