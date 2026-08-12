package com.acr.review.service.impl;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import com.acr.common.annotation.DataScope;
import com.acr.common.exception.ServiceException;
import com.acr.common.utils.SecurityUtils;
import com.acr.common.utils.StringUtils;
import com.acr.review.delivery.ReviewCommentBodyRenderer;
import com.acr.review.delivery.ReviewDeliveryConstants;
import com.acr.review.delivery.ReviewDeliveryIntentService;
import com.acr.review.delivery.ReviewDeliveryRuntimeSettings;
import com.acr.review.delivery.ReviewInlineCommentRenderer;
import com.acr.review.delivery.ReviewNotifyMessageRenderer;
import com.acr.review.delivery.ReviewSummaryContent;
import com.acr.review.delivery.ReviewSummaryContentFactory;
import com.acr.review.domain.GitCredential;
import com.acr.review.domain.ReviewChangeKeyGuard;
import com.acr.review.domain.ReviewCommentSyncResult;
import com.acr.review.domain.ReviewDeliveryRecord;
import com.acr.review.domain.ReviewIssue;
import com.acr.review.domain.ReviewPipelineConstants;
import com.acr.review.domain.ReviewProject;
import com.acr.review.domain.ReviewRoundReconcileResult;
import com.acr.review.domain.ReviewTask;
import com.acr.review.domain.ReviewTaskRun;
import com.acr.review.git.GitAccessContext;
import com.acr.review.git.GitAdapterRegistry;
import com.acr.review.git.GitInlineCommentRequest;
import com.acr.review.git.GitInlineCommentUnsupportedException;
import com.acr.review.git.GitPullRequestComment;
import com.acr.review.git.GitPullRequestCommentClient;
import com.acr.review.git.GitPullRequestCommentException;
import com.acr.review.git.GitRepositoryCoordinates;
import com.acr.review.git.GitTokenSanitizer;
import com.acr.review.domain.result.ReviewTopIssue;
import com.acr.review.service.ReviewScoringConstants;
import com.acr.review.mapper.GitCredentialMapper;
import com.acr.review.mapper.ReviewDeliveryRecordMapper;
import com.acr.review.mapper.ReviewIssueMapper;
import com.acr.review.mapper.ReviewProjectMapper;
import com.acr.review.mapper.ReviewTaskMapper;
import com.acr.review.mapper.ReviewTaskRunMapper;
import com.acr.review.notify.NotifyRobotException;
import com.acr.review.service.IGitCredentialService;
import com.acr.review.service.IReviewDeliveryService;
import com.acr.review.service.IReviewIssueService;
import com.acr.review.service.IReviewNotifyChannelService;
import com.acr.review.service.IReviewNotifyChannelService.DecryptedNotifyChannel;
import com.acr.review.service.ReviewIssueDispositionEnricher;
import com.acr.review.service.ReviewProjectAccessService;
import com.acr.review.notify.NotifyRobotClients;
import com.acr.system.service.ISysDeptService;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;

/** 审查结果投递工作节点：外部副作用与审查事实解耦，失败只推进投递状态机。 */
@Service
public class ReviewDeliveryServiceImpl implements IReviewDeliveryService
{
    private static final Logger log = LoggerFactory.getLogger(ReviewDeliveryServiceImpl.class);

    private final ReviewDeliveryRecordMapper deliveryMapper;
    private final ReviewTaskMapper taskMapper;
    private final ReviewTaskRunMapper runMapper;
    private final ReviewProjectMapper projectMapper;
    private final ReviewIssueMapper issueMapper;
    private final IGitCredentialService credentialService;
    private final IReviewNotifyChannelService notifyChannelService;
    private final NotifyRobotClients robotClients;
    private final ReviewSummaryContentFactory contentFactory;
    private final ISysDeptService deptService;
    private final GitAdapterRegistry adapterRegistry;
    private final GitCredentialMapper credentialMapper;
    private final IReviewIssueService issueService;
    private final ReviewDeliveryIntentService deliveryIntentService;
    private final ReviewDeliveryRuntimeSettings deliverySettings;
    private final ReviewProjectAccessService projectAccessService;

    public ReviewDeliveryServiceImpl(ReviewDeliveryRecordMapper deliveryMapper,
                                     ReviewTaskMapper taskMapper,
                                     ReviewTaskRunMapper runMapper,
                                     ReviewProjectMapper projectMapper,
                                     ReviewIssueMapper issueMapper,
                                     IGitCredentialService credentialService,
                                     IReviewNotifyChannelService notifyChannelService,
                                     NotifyRobotClients robotClients,
                                     ReviewSummaryContentFactory contentFactory,
                                     ISysDeptService deptService,
                                     GitAdapterRegistry adapterRegistry,
                                     GitCredentialMapper credentialMapper,
                                     @Lazy IReviewIssueService issueService,
                                     ReviewDeliveryIntentService deliveryIntentService,
                                     ReviewDeliveryRuntimeSettings deliverySettings,
                                     ReviewProjectAccessService projectAccessService)
    {
        this.deliveryMapper = deliveryMapper;
        this.taskMapper = taskMapper;
        this.runMapper = runMapper;
        this.projectMapper = projectMapper;
        this.issueMapper = issueMapper;
        this.credentialService = credentialService;
        this.notifyChannelService = notifyChannelService;
        this.robotClients = robotClients;
        this.contentFactory = contentFactory;
        this.deptService = deptService;
        this.adapterRegistry = adapterRegistry;
        this.credentialMapper = credentialMapper;
        this.issueService = issueService;
        this.deliveryIntentService = deliveryIntentService;
        this.deliverySettings = deliverySettings;
        this.projectAccessService = projectAccessService;
    }

    @Override
    public void deliverAfterSuccess(ReviewTask task, ReviewTaskRun run, ReviewRoundReconcileResult reconcile)
    {
        deliveryIntentService.enqueueSummary(task, run,
            ReviewDeliveryConstants.TRIGGER_TASK_SUCCESS, "system");
        deliveryIntentService.enqueueInlineComments(task, run,
            ReviewDeliveryConstants.TRIGGER_TASK_SUCCESS, "system");
    }

    @Override
    public void deliverNotifyAfterTerminal(ReviewTask task, ReviewTaskRun run, ReviewRoundReconcileResult reconcile)
    {
        deliveryIntentService.enqueueTerminalNotification(task, run, "system");
    }

    @Override
    public void retryDelivery(Long taskId)
    {
        ReviewTask anchor = taskMapper.selectReviewTaskById(taskId);
        if (anchor == null)
        {
            throw new ServiceException("审查任务不存在");
        }
        checkTaskDataScope(anchor);

        ReviewTask latest = taskMapper.selectLatestSuccessByProjectAndPr(anchor.getProjectId(), anchor.getPrNumber());
        if (latest == null)
        {
            throw new ServiceException("该 PR 尚无成功审查结果，无法投递评论");
        }
        ReviewTaskRun run = pickLatestSuccessRun(latest.getTaskId());
        deliveryIntentService.enqueueSummary(latest, run,
            ReviewDeliveryConstants.TRIGGER_MANUAL_RETRY, currentOperator());
    }

    @Override
    public ReviewCommentSyncResult rerenderSummaryComment(Long projectId, Integer prNumber)
    {
        if (projectId == null || prNumber == null || prNumber <= 0)
        {
            return ReviewCommentSyncResult.skipped();
        }
        ReviewTask latest = taskMapper.selectLatestSuccessByProjectAndPr(projectId, prNumber);
        if (latest == null)
        {
            log.info("问题处置后跳过评论重渲染：PR 无 SUCCESS 任务, projectId={}, pr={}", projectId, prNumber);
            return ReviewCommentSyncResult.skipped();
        }
        ReviewTaskRun run = pickLatestSuccessRun(latest.getTaskId());
        ReviewDeliveryRecord record = deliveryIntentService.enqueueSummary(latest, run,
            ReviewDeliveryConstants.TRIGGER_ISSUE_DISPOSITION, currentOperator());
        return ReviewCommentSyncResult.of(ReviewDeliveryConstants.STATUS_PENDING, null,
            record == null ? null : record.getDeliveryId());
    }

    @Override
    public void retryDeliveryById(Long deliveryId)
    {
        ReviewDeliveryRecord record = deliveryMapper.selectDeliveryById(deliveryId);
        if (record == null)
        {
            throw new ServiceException("投递记录不存在");
        }
        projectAccessService.requireOperate(record.getProjectId());

        if (ReviewDeliveryConstants.isSummaryCommentChannel(record.getChannel()))
        {
            retryDelivery(record.getTaskId());
            return;
        }
        if (ReviewDeliveryConstants.isInlineCommentChannel(record.getChannel())
            || ReviewDeliveryConstants.isImChannel(record.getChannel())
            || ReviewDeliveryConstants.CHANNEL_IM_NOTIFICATION.equals(record.getChannel()))
        {
            deliveryIntentService.requeue(deliveryId, currentOperator());
            return;
        }
        throw new ServiceException("不支持的投递渠道：" + record.getChannel());
    }

    @Override
    public void markManualHandled(Long deliveryId)
    {
        ReviewDeliveryRecord record = deliveryMapper.selectDeliveryById(deliveryId);
        if (record == null)
        {
            throw new ServiceException("投递记录不存在");
        }
        projectAccessService.requireOperate(record.getProjectId());
        if (!ReviewDeliveryConstants.STATUS_MANUAL.equals(record.getDeliveryStatus()))
        {
            throw new ServiceException("仅「待人工处置」的投递可标记已处理");
        }
        if (deliveryMapper.markManualHandled(deliveryId, currentOperator(),
            ReviewDeliveryConstants.ERROR_MANUAL_HANDLED, "人工已标记处理，不再自动投递") != 1)
        {
            throw new ServiceException("标记失败，投递状态可能已变更");
        }
    }

    @Override
    public void executeClaimedDelivery(Long deliveryId, String leaseOwner)
    {
        ReviewDeliveryRecord record = deliveryMapper.selectDeliveryById(deliveryId);
        if (record == null || !leaseOwner.equals(record.getLeaseOwner()))
        {
            return;
        }
        ReviewTask task = taskMapper.selectReviewTaskById(record.getTaskId());
        ReviewTaskRun run = record.getRunId() == null
            ? (task == null ? null : pickTerminalRun(task))
            : runMapper.selectReviewTaskRunById(record.getRunId());
        AtomicReference<String> snapshotRef = new AtomicReference<>();
        try
        {
            if (task == null)
            {
                throw new ServiceException("关联审查任务不存在");
            }
            String externalId = null;
            String resolvedChannel = null;
            if (ReviewDeliveryConstants.isSummaryCommentChannel(record.getChannel()))
            {
                if (!ReviewPipelineConstants.TASK_SUCCESS.equals(task.getTaskStatus()))
                {
                    throw new ServiceException("总结评论仅允许投递成功审查结论");
                }
                try
                {
                    externalId = writeComment(task, run, null, snapshotRef);
                }
                catch (StaleHeadDeliveryException stale)
                {
                    String message = stale.getMessage();
                    if (deliveryMapper.failDelivery(deliveryId, leaseOwner, ReviewDeliveryConstants.STATUS_SKIPPED,
                        ReviewDeliveryConstants.ERROR_SKIPPED_STALE, message, null, snapshotRef.get()) != 1)
                    {
                        log.warn("旧 head 总结评论跳过结果因租约过期未能提交, deliveryId={}", deliveryId);
                    }
                    else
                    {
                        log.info("旧 head 总结评论已跳过(SKIPPED_STALE), deliveryId={}, taskId={}",
                            deliveryId, task.getTaskId());
                    }
                    return;
                }
            }
            else if (ReviewDeliveryConstants.isInlineCommentChannel(record.getChannel()))
            {
                if (!ReviewPipelineConstants.TASK_SUCCESS.equals(task.getTaskStatus()))
                {
                    throw new ServiceException("行内评论仅允许投递成功审查结论");
                }
                externalId = writeInlineComment(task, run, record, snapshotRef);
            }
            else if (ReviewDeliveryConstants.isImChannel(record.getChannel())
                || ReviewDeliveryConstants.CHANNEL_IM_NOTIFICATION.equals(record.getChannel()))
            {
                ImDeliveryResult result = sendIm(task, run, record);
                resolvedChannel = result.channelType();
                snapshotRef.set(result.contentSnapshot());
            }
            else
            {
                throw new ServiceException("不支持的投递渠道：" + record.getChannel());
            }
            if (deliveryMapper.completeDelivery(deliveryId, leaseOwner, resolvedChannel,
                externalId, snapshotRef.get()) != 1)
            {
                log.warn("投递外部调用已完成但本地租约围栏拒绝提交，后续重试将依赖渠道幂等, deliveryId={}", deliveryId);
            }
        }
        catch (GitInlineCommentUnsupportedException ex)
        {
            String message = sanitizeFailure(ex, null);
            if (deliveryMapper.failDelivery(deliveryId, leaseOwner, ReviewDeliveryConstants.STATUS_SKIPPED,
                ReviewDeliveryConstants.ERROR_INLINE_UNSUPPORTED, message, null, snapshotRef.get()) != 1)
            {
                log.warn("行内评论不支持结果因租约过期未能提交, deliveryId={}, reason={}", deliveryId, message);
            }
            else
            {
                log.info("平台不支持行内评论，已跳过, deliveryId={}, reason={}", deliveryId, message);
            }
        }
        catch (Exception ex)
        {
            int completedAttempts = record.getAttemptCount() == null ? 0 : record.getAttemptCount();
            boolean configurationError = ex instanceof ServiceException;
            boolean manual = configurationError || completedAttempts + 1 >= deliverySettings.maxAttempts();
            String status = manual ? ReviewDeliveryConstants.STATUS_MANUAL : ReviewDeliveryConstants.STATUS_FAILED;
            Integer delay = manual ? null : deliverySettings.retryDelaySeconds(completedAttempts);
            String errorCode = configurationError
                ? ReviewDeliveryConstants.ERROR_CONFIGURATION : ReviewDeliveryConstants.ERROR_EXTERNAL_CALL;
            String message = sanitizeFailure(ex, null);
            if (deliveryMapper.failDelivery(deliveryId, leaseOwner, status, errorCode, message,
                delay, snapshotRef.get()) != 1)
            {
                log.warn("投递失败结果因租约过期未能提交, deliveryId={}, reason={}", deliveryId, message);
            }
            else if (manual)
            {
                log.warn("投递达到人工处置条件, deliveryId={}, reason={}", deliveryId, message);
            }
            else
            {
                log.warn("投递失败并进入自动退避, deliveryId={}, retryAfter={}s, reason={}",
                    deliveryId, delay, message);
            }
        }
    }

    private ImDeliveryResult sendIm(ReviewTask task, ReviewTaskRun run, ReviewDeliveryRecord record)
    {
        ReviewProject project = projectMapper.selectReviewProjectById(task.getProjectId());
        if (project == null || !"Y".equals(project.getNotifyEnabled()) || project.getNotifyChannelId() == null)
        {
            throw new ServiceException("项目未启用通知或未绑定渠道");
        }
        boolean success = ReviewPipelineConstants.TASK_SUCCESS.equals(task.getTaskStatus());
        boolean failed = ReviewPipelineConstants.TASK_FAILED.equals(task.getTaskStatus());
        if (!success && !failed)
        {
            throw new ServiceException("关联任务尚未结束，无法投递通知");
        }
        if (failed && !"Y".equals(project.getNotifyOnFailure()))
        {
            throw new ServiceException("项目已关闭失败通知");
        }
        DecryptedNotifyChannel channel = notifyChannelService.getDecryptedChannel(project.getNotifyChannelId(), true);
        if (!ReviewDeliveryConstants.isSupportedNotifyChannelType(channel.channelType()))
        {
            throw new ServiceException("通知渠道类型不受支持：" + channel.channelType());
        }
        if (!record.getChannel().equals(channel.channelType()))
        {
            log.info("投递记录渠道 {} 与项目当前渠道 {} 不一致，按当前配置投递, deliveryId={}",
                record.getChannel(), channel.channelType(), record.getDeliveryId());
        }
        ReviewSummaryContent content = contentFactory.build(task, run, project,
            resolveRecheckingForRender(null, task));
        String title = success
            ? "AI Code Review · " + content.getConclusionLabel()
            : "AI Code Review · 执行失败";
        String body = success
            ? ReviewNotifyMessageRenderer.renderSuccess(content)
            : ReviewNotifyMessageRenderer.renderFailed(content);
        String snapshot = buildContentSnapshot(ReviewDeliveryConstants.SNAPSHOT_KIND_IM,
            channel.channelType(), title, body);
        robotClients.require(channel.channelType()).send(
            channel.webhookUrl(), channel.secret(), title, body);
        return new ImDeliveryResult(channel.channelType(), snapshot);
    }

    @Override
    public ReviewDeliveryRecord selectSummaryDelivery(Long projectId, Integer prNumber)
    {
        if (projectId == null || prNumber == null)
        {
            return null;
        }
        ReviewProject project = projectMapper.selectReviewProjectById(projectId);
        if (project == null)
        {
            return null;
        }
        return deliveryMapper.selectByProjectAndPr(projectId, prNumber,
            ReviewDeliveryConstants.channelForProvider(project.getProvider()));
    }

    @Override
    public ReviewDeliveryRecord selectLatestImDelivery(Long taskId)
    {
        ReviewTask task = taskMapper.selectReviewTaskById(taskId);
        if (task == null)
        {
            throw new ServiceException("审查任务不存在");
        }
        projectAccessService.requireView(task.getProjectId());
        return deliveryMapper.selectLatestImByTaskId(taskId);
    }

    @Override
    public ReviewDeliveryRecord selectInlineDeliveryByIssueId(Long issueId)
    {
        if (issueId == null)
        {
            return null;
        }
        ReviewIssue issue = issueMapper.selectIssueById(issueId);
        if (issue == null)
        {
            throw new ServiceException("问题不存在");
        }
        projectAccessService.requireView(issue.getProjectId());
        return deliveryMapper.selectByIssueId(issueId);
    }

    @Override
    public List<ReviewDeliveryRecord> selectInlineDeliveriesByTaskId(Long taskId)
    {
        ReviewTask task = taskMapper.selectReviewTaskById(taskId);
        if (task == null)
        {
            throw new ServiceException("审查任务不存在");
        }
        projectAccessService.requireView(task.getProjectId());
        List<ReviewDeliveryRecord> list = deliveryMapper.selectInlineByTaskId(taskId);
        return list == null ? Collections.emptyList() : list;
    }

    @Override
    public ReviewDeliveryRecord selectDeliveryById(Long deliveryId)
    {
        ReviewDeliveryRecord record = deliveryMapper.selectDeliveryById(deliveryId);
        if (record == null)
        {
            throw new ServiceException("投递记录不存在");
        }
        projectAccessService.requireView(record.getProjectId());
        return record;
    }

    @Override
    public Map<String, Object> selectDeliveryContent(Long deliveryId)
    {
        // 与列表同数据范围：先按投递记录所属项目做部门校验
        ReviewDeliveryRecord record = selectDeliveryById(deliveryId);
        String snapshot = deliveryMapper.selectContentSnapshotById(deliveryId);
        if (StringUtils.isEmpty(snapshot))
        {
            return Collections.emptyMap();
        }
        try
        {
            JSONObject parsed = JSON.parseObject(snapshot);
            if (parsed == null)
            {
                return Collections.emptyMap();
            }
            Map<String, Object> content = new HashMap<>(parsed);
            // 结构化归属字段：历史快照正文无归属行时，详情弹窗仍可展示系统/项目
            content.put("businessSystemName", record == null ? null : record.getBusinessSystemName());
            content.put("projectName", record == null ? null : record.getProjectName());
            return content;
        }
        catch (Exception ex)
        {
            log.warn("投递正文快照解析失败, deliveryId={}", deliveryId);
            return Collections.emptyMap();
        }
    }

    @Override
    @DataScope(deptAlias = "d", userAlias = "owner", permission = "review:delivery:list")
    public List<ReviewDeliveryRecord> selectDeliveryList(ReviewDeliveryRecord query)
    {
        projectAccessService.applyQueryScope(query);
        return deliveryMapper.selectDeliveryList(query);
    }

    @Override
    @DataScope(deptAlias = "d", userAlias = "owner", permission = "review:delivery:list")
    public int countDeliveryList(ReviewDeliveryRecord query)
    {
        projectAccessService.applyQueryScope(query);
        return deliveryMapper.countDeliveryList(query);
    }

    private String writeComment(ReviewTask task, ReviewTaskRun run, ReviewRoundReconcileResult reconcile,
                                AtomicReference<String> snapshotRef)
    {
        requireLatestForSummaryComment(task);
        ReviewProject project = projectMapper.selectReviewProjectById(task.getProjectId());
        if (project == null || !"0".equals(project.getStatus()))
        {
            throw new GitPullRequestCommentException("项目不存在或已停用，无法投递评论");
        }
        String provider = project.getProvider();

        GitCredential credential = credentialMapper.selectGitCredentialById(project.getCredentialId());
        if (credential == null || !"0".equals(credential.getStatus()))
        {
            throw new GitPullRequestCommentException("项目绑定的 Git 凭据不存在或已停用");
        }

        String token;
        try
        {
            token = credentialService.getPlainToken(project.getCredentialId(), true);
        }
        catch (ServiceException ex)
        {
            throw new GitPullRequestCommentException("Git 凭据不可用：" + ex.getMessage());
        }

        GitAccessContext access = GitAccessContext.of(token,
            GitCredentialServiceImpl.resolveServerUrl(provider, credential.getServerUrl()));

        String fullPath = StringUtils.isNotEmpty(project.getRepositoryFullPath())
            ? project.getRepositoryFullPath()
            : project.getRepositoryOwner() + "/" + project.getRepositoryName();
        GitRepositoryCoordinates repository = new GitRepositoryCoordinates(
            project.getRepositoryOwner(), project.getRepositoryName(), fullPath, project.getRepositoryUrl());
        ReviewSummaryContent content = contentFactory.build(task, run, project,
            resolveRecheckingForRender(reconcile, task));
        enrichDisposition(content, task);
        content = withInlinePreview(content, project);
        String body = ReviewCommentBodyRenderer.render(content);
        String channel = ReviewDeliveryConstants.channelForProvider(provider);
        if (snapshotRef != null)
        {
            snapshotRef.set(buildContentSnapshot(
                ReviewDeliveryConstants.SNAPSHOT_KIND_SUMMARY_COMMENT, channel, "", body));
        }

        // 外部写前再围栏一次，缩小建单并发下的 TOCTOU 窗口
        requireLatestForSummaryComment(task);
        GitPullRequestCommentClient commentClient = adapterRegistry.requireCommentClient(provider);
        Optional<GitPullRequestComment> existing = commentClient.findCommentWithMarker(
            repository, access, task.getPrNumber(), ReviewDeliveryConstants.COMMENT_MARKER);
        if (existing.isPresent())
        {
            return commentClient.updateIssueComment(
                repository, access, existing.get().id(), body).id();
        }
        return commentClient.createIssueComment(
            repository, access, task.getPrNumber(), body).id();
    }

    private void requireLatestForSummaryComment(ReviewTask task)
    {
        if (!ReviewChangeKeyGuard.isLatestForChangeKey(taskMapper, task))
        {
            throw new StaleHeadDeliveryException(
                "同一变更已有更新任务，本任务仅保留历史运行结果，跳过总结评论覆盖");
        }
    }

    /** 总结评论 head 围栏：旧任务不得覆盖当前 marker。 */
    static final class StaleHeadDeliveryException extends RuntimeException
    {
        StaleHeadDeliveryException(String message)
        {
            super(message);
        }
    }

    private String writeInlineComment(ReviewTask task, ReviewTaskRun run, ReviewDeliveryRecord record,
                                      AtomicReference<String> snapshotRef)
    {
        if (record.getIssueId() == null)
        {
            throw new ServiceException("行内评论缺少关联问题 ID");
        }
        ReviewProject project = projectMapper.selectReviewProjectById(task.getProjectId());
        if (project == null || !"0".equals(project.getStatus()))
        {
            throw new GitPullRequestCommentException("项目不存在或已停用，无法投递行内评论");
        }
        String provider = project.getProvider();
        GitCredential credential = credentialMapper.selectGitCredentialById(project.getCredentialId());
        if (credential == null || !"0".equals(credential.getStatus()))
        {
            throw new GitPullRequestCommentException("项目绑定的 Git 凭据不存在或已停用");
        }
        String token;
        try
        {
            token = credentialService.getPlainToken(project.getCredentialId(), true);
        }
        catch (ServiceException ex)
        {
            throw new GitPullRequestCommentException("Git 凭据不可用：" + ex.getMessage());
        }
        GitAccessContext access = GitAccessContext.of(token,
            GitCredentialServiceImpl.resolveServerUrl(provider, credential.getServerUrl()));
        String fullPath = StringUtils.isNotEmpty(project.getRepositoryFullPath())
            ? project.getRepositoryFullPath()
            : project.getRepositoryOwner() + "/" + project.getRepositoryName();
        GitRepositoryCoordinates repository = new GitRepositoryCoordinates(
            project.getRepositoryOwner(), project.getRepositoryName(), fullPath, project.getRepositoryUrl());

        ReviewTopIssue issue = findIssueForInline(run, record.getIssueId());
        if (issue == null)
        {
            throw new ServiceException("行内评论关联问题不在本轮结果中，issueId=" + record.getIssueId());
        }
        Integer endLine = issue.getEndLine() != null ? issue.getEndLine() : issue.getStartLine();
        if (StringUtils.isEmpty(issue.getFilePath()) || endLine == null)
        {
            throw new ServiceException("行内评论缺少文件路径或行号，issueId=" + record.getIssueId());
        }
        String marker = ReviewDeliveryConstants.inlineCommentMarker(record.getIssueId());
        String body = ReviewInlineCommentRenderer.render(issue, task.getTaskId());
        String channel = ReviewDeliveryConstants.inlineChannelForProvider(provider);
        if (snapshotRef != null)
        {
            snapshotRef.set(buildContentSnapshot(
                ReviewDeliveryConstants.SNAPSHOT_KIND_INLINE_COMMENT, channel, "", body));
        }
        GitPullRequestCommentClient commentClient = adapterRegistry.requireCommentClient(provider);
        Optional<GitPullRequestComment> existing = commentClient.findInlineCommentWithMarker(
            repository, access, task.getPrNumber(), marker);
        if (existing.isPresent())
        {
            return existing.get().id();
        }
        String headSha = StringUtils.defaultIfEmpty(task.getHeadSha(),
            run == null ? null : run.getSnapshotHeadSha());
        GitInlineCommentRequest request = new GitInlineCommentRequest(
            issue.getFilePath(), issue.getStartLine(), endLine, body, headSha);
        return commentClient.createInlineComment(repository, access, task.getPrNumber(), request).id();
    }

    private ReviewTopIssue findIssueForInline(ReviewTaskRun run, Long issueId)
    {
        for (ReviewTopIssue issue : ReviewSummaryContentFactory.resolveTopIssues(run))
        {
            if (issue != null && issueId.equals(issue.getIssueId()))
            {
                return issue;
            }
        }
        ReviewIssue ledger = issueMapper.selectIssueById(issueId);
        if (ledger == null)
        {
            return null;
        }
        ReviewTopIssue top = new ReviewTopIssue();
        top.setIssueId(ledger.getIssueId());
        top.setSeverity(ledger.getSeverity());
        top.setCategory(ledger.getCategory());
        top.setTitle(ledger.getTitle());
        top.setDescription(ledger.getDescription());
        top.setFilePath(ledger.getFilePath());
        top.setStartLine(ledger.getStartLine());
        top.setEndLine(ledger.getEndLine());
        top.setSuggestion(ledger.getSuggestion());
        return top;
    }

    /** 按项目门槛计算行内预告计数，写入总结评论范围段。 */
    static ReviewSummaryContent withInlinePreview(ReviewSummaryContent content, ReviewProject project)
    {
        if (content == null || project == null || !"0".equals(project.getInlineCommentEnabled()))
        {
            return content;
        }
        java.util.Set<String> allowed = ReviewInlineCommentRenderer.parseSeverities(project.getInlineSeverities());
        int total = 0;
        int critical = 0;
        int high = 0;
        for (ReviewTopIssue issue : content.getTopIssues())
        {
            if (issue == null || issue.getIssueId() == null)
            {
                continue;
            }
            if (!ReviewInlineCommentRenderer.severityAllowed(issue.getSeverity(), allowed))
            {
                continue;
            }
            Integer end = issue.getEndLine() != null ? issue.getEndLine() : issue.getStartLine();
            if (StringUtils.isEmpty(issue.getFilePath()) || end == null)
            {
                continue;
            }
            total++;
            String sev = issue.getSeverity() == null ? "" : issue.getSeverity().trim().toUpperCase();
            if (ReviewScoringConstants.SEVERITY_CRITICAL.equals(sev))
            {
                critical++;
            }
            else if (ReviewScoringConstants.SEVERITY_HIGH.equals(sev))
            {
                high++;
            }
        }
        if (total <= 0)
        {
            return content;
        }
        return ReviewSummaryContent.builder()
            .taskStatus(content.getTaskStatus())
            .taskId(content.getTaskId())
            .runId(content.getRunId())
            .conclusion(content.getConclusion())
            .conclusionLabel(content.getConclusionLabel())
            .totalScore(content.getTotalScore())
            .headShaShort(content.getHeadShaShort())
            .prNumber(content.getPrNumber())
            .prTitle(content.getPrTitle())
            .prAuthor(content.getPrAuthor())
            .repositoryOwner(content.getRepositoryOwner())
            .repositoryName(content.getRepositoryName())
            .projectName(content.getProjectName())
            .businessSystemName(content.getBusinessSystemName())
            .sourceBranch(content.getSourceBranch())
            .targetBranch(content.getTargetBranch())
            .changedFiles(content.getChangedFiles())
            .additions(content.getAdditions())
            .deletions(content.getDeletions())
            .topIssues(content.getTopIssues())
            .recheckingTitles(content.getRecheckingTitles())
            .scopeStats(content.getScopeStats())
            .prUrl(content.getPrUrl())
            .detailUrl(content.getDetailUrl())
            .failureType(content.getFailureType())
            .failureTypeLabel(content.getFailureTypeLabel())
            .commitMessage(content.getCommitMessage())
            .summaryText(content.getSummaryText())
            .reviewTime(content.getReviewTime())
            .eventSource(content.getEventSource())
            .inlineCommentCount(total)
            .inlineCriticalCount(critical)
            .inlineHighCount(high)
            .build();
    }

    private void enrichDisposition(ReviewSummaryContent content, ReviewTask task)
    {
        if (content == null || content.getTopIssues().isEmpty() || task == null
            || task.getProjectId() == null || task.getPrNumber() == null)
        {
            return;
        }
        String refBranch = ReviewIssueServiceImpl.resolveRefBranch(task);
        List<ReviewIssue> issues = issueMapper.selectByProjectAndPr(
            task.getProjectId(), task.getPrNumber(), refBranch);
        if (issues == null || issues.isEmpty())
        {
            return;
        }
        Map<String, ReviewIssue> byFp = new HashMap<>();
        for (ReviewIssue issue : issues)
        {
            byFp.put(issue.getFingerprint(), issue);
        }
        ReviewIssueDispositionEnricher.enrich(content.getTopIssues(), byFp);
    }

    /**
     * 首投递使用本轮对账结果；重渲染/人工重试传 null 时从台账派生 RECHECKING 标题。
     * 派生失败静默降级为空，不阻塞评论/通知主体。
     */
    private ReviewRoundReconcileResult resolveRecheckingForRender(ReviewRoundReconcileResult reconcile,
                                                                  ReviewTask task)
    {
        if (reconcile != null)
        {
            return reconcile;
        }
        if (task == null || task.getProjectId() == null || task.getPrNumber() == null)
        {
            return ReviewRoundReconcileResult.empty();
        }
        try
        {
            return ReviewRoundReconcileResult.forRecheckingTitles(
                issueService.listRecheckingTitles(task.getProjectId(), task.getPrNumber(),
                    ReviewIssueServiceImpl.resolveRefBranch(task)));
        }
        catch (Exception ex)
        {
            log.warn("派生待复核标题失败（评论/通知降级为空）, projectId={}, pr={}",
                task.getProjectId(), task.getPrNumber(), ex);
            return ReviewRoundReconcileResult.empty();
        }
    }

    static String buildContentSnapshot(String kind, String channelType, String title, String body)
    {
        JSONObject json = new JSONObject();
        json.put("kind", kind);
        json.put("channelType", channelType == null ? "" : channelType);
        json.put("title", title == null ? "" : title);
        json.put("body", body == null ? "" : body);
        return json.toJSONString();
    }

    private ReviewTaskRun pickLatestSuccessRun(Long taskId)
    {
        List<ReviewTaskRun> runs = runMapper.selectRunsByTaskId(taskId);
        if (runs == null || runs.isEmpty())
        {
            return null;
        }
        ReviewTaskRun best = null;
        for (ReviewTaskRun run : runs)
        {
            if (!ReviewPipelineConstants.RUN_SUCCESS.equals(run.getRunStatus()))
            {
                continue;
            }
            if (best == null || (run.getAttemptNo() != null
                && (best.getAttemptNo() == null || run.getAttemptNo() > best.getAttemptNo())))
            {
                best = run;
            }
        }
        return best != null ? best : runs.get(0);
    }

    private ReviewTaskRun pickTerminalRun(ReviewTask task)
    {
        if (ReviewPipelineConstants.TASK_SUCCESS.equals(task.getTaskStatus()))
        {
            return pickLatestSuccessRun(task.getTaskId());
        }
        List<ReviewTaskRun> runs = runMapper.selectRunsByTaskId(task.getTaskId());
        if (runs == null || runs.isEmpty())
        {
            return null;
        }
        ReviewTaskRun best = null;
        for (ReviewTaskRun run : runs)
        {
            if (!ReviewPipelineConstants.RUN_FAILED.equals(run.getRunStatus()))
            {
                continue;
            }
            if (best == null || (run.getAttemptNo() != null
                && (best.getAttemptNo() == null || run.getAttemptNo() > best.getAttemptNo())))
            {
                best = run;
            }
        }
        return best != null ? best : runs.get(0);
    }

    private void checkTaskDataScope(ReviewTask task)
    {
        projectAccessService.requireOperate(task.getProjectId());
    }

    private static String currentOperator()
    {
        try
        {
            return SecurityUtils.getUsername();
        }
        catch (RuntimeException ex)
        {
            return "system";
        }
    }

    private static String sanitizeFailure(Exception ex, String token)
    {
        if (ex instanceof GitPullRequestCommentException commentEx)
        {
            return truncate(commentEx.getMessage());
        }
        if (ex instanceof NotifyRobotException notifyEx)
        {
            return truncate(notifyEx.getMessage());
        }
        if (ex instanceof ServiceException serviceEx)
        {
            return truncate(serviceEx.getMessage());
        }
        return truncate(GitTokenSanitizer.sanitize(
            StringUtils.defaultIfEmpty(ex.getMessage(), ex.getClass().getSimpleName()), token));
    }

    private static String truncate(String message)
    {
        if (message == null)
        {
            return null;
        }
        return message.length() > ReviewDeliveryConstants.MAX_FAILURE_MESSAGE_CHARS
            ? message.substring(0, ReviewDeliveryConstants.MAX_FAILURE_MESSAGE_CHARS)
            : message;
    }

    private record ImDeliveryResult(String channelType, String contentSnapshot)
    {
    }
}
