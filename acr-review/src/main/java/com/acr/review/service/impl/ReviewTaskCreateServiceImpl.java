package com.acr.review.service.impl;

import java.util.Date;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.acr.common.exception.ServiceException;
import com.acr.common.utils.StringUtils;
import com.acr.review.domain.ReviewPipelineConstants;
import com.acr.review.domain.ReviewProject;
import com.acr.review.domain.ReviewTask;
import com.acr.review.domain.ReviewWebhookEvent;
import com.acr.review.git.GitPullRequestEvent;
import com.acr.review.git.GitPushEvent;
import com.acr.review.mapper.ReviewTaskMapper;
import com.acr.review.mapper.ReviewWebhookEventMapper;
import com.acr.review.service.IReviewTaskCreateService;
import com.acr.review.service.IReviewTaskExecutionService;
import com.acr.review.service.IReviewTaskSnapshotService;

/** 审查任务创建（事务）：任务与事件受理结果同生共死；提交后调度异步执行。 */
@Service
public class ReviewTaskCreateServiceImpl implements IReviewTaskCreateService
{
    private final ReviewTaskMapper taskMapper;
    private final ReviewWebhookEventMapper eventMapper;
    private final IReviewTaskExecutionService executionService;
    private final IReviewTaskSnapshotService snapshotService;

    public ReviewTaskCreateServiceImpl(ReviewTaskMapper taskMapper,
                                       ReviewWebhookEventMapper eventMapper,
                                       IReviewTaskExecutionService executionService,
                                       IReviewTaskSnapshotService snapshotService)
    {
        this.taskMapper = taskMapper;
        this.eventMapper = eventMapper;
        this.executionService = executionService;
        this.snapshotService = snapshotService;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createTaskFromEvent(ReviewProject project, ReviewWebhookEvent event, GitPullRequestEvent prEvent)
    {
        ReviewTask task = new ReviewTask();
        task.setProjectId(project.getProjectId());
        task.setEventId(event.getEventId());
        task.setProvider(event.getProvider());
        task.setEventSource(ReviewPipelineConstants.EVENT_SOURCE_PR);
        task.setPrNumber(prEvent.prNumber());
        task.setPrTitle(prEvent.prTitle());
        task.setPrAuthor(prEvent.prAuthor());
        task.setSourceBranch(prEvent.sourceBranch());
        task.setTargetBranch(prEvent.targetBranch());
        task.setBaseSha(prEvent.baseSha());
        task.setHeadSha(prEvent.headSha());
        task.setAdditions(prEvent.additions());
        task.setDeletions(prEvent.deletions());
        task.setChangedFiles(prEvent.changedFiles());
        task.setTriggerType("WEBHOOK");
        task.setTaskStatus("PENDING");
        task.setAttemptCount(0);
        task.setCreateBy("webhook");
        snapshotService.freezeExecutionSnapshot(project, task);
        return persistAndSchedule(task, event, "已受理，生成审查任务 #");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createTaskFromPushEvent(ReviewProject project, ReviewWebhookEvent event, GitPushEvent pushEvent)
    {
        ReviewTask task = new ReviewTask();
        task.setProjectId(project.getProjectId());
        task.setEventId(event.getEventId());
        task.setProvider(event.getProvider());
        task.setEventSource(ReviewPipelineConstants.EVENT_SOURCE_PUSH);
        task.setPrNumber(0);
        task.setPrTitle(resolvePushTitle(pushEvent));
        task.setPrAuthor(pushEvent.pusher());
        task.setSourceBranch(pushEvent.branch());
        task.setTargetBranch(pushEvent.branch());
        task.setBaseSha(pushEvent.beforeSha());
        task.setHeadSha(pushEvent.afterSha());
        task.setAdditions(null);
        task.setDeletions(null);
        task.setChangedFiles(null);
        task.setTriggerType("WEBHOOK");
        task.setTaskStatus("PENDING");
        task.setAttemptCount(0);
        task.setCreateBy("webhook");
        snapshotService.freezeExecutionSnapshot(project, task);
        return persistAndSchedule(task, event, "已受理推送审查，生成审查任务 #");
    }

    private Long persistAndSchedule(ReviewTask task, ReviewWebhookEvent event, String acceptedPrefix)
    {
        try
        {
            taskMapper.insertReviewTask(task);
        }
        catch (DuplicateKeyException e)
        {
            throw new ServiceException("事件已生成审查任务，请勿重复建单");
        }

        event.setTaskId(task.getTaskId());
        event.setProcessStatus("ACCEPTED");
        event.setProcessMessage(acceptedPrefix + task.getTaskId());
        event.setProcessTime(new Date());
        eventMapper.updateProcessResult(event);
        executionService.scheduleExecution(task.getTaskId());
        return task.getTaskId();
    }

    /** 推送任务标题：优先最新提交摘要，否则「N 个提交」。 */
    static String resolvePushTitle(GitPushEvent pushEvent)
    {
        if (pushEvent == null)
        {
            return "推送审查";
        }
        if (StringUtils.isNotEmpty(pushEvent.headCommitMessage()))
        {
            String message = pushEvent.headCommitMessage().trim();
            int newline = message.indexOf('\n');
            if (newline >= 0)
            {
                message = message.substring(0, newline).trim();
            }
            if (!message.isEmpty())
            {
                return message.length() > 200 ? message.substring(0, 200) : message;
            }
        }
        Integer count = pushEvent.commitCount();
        if (count != null && count > 0)
        {
            return count + " 个提交";
        }
        return "推送审查";
    }
}
