package com.acr.review.service.impl;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import com.acr.common.annotation.DataScope;
import com.acr.common.exception.ServiceException;
import com.acr.common.utils.StringUtils;
import com.acr.review.delivery.ReviewCommentBodyRenderer;
import com.acr.review.delivery.ReviewDeliveryConstants;
import com.acr.review.delivery.ReviewNotifyMessageRenderer;
import com.acr.review.delivery.ReviewSummaryContent;
import com.acr.review.delivery.ReviewSummaryContentFactory;
import com.acr.review.domain.GitCredential;
import com.acr.review.domain.ReviewCommentSyncResult;
import com.acr.review.domain.ReviewDeliveryRecord;
import com.acr.review.domain.ReviewIssue;
import com.acr.review.domain.ReviewPipelineConstants;
import com.acr.review.domain.ReviewProject;
import com.acr.review.domain.ReviewTask;
import com.acr.review.domain.ReviewTaskRun;
import com.acr.review.git.GitAccessContext;
import com.acr.review.git.GitAdapterRegistry;
import com.acr.review.git.GitPullRequestComment;
import com.acr.review.git.GitPullRequestCommentClient;
import com.acr.review.git.GitPullRequestCommentException;
import com.acr.review.git.GitRepositoryCoordinates;
import com.acr.review.git.github.GitHubPullRequestCommentClient;
import com.acr.review.mapper.GitCredentialMapper;
import com.acr.review.mapper.ReviewDeliveryRecordMapper;
import com.acr.review.mapper.ReviewIssueMapper;
import com.acr.review.mapper.ReviewProjectMapper;
import com.acr.review.mapper.ReviewTaskMapper;
import com.acr.review.mapper.ReviewTaskRunMapper;
import com.acr.review.notify.NotifyRobotException;
import com.acr.review.service.IGitCredentialService;
import com.acr.review.service.IReviewDeliveryService;
import com.acr.review.service.IReviewNotifyChannelService;
import com.acr.review.service.IReviewNotifyChannelService.DecryptedNotifyChannel;
import com.acr.review.service.ReviewIssueDispositionEnricher;
import com.acr.review.notify.NotifyRobotClients;
import com.acr.system.service.ISysDeptService;

/** 审查结果投递：总结评论 + IM 群机器人；失败不污染审查结论。 */
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
                                     GitCredentialMapper credentialMapper)
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
    }

    @Override
    public void deliverAfterSuccess(ReviewTask task, ReviewTaskRun run)
    {
        if (task == null || !ReviewPipelineConstants.TASK_SUCCESS.equals(task.getTaskStatus()))
        {
            return;
        }
        try
        {
            String externalId = writeComment(task, run);
            upsertGithubResult(task, run, ReviewDeliveryConstants.STATUS_SUCCESS, externalId, null,
                ReviewDeliveryConstants.TRIGGER_TASK_SUCCESS);
        }
        catch (Exception ex)
        {
            String message = sanitizeFailure(ex, null);
            log.warn("审查结果投递失败（不影响任务状态）, taskId={}, reason={}", task.getTaskId(), message);
            try
            {
                upsertGithubResult(task, run, ReviewDeliveryConstants.STATUS_FAILED, null, message,
                    ReviewDeliveryConstants.TRIGGER_TASK_SUCCESS);
            }
            catch (Exception persistEx)
            {
                log.warn("投递失败记录落库异常, taskId={}", task.getTaskId(), persistEx);
            }
        }
    }

    @Override
    public void deliverNotifyAfterTerminal(ReviewTask task, ReviewTaskRun run)
    {
        if (task == null || task.getTaskId() == null)
        {
            return;
        }
        String status = task.getTaskStatus();
        boolean success = ReviewPipelineConstants.TASK_SUCCESS.equals(status);
        boolean failed = ReviewPipelineConstants.TASK_FAILED.equals(status);
        if (!success && !failed)
        {
            return;
        }
        try
        {
            ReviewProject project = projectMapper.selectReviewProjectById(task.getProjectId());
            if (project == null || !"Y".equals(project.getNotifyEnabled()))
            {
                return;
            }
            if (failed && !"Y".equals(project.getNotifyOnFailure()))
            {
                return;
            }
            if (project.getNotifyChannelId() == null)
            {
                log.warn("项目已启用通知但未绑定渠道，跳过 IM 投递, taskId={}, projectId={}",
                    task.getTaskId(), project.getProjectId());
                return;
            }
            DecryptedNotifyChannel channel;
            try
            {
                channel = notifyChannelService.getDecryptedChannel(project.getNotifyChannelId(), true);
            }
            catch (ServiceException ex)
            {
                // 渠道停用/删除/解密失败：仍按真实渠道类型落幂等键，避免 UNKNOWN_IM 脏值
                upsertImResult(task, run, resolveChannelType(project), ReviewDeliveryConstants.STATUS_FAILED,
                    StringUtils.defaultIfEmpty(ex.getMessage(), "通知渠道不可用"),
                    ReviewDeliveryConstants.TRIGGER_TASK_SUCCESS);
                return;
            }
            ReviewSummaryContent content = contentFactory.build(task, run, project);
            String title = success
                ? "AI Code Review · " + content.getConclusionLabel()
                : "AI Code Review · 执行失败";
            String body = success
                ? ReviewNotifyMessageRenderer.renderSuccess(content)
                : ReviewNotifyMessageRenderer.renderFailed(content);
            robotClients.require(channel.channelType()).send(
                channel.webhookUrl(), channel.secret(), title, body);
            upsertImResult(task, run, channel.channelType(), ReviewDeliveryConstants.STATUS_SUCCESS, null,
                ReviewDeliveryConstants.TRIGGER_TASK_SUCCESS);
        }
        catch (Exception ex)
        {
            String message = sanitizeFailure(ex, null);
            log.warn("IM 通知投递失败（不影响任务状态）, taskId={}, reason={}", task.getTaskId(), message);
            try
            {
                ReviewProject project = projectMapper.selectReviewProjectById(task.getProjectId());
                String channelType = resolveChannelType(project);
                upsertImResult(task, run, channelType, ReviewDeliveryConstants.STATUS_FAILED, message,
                    ReviewDeliveryConstants.TRIGGER_TASK_SUCCESS);
            }
            catch (Exception persistEx)
            {
                log.warn("IM 投递失败记录落库异常, taskId={}", task.getTaskId(), persistEx);
            }
        }
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
        try
        {
            String externalId = writeComment(latest, run);
            upsertGithubResult(latest, run, ReviewDeliveryConstants.STATUS_SUCCESS, externalId, null,
                ReviewDeliveryConstants.TRIGGER_MANUAL_RETRY);
        }
        catch (Exception ex)
        {
            String message = sanitizeFailure(ex, null);
            upsertGithubResult(latest, run, ReviewDeliveryConstants.STATUS_FAILED, null, message,
                ReviewDeliveryConstants.TRIGGER_MANUAL_RETRY);
            throw new ServiceException("投递重试失败：" + message);
        }
    }

    @Override
    public ReviewCommentSyncResult rerenderSummaryComment(Long projectId, Integer prNumber)
    {
        if (projectId == null || prNumber == null)
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
        try
        {
            String externalId = writeComment(latest, run);
            ReviewDeliveryRecord record = upsertGithubResult(latest, run,
                ReviewDeliveryConstants.STATUS_SUCCESS, externalId, null,
                ReviewDeliveryConstants.TRIGGER_ISSUE_DISPOSITION);
            return ReviewCommentSyncResult.of(ReviewDeliveryConstants.STATUS_SUCCESS, null,
                record == null ? null : record.getDeliveryId());
        }
        catch (Exception ex)
        {
            String message = sanitizeFailure(ex, null);
            log.warn("问题处置后评论重渲染失败（不回滚处置）, projectId={}, pr={}, reason={}",
                projectId, prNumber, message);
            Long deliveryId = null;
            try
            {
                ReviewDeliveryRecord record = upsertGithubResult(latest, run,
                    ReviewDeliveryConstants.STATUS_FAILED, null, message,
                    ReviewDeliveryConstants.TRIGGER_ISSUE_DISPOSITION);
                deliveryId = record == null ? null : record.getDeliveryId();
            }
            catch (Exception persistEx)
            {
                log.warn("评论重渲染失败记录落库异常, projectId={}, pr={}", projectId, prNumber, persistEx);
            }
            return ReviewCommentSyncResult.of(ReviewDeliveryConstants.STATUS_FAILED, message, deliveryId);
        }
    }

    @Override
    public void retryDeliveryById(Long deliveryId)
    {
        ReviewDeliveryRecord record = deliveryMapper.selectDeliveryById(deliveryId);
        if (record == null)
        {
            throw new ServiceException("投递记录不存在");
        }
        ReviewProject project = projectMapper.selectReviewProjectById(record.getProjectId());
        if (project == null)
        {
            throw new ServiceException("投递记录所属项目不存在");
        }
        deptService.checkDeptDataScope(project.getDeptId());

        if (ReviewDeliveryConstants.isSummaryCommentChannel(record.getChannel()))
        {
            retryDelivery(record.getTaskId());
            return;
        }
        if (!ReviewDeliveryConstants.isImChannel(record.getChannel()))
        {
            throw new ServiceException("不支持的投递渠道：" + record.getChannel());
        }

        ReviewTask task = taskMapper.selectReviewTaskById(record.getTaskId());
        if (task == null)
        {
            throw new ServiceException("关联审查任务不存在");
        }
        ReviewTaskRun run = pickTerminalRun(task);
        try
        {
            if (!"Y".equals(project.getNotifyEnabled()) || project.getNotifyChannelId() == null)
            {
                throw new ServiceException("项目未启用通知或未绑定渠道");
            }
            DecryptedNotifyChannel channel = notifyChannelService.getDecryptedChannel(
                project.getNotifyChannelId(), true);
            if (!record.getChannel().equals(channel.channelType()))
            {
                // 仍按原记录渠道类型写入幂等键；发送用当前绑定渠道（项目实时配置）
                log.info("投递记录渠道 {} 与项目当前渠道 {} 不一致，按当前渠道补发, deliveryId={}",
                    record.getChannel(), channel.channelType(), deliveryId);
            }
            ReviewSummaryContent content = contentFactory.build(task, run, project);
            boolean success = ReviewPipelineConstants.TASK_SUCCESS.equals(task.getTaskStatus());
            boolean failed = ReviewPipelineConstants.TASK_FAILED.equals(task.getTaskStatus());
            if (!success && !failed)
            {
                throw new ServiceException("关联任务尚未结束，无法补发通知");
            }
            if (failed && !"Y".equals(project.getNotifyOnFailure()))
            {
                throw new ServiceException("项目已关闭失败通知");
            }
            String title = success
                ? "AI Code Review · " + content.getConclusionLabel()
                : "AI Code Review · 执行失败";
            String body = success
                ? ReviewNotifyMessageRenderer.renderSuccess(content)
                : ReviewNotifyMessageRenderer.renderFailed(content);
            robotClients.require(channel.channelType()).send(
                channel.webhookUrl(), channel.secret(), title, body);
            upsertImResult(task, run, record.getChannel(), ReviewDeliveryConstants.STATUS_SUCCESS, null,
                ReviewDeliveryConstants.TRIGGER_MANUAL_RETRY);
        }
        catch (Exception ex)
        {
            String message = sanitizeFailure(ex, null);
            upsertImResult(task, run, record.getChannel(), ReviewDeliveryConstants.STATUS_FAILED, message,
                ReviewDeliveryConstants.TRIGGER_MANUAL_RETRY);
            throw new ServiceException("投递补发失败：" + message);
        }
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
        ReviewProject project = projectMapper.selectReviewProjectById(task.getProjectId());
        if (project == null)
        {
            throw new ServiceException("审查任务所属项目不存在");
        }
        deptService.checkDeptDataScope(project.getDeptId());
        return deliveryMapper.selectLatestImByTaskId(taskId);
    }

    @Override
    public ReviewDeliveryRecord selectDeliveryById(Long deliveryId)
    {
        ReviewDeliveryRecord record = deliveryMapper.selectDeliveryById(deliveryId);
        if (record == null)
        {
            throw new ServiceException("投递记录不存在");
        }
        ReviewProject project = projectMapper.selectReviewProjectById(record.getProjectId());
        if (project == null)
        {
            throw new ServiceException("投递记录所属项目不存在");
        }
        deptService.checkDeptDataScope(project.getDeptId());
        return record;
    }

    @Override
    @DataScope(deptAlias = "d", userAlias = "owner", permission = "review:delivery:list")
    public List<ReviewDeliveryRecord> selectDeliveryList(ReviewDeliveryRecord query)
    {
        return deliveryMapper.selectDeliveryList(query);
    }

    @Override
    @DataScope(deptAlias = "d", userAlias = "owner", permission = "review:delivery:list")
    public int countDeliveryList(ReviewDeliveryRecord query)
    {
        return deliveryMapper.countDeliveryList(query);
    }

    private String writeComment(ReviewTask task, ReviewTaskRun run)
    {
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
        ReviewSummaryContent content = contentFactory.build(task, run, project);
        enrichDisposition(content, task.getProjectId(), task.getPrNumber());
        String body = ReviewCommentBodyRenderer.render(content);

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

    private void enrichDisposition(ReviewSummaryContent content, Long projectId, Integer prNumber)
    {
        if (content == null || content.getTopIssues().isEmpty() || projectId == null || prNumber == null)
        {
            return;
        }
        List<ReviewIssue> issues = issueMapper.selectByProjectAndPr(projectId, prNumber);
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

    private ReviewDeliveryRecord upsertGithubResult(ReviewTask task, ReviewTaskRun run, String status,
                                                    String externalId, String failureMessage, String triggerSource)
    {
        ReviewProject project = projectMapper.selectReviewProjectById(task.getProjectId());
        String provider = project != null && StringUtils.isNotEmpty(project.getProvider())
            ? project.getProvider() : ReviewDeliveryConstants.PROVIDER_GITHUB;
        String key = ReviewDeliveryConstants.idempotencyKey(provider, task.getProjectId(), task.getPrNumber());
        return upsertResult(task, run, provider,
            ReviewDeliveryConstants.channelForProvider(provider), key, externalId, status, failureMessage,
            triggerSource);
    }

    private ReviewDeliveryRecord upsertImResult(ReviewTask task, ReviewTaskRun run, String channelType,
                                                String status, String failureMessage, String triggerSource)
    {
        String type = StringUtils.isNotEmpty(channelType) ? channelType : "UNKNOWN_IM";
        String key = ReviewDeliveryConstants.imIdempotencyKey(type, task.getTaskId());
        String provider = ReviewDeliveryConstants.PROVIDER_GITHUB;
        ReviewProject project = projectMapper.selectReviewProjectById(task.getProjectId());
        if (project != null && StringUtils.isNotEmpty(project.getProvider()))
        {
            provider = project.getProvider();
        }
        return upsertResult(task, run, provider, type, key, null, status, failureMessage, triggerSource);
    }

    private ReviewDeliveryRecord upsertResult(ReviewTask task, ReviewTaskRun run, String provider, String channel,
                                              String key, String externalId, String status, String failureMessage,
                                              String triggerSource)
    {
        Date now = new Date();
        ReviewDeliveryRecord existing = deliveryMapper.selectByIdempotencyKey(key);

        ReviewDeliveryRecord record = new ReviewDeliveryRecord();
        record.setTaskId(task.getTaskId());
        record.setRunId(run == null ? null : run.getRunId());
        record.setProjectId(task.getProjectId());
        record.setProvider(provider);
        record.setChannel(channel);
        record.setPrNumber(task.getPrNumber());
        record.setIdempotencyKey(key);
        record.setExternalId(externalId != null ? externalId
            : (existing == null ? null : existing.getExternalId()));
        record.setDeliveryStatus(status);
        record.setFailureMessage(truncate(failureMessage));
        record.setLastAttemptTime(now);
        record.setTriggerSource(triggerSource);
        record.setCreateBy("system");
        record.setUpdateBy("system");

        if (existing == null)
        {
            record.setAttemptCount(1);
            try
            {
                deliveryMapper.insertDelivery(record);
                return record;
            }
            catch (DuplicateKeyException conflict)
            {
                log.info("投递记录唯一键冲突，改更新已有行, taskId={}, channel={}", task.getTaskId(), channel);
                deliveryMapper.updateDeliveryResult(record);
                ReviewDeliveryRecord after = deliveryMapper.selectByIdempotencyKey(key);
                if (after != null)
                {
                    record.setDeliveryId(after.getDeliveryId());
                }
                return record;
            }
        }
        record.setDeliveryId(existing.getDeliveryId());
        deliveryMapper.updateDeliveryResult(record);
        return record;
    }

    private String resolveChannelType(ReviewProject project)
    {
        if (project == null || project.getNotifyChannelId() == null)
        {
            return null;
        }
        try
        {
            return notifyChannelService.selectReviewNotifyChannelById(project.getNotifyChannelId())
                .getChannelType();
        }
        catch (Exception ex)
        {
            return null;
        }
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
        ReviewProject project = projectMapper.selectReviewProjectById(task.getProjectId());
        if (project == null)
        {
            throw new ServiceException("审查任务所属项目不存在");
        }
        deptService.checkDeptDataScope(project.getDeptId());
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
        return truncate(GitHubPullRequestCommentClient.sanitize(
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
}
