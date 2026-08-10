package com.acr.review.service.impl;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import com.acr.review.delivery.ReviewDeliveryIntentService;
import com.acr.review.domain.ReviewRoundReconcileResult;
import com.acr.review.domain.ReviewTask;
import com.acr.review.domain.ReviewTaskRun;
import com.acr.review.mapper.ReviewTaskMapper;
import com.acr.review.mapper.ReviewTaskRunMapper;
import com.acr.review.service.IReviewIssueService;

class ReviewTaskCompletionServiceTest
{
    private final ReviewTaskMapper taskMapper = mock(ReviewTaskMapper.class);
    private final ReviewTaskRunMapper runMapper = mock(ReviewTaskRunMapper.class);
    private final IReviewIssueService issueService = mock(IReviewIssueService.class);
    private final ReviewDeliveryIntentService intentService = mock(ReviewDeliveryIntentService.class);
    private final ReviewTaskCompletionService service = new ReviewTaskCompletionService(
        taskMapper, runMapper, issueService, intentService);

    @Test
    void successPersistsFactsReconciliationAndIntentsInOneBoundary()
    {
        ReviewTask task = task();
        ReviewTaskRun run = run();
        when(issueService.reconcileAfterSuccess(task, run)).thenReturn(ReviewRoundReconcileResult.empty());
        when(taskMapper.updateTaskExecution(task)).thenReturn(1);

        service.completeSuccess(task, run);

        InOrder order = inOrder(runMapper, issueService, taskMapper, intentService);
        order.verify(runMapper).updateReviewTaskRun(run);
        order.verify(issueService).reconcileAfterSuccess(task, run);
        order.verify(taskMapper).updateTaskExecution(task);
        order.verify(intentService).enqueueAfterSuccess(task, run);
    }

    @Test
    void reconciliationFailurePreventsTerminalTaskAndDeliveryIntent()
    {
        ReviewTask task = task();
        ReviewTaskRun run = run();
        when(issueService.reconcileAfterSuccess(task, run)).thenThrow(new IllegalStateException("db unavailable"));

        assertThrows(ReviewTaskCompletionException.class, () -> service.completeSuccess(task, run));
        verify(taskMapper, never()).updateTaskExecution(any());
        verify(intentService, never()).enqueueAfterSuccess(any(), any());
    }

    @Test
    void terminalFailureCreatesNotificationIntentAfterFencedTaskUpdate()
    {
        ReviewTask task = task();
        ReviewTaskRun run = run();
        when(taskMapper.updateTaskExecution(task)).thenReturn(1);

        service.persistFailure(task, run, true);

        verify(intentService).enqueueTerminalNotification(task, run, "system");
    }

    private static ReviewTask task()
    {
        ReviewTask task = new ReviewTask();
        task.setTaskId(10L);
        task.setExecutionEpoch(2L);
        task.setLeaseOwner("worker");
        return task;
    }

    private static ReviewTaskRun run()
    {
        ReviewTaskRun run = new ReviewTaskRun();
        run.setRunId(100L);
        return run;
    }
}
