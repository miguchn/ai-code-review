package com.acr.review.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.acr.review.delivery.ReviewDeliveryIntentService;
import com.acr.review.domain.ReviewRoundReconcileResult;
import com.acr.review.domain.ReviewTask;
import com.acr.review.domain.ReviewTaskRun;
import com.acr.review.mapper.ReviewTaskMapper;
import com.acr.review.mapper.ReviewTaskRunMapper;
import com.acr.review.service.IReviewIssueService;

/**
 * 审查终态事务边界：run、task、问题对账和投递意图要么同时提交，要么全部回滚。
 * 该服务不得执行 Git/IM 等外部调用。
 */
@Service
public class ReviewTaskCompletionService
{
    private final ReviewTaskMapper taskMapper;
    private final ReviewTaskRunMapper runMapper;
    private final IReviewIssueService issueService;
    private final ReviewDeliveryIntentService deliveryIntentService;

    public ReviewTaskCompletionService(ReviewTaskMapper taskMapper,
                                       ReviewTaskRunMapper runMapper,
                                       IReviewIssueService issueService,
                                       ReviewDeliveryIntentService deliveryIntentService)
    {
        this.taskMapper = taskMapper;
        this.runMapper = runMapper;
        this.issueService = issueService;
        this.deliveryIntentService = deliveryIntentService;
    }

    @Transactional
    public ReviewRoundReconcileResult completeSuccess(ReviewTask task, ReviewTaskRun run)
    {
        try
        {
            runMapper.updateReviewTaskRun(run);
            ReviewRoundReconcileResult reconcile = issueService.reconcileAfterSuccess(task, run);
            persistOwnedTask(task);
            deliveryIntentService.enqueueAfterSuccess(task, run);
            return reconcile == null ? ReviewRoundReconcileResult.empty() : reconcile;
        }
        catch (ReviewTaskLeaseLostException ex)
        {
            throw ex;
        }
        catch (RuntimeException ex)
        {
            throw new ReviewTaskCompletionException("审查结果终态事务提交失败", ex);
        }
    }

    @Transactional
    public void persistFailure(ReviewTask task, ReviewTaskRun run, boolean terminal)
    {
        try
        {
            if (run != null && run.getRunId() != null)
            {
                runMapper.updateReviewTaskRun(run);
            }
            persistOwnedTask(task);
            if (terminal)
            {
                deliveryIntentService.enqueueTerminalNotification(task, run, "system");
            }
        }
        catch (ReviewTaskLeaseLostException ex)
        {
            throw ex;
        }
        catch (RuntimeException ex)
        {
            throw new ReviewTaskCompletionException("审查失败终态事务提交失败", ex);
        }
    }

    private void persistOwnedTask(ReviewTask task)
    {
        if (taskMapper.updateTaskExecution(task) != 1)
        {
            throw new ReviewTaskLeaseLostException(task.getTaskId(), task.getExecutionEpoch());
        }
    }
}
