package com.acr.review.service.impl;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import com.acr.common.ai.LlmCallService;
import com.acr.common.exception.ServiceException;
import com.acr.review.domain.ReviewPipelineConstants;
import com.acr.review.domain.ReviewProject;
import com.acr.review.domain.ReviewTask;
import com.acr.review.domain.ReviewTaskRun;
import com.acr.review.domain.ReviewTemplate;
import com.acr.review.engine.OpenCodeReviewCliAdapter;
import com.acr.review.engine.OcrModelConfigMapper;
import com.acr.review.engine.ReviewEngineWorkspaceManager;
import com.acr.review.engine.config.ReviewEngineProperties;
import com.acr.review.git.GitPullRequestDiffFetcher;
import com.acr.review.git.GitPullRequestDiffResult;
import com.acr.review.git.GitPullRequestMetadataFetcher;
import com.acr.review.git.GitPullRequestWorkspacePreparer;
import com.acr.review.mapper.ReviewProjectMapper;
import com.acr.review.mapper.ReviewTaskMapper;
import com.acr.review.mapper.ReviewTaskRunMapper;
import com.acr.review.service.IGitCredentialService;
import com.acr.review.service.IReviewTemplateService;
import com.acr.review.service.ReviewConclusionResolver;
import com.acr.review.service.ReviewPromptComposer;
import com.acr.review.service.ReviewPromptRenderer;
import com.acr.review.service.ReviewScoreResultParser;
import com.acr.system.domain.SysAiModelConfig;
import com.acr.system.service.ISysAiModelConfigService;

class ReviewTaskExecutionServiceImplTest
{
    private final ReviewTaskMapper taskMapper = mock(ReviewTaskMapper.class);
    private final ReviewTaskRunMapper runMapper = mock(ReviewTaskRunMapper.class);
    private final ReviewProjectMapper projectMapper = mock(ReviewProjectMapper.class);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private final IGitCredentialService credentialService = mock(IGitCredentialService.class);
    private final ISysAiModelConfigService modelConfigService = mock(ISysAiModelConfigService.class);
    private final IReviewTemplateService templateService = mock(IReviewTemplateService.class);
    private final GitPullRequestDiffFetcher diffFetcher = mock(GitPullRequestDiffFetcher.class);
    private ReviewTaskExecutionServiceImpl service;

    @BeforeEach
    void setUp()
    {
        ReviewEngineProperties properties = new ReviewEngineProperties();
        properties.setMaxConcurrency(2);
        service = new ReviewTaskExecutionServiceImpl(
            taskMapper, runMapper, projectMapper,
            credentialService,
            modelConfigService,
            mock(OcrModelConfigMapper.class),
            mock(GitPullRequestWorkspacePreparer.class),
            diffFetcher,
            mock(GitPullRequestMetadataFetcher.class),
            mock(OpenCodeReviewCliAdapter.class),
            mock(ReviewEngineWorkspaceManager.class),
            properties,
            new ReviewConclusionResolver(),
            new ReviewPromptRenderer(),
            new ReviewPromptComposer(),
            new ReviewScoreResultParser(),
            mock(LlmCallService.class),
            eventPublisher,
            new ReviewTaskSnapshotServiceImpl(templateService, modelConfigService, properties),
            120);
    }

    @Test
    void skipsWhenClaimFails()
    {
        when(taskMapper.claimTask(eq(9L), any(), any(Date.class), anyInt())).thenReturn(0);
        service.executeTask(9L);
        verify(runMapper, never()).insertReviewTaskRun(any());
    }

    @Test
    void marksTaskFailedWhenRunInsertCrashes()
    {
        // run 插入异常（如快照列约束冲突）时必须把任务落为 FAILED，不能留下僵尸 RUNNING
        ReviewTask task = new ReviewTask();
        task.setTaskId(11L);
        task.setProjectId(2L);
        task.setBaseSha("abc1234");
        task.setHeadSha("def5678");
        when(taskMapper.claimTask(eq(11L), any(), any(Date.class), anyInt())).thenReturn(1);
        when(taskMapper.selectReviewTaskById(11L)).thenReturn(task);
        when(runMapper.selectMaxAttemptNo(11L)).thenReturn(null);
        org.mockito.Mockito.doThrow(new org.springframework.dao.DataIntegrityViolationException("Column 'snapshot_review_mode' cannot be null"))
            .when(runMapper).insertReviewTaskRun(any(ReviewTaskRun.class));

        service.executeTask(11L);

        org.mockito.ArgumentCaptor<ReviewTask> captor = org.mockito.ArgumentCaptor.forClass(ReviewTask.class);
        verify(taskMapper).updateTaskExecution(captor.capture());
        ReviewTask saved = captor.getValue();
        org.junit.jupiter.api.Assertions.assertEquals(ReviewPipelineConstants.TASK_FAILED, saved.getTaskStatus());
        org.junit.jupiter.api.Assertions.assertEquals(ReviewPipelineConstants.FAILURE_UNKNOWN, saved.getFailureType());
    }

    @Test
    void insertRunCopiesSnapshotReviewModeFromTask()
    {
        ReviewTask task = new ReviewTask();
        task.setTaskId(12L);
        task.setProjectId(2L);
        task.setBaseSha("abc1234");
        task.setHeadSha("def5678");
        task.setSnapshotReviewMode("OCR_PR_DIFF");
        when(taskMapper.claimTask(eq(12L), any(), any(Date.class), anyInt())).thenReturn(1);
        when(taskMapper.selectReviewTaskById(12L)).thenReturn(task);
        when(runMapper.selectMaxAttemptNo(12L)).thenReturn(null);
        when(projectMapper.selectReviewProjectById(2L)).thenReturn(null);

        service.executeTask(12L);

        org.mockito.ArgumentCaptor<ReviewTaskRun> captor = org.mockito.ArgumentCaptor.forClass(ReviewTaskRun.class);
        verify(runMapper).insertReviewTaskRun(captor.capture());
        // 历史兼容值在 run 建立时即归一化，且不为 NULL（兜底 NOT NULL 约束）
        org.junit.jupiter.api.Assertions.assertEquals(
            ReviewPipelineConstants.REVIEW_MODE_OCR_ENGINE, captor.getValue().getSnapshotReviewMode());
        // 项目不存在应落 FAILED 而非抛异常传播（一次状态推进 + 一次失败落库，取最后一次）
        org.mockito.ArgumentCaptor<ReviewTask> taskCaptor = org.mockito.ArgumentCaptor.forClass(ReviewTask.class);
        verify(taskMapper, org.mockito.Mockito.times(2)).updateTaskExecution(taskCaptor.capture());
        ReviewTask failedTask = taskCaptor.getAllValues().get(taskCaptor.getAllValues().size() - 1);
        org.junit.jupiter.api.Assertions.assertEquals(
            ReviewPipelineConstants.TASK_FAILED, failedTask.getTaskStatus());
        org.junit.jupiter.api.Assertions.assertEquals(
            ReviewPipelineConstants.FAILURE_CONFIG_MISSING, failedTask.getFailureType());
    }

    @Test
    void legacyTaskWithoutSnapshotIsRepairedFromProjectConfig()
    {
        // 快照冻结上线前建单的历史任务：执行时按项目当前配置补冻结快照，随后进入正常 LLM 链路
        ReviewTask task = new ReviewTask();
        task.setTaskId(13L);
        task.setProjectId(2L);
        task.setBaseSha("abc1234");
        task.setHeadSha("def5678");
        when(taskMapper.claimTask(eq(13L), any(), any(Date.class), anyInt())).thenReturn(1);
        when(taskMapper.selectReviewTaskById(13L)).thenReturn(task);
        when(runMapper.selectMaxAttemptNo(13L)).thenReturn(null);

        ReviewProject project = new ReviewProject();
        project.setProjectId(2L);
        project.setStatus("0");
        project.setProvider("GITHUB");
        project.setReviewMode("LLM_DIRECT");
        project.setModelId(3L);
        project.setTemplateId(4L);
        when(projectMapper.selectReviewProjectById(2L)).thenReturn(project);

        SysAiModelConfig model = new SysAiModelConfig();
        model.setModelId(3L);
        model.setModelName("DeepSeek");
        model.setProvider("deepseek");
        model.setModel("deepseek-chat");
        model.setEnabled("1");
        model.setApiKey("secret");
        when(modelConfigService.selectRuntimeConfigById(3L)).thenReturn(model);
        ReviewTemplate template = new ReviewTemplate();
        template.setTemplateId(4L);
        template.setTemplateName("Java");
        template.setTemplateCode("builtin_java");
        template.setVersionNo(2);
        template.setContent("模板正文");
        template.setStatus("0");
        when(templateService.selectEnabledTemplateById(4L)).thenReturn(template);
        when(diffFetcher.fetchDiff(any(), any(), eq("abc1234"), eq("def5678")))
            .thenReturn(GitPullRequestDiffResult.fail(ReviewPipelineConstants.FAILURE_RATE_LIMIT, "limited"));

        service.executeTask(13L);

        org.mockito.ArgumentCaptor<ReviewTask> snapshotCaptor = org.mockito.ArgumentCaptor.forClass(ReviewTask.class);
        verify(taskMapper).updateTaskSnapshot(snapshotCaptor.capture());
        ReviewTask frozen = snapshotCaptor.getValue();
        org.junit.jupiter.api.Assertions.assertEquals("LLM_DIRECT", frozen.getSnapshotReviewMode());
        org.junit.jupiter.api.Assertions.assertEquals(Long.valueOf(3L), frozen.getSnapshotModelId());
        org.junit.jupiter.api.Assertions.assertEquals(Long.valueOf(4L), frozen.getSnapshotTemplateId());
        org.junit.jupiter.api.Assertions.assertEquals("模板正文", frozen.getSnapshotPromptContent());
        // 补冻结后走到了 LLM 链路的 Diff 拉取，证明配置解析已通过
        verify(diffFetcher).fetchDiff(any(), any(), eq("abc1234"), eq("def5678"));
    }

    @Test
    void legacyTaskFailsWithGuidanceWhenProjectNotConfigured()
    {
        // 历史任务 + 项目仍未配置审查方式：失败消息必须给出配置指引，而不是「不支持的审查方式：null」
        ReviewTask task = new ReviewTask();
        task.setTaskId(14L);
        task.setProjectId(2L);
        task.setBaseSha("abc1234");
        task.setHeadSha("def5678");
        when(taskMapper.claimTask(eq(14L), any(), any(Date.class), anyInt())).thenReturn(1);
        when(taskMapper.selectReviewTaskById(14L)).thenReturn(task);
        when(runMapper.selectMaxAttemptNo(14L)).thenReturn(null);

        ReviewProject project = new ReviewProject();
        project.setProjectId(2L);
        project.setStatus("0");
        project.setProvider("GITHUB");
        when(projectMapper.selectReviewProjectById(2L)).thenReturn(project);

        service.executeTask(14L);

        verify(taskMapper, never()).updateTaskSnapshot(any());
        org.mockito.ArgumentCaptor<ReviewTask> captor = org.mockito.ArgumentCaptor.forClass(ReviewTask.class);
        verify(taskMapper, org.mockito.Mockito.times(2)).updateTaskExecution(captor.capture());
        ReviewTask failed = captor.getAllValues().get(captor.getAllValues().size() - 1);
        org.junit.jupiter.api.Assertions.assertEquals(ReviewPipelineConstants.TASK_FAILED, failed.getTaskStatus());
        org.junit.jupiter.api.Assertions.assertEquals(ReviewPipelineConstants.FAILURE_CONFIG_MISSING, failed.getFailureType());
        assertTrue(failed.getFailureMessage().contains("未配置有效的审查方式"));
    }

    @Test
    void retryRejectsCompletedTask()
    {
        ReviewTask task = new ReviewTask();
        task.setTaskId(3L);
        task.setTaskStatus(ReviewPipelineConstants.TASK_SUCCESS);
        when(taskMapper.selectReviewTaskById(3L)).thenReturn(task);
        ServiceException ex = assertThrows(ServiceException.class, () -> service.retryTask(3L));
        assertTrue(ex.getMessage().contains("已完成"));
    }

    @Test
    void retryRejectsFreshRunningTask()
    {
        ReviewTask task = new ReviewTask();
        task.setTaskId(6L);
        task.setTaskStatus(ReviewPipelineConstants.TASK_RUNNING);
        task.setStartedTime(new Date());
        when(taskMapper.selectReviewTaskById(6L)).thenReturn(task);
        ServiceException ex = assertThrows(ServiceException.class, () -> service.retryTask(6L));
        assertTrue(ex.getMessage().contains("执行中"));
    }

    @Test
    void retryAllowsStaleRunningTask()
    {
        ReviewTask task = new ReviewTask();
        task.setTaskId(7L);
        task.setTaskStatus(ReviewPipelineConstants.TASK_RUNNING);
        task.setStartedTime(new Date(System.currentTimeMillis() - 60L * 60_000L));
        when(taskMapper.selectReviewTaskById(7L)).thenReturn(task);
        service.retryTask(7L);
        verify(eventPublisher).publishEvent(any());
    }

    @Test
    void retryAllowsPendingTask()
    {
        ReviewTask task = new ReviewTask();
        task.setTaskId(4L);
        task.setTaskStatus(ReviewPipelineConstants.TASK_PENDING);
        when(taskMapper.selectReviewTaskById(4L)).thenReturn(task);
        service.retryTask(4L);
        verify(eventPublisher).publishEvent(any());
    }

    @Test
    void retryAllowsFailedTask()
    {
        ReviewTask task = new ReviewTask();
        task.setTaskId(5L);
        task.setTaskStatus(ReviewPipelineConstants.TASK_FAILED);
        when(taskMapper.selectReviewTaskById(5L)).thenReturn(task);
        service.retryTask(5L);
        verify(eventPublisher).publishEvent(any());
    }

    @Test
    void schedulePublishesEvent()
    {
        service.scheduleExecution(8L);
        verify(eventPublisher).publishEvent(any());
    }
}
