package com.acr.review.service.impl;

import java.nio.file.Path;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import com.acr.common.ai.LlmCallResult;
import com.acr.common.ai.LlmCallService;
import com.acr.common.exception.ServiceException;
import com.acr.common.utils.StringUtils;
import com.acr.review.domain.ReviewPipelineConstants;
import com.acr.review.domain.ReviewProject;
import com.acr.review.domain.ReviewTask;
import com.acr.review.domain.ReviewTaskRun;
import com.acr.review.domain.result.ReviewScoreDimension;
import com.acr.review.domain.result.ReviewScoreResult;
import com.acr.review.engine.OpenCodeReviewCliAdapter;
import com.acr.review.engine.OcrModelConfigMapper;
import com.acr.review.engine.ReviewEngineFailureType;
import com.acr.review.engine.ReviewEngineInvocationType;
import com.acr.review.engine.ReviewEngineRequest;
import com.acr.review.engine.ReviewEngineResult;
import com.acr.review.engine.ReviewEngineWorkspaceManager;
import com.acr.review.engine.config.ReviewEngineProperties;
import com.acr.review.git.GitPullRequestDiffFetcher;
import com.acr.review.git.GitPullRequestDiffResult;
import com.acr.review.git.GitPullRequestMetadata;
import com.acr.review.git.GitPullRequestMetadataFetcher;
import com.acr.review.git.GitPullRequestWorkspacePreparer;
import com.acr.review.git.GitPullRequestWorkspaceRequest;
import com.acr.review.git.GitPullRequestWorkspaceResult;
import com.acr.review.git.GitRepositoryCoordinates;
import com.acr.review.mapper.ReviewProjectMapper;
import com.acr.review.mapper.ReviewTaskMapper;
import com.acr.review.mapper.ReviewTaskRunMapper;
import com.acr.review.service.IGitCredentialService;
import com.acr.review.service.IReviewTaskExecutionService;
import com.acr.review.service.IReviewTaskSnapshotService;
import com.acr.review.service.ReviewConclusionResolver;
import com.acr.review.service.ReviewPromptComposer;
import com.acr.review.service.ReviewPromptRenderer;
import com.acr.review.service.ReviewScoreParseResult;
import com.acr.review.service.ReviewScoreResultParser;
import com.acr.review.service.ReviewScoringConstants;
import com.acr.review.service.ReviewTaskExecuteEvent;
import com.acr.system.domain.SysAiModelConfig;
import com.acr.system.service.ISysAiModelConfigService;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 审查任务执行编排。
 * 大模型审查与审查引擎两条链路互斥，按任务创建时冻结的快照分流。
 */
@Service
public class ReviewTaskExecutionServiceImpl implements IReviewTaskExecutionService
{
    private static final Logger log = LoggerFactory.getLogger(ReviewTaskExecutionServiceImpl.class);

    private final ReviewTaskMapper taskMapper;
    private final ReviewTaskRunMapper runMapper;
    private final ReviewProjectMapper projectMapper;
    private final IGitCredentialService credentialService;
    private final ISysAiModelConfigService aiModelConfigService;
    private final OcrModelConfigMapper modelConfigMapper;
    private final GitPullRequestWorkspacePreparer workspacePreparer;
    private final GitPullRequestDiffFetcher diffFetcher;
    private final GitPullRequestMetadataFetcher metadataFetcher;
    private final OpenCodeReviewCliAdapter reviewEngine;
    private final ReviewEngineWorkspaceManager workspaceManager;
    private final ReviewEngineProperties engineProperties;
    private final ReviewConclusionResolver conclusionResolver;
    private final ReviewPromptRenderer promptRenderer;
    private final ReviewPromptComposer promptComposer;
    private final ReviewScoreResultParser scoreResultParser;
    private final LlmCallService llmCallService;
    private final ApplicationEventPublisher eventPublisher;
    private final IReviewTaskSnapshotService snapshotService;
    private final Semaphore concurrencyLimiter;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final int llmTimeoutSeconds;

    public ReviewTaskExecutionServiceImpl(ReviewTaskMapper taskMapper,
                                          ReviewTaskRunMapper runMapper,
                                          ReviewProjectMapper projectMapper,
                                          IGitCredentialService credentialService,
                                          ISysAiModelConfigService aiModelConfigService,
                                          OcrModelConfigMapper modelConfigMapper,
                                          GitPullRequestWorkspacePreparer workspacePreparer,
                                          GitPullRequestDiffFetcher diffFetcher,
                                          GitPullRequestMetadataFetcher metadataFetcher,
                                          OpenCodeReviewCliAdapter reviewEngine,
                                          ReviewEngineWorkspaceManager workspaceManager,
                                          ReviewEngineProperties engineProperties,
                                          ReviewConclusionResolver conclusionResolver,
                                          ReviewPromptRenderer promptRenderer,
                                          ReviewPromptComposer promptComposer,
                                          ReviewScoreResultParser scoreResultParser,
                                          LlmCallService llmCallService,
                                          ApplicationEventPublisher eventPublisher,
                                          IReviewTaskSnapshotService snapshotService,
                                          @Value("${review.task.llm-timeout-seconds:120}") int llmTimeoutSeconds)
    {
        this.taskMapper = taskMapper;
        this.runMapper = runMapper;
        this.projectMapper = projectMapper;
        this.credentialService = credentialService;
        this.aiModelConfigService = aiModelConfigService;
        this.modelConfigMapper = modelConfigMapper;
        this.workspacePreparer = workspacePreparer;
        this.diffFetcher = diffFetcher;
        this.metadataFetcher = metadataFetcher;
        this.reviewEngine = reviewEngine;
        this.workspaceManager = workspaceManager;
        this.engineProperties = engineProperties;
        this.conclusionResolver = conclusionResolver;
        this.promptRenderer = promptRenderer;
        this.promptComposer = promptComposer;
        this.scoreResultParser = scoreResultParser;
        this.llmCallService = llmCallService;
        this.eventPublisher = eventPublisher;
        this.snapshotService = snapshotService;
        this.llmTimeoutSeconds = llmTimeoutSeconds;
        this.concurrencyLimiter = new Semaphore(Math.max(1, engineProperties.getMaxConcurrency()), true);
    }

    @Override
    public void scheduleExecution(Long taskId)
    {
        if (taskId == null)
        {
            return;
        }
        eventPublisher.publishEvent(new ReviewTaskExecuteEvent(this, taskId));
    }

    @Override
    public void retryTask(Long taskId)
    {
        ReviewTask task = taskMapper.selectReviewTaskById(taskId);
        if (task == null)
        {
            throw new ServiceException("审查任务不存在");
        }
        String status = task.getTaskStatus();
        // PENDING：建单后通常自动执行；若异步调度丢失或进程重启，允许手动触发。
        // FAILED：失败后人工重试，历史 run 保留。
        // RUNNING 超过回收阈值：上次执行已随进程中断，允许人工回收后重试。
        boolean staleRunning = ReviewPipelineConstants.TASK_RUNNING.equals(status) && isStaleRunning(task);
        if (!ReviewPipelineConstants.TASK_PENDING.equals(status)
            && !ReviewPipelineConstants.TASK_FAILED.equals(status)
            && !staleRunning)
        {
            throw new ServiceException(retryBlockedMessage(status));
        }
        scheduleExecution(taskId);
    }

    private boolean isStaleRunning(ReviewTask task)
    {
        Date startedTime = task.getStartedTime();
        if (startedTime == null)
        {
            return true;
        }
        long staleMillis = ReviewPipelineConstants.STALE_RUNNING_TIMEOUT_MINUTES * 60_000L;
        return System.currentTimeMillis() - startedTime.getTime() > staleMillis;
    }

    @Override
    public void executeTask(Long taskId)
    {
        Date started = new Date();
        int claimed = taskMapper.claimTask(taskId, ReviewPipelineConstants.STEP_RESOLVE_CONFIG, started,
            ReviewPipelineConstants.STALE_RUNNING_TIMEOUT_MINUTES);
        if (claimed != 1)
        {
            log.info("审查任务未被领取（可能已在执行或状态不符）, taskId={}", taskId);
            return;
        }

        long beginMs = System.currentTimeMillis();
        ReviewTask task = null;
        ReviewTaskRun run = null;
        try
        {
            task = taskMapper.selectReviewTaskById(taskId);
            if (task == null)
            {
                log.warn("审查任务领取后未找到记录, taskId={}", taskId);
                return;
            }

            run = insertRun(task, started);
            task.setAttemptCount(run.getAttemptNo());
            task.setLatestRunId(run.getRunId());
            task.setStartedTime(started);
            task.setCurrentStep(ReviewPipelineConstants.STEP_RESOLVE_CONFIG);
            taskMapper.updateTaskExecution(task);

            materializeSnapshotIfMissing(task);
            ExecutionPlan plan = resolveConfig(task, run);
            if (ReviewPipelineConstants.isOcrEngineMode(plan.reviewMode()))
            {
                executeOcrPath(task, run, plan, beginMs);
            }
            else
            {
                executeLlmPath(task, run, plan, beginMs);
            }
        }
        catch (ReviewExecutionException ex)
        {
            fail(task, run, beginMs, ex.failureType(),
                currentStepOf(run),
                ex.getMessage());
        }
        catch (ServiceException ex)
        {
            fail(task, run, beginMs, ReviewPipelineConstants.FAILURE_CONFIG_MISSING,
                currentStepOf(run),
                ex.getMessage());
        }
        catch (RuntimeException | java.io.IOException ex)
        {
            log.error("审查任务执行失败, taskId={}, runId={}", taskId, run == null ? null : run.getRunId(), ex);
            fail(task, run, beginMs, ReviewPipelineConstants.FAILURE_UNKNOWN,
                currentStepOf(run),
                "审查执行内部异常：" + StringUtils.defaultIfEmpty(ex.getMessage(), ex.getClass().getSimpleName()));
        }
    }

    /** 领取后立即建立 run 记录；快照审查方式取自任务建单冻结值，保证任意失败都有可审计的执行记录。 */
    private ReviewTaskRun insertRun(ReviewTask task, Date started)
    {
        ReviewTaskRun run = new ReviewTaskRun();
        run.setTaskId(task.getTaskId());
        run.setAttemptNo(nextAttemptNo(task.getTaskId()));
        run.setRunStatus(ReviewPipelineConstants.RUN_RUNNING);
        run.setCurrentStep(ReviewPipelineConstants.STEP_RESOLVE_CONFIG);
        run.setSnapshotReviewMode(ReviewPipelineConstants.normalizeReviewMode(task.getSnapshotReviewMode()));
        run.setSnapshotBaseSha(task.getBaseSha());
        run.setSnapshotHeadSha(task.getHeadSha());
        run.setStartedTime(started);
        run.setCreateBy("system");
        runMapper.insertReviewTaskRun(run);
        return run;
    }

    /**
     * 快照冻结上线前建单的历史任务没有执行快照，执行前按项目当前配置补冻结并落库，
     * 之后与正常任务一致只读快照。项目未配置时由快照服务抛出带配置指引的异常。
     */
    private void materializeSnapshotIfMissing(ReviewTask task)
    {
        if (StringUtils.isNotEmpty(task.getSnapshotReviewMode()))
        {
            return;
        }
        ReviewProject project = projectMapper.selectReviewProjectById(task.getProjectId());
        if (project == null || !"0".equals(project.getStatus()))
        {
            throw new ReviewExecutionException(ReviewPipelineConstants.FAILURE_CONFIG_MISSING,
                "项目不存在或已停用，无法执行审查");
        }
        snapshotService.freezeExecutionSnapshot(project, task);
        taskMapper.updateTaskSnapshot(task);
        log.info("历史任务缺少执行快照，已按项目当前配置补冻结, taskId={}, reviewMode={}",
            task.getTaskId(), task.getSnapshotReviewMode());
    }

    private String currentStepOf(ReviewTaskRun run)
    {
        return run == null || StringUtils.isEmpty(run.getCurrentStep())
            ? ReviewPipelineConstants.STEP_RESOLVE_CONFIG
            : run.getCurrentStep();
    }

    private void executeOcrPath(ReviewTask task, ReviewTaskRun run, ExecutionPlan plan, long beginMs)
        throws java.io.IOException
    {
        updateStep(task, run, ReviewPipelineConstants.STEP_PREPARE_WORKSPACE);
        // 与大模型路径共用同一 PR 详情请求结果：补充提交者/增删行/Commit Message，不额外扩请求。
        applyPrMetadata(task, run, plan);
        Path workspace = workspaceManager.createIsolatedWorkspace();
        boolean acquired = false;
        try
        {
            GitPullRequestWorkspaceResult workspaceResult = workspacePreparer.prepare(
                new GitPullRequestWorkspaceRequest(
                    plan.repository(), plan.token(), task.getBaseSha(), task.getHeadSha(), workspace.toString()));
            if (!workspaceResult.success())
            {
                fail(task, run, beginMs, workspaceResult.failureType(),
                    ReviewPipelineConstants.STEP_PREPARE_WORKSPACE, workspaceResult.message());
                return;
            }

            acquired = concurrencyLimiter.tryAcquire();
            if (!acquired)
            {
                fail(task, run, beginMs, ReviewPipelineConstants.FAILURE_CONCURRENCY,
                    ReviewPipelineConstants.STEP_INVOKE_ENGINE, "审查引擎并发已达上限，请稍后重试");
                return;
            }

            updateStep(task, run, ReviewPipelineConstants.STEP_INVOKE_ENGINE);
            ReviewEngineRequest request = new ReviewEngineRequest();
            request.setWorkingDirectory(workspaceResult.workingDirectory());
            request.setBaseSha(task.getBaseSha());
            request.setHeadSha(task.getHeadSha());
            request.setProjectKey(String.valueOf(task.getProjectId()));
            request.setRepositoryKey(plan.repository().owner() + "/" + plan.repository().repository());
            request.setModelEnvironment(plan.modelEnvironment());
            request.setTimeoutSeconds(engineProperties.getDefaultTimeoutSeconds());
            request.setInvocationType(ReviewEngineInvocationType.REVIEW);

            ReviewEngineResult engineResult = reviewEngine.execute(request);
            if (engineResult.getEngineVersion() != null)
            {
                run.setSnapshotEngineVersion(engineResult.getEngineVersion());
            }
            if (!engineResult.isSuccess())
            {
                fail(task, run, beginMs, mapEngineFailure(engineResult.getFailureType()),
                    ReviewPipelineConstants.STEP_INVOKE_ENGINE,
                    StringUtils.defaultIfEmpty(engineResult.getFailureReason(), "审查引擎执行失败"));
                return;
            }

            persistSuccess(task, run, beginMs, engineResult.getStructuredResult(),
                Math.max(engineResult.getDurationMs(), System.currentTimeMillis() - beginMs));
        }
        finally
        {
            if (acquired)
            {
                concurrencyLimiter.release();
            }
            workspaceManager.cleanup(workspace);
        }
    }

    private void executeLlmPath(ReviewTask task, ReviewTaskRun run, ExecutionPlan plan, long beginMs)
    {
        updateStep(task, run, ReviewPipelineConstants.STEP_PREPARE_WORKSPACE);
        GitPullRequestDiffResult diffResult = diffFetcher.fetchDiff(
            plan.repository(), plan.token(), task.getBaseSha(), task.getHeadSha());
        if (!diffResult.success())
        {
            fail(task, run, beginMs, diffResult.failureType(),
                ReviewPipelineConstants.STEP_PREPARE_WORKSPACE, diffResult.message());
            return;
        }
        if (StringUtils.isEmpty(diffResult.diffContent()))
        {
            fail(task, run, beginMs, ReviewPipelineConstants.FAILURE_WORKSPACE_PREPARE,
                ReviewPipelineConstants.STEP_PREPARE_WORKSPACE, "PR Diff 为空，没有可审查的变更");
            return;
        }

        applyPrMetadata(task, run, plan);
        String prDescription = StringUtils.isEmpty(run.getPrDescription())
            ? "（未获取到 PR 描述）" : run.getPrDescription();
        String commitMessages = StringUtils.isEmpty(run.getCommitMessages())
            ? "（未获取到 Commit Message）" : run.getCommitMessages();

        String templateBody = promptComposer.stripConflictingOutputInstructions(plan.promptContent());
        String renderedBody = promptRenderer.render(
            templateBody, task, diffResult.diffContent(), prDescription, commitMessages);
        String finalPrompt = promptComposer.composeFromSnapshotTemplate(templateBody, renderedBody);
        run.setRenderedPrompt(truncate(finalPrompt, ReviewScoringConstants.MAX_RENDERED_PROMPT_CHARS));
        run.setProtocolVersion(ReviewScoringConstants.PROTOCOL_VERSION);
        run.setScoreWeightsJson(toJsonQuietly(ReviewScoringConstants.scoreWeights()));
        run.setScoreThreshold(null);
        runMapper.updateReviewTaskRun(run);

        updateStep(task, run, ReviewPipelineConstants.STEP_INVOKE_MODEL);
        LlmCallResult llmResult = llmCallService.chat(plan.modelId(), finalPrompt);
        if (!llmResult.isSuccess())
        {
            fail(task, run, beginMs, ReviewPipelineConstants.FAILURE_MODEL,
                ReviewPipelineConstants.STEP_INVOKE_MODEL,
                StringUtils.defaultIfEmpty(llmResult.getErrorMessage(), "大模型审查调用失败"));
            return;
        }

        ReviewScoreParseResult parseResult = scoreResultParser.parse(llmResult.getContent());
        if (!parseResult.isSuccess())
        {
            persistLlmFormatFailure(task, run, beginMs, parseResult,
                Math.max(llmResult.getLatencyMs(), System.currentTimeMillis() - beginMs));
            return;
        }
        persistLlmSuccess(task, run, beginMs, parseResult,
            Math.max(llmResult.getLatencyMs(), System.currentTimeMillis() - beginMs));
    }

    private ExecutionPlan resolveConfig(ReviewTask task, ReviewTaskRun run)
    {
        ReviewProject project = projectMapper.selectReviewProjectById(task.getProjectId());
        if (project == null || !"0".equals(project.getStatus()))
        {
            throw new ReviewExecutionException(ReviewPipelineConstants.FAILURE_CONFIG_MISSING, "项目不存在或已停用，无法执行审查");
        }
        if (!"GITHUB".equalsIgnoreCase(project.getProvider()))
        {
            throw new ReviewExecutionException(ReviewPipelineConstants.FAILURE_CONFIG_MISSING, "当前仅支持 GitHub 项目审查");
        }

        String reviewMode = ReviewPipelineConstants.normalizeReviewMode(task.getSnapshotReviewMode());
        String token;
        try
        {
            token = credentialService.getPlainToken(project.getCredentialId(), true);
        }
        catch (ServiceException ex)
        {
            throw new ReviewExecutionException(ReviewPipelineConstants.FAILURE_CREDENTIAL_ERROR,
                "GitHub 凭据不可用：" + ex.getMessage());
        }

        GitRepositoryCoordinates repository = new GitRepositoryCoordinates(
            project.getRepositoryOwner(), project.getRepositoryName(), project.getRepositoryUrl());

        if (ReviewPipelineConstants.isLlmDirectMode(reviewMode))
        {
            if (task.getSnapshotModelId() == null)
            {
                throw new ReviewExecutionException(ReviewPipelineConstants.FAILURE_CONFIG_MISSING,
                    "任务缺少大模型快照，无法执行审查");
            }
            if (StringUtils.isEmpty(task.getSnapshotPromptContent()))
            {
                throw new ReviewExecutionException(ReviewPipelineConstants.FAILURE_CONFIG_MISSING,
                    "任务缺少审查模板正文快照，无法执行审查");
            }

            run.setSnapshotReviewMode(ReviewPipelineConstants.REVIEW_MODE_LLM_DIRECT);
            run.setSnapshotEngineCode(null);
            run.setSnapshotEngineName(null);
            run.setSnapshotModelId(task.getSnapshotModelId());
            run.setSnapshotModelName(task.getSnapshotModelName());
            run.setSnapshotModelProvider(task.getSnapshotModelProvider());
            run.setSnapshotModel(task.getSnapshotModel());
            run.setSnapshotTemplateId(task.getSnapshotTemplateId());
            run.setSnapshotTemplateName(task.getSnapshotTemplateName());
            run.setSnapshotTemplateCode(task.getSnapshotTemplateCode());
            run.setSnapshotTemplateVersion(task.getSnapshotTemplateVersion());
            run.setSnapshotPromptContent(task.getSnapshotPromptContent());
            run.setSnapshotTimeoutSeconds(llmTimeoutSeconds);
            runMapper.updateReviewTaskRun(run);

            return new ExecutionPlan(reviewMode, repository, token, task.getSnapshotModelId(),
                null, task.getSnapshotPromptContent(), null);
        }

        if (!ReviewPipelineConstants.isOcrEngineMode(reviewMode))
        {
            throw new ReviewExecutionException(ReviewPipelineConstants.FAILURE_CONFIG_MISSING,
                "不支持的审查方式：" + reviewMode);
        }
        String engineCode = task.getSnapshotEngineCode();
        if (!ReviewPipelineConstants.ENGINE_OPEN_CODE_REVIEW.equals(engineCode))
        {
            throw new ReviewExecutionException(ReviewPipelineConstants.FAILURE_CONFIG_MISSING, "当前仅支持 open-code-review 审查引擎");
        }
        // OCR 的平台默认模型仅用于引擎运行环境，不属于项目配置或审查模板快照。
        SysAiModelConfig runtimeModel = aiModelConfigService.selectDefaultRuntimeConfig();
        if (runtimeModel == null || !"1".equals(runtimeModel.getEnabled()) || StringUtils.isEmpty(runtimeModel.getApiKey()))
        {
            throw new ReviewExecutionException(ReviewPipelineConstants.FAILURE_CONFIG_MISSING,
                "审查引擎需要平台默认模型作为运行时模型，请先在「模型服务」配置并启用默认模型");
        }

        run.setSnapshotReviewMode(ReviewPipelineConstants.REVIEW_MODE_OCR_ENGINE);
        run.setSnapshotEngineCode(engineCode);
        run.setSnapshotEngineName(task.getSnapshotEngineName());
        run.setSnapshotModelId(runtimeModel.getModelId());
        run.setSnapshotModelName(runtimeModel.getModelName() + "（引擎运行时）");
        run.setSnapshotModelProvider(runtimeModel.getProvider());
        run.setSnapshotModel(runtimeModel.getModel());
        run.setSnapshotTemplateId(null);
        run.setSnapshotTemplateName(null);
        run.setSnapshotTemplateCode(null);
        run.setSnapshotTemplateVersion(null);
        run.setSnapshotPromptContent(null);
        run.setSnapshotTimeoutSeconds(engineProperties.getDefaultTimeoutSeconds());
        runMapper.updateReviewTaskRun(run);

        return new ExecutionPlan(reviewMode, repository, token, null, engineCode, null,
            modelConfigMapper.toEnvironment(runtimeModel));
    }

    private void persistSuccess(ReviewTask task, ReviewTaskRun run, long beginMs,
                                Map<String, Object> structured, long durationHint)
    {
        updateStep(task, run, ReviewPipelineConstants.STEP_PERSIST_RESULT);
        String conclusion = conclusionResolver.resolve(structured);
        Object explicit = structured == null ? null : structured.get("conclusion");
        if (explicit != null)
        {
            String value = String.valueOf(explicit).trim().toUpperCase();
            if (ReviewPipelineConstants.CONCLUSION_PASS.equals(value)
                || ReviewPipelineConstants.CONCLUSION_WARN.equals(value)
                || ReviewPipelineConstants.CONCLUSION_BLOCK.equals(value))
            {
                conclusion = value;
            }
        }
        String summary = conclusionResolver.summarize(structured, conclusion);
        long duration = Math.max(durationHint, System.currentTimeMillis() - beginMs);
        Date finished = new Date();

        run.setRunStatus(ReviewPipelineConstants.RUN_SUCCESS);
        run.setCurrentStep(ReviewPipelineConstants.STEP_PERSIST_RESULT);
        run.setReviewConclusion(conclusion);
        run.setResultSummary(summary);
        run.setResultJson(toResultJson(structured));
        run.setDurationMs(duration);
        run.setFinishedTime(finished);
        run.setFailureStep(null);
        run.setFailureType(null);
        run.setFailureMessage(null);
        runMapper.updateReviewTaskRun(run);

        task.setTaskStatus(ReviewPipelineConstants.TASK_SUCCESS);
        task.setReviewConclusion(conclusion);
        task.setCurrentStep(ReviewPipelineConstants.STEP_PERSIST_RESULT);
        task.setFailureStep(null);
        task.setFailureType(null);
        task.setFailureMessage(null);
        task.setFinishedTime(finished);
        task.setDurationMs(duration);
        taskMapper.updateTaskExecution(task);
    }

    private void persistLlmSuccess(ReviewTask task, ReviewTaskRun run, long beginMs,
                                   ReviewScoreParseResult parseResult, long durationHint)
    {
        updateStep(task, run, ReviewPipelineConstants.STEP_PERSIST_RESULT);
        ReviewScoreResult result = parseResult.getResult();
        String conclusion = scoreResultParser.resolveConclusion(result);
        String summary = scoreResultParser.summarize(result, conclusion);
        long duration = Math.max(durationHint, System.currentTimeMillis() - beginMs);
        Date finished = new Date();
        Map<String, Integer> scores = dimensionScoreMap(result);

        run.setRunStatus(ReviewPipelineConstants.RUN_SUCCESS);
        run.setCurrentStep(ReviewPipelineConstants.STEP_PERSIST_RESULT);
        run.setReviewConclusion(conclusion);
        run.setResultSummary(summary);
        run.setResultJson(toJsonQuietly(result));
        run.setTotalScore(result.getTotalScore());
        run.setScoreCorrectness(scores.get(ReviewScoringConstants.DIM_CORRECTNESS));
        run.setScoreSecurity(scores.get(ReviewScoringConstants.DIM_SECURITY));
        run.setScorePractice(scores.get(ReviewScoringConstants.DIM_PRACTICE));
        run.setScorePerformance(scores.get(ReviewScoringConstants.DIM_PERFORMANCE));
        run.setScoreCommitQuality(scores.get(ReviewScoringConstants.DIM_COMMIT_QUALITY));
        run.setProtocolVersion(ReviewScoringConstants.PROTOCOL_VERSION);
        run.setScoreWeightsJson(toJsonQuietly(ReviewScoringConstants.scoreWeights()));
        run.setScoreThreshold(null);
        run.setFocusIssueCount(result.getFocusIssueCount());
        run.setHasCriticalSecurity(Boolean.TRUE.equals(result.getHasCriticalSecurityIssue()) ? "1" : "0");
        run.setTopIssuesJson(toJsonQuietly(result.getTopIssues()));
        run.setParseStatus(ReviewScoringConstants.PARSE_SUCCESS);
        run.setParseError(null);
        run.setRawResponseExcerpt(parseResult.getRawExcerpt());
        run.setDurationMs(duration);
        run.setFinishedTime(finished);
        run.setFailureStep(null);
        run.setFailureType(null);
        run.setFailureMessage(null);
        runMapper.updateReviewTaskRun(run);

        task.setTaskStatus(ReviewPipelineConstants.TASK_SUCCESS);
        task.setReviewConclusion(conclusion);
        task.setCurrentStep(ReviewPipelineConstants.STEP_PERSIST_RESULT);
        task.setFailureStep(null);
        task.setFailureType(null);
        task.setFailureMessage(null);
        task.setFinishedTime(finished);
        task.setDurationMs(duration);
        task.setTotalScore(result.getTotalScore());
        task.setScoreCorrectness(run.getScoreCorrectness());
        task.setScoreSecurity(run.getScoreSecurity());
        task.setScorePractice(run.getScorePractice());
        task.setScorePerformance(run.getScorePerformance());
        task.setScoreCommitQuality(run.getScoreCommitQuality());
        task.setProtocolVersion(ReviewScoringConstants.PROTOCOL_VERSION);
        task.setFocusIssueCount(result.getFocusIssueCount());
        task.setHasCriticalSecurity(run.getHasCriticalSecurity());
        task.setParseStatus(ReviewScoringConstants.PARSE_SUCCESS);
        taskMapper.updateTaskExecution(task);
    }

    private void persistLlmFormatFailure(ReviewTask task, ReviewTaskRun run, long beginMs,
                                         ReviewScoreParseResult parseResult, long durationHint)
    {
        updateStep(task, run, ReviewPipelineConstants.STEP_PERSIST_RESULT);
        long duration = Math.max(durationHint, System.currentTimeMillis() - beginMs);
        Date finished = new Date();
        String message = truncate(StringUtils.defaultIfEmpty(parseResult.getErrorMessage(), "审查结果格式异常"), 480);

        run.setRunStatus(ReviewPipelineConstants.RUN_FAILED);
        run.setCurrentStep(ReviewPipelineConstants.STEP_PERSIST_RESULT);
        run.setFailureStep(ReviewPipelineConstants.STEP_PERSIST_RESULT);
        run.setFailureType(ReviewPipelineConstants.FAILURE_RESULT_FORMAT);
        run.setFailureMessage(message);
        run.setParseStatus(ReviewScoringConstants.PARSE_FAILED);
        run.setParseError(message);
        run.setRawResponseExcerpt(parseResult.getRawExcerpt());
        run.setProtocolVersion(ReviewScoringConstants.PROTOCOL_VERSION);
        run.setScoreWeightsJson(toJsonQuietly(ReviewScoringConstants.scoreWeights()));
        run.setScoreThreshold(null);
        run.setResultJson(null);
        run.setResultSummary("结果格式异常，未生成可统计的标准化审查结果");
        run.setDurationMs(duration);
        run.setFinishedTime(finished);
        runMapper.updateReviewTaskRun(run);

        task.setTaskStatus(ReviewPipelineConstants.TASK_FAILED);
        task.setCurrentStep(ReviewPipelineConstants.STEP_PERSIST_RESULT);
        task.setFailureStep(ReviewPipelineConstants.STEP_PERSIST_RESULT);
        task.setFailureType(ReviewPipelineConstants.FAILURE_RESULT_FORMAT);
        task.setFailureMessage(message);
        task.setParseStatus(ReviewScoringConstants.PARSE_FAILED);
        task.setProtocolVersion(ReviewScoringConstants.PROTOCOL_VERSION);
        task.setFinishedTime(finished);
        task.setDurationMs(duration);
        taskMapper.updateTaskExecution(task);
    }

    private Map<String, Integer> dimensionScoreMap(ReviewScoreResult result)
    {
        Map<String, Integer> scores = new HashMap<>();
        if (result == null || result.getScores() == null)
        {
            return scores;
        }
        for (ReviewScoreDimension dimension : result.getScores())
        {
            if (dimension != null && dimension.getDimension() != null)
            {
                scores.put(dimension.getDimension(), dimension.getScore());
            }
        }
        return scores;
    }

    private String toJsonQuietly(Object value)
    {
        try
        {
            return objectMapper.writeValueAsString(value == null ? Map.of() : value);
        }
        catch (Exception ex)
        {
            return null;
        }
    }

    private void updateStep(ReviewTask task, ReviewTaskRun run, String step)
    {
        run.setCurrentStep(step);
        runMapper.updateReviewTaskRun(run);
        task.setCurrentStep(step);
        taskMapper.updateTaskExecution(task);
    }

    /**
     * 拉取并落库 PR 元数据。PR 详情接口一次拿齐描述/作者/增删行；提交说明失败不阻断。
     */
    private void applyPrMetadata(ReviewTask task, ReviewTaskRun run, ExecutionPlan plan)
    {
        GitPullRequestMetadata metadata = metadataFetcher.fetch(
            plan.repository(), plan.token(), task.getPrNumber());
        if (metadata == null || !metadata.fetched())
        {
            log.info("PR 元数据未获取成功，继续审查。taskId={}, reason={}",
                task.getTaskId(), metadata == null ? "空结果" : metadata.message());
            return;
        }
        String description = metadata.prDescription() == null ? "" : metadata.prDescription();
        String commits = metadata.commitMessages() == null ? "" : metadata.commitMessages();
        run.setPrDescription(truncate(description, ReviewScoringConstants.MAX_PR_DESCRIPTION_CHARS));
        run.setCommitMessages(truncate(commits, ReviewScoringConstants.MAX_COMMIT_MESSAGES_CHARS));
        if (StringUtils.isNotEmpty(metadata.prAuthor()))
        {
            task.setPrAuthor(metadata.prAuthor());
        }
        if (metadata.additions() != null)
        {
            task.setAdditions(metadata.additions());
        }
        if (metadata.deletions() != null)
        {
            task.setDeletions(metadata.deletions());
        }
        if (metadata.changedFiles() != null)
        {
            task.setChangedFiles(metadata.changedFiles());
        }
        runMapper.updateReviewTaskRun(run);
        taskMapper.updateTaskExecution(task);
    }

    private void fail(ReviewTask task, ReviewTaskRun run, long beginMs, String failureType, String failureStep, String message)
    {
        Date finished = new Date();
        long duration = System.currentTimeMillis() - beginMs;
        String safeMessage = truncate(message, 480);

        if (run != null && run.getRunId() != null)
        {
            run.setRunStatus(ReviewPipelineConstants.RUN_FAILED);
            run.setCurrentStep(failureStep);
            run.setFailureStep(failureStep);
            run.setFailureType(failureType);
            run.setFailureMessage(safeMessage);
            run.setDurationMs(duration);
            run.setFinishedTime(finished);
            runMapper.updateReviewTaskRun(run);
        }

        if (task != null)
        {
            task.setTaskStatus(ReviewPipelineConstants.TASK_FAILED);
            task.setCurrentStep(failureStep);
            task.setFailureStep(failureStep);
            task.setFailureType(failureType);
            task.setFailureMessage(safeMessage);
            task.setFinishedTime(finished);
            task.setDurationMs(duration);
            taskMapper.updateTaskExecution(task);
        }
    }

    private int nextAttemptNo(Long taskId)
    {
        Integer max = runMapper.selectMaxAttemptNo(taskId);
        return max == null ? 1 : max + 1;
    }

    private String mapEngineFailure(ReviewEngineFailureType failureType)
    {
        if (failureType == null)
        {
            return ReviewPipelineConstants.FAILURE_ENGINE;
        }
        return switch (failureType)
        {
            case TIMEOUT -> ReviewPipelineConstants.FAILURE_TIMEOUT;
            case MODEL_CALL_FAILED -> ReviewPipelineConstants.FAILURE_MODEL;
            case CONCURRENCY_LIMIT -> ReviewPipelineConstants.FAILURE_CONCURRENCY;
            case WORKSPACE_ERROR -> ReviewPipelineConstants.FAILURE_WORKSPACE_PREPARE;
            default -> ReviewPipelineConstants.FAILURE_ENGINE;
        };
    }

    private String toResultJson(Map<String, Object> structuredResult)
    {
        try
        {
            String json = objectMapper.writeValueAsString(structuredResult == null ? Map.of() : structuredResult);
            if (json.length() > ReviewPipelineConstants.MAX_RESULT_JSON_CHARS)
            {
                return json.substring(0, ReviewPipelineConstants.MAX_RESULT_JSON_CHARS);
            }
            return json;
        }
        catch (Exception ex)
        {
            return "{\"message\":\"结构化结果序列化失败\"}";
        }
    }

    private String truncate(String value, int max)
    {
        if (value == null)
        {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }

    static String retryBlockedMessage(String taskStatus)
    {
        return switch (StringUtils.defaultString(taskStatus))
        {
            case ReviewPipelineConstants.TASK_RUNNING ->
                "当前任务状态为「执行中」，请等待本次执行结束后再操作。若执行已中断超过 "
                    + ReviewPipelineConstants.STALE_RUNNING_TIMEOUT_MINUTES + " 分钟，可再次点击重试回收。";
            case ReviewPipelineConstants.TASK_SUCCESS ->
                "当前任务状态为「已完成」，不允许再次执行以免覆盖有效结论。如需重新审查，请等待新的 PR 事件生成新任务。";
            case "CANCELLED" ->
                "当前任务状态为「已取消」，不能执行。请关注后续 PR 事件或联系管理员。";
            default ->
                "仅「待执行」或「已失败」任务可手动触发执行。当前状态：" + taskStatus + "。请先在任务详情确认状态后再操作。";
        };
    }

    private record ExecutionPlan(
        String reviewMode,
        GitRepositoryCoordinates repository,
        String token,
        Long modelId,
        String engineCode,
        String promptContent,
        Map<String, String> modelEnvironment
    )
    {
    }

    private static final class ReviewExecutionException extends RuntimeException
    {
        private final String failureType;

        private ReviewExecutionException(String failureType, String message)
        {
            super(message);
            this.failureType = failureType;
        }

        private String failureType()
        {
            return failureType;
        }
    }
}
