package com.acr.review.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import com.acr.common.ai.LlmCallService;
import com.acr.common.exception.ServiceException;
import com.acr.review.domain.ReviewPipelineConstants;
import com.acr.review.domain.GitCredential;
import com.acr.review.domain.ReviewProject;
import com.acr.review.domain.ReviewTask;
import com.acr.review.domain.ReviewTaskRun;
import com.acr.review.domain.ReviewTemplate;
import com.acr.review.engine.OpenCodeReviewCliAdapter;
import com.acr.review.engine.OcrModelConfigMapper;
import com.acr.review.engine.ReviewEngineResultMapper;
import com.acr.review.engine.ReviewEngineWorkspaceManager;
import com.acr.review.engine.config.ReviewEngineProperties;
import com.acr.review.delivery.ReviewDeliveryIntentService;
import com.acr.review.git.GitAdapterRegistry;
import com.acr.review.git.GitCommandRunner;
import com.acr.review.git.GitPullRequestDiffFetcher;
import com.acr.review.git.GitPullRequestDiffResult;
import com.acr.review.git.GitPullRequestMetadataFetcher;
import com.acr.review.git.GitPullRequestWorkspacePreparer;
import com.acr.review.mapper.GitCredentialMapper;
import com.acr.review.mapper.ReviewProjectMapper;
import com.acr.review.mapper.ReviewTaskMapper;
import com.acr.review.mapper.ReviewTaskRunMapper;
import com.acr.review.service.IGitCredentialService;
import com.acr.review.service.IReviewIssueService;
import com.acr.review.service.IReviewTemplateService;
import com.acr.review.service.ReviewConclusionResolver;
import com.acr.review.service.ReviewPromptComposer;
import com.acr.review.service.ReviewPromptRenderer;
import com.acr.review.scope.UnifiedDiffParser;
import com.acr.review.scheduling.ReviewResourceBudgetService;
import com.acr.review.scheduling.ReviewTaskLeaseManager;
import com.acr.review.scheduling.ReviewTaskRetryPolicy;
import com.acr.review.scheduling.ReviewTaskRuntimeSettings;
import com.acr.review.scheduling.ReviewTaskWorkerIdentity;
import com.acr.review.service.ReviewScoreResultParser;
import com.acr.system.domain.SysAiModelConfig;
import com.acr.system.service.ISysAiModelConfigService;

class ReviewTaskExecutionServiceImplTest
{
    private final ReviewTaskMapper taskMapper = mock(ReviewTaskMapper.class);
    private final ReviewTaskRunMapper runMapper = mock(ReviewTaskRunMapper.class);
    private final ReviewProjectMapper projectMapper = mock(ReviewProjectMapper.class);
    private final GitCredentialMapper credentialMapper = mock(GitCredentialMapper.class);
    private final GitAdapterRegistry adapterRegistry = mock(GitAdapterRegistry.class);
    private final ApplicationEventPublisher eventPublisher = mock(ApplicationEventPublisher.class);
    private final IGitCredentialService credentialService = mock(IGitCredentialService.class);
    private final ISysAiModelConfigService modelConfigService = mock(ISysAiModelConfigService.class);
    private final IReviewTemplateService templateService = mock(IReviewTemplateService.class);
    private final GitPullRequestDiffFetcher diffFetcher = mock(GitPullRequestDiffFetcher.class);
    private final com.acr.review.git.GitFileContentFetcher fileContentFetcher =
        mock(com.acr.review.git.GitFileContentFetcher.class);
    private final LlmCallService llmCallService = mock(LlmCallService.class);
    private final OcrModelConfigMapper modelConfigMapper = mock(OcrModelConfigMapper.class);
    private final GitPullRequestWorkspacePreparer workspacePreparer = mock(GitPullRequestWorkspacePreparer.class);
    private final GitPullRequestMetadataFetcher metadataFetcher = mock(GitPullRequestMetadataFetcher.class);
    private final OpenCodeReviewCliAdapter reviewEngine = mock(OpenCodeReviewCliAdapter.class);
    private final ReviewEngineWorkspaceManager workspaceManager = mock(ReviewEngineWorkspaceManager.class);
    private final IReviewIssueService issueService = mock(IReviewIssueService.class);
    private final ReviewDeliveryIntentService deliveryIntentService = mock(ReviewDeliveryIntentService.class);
    private final ReviewTaskCompletionService completionService = new ReviewTaskCompletionService(
        taskMapper, runMapper, issueService, deliveryIntentService);
    private final GitCommandRunner gitCommandRunner = mock(GitCommandRunner.class);
    private final ReviewTaskRuntimeSettings runtimeSettings = mock(ReviewTaskRuntimeSettings.class);
    private final ReviewTaskRetryPolicy retryPolicy = new ReviewTaskRetryPolicy(runtimeSettings);
    private final ReviewTaskWorkerIdentity workerIdentity = mock(ReviewTaskWorkerIdentity.class);
    private final ReviewTaskLeaseManager leaseManager = mock(ReviewTaskLeaseManager.class);
    private final ReviewResourceBudgetService budgetService = mock(ReviewResourceBudgetService.class);
    private ReviewTaskExecutionServiceImpl service;

    @BeforeEach
    void setUp()
    {
        ReviewEngineProperties properties = new ReviewEngineProperties();
        properties.setMaxConcurrency(2);
        when(runtimeSettings.leaseSeconds()).thenReturn(900);
        when(runtimeSettings.maxRetries()).thenReturn(3);
        when(runtimeSettings.retryDelaySeconds(anyInt())).thenReturn(30);
        when(runtimeSettings.budgetBackoffSeconds()).thenReturn(30);
        when(workerIdentity.owner()).thenReturn("worker-test");
        when(taskMapper.updateTaskExecution(any())).thenReturn(1);
        when(taskMapper.updateTaskSnapshot(any())).thenReturn(1);
        when(budgetService.tryAcquireOcrExecution()).thenReturn(mock(com.acr.review.scheduling.ReviewBudgetLease.class));
        when(budgetService.tryAcquireLlmCall()).thenReturn(mock(com.acr.review.scheduling.ReviewBudgetLease.class));
        wireGithubAdapters();
        service = new ReviewTaskExecutionServiceImpl(
            taskMapper, runMapper, projectMapper, credentialMapper,
            credentialService, adapterRegistry,
            modelConfigService,
            modelConfigMapper,
            reviewEngine,
            new ReviewEngineResultMapper(),
            workspaceManager,
            properties,
            new ReviewConclusionResolver(),
            new ReviewPromptRenderer(),
            new ReviewPromptComposer(),
            new ReviewScoreResultParser(),
            new com.acr.review.scope.UnifiedDiffParser(),
            new com.acr.review.scope.ReviewScopeDecisionService(),
            new com.acr.review.scope.ReviewScopePromptAssembler(),
            llmCallService,
            eventPublisher,
            new ReviewTaskSnapshotServiceImpl(templateService, modelConfigService, properties),
            completionService,
            gitCommandRunner,
            runtimeSettings,
            retryPolicy,
            workerIdentity,
            leaseManager,
            budgetService,
            120);
    }

    private void wireGithubAdapters()
    {
        when(adapterRegistry.requireDiffFetcher("GITHUB")).thenReturn(diffFetcher);
        when(adapterRegistry.requireMetadataFetcher("GITHUB")).thenReturn(metadataFetcher);
        when(adapterRegistry.requireFileContentFetcher("GITHUB")).thenReturn(fileContentFetcher);
        when(adapterRegistry.requireWorkspacePreparer("GITHUB")).thenReturn(workspacePreparer);
        GitCredential credential = new GitCredential();
        credential.setCredentialId(5L);
        credential.setProvider("GITHUB");
        credential.setStatus("0");
        when(credentialMapper.selectGitCredentialById(5L)).thenReturn(credential);
    }

    @Test
    void skipsWhenClaimFails()
    {
        when(taskMapper.claimTask(eq(9L), any(), any(), anyInt())).thenReturn(0);
        service.executeTask(9L);
        verify(runMapper, never()).insertReviewTaskRun(any());
    }

    @Test
    void stopsOldEpochBeforeBusinessExecutionWhenFencedUpdateIsRejected()
    {
        ReviewTask task = new ReviewTask();
        task.setTaskId(10L);
        task.setProjectId(2L);
        task.setSnapshotReviewMode(ReviewPipelineConstants.REVIEW_MODE_LLM_DIRECT);
        task.setExecutionEpoch(5L);
        task.setLeaseOwner("worker-test");
        when(taskMapper.claimTask(eq(10L), any(), any(), anyInt())).thenReturn(1);
        when(taskMapper.selectReviewTaskById(10L)).thenReturn(task);
        when(taskMapper.updateTaskExecution(any())).thenReturn(0);

        service.executeTask(10L);

        verify(projectMapper, never()).selectReviewProjectById(any());
        verify(deliveryIntentService, never()).enqueueAfterSuccess(any(), any());
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
        when(taskMapper.claimTask(eq(11L), any(), any(), anyInt())).thenReturn(1);
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
        verify(deliveryIntentService, never()).enqueueAfterSuccess(any(), any());
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
        when(taskMapper.claimTask(eq(12L), any(), any(), anyInt())).thenReturn(1);
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
        when(taskMapper.claimTask(eq(13L), any(), any(), anyInt())).thenReturn(1);
        when(taskMapper.selectReviewTaskById(13L)).thenReturn(task);
        when(runMapper.selectMaxAttemptNo(13L)).thenReturn(null);

        ReviewProject project = githubProject();
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
        when(taskMapper.claimTask(eq(14L), any(), any(), anyInt())).thenReturn(1);
        when(taskMapper.selectReviewTaskById(14L)).thenReturn(task);
        when(runMapper.selectMaxAttemptNo(14L)).thenReturn(null);

        ReviewProject project = githubProject();
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
        when(taskMapper.selectReviewTaskById(6L)).thenReturn(task);
        ServiceException ex = assertThrows(ServiceException.class, () -> service.retryTask(6L));
        assertTrue(ex.getMessage().contains("执行中"));
    }

    @Test
    void retryAllowsDatabaseExpiredRunningTask()
    {
        ReviewTask task = new ReviewTask();
        task.setTaskId(7L);
        task.setTaskStatus(ReviewPipelineConstants.TASK_RUNNING);
        when(taskMapper.selectReviewTaskById(7L)).thenReturn(task);
        when(taskMapper.requeueExpiredTask(7L)).thenReturn(1);
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
    void transientModelTimeoutMovesTaskToRetryingWithoutTerminalNotification()
    {
        ReviewTask task = llmTask(20L);
        task.setRetryCount(0);
        stubLlmPathPrerequisites(task);
        when(diffFetcher.fetchDiff(any(), any(), eq("abc1234"), eq("def5678")))
            .thenReturn(GitPullRequestDiffResult.ok(
                "diff --git a/A.java b/A.java\n--- a/A.java\n+++ b/A.java\n@@ -1 +1 @@\n-old\n+new\n"));
        when(llmCallService.chat(eq(3L), any(), anyInt()))
            .thenReturn(com.acr.common.ai.LlmCallResult.failure(
                com.acr.common.enums.LlmCallErrorType.TIMEOUT, "模型调用超时", 120_000L, null));

        service.executeTask(20L);

        org.mockito.ArgumentCaptor<ReviewTask> captor = org.mockito.ArgumentCaptor.forClass(ReviewTask.class);
        verify(taskMapper, org.mockito.Mockito.atLeastOnce()).updateTaskExecution(captor.capture());
        ReviewTask saved = captor.getAllValues().get(captor.getAllValues().size() - 1);
        assertEquals(ReviewPipelineConstants.TASK_RETRYING, saved.getTaskStatus());
        assertEquals(ReviewPipelineConstants.FAILURE_TIMEOUT, saved.getLastErrorCode());
        assertEquals(1, saved.getRetryCount());
        verify(deliveryIntentService, never()).enqueueTerminalNotification(any(), any(), anyString());
    }

    @Test
    void llmPathAppliesScopeDecisionAndExpansion()
    {
        // 锁文件排除、依赖清单扩展拉取失败降级保留 L0、配置文件扩展成功追加全文
        ReviewTask task = llmTask(21L);
        stubLlmPathPrerequisites(task);
        when(diffFetcher.fetchDiff(any(), any(), eq("abc1234"), eq("def5678")))
            .thenReturn(GitPullRequestDiffResult.ok(scopeTestDiff()));
        when(fileContentFetcher.fetchFileContent(any(), any(), eq("pom.xml"), eq("def5678")))
            .thenReturn(com.acr.review.git.GitFileContentResult.fail("IO"));
        when(fileContentFetcher.fetchFileContent(any(), any(), eq("src/main/resources/application.yml"), eq("def5678")))
            .thenReturn(com.acr.review.git.GitFileContentResult.ok("server:\n  port: 8080\ntimeout: 30\n"));
        when(llmCallService.chat(eq(3L), any(), anyInt()))
            .thenReturn(com.acr.common.ai.LlmCallResult.failure(
                com.acr.common.enums.LlmCallErrorType.UNKNOWN, "stop-here", 5L, null));

        service.executeTask(21L);

        org.mockito.ArgumentCaptor<ReviewTaskRun> runCaptor = org.mockito.ArgumentCaptor.forClass(ReviewTaskRun.class);
        verify(runMapper, org.mockito.Mockito.atLeastOnce()).updateReviewTaskRun(runCaptor.capture());
        ReviewTaskRun run = runCaptor.getAllValues().get(runCaptor.getAllValues().size() - 1);
        String prompt = run.getRenderedPrompt();
        org.junit.jupiter.api.Assertions.assertFalse(prompt.contains("package-lock"), "锁文件不得进入 Prompt");
        assertTrue(prompt.contains("Main.java"), "普通变更文件应保留");
        assertTrue(prompt.contains("===== 高影响扩展文件完整内容（规则：CONFIG"), "配置扩展应追加全文段");
        assertTrue(prompt.contains("port: 8080"), "扩展全文内容应进入 Prompt");
        assertTrue(prompt.contains("<version>1.2.3</version>"), "拉取失败的 pom.xml 应保留 L0 hunk");
        assertTrue(prompt.contains("【审查范围说明"), "最终 Prompt 应含范围指令块");

        String snapshot = run.getScopeDecisionJson();
        org.junit.jupiter.api.Assertions.assertNotNull(snapshot);
        assertTrue(snapshot.contains("package-lock.json") && snapshot.contains("DEFAULT_EXCLUDE"));
        assertTrue(snapshot.contains("FULL"), "yml 应记 FULL: " + snapshot);
        assertTrue(snapshot.contains("DEGRADED"), "pom 应记 DEGRADED: " + snapshot);
        verify(llmCallService).chat(eq(3L), any(), eq(120_000));
    }

    @Test
    void pushTaskFillsCommitMessagesFromTitleWithoutFetchingPrMetadata()
    {
        // PUSH 跳过 PR 元数据拉取，但用建单时的 prTitle（最新提交摘要）填充 run.commitMessages。
        ReviewTask task = llmTask(28L);
        task.setEventSource(ReviewPipelineConstants.EVENT_SOURCE_PUSH);
        task.setPrNumber(0);
        task.setTargetBranch("main");
        task.setPrTitle("feat: tighten webhook form unwrap");
        task.setSnapshotPromptContent(
            "描述：{{pr_description}}\n提交：{{commit_messages}}\n变更：\n{{diff}}");
        stubLlmPathPrerequisites(task);
        when(diffFetcher.fetchDiff(any(), any(), eq("abc1234"), eq("def5678")))
            .thenReturn(GitPullRequestDiffResult.ok(scopeTestDiff()));
        when(fileContentFetcher.fetchFileContent(any(), any(), eq("pom.xml"), eq("def5678")))
            .thenReturn(com.acr.review.git.GitFileContentResult.fail("IO"));
        when(fileContentFetcher.fetchFileContent(any(), any(), eq("src/main/resources/application.yml"), eq("def5678")))
            .thenReturn(com.acr.review.git.GitFileContentResult.ok("server:\n  port: 8080\n"));
        when(llmCallService.chat(eq(3L), any(), anyInt()))
            .thenReturn(com.acr.common.ai.LlmCallResult.failure(
                com.acr.common.enums.LlmCallErrorType.UNKNOWN, "stop-here", 5L, null));

        service.executeTask(28L);

        // setUp 会对 requireMetadataFetcher 做 stubbing 调用；此处只断言业务路径未真正 fetch。
        verify(metadataFetcher, never()).fetch(any(), any(), anyInt());
        org.mockito.ArgumentCaptor<ReviewTaskRun> runCaptor = org.mockito.ArgumentCaptor.forClass(ReviewTaskRun.class);
        verify(runMapper, org.mockito.Mockito.atLeastOnce()).updateReviewTaskRun(runCaptor.capture());
        ReviewTaskRun run = runCaptor.getAllValues().stream()
            .filter(r -> "feat: tighten webhook form unwrap".equals(r.getCommitMessages()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("run.commitMessages 未被任务标题填充"));
        org.junit.jupiter.api.Assertions.assertEquals(
            "feat: tighten webhook form unwrap", run.getCommitMessages());
        org.junit.jupiter.api.Assertions.assertNull(run.getPrDescription());
        String prompt = run.getRenderedPrompt();
        org.junit.jupiter.api.Assertions.assertNotNull(prompt);
        assertTrue(prompt.contains("（推送审查无合并请求描述）"), "PUSH 应用 push 语义描述兜底");
        assertTrue(prompt.contains("feat: tighten webhook form unwrap"), "提交信息应进入提示词");
        org.junit.jupiter.api.Assertions.assertFalse(prompt.contains("（未获取到 PR 描述）"));
        assertTrue(prompt.contains("推送审查场景：结合推送携带的 Commit Message 判断"),
            "协议附录 COMMIT_QUALITY 应为 push 版括注");
        assertTrue(prompt.contains("禁止以缺少 PR/合并请求描述为由输出问题或扣分"));
        org.junit.jupiter.api.Assertions.assertFalse(
            prompt.contains("必须结合 PR 标题、PR 描述与 Commit Message 判断"));
    }

    @Test
    void llmPathSkipsModelWhenScopeEmpty()
    {
        // 全部文件命中排除：不调用模型，任务按通过完成并说明原因
        ReviewTask task = llmTask(22L);
        stubLlmPathPrerequisites(task);
        when(diffFetcher.fetchDiff(any(), any(), eq("abc1234"), eq("def5678")))
            .thenReturn(GitPullRequestDiffResult.ok(
                "diff --git a/package-lock.json b/package-lock.json\n"
                    + "index 1111111..2222222 100644\n"
                    + "--- a/package-lock.json\n"
                    + "+++ b/package-lock.json\n"
                    + "@@ -1 +1 @@\n"
                    + "-\"version\": \"1.0.0\"\n"
                    + "+\"version\": \"1.0.1\"\n"));

        service.executeTask(22L);

        verify(llmCallService, never()).chat(any(), any(), anyInt());
        org.mockito.ArgumentCaptor<ReviewTask> taskCaptor = org.mockito.ArgumentCaptor.forClass(ReviewTask.class);
        verify(taskMapper, org.mockito.Mockito.atLeastOnce()).updateTaskExecution(taskCaptor.capture());
        ReviewTask finished = taskCaptor.getAllValues().get(taskCaptor.getAllValues().size() - 1);
        org.junit.jupiter.api.Assertions.assertEquals(ReviewPipelineConstants.TASK_SUCCESS, finished.getTaskStatus());
        org.junit.jupiter.api.Assertions.assertEquals(ReviewPipelineConstants.CONCLUSION_PASS, finished.getReviewConclusion());

        org.mockito.ArgumentCaptor<ReviewTaskRun> runCaptor = org.mockito.ArgumentCaptor.forClass(ReviewTaskRun.class);
        verify(runMapper, org.mockito.Mockito.atLeastOnce()).updateReviewTaskRun(runCaptor.capture());
        ReviewTaskRun run = runCaptor.getAllValues().get(runCaptor.getAllValues().size() - 1);
        org.junit.jupiter.api.Assertions.assertEquals(ReviewPipelineConstants.RUN_SUCCESS, run.getRunStatus());
        assertTrue(run.getResultSummary().contains("无有效审查范围"));
        org.junit.jupiter.api.Assertions.assertNotNull(run.getScopeDecisionJson());
        assertTrue(run.getScopeDecisionJson().contains("package-lock.json"));
        verify(deliveryIntentService).enqueueAfterSuccess(any(ReviewTask.class), any(ReviewTaskRun.class));
    }

    @Test
    void llmPathDegradesToFullDiffWhenDecisionFails()
    {
        // 决策服务异常：降级为全量 Diff 并记录降级原因，审查不中断
        ReviewTaskExecutionServiceImpl degradedService = new ReviewTaskExecutionServiceImpl(
            taskMapper, runMapper, projectMapper, credentialMapper,
            credentialService, adapterRegistry,
            modelConfigService,
            mock(OcrModelConfigMapper.class),
            mock(OpenCodeReviewCliAdapter.class),
            new ReviewEngineResultMapper(),
            mock(ReviewEngineWorkspaceManager.class),
            engineProperties(),
            new ReviewConclusionResolver(),
            new ReviewPromptRenderer(),
            new ReviewPromptComposer(),
            new ReviewScoreResultParser(),
            new com.acr.review.scope.UnifiedDiffParser(),
            throwingDecisionService(),
            new com.acr.review.scope.ReviewScopePromptAssembler(),
            llmCallService,
            eventPublisher,
            new ReviewTaskSnapshotServiceImpl(templateService, modelConfigService, engineProperties()),
            completionService,
            gitCommandRunner,
            runtimeSettings,
            retryPolicy,
            workerIdentity,
            leaseManager,
            budgetService,
            120);
        ReviewTask task = llmTask(23L);
        stubLlmPathPrerequisites(task);
        when(diffFetcher.fetchDiff(any(), any(), eq("abc1234"), eq("def5678")))
            .thenReturn(GitPullRequestDiffResult.ok(scopeTestDiff()));
        when(llmCallService.chat(eq(3L), any(), anyInt()))
            .thenReturn(com.acr.common.ai.LlmCallResult.failure(
                com.acr.common.enums.LlmCallErrorType.UNKNOWN, "stop-here", 5L, null));

        degradedService.executeTask(23L);

        org.mockito.ArgumentCaptor<ReviewTaskRun> runCaptor = org.mockito.ArgumentCaptor.forClass(ReviewTaskRun.class);
        verify(runMapper, org.mockito.Mockito.atLeastOnce()).updateReviewTaskRun(runCaptor.capture());
        ReviewTaskRun run = runCaptor.getAllValues().get(runCaptor.getAllValues().size() - 1);
        assertTrue(run.getRenderedPrompt().contains("package-lock"), "降级后应保留全量 Diff");
        assertTrue(run.getScopeDecisionJson().contains("DECISION_FAILED"));
        verify(llmCallService).chat(eq(3L), any(), eq(120_000));
    }

    private com.acr.review.scope.ReviewScopeDecisionService throwingDecisionService()
    {
        return new com.acr.review.scope.ReviewScopeDecisionService()
        {
            @Override
            public com.acr.review.scope.ReviewScopeDecision decide(
                com.acr.review.scope.DiffParseResult parsed, com.acr.review.scope.ReviewScopeConfig config)
            {
                throw new IllegalStateException("模拟决策异常");
            }
        };
    }

    @Test
    void llmPathTagsOriginAndPersistsScopeStats()
    {
        // 步 5 端到端：存量 CRITICAL 剔除（不占 Top 3、不阻断）、未知文件按 NEW 计、扩展 FULL 文件任意行号 NEW
        ReviewTask task = llmTask(24L);
        stubLlmPathPrerequisites(task);
        when(diffFetcher.fetchDiff(any(), any(), eq("abc1234"), eq("def5678")))
            .thenReturn(GitPullRequestDiffResult.ok(scopeTestDiff()));
        when(fileContentFetcher.fetchFileContent(any(), any(), eq("pom.xml"), eq("def5678")))
            .thenReturn(com.acr.review.git.GitFileContentResult.fail("IO"));
        when(fileContentFetcher.fetchFileContent(any(), any(), eq("src/main/resources/application.yml"), eq("def5678")))
            .thenReturn(com.acr.review.git.GitFileContentResult.ok("server:\n  port: 8080\ntimeout: 30\n"));
        when(llmCallService.chat(eq(3L), any(), anyInt()))
            .thenReturn(com.acr.common.ai.LlmCallResult.success(8L, originModelResponse(), null));

        service.executeTask(24L);

        org.mockito.ArgumentCaptor<ReviewTaskRun> runCaptor = org.mockito.ArgumentCaptor.forClass(ReviewTaskRun.class);
        verify(runMapper, org.mockito.Mockito.atLeastOnce()).updateReviewTaskRun(runCaptor.capture());
        ReviewTaskRun run = runCaptor.getAllValues().get(runCaptor.getAllValues().size() - 1);

        org.junit.jupiter.api.Assertions.assertEquals(ReviewPipelineConstants.RUN_SUCCESS, run.getRunStatus());
        org.junit.jupiter.api.Assertions.assertEquals(ReviewPipelineConstants.CONCLUSION_WARN, run.getReviewConclusion(),
            "存量 CRITICAL 剔除后按新增 HIGH 评估应为警告");
        org.junit.jupiter.api.Assertions.assertEquals(1, run.getFocusIssueCount(), "focusIssueCount = NEW 中 CRITICAL/HIGH");

        String topIssues = run.getTopIssuesJson();
        org.junit.jupiter.api.Assertions.assertFalse(topIssues.contains("存量问题"), "存量问题应被剔除: " + topIssues);
        assertTrue(topIssues.contains("扩展全文文件问题"), "FULL 扩展文件问题按 NEW 保留: " + topIssues);
        org.junit.jupiter.api.Assertions.assertEquals(3, countOccurrences(topIssues, "\"origin\":\"NEW\""));

        String resultJson = run.getResultJson();
        assertTrue(resultJson.contains("\"scopeStats\""), "resultJson 应含 scopeStats: " + resultJson);
        assertTrue(resultJson.contains("\"newCount\":3"), resultJson);
        assertTrue(resultJson.contains("\"existingCount\":1"), resultJson);
        assertTrue(resultJson.contains("\"originUnverifiable\":1"), resultJson);
        assertTrue(resultJson.contains("\"includedFiles\":3"), resultJson);
        assertTrue(resultJson.contains("\"excludedFiles\":1"), resultJson);
        assertTrue(resultJson.contains("\"expandedFiles\":2"), resultJson);
        org.junit.jupiter.api.Assertions.assertEquals("1.2", run.getProtocolVersion());
    }

    @Test
    void llmPathKeepsExistingIssuesWhenReportExistingEnabled()
    {
        // 快照 scope_report_existing=Y：存量问题标注保留（仅信息展示），仍不影响结论
        ReviewTask task = llmTask(25L);
        task.setSnapshotScopeReportExisting("Y");
        stubLlmPathPrerequisites(task);
        when(diffFetcher.fetchDiff(any(), any(), eq("abc1234"), eq("def5678")))
            .thenReturn(GitPullRequestDiffResult.ok(scopeTestDiff()));
        when(fileContentFetcher.fetchFileContent(any(), any(), eq("pom.xml"), eq("def5678")))
            .thenReturn(com.acr.review.git.GitFileContentResult.fail("IO"));
        when(fileContentFetcher.fetchFileContent(any(), any(), eq("src/main/resources/application.yml"), eq("def5678")))
            .thenReturn(com.acr.review.git.GitFileContentResult.ok("server:\n  port: 8080\ntimeout: 30\n"));
        when(llmCallService.chat(eq(3L), any(), anyInt()))
            .thenReturn(com.acr.common.ai.LlmCallResult.success(8L, originModelResponse(), null));

        service.executeTask(25L);

        org.mockito.ArgumentCaptor<ReviewTaskRun> runCaptor = org.mockito.ArgumentCaptor.forClass(ReviewTaskRun.class);
        verify(runMapper, org.mockito.Mockito.atLeastOnce()).updateReviewTaskRun(runCaptor.capture());
        ReviewTaskRun run = runCaptor.getAllValues().get(runCaptor.getAllValues().size() - 1);

        String topIssues = run.getTopIssuesJson();
        assertTrue(topIssues.contains("存量问题"), "reportExisting=Y 时存量问题应保留: " + topIssues);
        assertTrue(topIssues.contains("\"origin\":\"EXISTING\""), topIssues);
        org.junit.jupiter.api.Assertions.assertEquals(1, run.getFocusIssueCount(), "存量保留也不计 focusIssueCount");
        org.junit.jupiter.api.Assertions.assertEquals(ReviewPipelineConstants.CONCLUSION_WARN, run.getReviewConclusion(),
            "存量 CRITICAL 保留也不影响结论");
    }

    /**
     * 模型响应：新增 HIGH（Main.java:12，新增行）、存量 CRITICAL（Main.java:50，hunk 外）、
     * 不可判定 MEDIUM（未知文件）、扩展 FULL 文件 LOW（application.yml:99，任意行号 NEW）。
     */
    private String originModelResponse()
    {
        return """
            {
              "protocolVersion":"1.1",
              "scores":[
                {"dimension":"CORRECTNESS","score":30,"maxScore":40,"reason":"ok"},
                {"dimension":"SECURITY","score":20,"maxScore":30,"reason":"ok"},
                {"dimension":"PRACTICE","score":15,"maxScore":20,"reason":"ok"},
                {"dimension":"PERFORMANCE","score":4,"maxScore":5,"reason":"ok"},
                {"dimension":"COMMIT_QUALITY","score":3,"maxScore":5,"reason":"ok"}
              ],
              "summary":"发现新增与存量问题",
              "topIssues":[
                {"rank":1,"severity":"CRITICAL","category":"security","title":"存量问题","description":"d","filePath":"src/main/java/Main.java","startLine":50,"endLine":50,"evidence":"e","suggestion":"s"},
                {"rank":2,"severity":"HIGH","category":"bug","title":"新增行问题","description":"d","filePath":"src/main/java/Main.java","startLine":12,"endLine":12,"evidence":"e","suggestion":"s"},
                {"rank":3,"severity":"MEDIUM","category":"practice","title":"不可判定问题","description":"d","filePath":"src/Ghost.java","startLine":3,"endLine":3,"evidence":"e","suggestion":"s"},
                {"rank":4,"severity":"LOW","category":"config","title":"扩展全文文件问题","description":"d","filePath":"src/main/resources/application.yml","startLine":99,"endLine":99,"evidence":"e","suggestion":"s"}
              ],
              "focusIssueCount":4,
              "hasCriticalSecurityIssue":true
            }
            """;
    }

    private int countOccurrences(String text, String needle)
    {
        int count = 0;
        for (int index = text.indexOf(needle); index >= 0; index = text.indexOf(needle, index + needle.length()))
        {
            count++;
        }
        return count;
    }

    // ---------- 步 6：OCR 路径范围排除与决策快照 ----------

    @Test
    void ocrPathPersistsTopIssuesAndTriggersReconcile()
    {
        ReviewTask task = ocrTask(30L);
        stubOcrPathPrerequisites(task);
        when(diffFetcher.fetchDiff(any(), any(), eq("abc1234"), eq("def5678")))
            .thenReturn(GitPullRequestDiffResult.ok(scopeTestDiff()));
        java.util.Map<String, Object> comment = new java.util.HashMap<>();
        comment.put("path", "src/Auth.java");
        comment.put("content", "**SQL 注入**: 未参数化查询。建议使用 PreparedStatement。");
        comment.put("existing_code", "String sql = \"select * from t where id=\" + id;");
        comment.put("suggestion_code", "PreparedStatement ps = conn.prepareStatement(\"?\");");
        comment.put("start_line", 12);
        comment.put("end_line", 14);
        comment.put("category", "security");
        comment.put("severity", "critical");
        when(reviewEngine.execute(any())).thenReturn(com.acr.review.engine.ReviewEngineResult.success(
            "open-code-review", "1.8.3", 5L, "{}", "",
            java.util.Map.of("comments", java.util.List.of(comment)), 0));
        when(issueService.reconcileAfterSuccess(any(), any()))
            .thenReturn(com.acr.review.domain.ReviewRoundReconcileResult.empty());

        service.executeTask(30L);

        org.mockito.ArgumentCaptor<ReviewTaskRun> runCaptor = org.mockito.ArgumentCaptor.forClass(ReviewTaskRun.class);
        verify(runMapper, org.mockito.Mockito.atLeastOnce()).updateReviewTaskRun(runCaptor.capture());
        ReviewTaskRun run = runCaptor.getAllValues().get(runCaptor.getAllValues().size() - 1);
        org.junit.jupiter.api.Assertions.assertEquals(ReviewPipelineConstants.RUN_SUCCESS, run.getRunStatus());
        org.junit.jupiter.api.Assertions.assertNotNull(run.getTopIssuesJson());
        assertTrue(run.getTopIssuesJson().contains("SQL 注入"), run.getTopIssuesJson());
        assertTrue(run.getTopIssuesJson().contains("\"category\":\"SECURITY\""), run.getTopIssuesJson());
        org.junit.jupiter.api.Assertions.assertEquals(1, run.getFocusIssueCount());
        org.junit.jupiter.api.Assertions.assertEquals("1", run.getHasCriticalSecurity());
        org.junit.jupiter.api.Assertions.assertEquals(
            com.acr.review.service.ReviewScoringConstants.PARSE_SUCCESS, run.getParseStatus());

        org.mockito.ArgumentCaptor<ReviewTask> taskCaptor = org.mockito.ArgumentCaptor.forClass(ReviewTask.class);
        verify(taskMapper, org.mockito.Mockito.atLeastOnce()).updateTaskExecution(taskCaptor.capture());
        ReviewTask finished = taskCaptor.getAllValues().get(taskCaptor.getAllValues().size() - 1);
        org.junit.jupiter.api.Assertions.assertEquals(1, finished.getFocusIssueCount());
        org.junit.jupiter.api.Assertions.assertEquals("1", finished.getHasCriticalSecurity());
        org.junit.jupiter.api.Assertions.assertEquals(
            com.acr.review.service.ReviewScoringConstants.PARSE_SUCCESS, finished.getParseStatus());

        org.mockito.ArgumentCaptor<ReviewTaskRun> reconcileRun =
            org.mockito.ArgumentCaptor.forClass(ReviewTaskRun.class);
        verify(issueService).reconcileAfterSuccess(any(ReviewTask.class), reconcileRun.capture());
        assertTrue(reconcileRun.getValue().getTopIssuesJson().contains("SQL 注入"));
        verify(deliveryIntentService).enqueueAfterSuccess(any(ReviewTask.class), any(ReviewTaskRun.class));
    }

    @Test
    void ocrPathPassesExcludePatternsAndPersistsScopeSnapshot()
    {
        // 平台默认 + 测试文件 + 项目排除合并经 --exclude 传入引擎；决策快照落 scope_decision_json
        ReviewTask task = ocrTask(26L);
        stubOcrPathPrerequisites(task);
        when(diffFetcher.fetchDiff(any(), any(), eq("abc1234"), eq("def5678")))
            .thenReturn(GitPullRequestDiffResult.ok(scopeTestDiff()));
        when(reviewEngine.execute(any())).thenReturn(com.acr.review.engine.ReviewEngineResult.success(
            "open-code-review", "1.8.3", 5L, "{}", "", java.util.Map.of("conclusion", "PASS"), 0));

        service.executeTask(26L);

        org.mockito.ArgumentCaptor<com.acr.review.engine.ReviewEngineRequest> requestCaptor =
            org.mockito.ArgumentCaptor.forClass(com.acr.review.engine.ReviewEngineRequest.class);
        verify(reviewEngine).execute(requestCaptor.capture());
        java.util.List<String> excludes = requestCaptor.getValue().getExcludePatterns();
        org.junit.jupiter.api.Assertions.assertNotNull(excludes);
        assertTrue(excludes.contains("**/package-lock.json"), "平台默认排除");
        assertTrue(excludes.contains("**/src/test/**"), "默认不审测试文件");
        assertTrue(excludes.contains("docs/**"), "项目排除规则");
        assertTrue(excludes.contains("*.generated.java"), "项目排除规则");

        org.mockito.ArgumentCaptor<ReviewTaskRun> runCaptor = org.mockito.ArgumentCaptor.forClass(ReviewTaskRun.class);
        verify(runMapper, org.mockito.Mockito.atLeastOnce()).updateReviewTaskRun(runCaptor.capture());
        String snapshot = null;
        for (ReviewTaskRun candidate : runCaptor.getAllValues())
        {
            if (candidate.getScopeDecisionJson() != null)
            {
                snapshot = candidate.getScopeDecisionJson();
            }
        }
        org.junit.jupiter.api.Assertions.assertNotNull(snapshot, "决策快照应落库");
        assertTrue(snapshot.contains("OCR_ENGINE"), snapshot);
        assertTrue(snapshot.contains("appliedExcludeGlobs"), snapshot);
        assertTrue(snapshot.contains("package-lock.json"), snapshot);
        assertTrue(snapshot.contains("DEFAULT_EXCLUDE"), snapshot);
        assertTrue(snapshot.contains("截断不适用") || snapshot.contains("L0 预算截断不适用"), snapshot);
    }

    @Test
    void ocrPathContinuesWithoutExcludesWhenDiffUnavailable()
    {
        // Diff 不可用：不加排除规则（引擎全量审查），快照记降级，审查不阻断
        ReviewTask task = ocrTask(27L);
        stubOcrPathPrerequisites(task);
        when(diffFetcher.fetchDiff(any(), any(), eq("abc1234"), eq("def5678")))
            .thenReturn(GitPullRequestDiffResult.fail("RATE_LIMIT", "limited"));
        when(reviewEngine.execute(any())).thenReturn(com.acr.review.engine.ReviewEngineResult.success(
            "open-code-review", "1.8.3", 5L, "{}", "", java.util.Map.of("conclusion", "PASS"), 0));

        service.executeTask(27L);

        org.mockito.ArgumentCaptor<com.acr.review.engine.ReviewEngineRequest> requestCaptor =
            org.mockito.ArgumentCaptor.forClass(com.acr.review.engine.ReviewEngineRequest.class);
        verify(reviewEngine).execute(requestCaptor.capture());
        assertTrue(requestCaptor.getValue().getExcludePatterns().isEmpty(), "降级时不得传排除规则");

        org.mockito.ArgumentCaptor<ReviewTaskRun> runCaptor = org.mockito.ArgumentCaptor.forClass(ReviewTaskRun.class);
        verify(runMapper, org.mockito.Mockito.atLeastOnce()).updateReviewTaskRun(runCaptor.capture());
        String snapshot = null;
        for (ReviewTaskRun candidate : runCaptor.getAllValues())
        {
            if (candidate.getScopeDecisionJson() != null)
            {
                snapshot = candidate.getScopeDecisionJson();
            }
        }
        org.junit.jupiter.api.Assertions.assertNotNull(snapshot);
        assertTrue(snapshot.contains("DIFF_UNAVAILABLE"), snapshot);

        org.mockito.ArgumentCaptor<ReviewTask> taskCaptor = org.mockito.ArgumentCaptor.forClass(ReviewTask.class);
        verify(taskMapper, org.mockito.Mockito.atLeastOnce()).updateTaskExecution(taskCaptor.capture());
        ReviewTask finished = taskCaptor.getAllValues().get(taskCaptor.getAllValues().size() - 1);
        org.junit.jupiter.api.Assertions.assertEquals(ReviewPipelineConstants.TASK_SUCCESS, finished.getTaskStatus());
    }

    @Test
    void ocrPathShortCircuitsWhenWorkspaceHasNoDiff()
    {
        // base==head：无树变更，直接按空范围通过，不调用引擎
        ReviewTask task = ocrTask(29L);
        task.setEventSource(ReviewPipelineConstants.EVENT_SOURCE_PUSH);
        task.setPrNumber(0);
        task.setBaseSha("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
        task.setHeadSha("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa");
        stubOcrPathPrerequisites(task);
        when(diffFetcher.fetchDiff(any(), any(), any(), any()))
            .thenReturn(GitPullRequestDiffResult.fail("EMPTY", "no compare"));
        when(workspacePreparer.prepare(any())).thenReturn(
            com.acr.review.git.GitPullRequestWorkspaceResult.ok(
                "/tmp/ocr-ws-29/repo",
                "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"));

        service.executeTask(29L);

        verify(reviewEngine, never()).execute(any());
        org.mockito.ArgumentCaptor<ReviewTask> taskCaptor = org.mockito.ArgumentCaptor.forClass(ReviewTask.class);
        verify(taskMapper, org.mockito.Mockito.atLeastOnce()).updateTaskExecution(taskCaptor.capture());
        ReviewTask finished = taskCaptor.getAllValues().get(taskCaptor.getAllValues().size() - 1);
        org.junit.jupiter.api.Assertions.assertEquals(ReviewPipelineConstants.TASK_SUCCESS, finished.getTaskStatus());
        org.junit.jupiter.api.Assertions.assertEquals(
            ReviewPipelineConstants.CONCLUSION_PASS, finished.getReviewConclusion());

        org.mockito.ArgumentCaptor<ReviewTaskRun> runCaptor = org.mockito.ArgumentCaptor.forClass(ReviewTaskRun.class);
        verify(runMapper, org.mockito.Mockito.atLeastOnce()).updateReviewTaskRun(runCaptor.capture());
        boolean sawEmptySummary = runCaptor.getAllValues().stream()
            .map(ReviewTaskRun::getResultSummary)
            .filter(java.util.Objects::nonNull)
            .anyMatch(s -> s.contains("本次推送无代码变更"));
        assertTrue(sawEmptySummary, "应落推送无代码变更摘要");
    }

    @Test
    void ocrBudgetExhaustionReturnsRetryingBeforeWorkspacePrepare() throws Exception
    {
        ReviewTask task = ocrTask(40L);
        task.setRetryCount(99);
        stubOcrPathPrerequisites(task);
        when(budgetService.tryAcquireOcrExecution()).thenReturn(null);

        service.executeTask(40L);

        verify(workspaceManager, never()).createIsolatedWorkspace();
        verify(workspacePreparer, never()).prepare(any());
        org.mockito.ArgumentCaptor<ReviewTask> taskCaptor = org.mockito.ArgumentCaptor.forClass(ReviewTask.class);
        verify(taskMapper, org.mockito.Mockito.atLeastOnce()).updateTaskExecution(taskCaptor.capture());
        ReviewTask finished = taskCaptor.getAllValues().get(taskCaptor.getAllValues().size() - 1);
        assertEquals(ReviewPipelineConstants.TASK_RETRYING, finished.getTaskStatus());
        assertEquals(ReviewPipelineConstants.FAILURE_CONCURRENCY, finished.getFailureType());
        assertEquals(99, finished.getRetryCount());
        assertEquals(30, finished.getRetryDelaySeconds());
        verify(deliveryIntentService, never()).enqueueTerminalNotification(any(), any(), any());
    }

    @Test
    void llmBudgetExhaustionReturnsRetryingWithoutCallingModel()
    {
        ReviewTask task = llmTask(41L);
        task.setRetryCount(5);
        stubLlmPathPrerequisites(task);
        when(diffFetcher.fetchDiff(any(), any(), eq("abc1234"), eq("def5678")))
            .thenReturn(GitPullRequestDiffResult.ok(scopeTestDiff()));
        when(budgetService.tryAcquireLlmCall()).thenReturn(null);

        service.executeTask(41L);

        verify(llmCallService, never()).chat(any(), any(), anyInt());
        org.mockito.ArgumentCaptor<ReviewTask> taskCaptor = org.mockito.ArgumentCaptor.forClass(ReviewTask.class);
        verify(taskMapper, org.mockito.Mockito.atLeastOnce()).updateTaskExecution(taskCaptor.capture());
        ReviewTask finished = taskCaptor.getAllValues().get(taskCaptor.getAllValues().size() - 1);
        assertEquals(ReviewPipelineConstants.TASK_RETRYING, finished.getTaskStatus());
        assertEquals(ReviewPipelineConstants.FAILURE_CONCURRENCY, finished.getFailureType());
        assertEquals(5, finished.getRetryCount());
    }

    /** OCR 快照任务（项目排除两条：docs/** 与 *.generated.java；其余快照列留空走平台默认）。 */
    private ReviewTask ocrTask(long taskId)
    {
        ReviewTask task = new ReviewTask();
        task.setTaskId(taskId);
        task.setProjectId(2L);
        task.setPrNumber(101);
        task.setBaseSha("abc1234");
        task.setHeadSha("def5678");
        task.setSnapshotReviewMode("OCR_ENGINE");
        task.setSnapshotEngineCode("OPEN_CODE_REVIEW");
        task.setSnapshotEngineName("open-code-review");
        task.setSnapshotScopeExcludePatterns("docs/**\n*.generated.java");
        return task;
    }

    private void stubOcrPathPrerequisites(ReviewTask task)
    {
        when(taskMapper.claimTask(eq(task.getTaskId()), any(), any(), anyInt())).thenReturn(1);
        when(taskMapper.selectReviewTaskById(task.getTaskId())).thenReturn(task);
        when(runMapper.selectMaxAttemptNo(task.getTaskId())).thenReturn(null);
        ReviewProject project = githubProject();
        when(projectMapper.selectReviewProjectById(2L)).thenReturn(project);
        when(credentialService.getPlainToken(5L, true)).thenReturn("token");

        SysAiModelConfig runtimeModel = new SysAiModelConfig();
        runtimeModel.setModelId(7L);
        runtimeModel.setModelName("Runtime");
        runtimeModel.setProvider("deepseek");
        runtimeModel.setModel("deepseek-chat");
        runtimeModel.setApiKey("test-api-key");
        runtimeModel.setEnabled("1");
        when(modelConfigService.selectDefaultRuntimeConfig()).thenReturn(runtimeModel);
        when(modelConfigMapper.toEnvironment(runtimeModel)).thenReturn(java.util.Map.of("OCR_MODEL", "deepseek-chat"));

        String workspacePath = "/tmp/ocr-ws-" + task.getTaskId();
        try
        {
            when(workspaceManager.createIsolatedWorkspace()).thenReturn(java.nio.file.Path.of(workspacePath));
        }
        catch (java.io.IOException ex)
        {
            throw new IllegalStateException(ex);
        }
        when(workspacePreparer.prepare(any())).thenReturn(
            com.acr.review.git.GitPullRequestWorkspaceResult.ok(workspacePath + "/repo", "abc1234", "def5678"));
        wireGithubAdapters();
    }

    private ReviewEngineProperties engineProperties()
    {
        ReviewEngineProperties properties = new ReviewEngineProperties();
        properties.setMaxConcurrency(2);
        return properties;
    }

    /** LLM 快照任务（范围配置列留空，走平台默认：排除测试/不报存量/开启扩展）。 */
    private ReviewTask llmTask(long taskId)
    {
        ReviewTask task = new ReviewTask();
        task.setTaskId(taskId);
        task.setProjectId(2L);
        task.setPrNumber(101);
        task.setBaseSha("abc1234");
        task.setHeadSha("def5678");
        task.setSnapshotReviewMode("LLM_DIRECT");
        task.setSnapshotModelId(3L);
        task.setSnapshotModelName("DeepSeek");
        task.setSnapshotModelProvider("deepseek");
        task.setSnapshotModel("deepseek-chat");
        task.setSnapshotTemplateId(4L);
        task.setSnapshotTemplateName("Java");
        task.setSnapshotTemplateCode("builtin_java");
        task.setSnapshotTemplateVersion(2);
        task.setSnapshotPromptContent("请审查以下变更：\n{{diff}}");
        return task;
    }

    private void stubLlmPathPrerequisites(ReviewTask task)
    {
        when(taskMapper.claimTask(eq(task.getTaskId()), any(), any(), anyInt())).thenReturn(1);
        when(taskMapper.selectReviewTaskById(task.getTaskId())).thenReturn(task);
        when(runMapper.selectMaxAttemptNo(task.getTaskId())).thenReturn(null);
        ReviewProject project = githubProject();
        when(projectMapper.selectReviewProjectById(2L)).thenReturn(project);
        when(credentialService.getPlainToken(5L, true)).thenReturn("token");
    }

    private static ReviewProject githubProject()
    {
        ReviewProject project = new ReviewProject();
        project.setProjectId(2L);
        project.setStatus("0");
        project.setProvider("GITHUB");
        project.setCredentialId(5L);
        project.setRepositoryOwner("acme");
        project.setRepositoryName("demo");
        project.setRepositoryFullPath("acme/demo");
        project.setRepositoryUrl("https://github.com/acme/demo");
        return project;
    }

    /** 覆盖四类文件：锁文件（默认排除）、普通 Java（L0）、依赖清单（扩展+拉取失败）、配置（扩展+拉取成功）。 */
    private String scopeTestDiff()
    {
        return "diff --git a/package-lock.json b/package-lock.json\n"
            + "index 1111111..2222222 100644\n"
            + "--- a/package-lock.json\n"
            + "+++ b/package-lock.json\n"
            + "@@ -1 +1 @@\n"
            + "-\"version\": \"1.0.0\"\n"
            + "+\"version\": \"1.0.1\"\n"
            + "diff --git a/src/main/java/Main.java b/src/main/java/Main.java\n"
            + "index 3333333..4444444 100644\n"
            + "--- a/src/main/java/Main.java\n"
            + "+++ b/src/main/java/Main.java\n"
            + "@@ -10,4 +10,5 @@ public class Main {\n"
            + "     void call() {\n"
            + "         helper();\n"
            + "+        audit();\n"
            + "     }\n"
            + " }\n"
            + "diff --git a/pom.xml b/pom.xml\n"
            + "index 5555555..6666666 100644\n"
            + "--- a/pom.xml\n"
            + "+++ b/pom.xml\n"
            + "@@ -1 +1 @@\n"
            + "-<version>1.2.2</version>\n"
            + "+<version>1.2.3</version>\n"
            + "diff --git a/src/main/resources/application.yml b/src/main/resources/application.yml\n"
            + "index 7777777..8888888 100644\n"
            + "--- a/src/main/resources/application.yml\n"
            + "+++ b/src/main/resources/application.yml\n"
            + "@@ -1 +1 @@\n"
            + "-timeout: 10\n"
            + "+timeout: 30\n";
    }

    @Test
    void retryAllowsFailedTask()
    {
        ReviewTask task = new ReviewTask();
        task.setTaskId(5L);
        task.setTaskStatus(ReviewPipelineConstants.TASK_FAILED);
        when(taskMapper.selectReviewTaskById(5L)).thenReturn(task);
        when(taskMapper.requeueFailedTask(5L)).thenReturn(1);
        service.retryTask(5L);
        verify(eventPublisher).publishEvent(any());
    }

    @Test
    void schedulePublishesEvent()
    {
        service.scheduleExecution(8L);
        verify(eventPublisher).publishEvent(any());
    }

    @Test
    void statsFromDiffCountsFilesAndLineChanges()
    {
        String diff = ""
            + "diff --git a/a.java b/a.java\n"
            + "--- a/a.java\n"
            + "+++ b/a.java\n"
            + "@@ -1,2 +1,3 @@\n"
            + " keep\n"
            + "-old\n"
            + "+new1\n"
            + "+new2\n"
            + "diff --git a/b.java b/b.java\n"
            + "--- a/b.java\n"
            + "+++ b/b.java\n"
            + "@@ -1 +1 @@\n"
            + "-x\n"
            + "+y\n";
        var parsed = new UnifiedDiffParser().parse(diff);
        var stats = ReviewTaskExecutionServiceImpl.statsFromDiff(parsed);
        assertEquals(2, stats.changedFiles());
        assertEquals(3, stats.additions());
        assertEquals(2, stats.deletions());

        var covered = ReviewTaskExecutionServiceImpl.coveredFilesFromDiff(parsed);
        assertEquals(java.util.Set.of("a.java", "b.java"), covered);
    }

    @Test
    void coveredFilesFromDiffUsesNewPathForRename()
    {
        String diff = ""
            + "diff --git a/src/OldName.java b/src/NewName.java\n"
            + "similarity index 90%\n"
            + "rename from src/OldName.java\n"
            + "rename to src/NewName.java\n"
            + "--- a/src/OldName.java\n"
            + "+++ b/src/NewName.java\n"
            + "@@ -1 +1 @@\n"
            + "-old\n"
            + "+new\n";
        var covered = ReviewTaskExecutionServiceImpl.coveredFilesFromDiff(new UnifiedDiffParser().parse(diff));
        assertTrue(covered.contains("src/NewName.java"));
        assertFalse(covered.contains("src/OldName.java"));
    }

    @Test
    void parseShortstatOutputSupportsVariants()
    {
        var full = ReviewTaskExecutionServiceImpl.parseShortstatOutput(
            " 1 file changed, 25 insertions(+), 3 deletions(-)\n");
        assertEquals(1, full.changedFiles());
        assertEquals(25, full.additions());
        assertEquals(3, full.deletions());

        var plural = ReviewTaskExecutionServiceImpl.parseShortstatOutput(
            "2 files changed, 1 insertion(+), 1 deletion(-)");
        assertEquals(2, plural.changedFiles());
        assertEquals(1, plural.additions());
        assertEquals(1, plural.deletions());

        var addOnly = ReviewTaskExecutionServiceImpl.parseShortstatOutput(
            "1 file changed, 10 insertions(+)");
        assertEquals(1, addOnly.changedFiles());
        assertEquals(10, addOnly.additions());
        assertEquals(0, addOnly.deletions());
    }

    @Test
    void applyDiffStatsIfAbsentDoesNotOverwriteExisting()
    {
        ReviewTask task = new ReviewTask();
        task.setChangedFiles(9);
        task.setAdditions(100);
        // deletions 留空，允许回填
        ReviewTaskExecutionServiceImpl.applyDiffStatsIfAbsent(task,
            new ReviewTaskExecutionServiceImpl.DiffChangeStats(2, 5, 3));
        assertEquals(9, task.getChangedFiles());
        assertEquals(100, task.getAdditions());
        assertEquals(3, task.getDeletions());
    }
}
