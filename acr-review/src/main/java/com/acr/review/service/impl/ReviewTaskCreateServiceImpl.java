package com.acr.review.service.impl;

import java.util.Date;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.acr.common.exception.ServiceException;
import com.acr.review.domain.ReviewProject;
import com.acr.review.domain.ReviewTask;
import com.acr.review.domain.ReviewWebhookEvent;
import com.acr.review.git.GitPullRequestEvent;
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
        task.setPrNumber(prEvent.prNumber());
        task.setPrTitle(prEvent.prTitle());
        task.setSourceBranch(prEvent.sourceBranch());
        task.setTargetBranch(prEvent.targetBranch());
        task.setBaseSha(prEvent.baseSha());
        task.setHeadSha(prEvent.headSha());
        task.setTriggerType("WEBHOOK");
        task.setTaskStatus("PENDING");
        task.setAttemptCount(0);
        task.setCreateBy("webhook");
        snapshotService.freezeExecutionSnapshot(project, task);
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
        event.setProcessMessage("已受理，生成审查任务 #" + task.getTaskId());
        event.setProcessTime(new Date());
        eventMapper.updateProcessResult(event);
        executionService.scheduleExecution(task.getTaskId());
        return task.getTaskId();
    }
}
