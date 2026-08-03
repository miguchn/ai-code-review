package com.acr.review.service.impl;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import com.acr.common.exception.ServiceException;
import com.acr.common.utils.StringUtils;
import com.acr.review.delivery.ReviewCommentBodyRenderer;
import com.acr.review.delivery.ReviewDeliveryConstants;
import com.acr.review.domain.ReviewDeliveryRecord;
import com.acr.review.domain.ReviewPipelineConstants;
import com.acr.review.domain.ReviewProject;
import com.acr.review.domain.ReviewTask;
import com.acr.review.domain.ReviewTaskRun;
import com.acr.review.git.GitPullRequestComment;
import com.acr.review.git.GitPullRequestCommentClient;
import com.acr.review.git.GitPullRequestCommentException;
import com.acr.review.git.GitRepositoryCoordinates;
import com.acr.review.git.github.GitHubPullRequestCommentClient;
import com.acr.review.mapper.ReviewDeliveryRecordMapper;
import com.acr.review.mapper.ReviewProjectMapper;
import com.acr.review.mapper.ReviewTaskMapper;
import com.acr.review.mapper.ReviewTaskRunMapper;
import com.acr.review.service.IGitCredentialService;
import com.acr.review.service.IReviewDeliveryService;
import com.acr.system.service.ISysDeptService;

/** GitHub PR 总结评论投递：成功后 upsert 投递记录，失败不污染审查结论。 */
@Service
public class ReviewDeliveryServiceImpl implements IReviewDeliveryService
{
    private static final Logger log = LoggerFactory.getLogger(ReviewDeliveryServiceImpl.class);

    private final ReviewDeliveryRecordMapper deliveryMapper;
    private final ReviewTaskMapper taskMapper;
    private final ReviewTaskRunMapper runMapper;
    private final ReviewProjectMapper projectMapper;
    private final IGitCredentialService credentialService;
    private final ISysDeptService deptService;
    private final GitPullRequestCommentClient commentClient;

    public ReviewDeliveryServiceImpl(ReviewDeliveryRecordMapper deliveryMapper,
                                     ReviewTaskMapper taskMapper,
                                     ReviewTaskRunMapper runMapper,
                                     ReviewProjectMapper projectMapper,
                                     IGitCredentialService credentialService,
                                     ISysDeptService deptService,
                                     GitPullRequestCommentClient commentClient)
    {
        this.deliveryMapper = deliveryMapper;
        this.taskMapper = taskMapper;
        this.runMapper = runMapper;
        this.projectMapper = projectMapper;
        this.credentialService = credentialService;
        this.deptService = deptService;
        this.commentClient = commentClient;
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
            upsertResult(task, run, ReviewDeliveryConstants.STATUS_SUCCESS, externalId, null);
        }
        catch (Exception ex)
        {
            String message = sanitizeFailure(ex, null);
            log.warn("审查结果投递失败（不影响任务状态）, taskId={}, reason={}", task.getTaskId(), message);
            try
            {
                upsertResult(task, run, ReviewDeliveryConstants.STATUS_FAILED, null, message);
            }
            catch (Exception persistEx)
            {
                log.warn("投递失败记录落库异常, taskId={}", task.getTaskId(), persistEx);
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
            upsertResult(latest, run, ReviewDeliveryConstants.STATUS_SUCCESS, externalId, null);
        }
        catch (Exception ex)
        {
            String message = sanitizeFailure(ex, null);
            upsertResult(latest, run, ReviewDeliveryConstants.STATUS_FAILED, null, message);
            throw new ServiceException("投递重试失败：" + message);
        }
    }

    @Override
    public ReviewDeliveryRecord selectSummaryDelivery(Long projectId, Integer prNumber)
    {
        if (projectId == null || prNumber == null)
        {
            return null;
        }
        return deliveryMapper.selectByProjectAndPr(projectId, prNumber,
            ReviewDeliveryConstants.CHANNEL_GITHUB_PR_SUMMARY);
    }

    private String writeComment(ReviewTask task, ReviewTaskRun run)
    {
        ReviewProject project = projectMapper.selectReviewProjectById(task.getProjectId());
        if (project == null || !"0".equals(project.getStatus()))
        {
            throw new GitPullRequestCommentException("项目不存在或已停用，无法投递评论");
        }
        if (!ReviewDeliveryConstants.PROVIDER_GITHUB.equalsIgnoreCase(project.getProvider()))
        {
            throw new GitPullRequestCommentException("当前仅支持向 GitHub 回写总结评论");
        }

        String token;
        try
        {
            token = credentialService.getPlainToken(project.getCredentialId(), true);
        }
        catch (ServiceException ex)
        {
            throw new GitPullRequestCommentException("GitHub 凭据不可用：" + ex.getMessage());
        }

        GitRepositoryCoordinates repository = new GitRepositoryCoordinates(
            project.getRepositoryOwner(), project.getRepositoryName(), project.getRepositoryUrl());
        String body = ReviewCommentBodyRenderer.render(task, run);

        Optional<GitPullRequestComment> existing = commentClient.findCommentWithMarker(
            repository, token, task.getPrNumber(), ReviewDeliveryConstants.COMMENT_MARKER);
        if (existing.isPresent())
        {
            return commentClient.updateIssueComment(
                repository, token, existing.get().id(), body).id();
        }
        return commentClient.createIssueComment(
            repository, token, task.getPrNumber(), body).id();
    }

    private void upsertResult(ReviewTask task, ReviewTaskRun run, String status,
                              String externalId, String failureMessage)
    {
        Date now = new Date();
        String key = ReviewDeliveryConstants.idempotencyKey(task.getProjectId(), task.getPrNumber());
        ReviewDeliveryRecord existing = deliveryMapper.selectByIdempotencyKey(key);

        ReviewDeliveryRecord record = new ReviewDeliveryRecord();
        record.setTaskId(task.getTaskId());
        record.setRunId(run == null ? null : run.getRunId());
        record.setProjectId(task.getProjectId());
        record.setProvider(ReviewDeliveryConstants.PROVIDER_GITHUB);
        record.setChannel(ReviewDeliveryConstants.CHANNEL_GITHUB_PR_SUMMARY);
        record.setPrNumber(task.getPrNumber());
        record.setIdempotencyKey(key);
        record.setExternalId(externalId != null ? externalId
            : (existing == null ? null : existing.getExternalId()));
        record.setDeliveryStatus(status);
        record.setFailureMessage(truncate(failureMessage));
        record.setLastAttemptTime(now);
        record.setCreateBy("system");
        record.setUpdateBy("system");

        if (existing == null)
        {
            record.setAttemptCount(1);
            deliveryMapper.insertDelivery(record);
        }
        else
        {
            deliveryMapper.updateDeliveryResult(record);
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
