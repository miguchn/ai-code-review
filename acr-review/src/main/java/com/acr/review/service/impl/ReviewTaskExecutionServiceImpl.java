package com.acr.review.service.impl;

import java.nio.file.Path;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
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
import com.acr.review.domain.GitCredential;
import com.acr.review.domain.ReviewPipelineConstants;
import com.acr.review.domain.ReviewProject;
import com.acr.review.domain.ReviewRoundReconcileResult;
import com.acr.review.domain.ReviewTask;
import com.acr.review.domain.ReviewTaskRun;
import com.acr.review.domain.result.ReviewScoreDimension;
import com.acr.review.domain.result.ReviewScoreResult;
import com.acr.review.domain.result.ReviewScopeStats;
import com.acr.review.domain.result.ReviewTopIssue;
import com.acr.review.engine.OpenCodeReviewCliAdapter;
import com.acr.review.engine.OcrModelConfigMapper;
import com.acr.review.engine.ReviewEngineFailureType;
import com.acr.review.engine.ReviewEngineInvocationType;
import com.acr.review.engine.ReviewEngineRequest;
import com.acr.review.engine.ReviewEngineResult;
import com.acr.review.engine.ReviewEngineResultMapper;
import com.acr.review.engine.ReviewEngineWorkspaceManager;
import com.acr.review.engine.config.ReviewEngineProperties;
import com.acr.review.git.GitAccessContext;
import com.acr.review.git.GitAdapterRegistry;
import com.acr.review.git.GitFileContentResult;
import com.acr.review.git.GitPullRequestDiffResult;
import com.acr.review.git.GitPullRequestMetadata;
import com.acr.review.git.GitPullRequestWorkspaceRequest;
import com.acr.review.git.GitPullRequestWorkspaceResult;
import com.acr.review.git.GitRepositoryCoordinates;
import com.acr.review.mapper.GitCredentialMapper;
import com.acr.review.mapper.ReviewProjectMapper;
import com.acr.review.mapper.ReviewTaskMapper;
import com.acr.review.mapper.ReviewTaskRunMapper;
import com.acr.review.scope.DiffParseResult;
import com.acr.review.scope.IssueOriginClassifier;
import com.acr.review.scope.ReviewScopeConfig;
import com.acr.review.scope.ReviewScopeDecision;
import com.acr.review.scope.ReviewScopeDecisionService;
import com.acr.review.scope.ReviewScopePromptAssembler;
import com.acr.review.scope.ReviewScopeRules;
import com.acr.review.scope.UnifiedDiffParser;
import com.acr.review.service.IGitCredentialService;
import com.acr.review.service.IReviewDeliveryService;
import com.acr.review.service.IReviewIssueService;
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
    private final GitCredentialMapper credentialMapper;
    private final IGitCredentialService credentialService;
    private final GitAdapterRegistry adapterRegistry;
    private final ISysAiModelConfigService aiModelConfigService;
    private final OcrModelConfigMapper modelConfigMapper;
    private final OpenCodeReviewCliAdapter reviewEngine;
    private final ReviewEngineResultMapper engineResultMapper;
    private final ReviewEngineWorkspaceManager workspaceManager;
    private final ReviewEngineProperties engineProperties;
    private final ReviewConclusionResolver conclusionResolver;
    private final ReviewPromptRenderer promptRenderer;
    private final ReviewPromptComposer promptComposer;
    private final ReviewScoreResultParser scoreResultParser;
    private final UnifiedDiffParser diffParser;
    private final ReviewScopeDecisionService scopeDecisionService;
    private final ReviewScopePromptAssembler scopeAssembler;
    private final LlmCallService llmCallService;
    private final ApplicationEventPublisher eventPublisher;
    private final IReviewTaskSnapshotService snapshotService;
    private final IReviewDeliveryService deliveryService;
    private final IReviewIssueService issueService;
    private final Semaphore concurrencyLimiter;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final int llmTimeoutSeconds;

    public ReviewTaskExecutionServiceImpl(ReviewTaskMapper taskMapper,
                                          ReviewTaskRunMapper runMapper,
                                          ReviewProjectMapper projectMapper,
                                          GitCredentialMapper credentialMapper,
                                          IGitCredentialService credentialService,
                                          GitAdapterRegistry adapterRegistry,
                                          ISysAiModelConfigService aiModelConfigService,
                                          OcrModelConfigMapper modelConfigMapper,
                                          OpenCodeReviewCliAdapter reviewEngine,
                                          ReviewEngineResultMapper engineResultMapper,
                                          ReviewEngineWorkspaceManager workspaceManager,
                                          ReviewEngineProperties engineProperties,
                                          ReviewConclusionResolver conclusionResolver,
                                          ReviewPromptRenderer promptRenderer,
                                          ReviewPromptComposer promptComposer,
                                          ReviewScoreResultParser scoreResultParser,
                                          UnifiedDiffParser diffParser,
                                          ReviewScopeDecisionService scopeDecisionService,
                                          ReviewScopePromptAssembler scopeAssembler,
                                          LlmCallService llmCallService,
                                          ApplicationEventPublisher eventPublisher,
                                          IReviewTaskSnapshotService snapshotService,
                                          IReviewDeliveryService deliveryService,
                                          IReviewIssueService issueService,
                                          @Value("${review.task.llm-timeout-seconds:120}") int llmTimeoutSeconds)
    {
        this.taskMapper = taskMapper;
        this.runMapper = runMapper;
        this.projectMapper = projectMapper;
        this.credentialMapper = credentialMapper;
        this.credentialService = credentialService;
        this.adapterRegistry = adapterRegistry;
        this.aiModelConfigService = aiModelConfigService;
        this.modelConfigMapper = modelConfigMapper;
        this.reviewEngine = reviewEngine;
        this.engineResultMapper = engineResultMapper;
        this.workspaceManager = workspaceManager;
        this.engineProperties = engineProperties;
        this.conclusionResolver = conclusionResolver;
        this.promptRenderer = promptRenderer;
        this.promptComposer = promptComposer;
        this.scoreResultParser = scoreResultParser;
        this.diffParser = diffParser;
        this.scopeDecisionService = scopeDecisionService;
        this.scopeAssembler = scopeAssembler;
        this.llmCallService = llmCallService;
        this.eventPublisher = eventPublisher;
        this.snapshotService = snapshotService;
        this.deliveryService = deliveryService;
        this.issueService = issueService;
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
        // M3.2 步 6：平台范围决策 → --exclude 规则 + 决策快照落库。
        // 决策依赖 Compare API 的 Diff，仅用于分类；失败时不加排除规则（保持引擎全量审查），不阻断。
        List<String> ocrExcludePatterns = resolveOcrScope(task, run, plan);
        Path workspace = workspaceManager.createIsolatedWorkspace();
        boolean acquired = false;
        try
        {
            GitPullRequestWorkspaceResult workspaceResult = adapterRegistry.requireWorkspacePreparer(plan.provider()).prepare(
                new GitPullRequestWorkspaceRequest(
                    plan.repository(), plan.access(), task.getBaseSha(), task.getHeadSha(), workspace.toString()));
            if (!workspaceResult.success())
            {
                fail(task, run, beginMs, workspaceResult.failureType(),
                    ReviewPipelineConstants.STEP_PREPARE_WORKSPACE, workspaceResult.message());
                return;
            }

            // base..head 无树变更时 OCR 会 0 选中并失败；对齐 LLM emptyScope，短路为通过。
            if (hasNoCommitDiff(workspaceResult.workingDirectory(), task.getBaseSha(), task.getHeadSha()))
            {
                String emptySummary = ReviewPipelineConstants.EVENT_SOURCE_PUSH.equals(task.getEventSource())
                    ? "本次推送无代码变更，未调用审查引擎，按通过处理"
                    : "本次变更无代码变更，未调用审查引擎，按通过处理";
                persistEmptyScopeSuccess(task, run, beginMs, emptySummary);
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
            request.setExcludePatterns(ocrExcludePatterns);
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
        GitPullRequestDiffResult diffResult = adapterRegistry.requireDiffFetcher(plan.provider()).fetchDiff(
            plan.repository(), plan.access(), task.getBaseSha(), task.getHeadSha());
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

        // M3.2 范围决策：解析 → 排除/扩展 → 扩展全文竞争剩余预算 → 决策快照落库。
        // 决策异常一律降级为全量 Diff，不阻断审查。
        ScopeResolution scope = resolveScope(task, plan, diffResult.diffContent());
        run.setScopeDecisionJson(toJsonQuietly(scope.snapshot()));
        runMapper.updateReviewTaskRun(run);
        if (scope.emptyScope())
        {
            persistEmptyScopeSuccess(task, run, beginMs);
            return;
        }

        applyPrMetadata(task, run, plan);
        boolean pushTask = ReviewPipelineConstants.EVENT_SOURCE_PUSH.equals(task.getEventSource());
        String prDescription = StringUtils.isEmpty(run.getPrDescription())
            ? (pushTask ? "（推送审查无合并请求描述）" : "（未获取到 PR 描述）")
            : run.getPrDescription();
        String commitMessages = StringUtils.isEmpty(run.getCommitMessages())
            ? "（未获取到 Commit Message）" : run.getCommitMessages();

        String templateBody = promptComposer.stripConflictingOutputInstructions(plan.promptContent());
        String renderedBody = promptRenderer.render(
            templateBody, task, scope.diffForPrompt(), prDescription, commitMessages);
        String finalPrompt = promptComposer.composeWithScope(
            renderedBody, scope.scopeApplied(), scope.hasFullContent(), pushTask);
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

        ReviewScoreParseResult parseResult = scoreResultParser.parse(llmResult.getContent(),
            scope.originClassifier(), scope.reportExisting());
        if (!parseResult.isSuccess())
        {
            persistLlmFormatFailure(task, run, beginMs, parseResult,
                Math.max(llmResult.getLatencyMs(), System.currentTimeMillis() - beginMs));
            return;
        }
        persistLlmSuccess(task, run, beginMs, parseResult, scope,
            Math.max(llmResult.getLatencyMs(), System.currentTimeMillis() - beginMs));
    }

    /**
     * 范围决策（M3.2）：统一 Diff 解析 → 文件级排除/扩展决策 → 高影响扩展全文按 head SHA 拉取并竞争剩余预算。
     * 任何内部异常都降级为全量 Diff（scopeApplied=false），保证审查不中断；
     * 决策结果（含降级原因）始终通过 ScopeResolution.snapshot 落库，可回查。
     * 成功时同时产出归属打标分类器与范围统计基数（步 5）：扩展全文纳入（FULL）的文件整体视为 NEW。
     */
    private ScopeResolution resolveScope(ReviewTask task, ExecutionPlan plan, String rawDiff)
    {
        try
        {
            DiffParseResult parsed = diffParser.parse(rawDiff);
            ReviewScopeConfig config = ReviewScopeConfig.fromTaskSnapshot(task);
            ReviewScopeDecision decision = scopeDecisionService.decide(parsed, config);

            Map<String, String> fetchedContents = new HashMap<>();
            Map<String, String> fetchFailures = new java.util.LinkedHashMap<>();
            for (ReviewScopeDecision.ExpandedFile file : scopeAssembler.planFetches(decision))
            {
                GitFileContentResult content = adapterRegistry.requireFileContentFetcher(plan.provider()).fetchFileContent(
                    plan.repository(), plan.access(), file.path(), task.getHeadSha());
                if (content.success())
                {
                    fetchedContents.put(file.path(), content.content());
                }
                else
                {
                    fetchFailures.put(file.path(), content.failureReason());
                    log.info("扩展文件全文拉取失败，降级保留 L0, taskId={}, path={}, reason={}",
                        task.getTaskId(), file.path(), content.failureReason());
                }
            }
            ReviewScopePromptAssembler.ReviewScopeAssembly assembly =
                scopeAssembler.assemble(decision, fetchedContents, fetchFailures);
            boolean emptyScope = decision.effectiveFileCount() == 0;

            java.util.Set<String> fullContentPaths = assembly.dispositions().stream()
                .filter(disposition -> ReviewScopePromptAssembler.STATUS_FULL.equals(disposition.status()))
                .map(ReviewScopePromptAssembler.ExpandedFileDisposition::path)
                .collect(java.util.stream.Collectors.toSet());
            IssueOriginClassifier originClassifier = new IssueOriginClassifier(parsed, fullContentPaths);
            ReviewScopeStats scopeStatsBase = new ReviewScopeStats();
            scopeStatsBase.setIncludedFiles(decision.includedFiles().size());
            scopeStatsBase.setExcludedFiles(decision.excludedFiles().size());
            scopeStatsBase.setExpandedFiles(decision.expandedFiles().size());
            scopeStatsBase.setTruncated(decision.truncated());

            return new ScopeResolution(true, assembly.diffForPrompt(), emptyScope,
                assembly.hasFullContent(), originClassifier, config.reportExisting(), scopeStatsBase,
                buildScopeSnapshot(decision, assembly, config));
        }
        catch (RuntimeException ex)
        {
            log.warn("审查范围决策失败，降级为全量 Diff, taskId={}", task.getTaskId(), ex);
            Map<String, Object> snapshot = new java.util.LinkedHashMap<>();
            snapshot.put("degraded", "DECISION_FAILED");
            snapshot.put("reason", StringUtils.defaultIfEmpty(ex.getMessage(), ex.getClass().getSimpleName()));
            return new ScopeResolution(false, rawDiff, false, false, null, false, null, snapshot);
        }
    }

    /** 决策快照：决策服务快照 + 扩展文件最终处置 + 生效配置（自包含，步 7 详情直接可读）。 */
    private Map<String, Object> buildScopeSnapshot(ReviewScopeDecision decision,
                                                   ReviewScopePromptAssembler.ReviewScopeAssembly assembly,
                                                   ReviewScopeConfig config)
    {
        Map<String, Object> snapshot = new java.util.LinkedHashMap<>(decision.toSnapshotMap());
        snapshot.put("expandedFiles", assembly.dispositions().stream()
            .map(disposition ->
            {
                Map<String, Object> item = new java.util.LinkedHashMap<String, Object>();
                item.put("path", disposition.path());
                item.put("rule", disposition.rule());
                item.put("status", disposition.status());
                if (disposition.reason() != null)
                {
                    item.put("reason", disposition.reason());
                }
                if (disposition.chars() > 0)
                {
                    item.put("chars", disposition.chars());
                }
                return item;
            })
            .toList());
        snapshot.put("finalDiffChars", assembly.diffForPrompt().length());
        snapshot.put("config", Map.of(
            "excludePatterns", config.excludePatterns(),
            "includeTests", config.includeTests(),
            "reportExisting", config.reportExisting(),
            "expandEnabled", config.expandEnabled()));
        return snapshot;
    }

    /**
     * OCR 路径范围决策（M3.2 步 6）：拉取 Compare Diff 仅用于平台分类决策，
     * 输出 CLI --exclude 规则集并落决策快照（与 LLM 路径同列、键结构对齐）。
     * Diff 不可用或决策异常时不加排除规则（引擎在真实工作区全量审查），审查不阻断。
     */
    private List<String> resolveOcrScope(ReviewTask task, ReviewTaskRun run, ExecutionPlan plan)
    {
        try
        {
            GitPullRequestDiffResult diffResult = adapterRegistry.requireDiffFetcher(plan.provider()).fetchDiff(
                plan.repository(), plan.access(), task.getBaseSha(), task.getHeadSha());
            if (!diffResult.success() || StringUtils.isEmpty(diffResult.diffContent()))
            {
                Map<String, Object> snapshot = new java.util.LinkedHashMap<>();
                snapshot.put("pathMode", ReviewPipelineConstants.REVIEW_MODE_OCR_ENGINE);
                snapshot.put("degraded", "DIFF_UNAVAILABLE");
                snapshot.put("reason", StringUtils.defaultIfEmpty(diffResult.message(), "PR Diff 为空或不可用"));
                run.setScopeDecisionJson(toJsonQuietly(snapshot));
                runMapper.updateReviewTaskRun(run);
                log.info("OCR 范围决策跳过（Diff 不可用），引擎按全量审查, taskId={}", task.getTaskId());
                return List.of();
            }

            DiffParseResult parsed = diffParser.parse(diffResult.diffContent());
            ReviewScopeConfig config = ReviewScopeConfig.fromTaskSnapshot(task);
            ReviewScopeDecision decision = scopeDecisionService.decide(parsed, config);

            // --exclude 为逗号分隔参数，含逗号的 glob 无法表达，剔除并记快照。
            List<String> usable = new java.util.ArrayList<>();
            List<String> skipped = new java.util.ArrayList<>();
            for (String pattern : ReviewScopeRules.mergedExcludeGlobs(config))
            {
                if (pattern.contains(","))
                {
                    skipped.add(pattern);
                }
                else
                {
                    usable.add(pattern);
                }
            }
            run.setScopeDecisionJson(toJsonQuietly(buildOcrScopeSnapshot(decision, config, usable, skipped)));
            runMapper.updateReviewTaskRun(run);
            return List.copyOf(usable);
        }
        catch (RuntimeException ex)
        {
            log.warn("OCR 范围决策失败，引擎按全量审查, taskId={}", task.getTaskId(), ex);
            Map<String, Object> snapshot = new java.util.LinkedHashMap<>();
            snapshot.put("pathMode", ReviewPipelineConstants.REVIEW_MODE_OCR_ENGINE);
            snapshot.put("degraded", "DECISION_FAILED");
            snapshot.put("reason", StringUtils.defaultIfEmpty(ex.getMessage(), ex.getClass().getSimpleName()));
            run.setScopeDecisionJson(toJsonQuietly(snapshot));
            runMapper.updateReviewTaskRun(run);
            return List.of();
        }
    }

    /**
     * OCR 决策快照：分类结果 + 实际生效的排除规则数 + 生效配置。
     * 不照搬 LLM 的 L0 截断字段——OCR 由引擎在真实工作区审查 --exclude 之外的变更文件，
     * 平台字符预算截断不适用；高影响扩展在工作区全文自然可见，无需平台拉取。
     */
    private Map<String, Object> buildOcrScopeSnapshot(ReviewScopeDecision decision, ReviewScopeConfig config,
                                                      List<String> usablePatterns, List<String> skippedPatterns)
    {
        Map<String, Object> snapshot = new java.util.LinkedHashMap<>();
        snapshot.put("pathMode", ReviewPipelineConstants.REVIEW_MODE_OCR_ENGINE);
        snapshot.put("excludedFiles", decision.excludedFiles().stream()
            .map(file -> Map.of("path", file.path(), "reason", file.reason())).toList());
        snapshot.put("expandedFiles", decision.expandedFiles().stream()
            .map(file -> Map.of("path", file.path(), "rule", file.rule())).toList());
        snapshot.put("recordOnlyFiles", decision.recordOnlyFiles().stream()
            .map(file -> Map.of("path", file.path(), "reason", file.reason())).toList());
        snapshot.put("appliedExcludeGlobs", usablePatterns.size());
        snapshot.put("skippedExcludePatterns", skippedPatterns);
        snapshot.put("note", "OCR 由引擎在真实工作区审查 --exclude 之外的变更文件，平台 L0 预算截断不适用；高影响扩展文件全文在工作区自然可见");
        snapshot.put("config", Map.of(
            "excludePatterns", config.excludePatterns(),
            "includeTests", config.includeTests(),
            "reportExisting", config.reportExisting(),
            "expandEnabled", config.expandEnabled()));
        return snapshot;
    }

    /** 无有效审查范围：不调用模型，按通过落库并在摘要说明原因；决策快照已在调用前落库。 */
    private void persistEmptyScopeSuccess(ReviewTask task, ReviewTaskRun run, long beginMs)
    {
        persistEmptyScopeSuccess(task, run, beginMs,
            "本次变更无有效审查范围（全部文件被排除规则命中或为删除、改名等记录类变更），未调用模型，按通过处理");
    }

    private void persistEmptyScopeSuccess(ReviewTask task, ReviewTaskRun run, long beginMs, String summary)
    {
        updateStep(task, run, ReviewPipelineConstants.STEP_PERSIST_RESULT);
        long duration = System.currentTimeMillis() - beginMs;
        Date finished = new Date();

        run.setRunStatus(ReviewPipelineConstants.RUN_SUCCESS);
        run.setCurrentStep(ReviewPipelineConstants.STEP_PERSIST_RESULT);
        run.setReviewConclusion(ReviewPipelineConstants.CONCLUSION_PASS);
        run.setResultSummary(summary);
        run.setDurationMs(duration);
        run.setFinishedTime(finished);
        run.setFailureStep(null);
        run.setFailureType(null);
        run.setFailureMessage(null);
        runMapper.updateReviewTaskRun(run);

        task.setTaskStatus(ReviewPipelineConstants.TASK_SUCCESS);
        task.setReviewConclusion(ReviewPipelineConstants.CONCLUSION_PASS);
        task.setCurrentStep(ReviewPipelineConstants.STEP_PERSIST_RESULT);
        task.setFailureStep(null);
        task.setFailureType(null);
        task.setFailureMessage(null);
        task.setFinishedTime(finished);
        task.setDurationMs(duration);
        taskMapper.updateTaskExecution(task);
        deliverQuietly(task, run);
    }

    /**
     * 工作区 base..head 是否无树变更（{@code git diff --quiet}）。
     * 检测失败时返回 false，继续走引擎，避免误短路。
     */
    static boolean hasNoCommitDiff(String workingDirectory, String baseSha, String headSha)
    {
        if (workingDirectory == null || workingDirectory.isBlank()
            || baseSha == null || baseSha.isBlank()
            || headSha == null || headSha.isBlank())
        {
            return false;
        }
        if (baseSha.equals(headSha))
        {
            return true;
        }
        try
        {
            ProcessBuilder builder = new ProcessBuilder(
                "git", "-C", workingDirectory, "diff", "--quiet", baseSha, headSha);
            builder.redirectErrorStream(true);
            builder.environment().put("GIT_TERMINAL_PROMPT", "0");
            Process process = builder.start();
            boolean finished = process.waitFor(30, java.util.concurrent.TimeUnit.SECONDS);
            if (!finished)
            {
                process.destroyForcibly();
                return false;
            }
            // 0=无差异，1=有差异，其它=异常
            return process.exitValue() == 0;
        }
        catch (Exception ex)
        {
            return false;
        }
    }

    private ExecutionPlan resolveConfig(ReviewTask task, ReviewTaskRun run)
    {
        ReviewProject project = projectMapper.selectReviewProjectById(task.getProjectId());
        if (project == null || !"0".equals(project.getStatus()))
        {
            throw new ReviewExecutionException(ReviewPipelineConstants.FAILURE_CONFIG_MISSING, "项目不存在或已停用，无法执行审查");
        }

        String provider = project.getProvider();
        GitCredential credential = credentialMapper.selectGitCredentialById(project.getCredentialId());
        if (credential == null || !"0".equals(credential.getStatus()))
        {
            throw new ReviewExecutionException(ReviewPipelineConstants.FAILURE_CREDENTIAL_ERROR, "项目绑定的 Git 凭据不存在或已停用");
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
                "Git 凭据不可用：" + ex.getMessage());
        }

        GitAccessContext access = GitAccessContext.of(token,
            GitCredentialServiceImpl.resolveServerUrl(provider, credential.getServerUrl()));

        String fullPath = StringUtils.isNotEmpty(project.getRepositoryFullPath())
            ? project.getRepositoryFullPath()
            : project.getRepositoryOwner() + "/" + project.getRepositoryName();
        GitRepositoryCoordinates repository = new GitRepositoryCoordinates(
            project.getRepositoryOwner(), project.getRepositoryName(), fullPath, project.getRepositoryUrl());

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

            return new ExecutionPlan(provider, reviewMode, repository, access, task.getSnapshotModelId(),
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

        return new ExecutionPlan(provider, reviewMode, repository, access, null, engineCode, null,
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
        List<ReviewTopIssue> topIssues = engineResultMapper.mapTopIssues(structured);
        int focusIssueCount = ReviewScoreResultParser.countFocusIssues(topIssues);
        String hasCriticalSecurity = hasCriticalSecurityIssue(topIssues) ? "1" : "0";
        long duration = Math.max(durationHint, System.currentTimeMillis() - beginMs);
        Date finished = new Date();

        run.setRunStatus(ReviewPipelineConstants.RUN_SUCCESS);
        run.setCurrentStep(ReviewPipelineConstants.STEP_PERSIST_RESULT);
        run.setReviewConclusion(conclusion);
        run.setResultSummary(summary);
        run.setResultJson(toResultJson(structured));
        run.setTopIssuesJson(toJsonQuietly(topIssues));
        run.setFocusIssueCount(focusIssueCount);
        run.setHasCriticalSecurity(hasCriticalSecurity);
        run.setParseStatus(ReviewScoringConstants.PARSE_SUCCESS);
        run.setParseError(null);
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
        task.setFocusIssueCount(focusIssueCount);
        task.setHasCriticalSecurity(hasCriticalSecurity);
        task.setParseStatus(ReviewScoringConstants.PARSE_SUCCESS);
        taskMapper.updateTaskExecution(task);
        deliverQuietly(task, run);
    }

    private static boolean hasCriticalSecurityIssue(List<ReviewTopIssue> issues)
    {
        if (issues == null || issues.isEmpty())
        {
            return false;
        }
        for (ReviewTopIssue issue : issues)
        {
            if (ReviewScoringConstants.SEVERITY_CRITICAL.equals(issue.getSeverity())
                && ReviewScoringConstants.DIM_SECURITY.equals(issue.getCategory()))
            {
                return true;
            }
        }
        return false;
    }

    private void persistLlmSuccess(ReviewTask task, ReviewTaskRun run, long beginMs,
                                   ReviewScoreParseResult parseResult, ScopeResolution scope, long durationHint)
    {
        updateStep(task, run, ReviewPipelineConstants.STEP_PERSIST_RESULT);
        ReviewScoreResult result = parseResult.getResult();
        // 范围统计（协议 v1.1）：决策侧计数来自范围决策，归属计数来自解析打标；降级时整体缺省。
        if (scope.scopeStatsBase() != null)
        {
            ReviewScopeStats stats = scope.scopeStatsBase();
            stats.setNewCount(parseResult.getNewCount());
            stats.setExistingCount(parseResult.getExistingCount());
            stats.setOriginUnverifiable(parseResult.getOriginUnverifiableCount());
            result.setScopeStats(stats);
        }
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
        deliverQuietly(task, run);
    }

    /** 对账 → 评论 → IM；任一步失败不影响审查结论。 */
    private void deliverQuietly(ReviewTask task, ReviewTaskRun run)
    {
        ReviewRoundReconcileResult reconcile = reconcileIssuesQuietly(task, run);
        try
        {
            deliveryService.deliverAfterSuccess(task, run, reconcile);
        }
        catch (Exception ex)
        {
            log.warn("审查结果投递调用异常（不影响任务状态）, taskId={}", task.getTaskId(), ex);
        }
        notifyQuietly(task, run, reconcile);
    }

    /** 问题对账失败不影响任务状态；失败时评论/通知退化为不含复核段。 */
    private ReviewRoundReconcileResult reconcileIssuesQuietly(ReviewTask task, ReviewTaskRun run)
    {
        try
        {
            ReviewRoundReconcileResult result = issueService.reconcileAfterSuccess(task, run);
            return result == null ? ReviewRoundReconcileResult.empty() : result;
        }
        catch (Exception ex)
        {
            log.warn("问题台账对账调用异常（不影响任务状态）, taskId={}",
                task == null ? null : task.getTaskId(), ex);
            return ReviewRoundReconcileResult.empty();
        }
    }

    /** IM 通知失败不影响任务状态。 */
    private void notifyQuietly(ReviewTask task, ReviewTaskRun run, ReviewRoundReconcileResult reconcile)
    {
        try
        {
            deliveryService.deliverNotifyAfterTerminal(task, run, reconcile);
        }
        catch (Exception ex)
        {
            log.warn("IM 通知投递调用异常（不影响任务状态）, taskId={}",
                task == null ? null : task.getTaskId(), ex);
        }
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
        notifyQuietly(task, run, ReviewRoundReconcileResult.empty());
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
     * PUSH 任务无 PR 元数据可拉：用建单时写入 prTitle 的最新提交摘要填充 commitMessages。
     */
    private void applyPrMetadata(ReviewTask task, ReviewTaskRun run, ExecutionPlan plan)
    {
        if (ReviewPipelineConstants.EVENT_SOURCE_PUSH.equals(task.getEventSource()))
        {
            run.setCommitMessages(truncate(task.getPrTitle(), ReviewScoringConstants.MAX_COMMIT_MESSAGES_CHARS));
            return;
        }
        GitPullRequestMetadata metadata = adapterRegistry.requireMetadataFetcher(plan.provider()).fetch(
            plan.repository(), plan.access(), task.getPrNumber());
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
            notifyQuietly(task, run, ReviewRoundReconcileResult.empty());
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
        String provider,
        String reviewMode,
        GitRepositoryCoordinates repository,
        GitAccessContext access,
        Long modelId,
        String engineCode,
        String promptContent,
        Map<String, String> modelEnvironment
    )
    {
    }

    /**
     * 范围决策输出。
     * scopeApplied=false 表示决策失败降级为全量 Diff；emptyScope=true 表示全部文件被排除/仅记录类，跳过模型调用。
     * originClassifier 为归属打标分类器（降级时为 null，解析退化为 v1.0 行为）；
     * scopeStatsBase 为范围统计的决策侧计数（归属计数在模型结果解析后回填）。
     */
    private record ScopeResolution(
        boolean scopeApplied,
        String diffForPrompt,
        boolean emptyScope,
        boolean hasFullContent,
        IssueOriginClassifier originClassifier,
        boolean reportExisting,
        ReviewScopeStats scopeStatsBase,
        Map<String, Object> snapshot
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
