package com.acr.review.scheduling;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.beans.factory.ObjectProvider;
import com.acr.review.domain.ReviewTask;
import com.acr.review.mapper.ReviewTaskMapper;
import com.acr.review.runtime.ReviewRuntimeAlertSettings;
import com.acr.review.service.IReviewTaskExecutionService;

class ReviewTaskDispatcherTest
{
    @Test
    void recoveryScanRequeuesExpiredLeasesAndDispatchesFairlyByProject()
    {
        ReviewTaskMapper mapper = mock(ReviewTaskMapper.class);
        ReviewTaskRuntimeSettings settings = mock(ReviewTaskRuntimeSettings.class);
        ReviewTaskWorkerIdentity identity = mock(ReviewTaskWorkerIdentity.class);
        ReviewResourceBudgetService budgetService = mock(ReviewResourceBudgetService.class);
        ReviewRuntimeAlertSettings alertSettings = mock(ReviewRuntimeAlertSettings.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<IReviewTaskExecutionService> provider = mock(ObjectProvider.class);
        IReviewTaskExecutionService executionService = mock(IReviewTaskExecutionService.class);
        ScheduledExecutorService scheduler = mock(ScheduledExecutorService.class);
        when(provider.getObject()).thenReturn(executionService);
        when(settings.maxRetries()).thenReturn(3);
        when(settings.retryBaseDelaySeconds()).thenReturn(30);
        when(settings.scanBatchSize()).thenReturn(3);
        when(budgetService.tryAcquireProject(anyLong())).thenReturn(true);
        when(mapper.selectDispatchableTasks(12)).thenReturn(List.of(
            candidate(1L, 10L), candidate(2L, 10L), candidate(3L, 20L), candidate(4L, 30L)));

        ReviewTaskDispatcher dispatcher = new ReviewTaskDispatcher(
            mapper, settings, identity, budgetService, alertSettings, provider, scheduler, directExecutor());
        dispatcher.scanOnce();

        InOrder recoveryOrder = inOrder(mapper);
        recoveryOrder.verify(mapper).terminalizeExpiredTasks(3);
        recoveryOrder.verify(mapper).requeueExpiredTasks(3, 30);
        InOrder executionOrder = inOrder(executionService);
        executionOrder.verify(executionService).executeTask(1L);
        executionOrder.verify(executionService).executeTask(3L);
        executionOrder.verify(executionService).executeTask(4L);
        verify(executionService, never()).executeTask(2L);
    }

    @Test
    void fairOrderAlternatesProjectsWithoutMonopoly()
    {
        List<Long> ordered = ReviewTaskDispatcher.fairOrder(List.of(
            candidate(1L, 10L),
            candidate(2L, 10L),
            candidate(3L, 10L),
            candidate(4L, 20L),
            candidate(5L, 30L)), 5);
        assertEquals(List.of(1L, 4L, 5L, 2L, 3L), ordered);
    }

    @Test
    void queueRejectionDefersTaskWithoutCallerRuns()
    {
        ReviewTaskMapper mapper = mock(ReviewTaskMapper.class);
        ReviewTaskRuntimeSettings settings = mock(ReviewTaskRuntimeSettings.class);
        ReviewTaskWorkerIdentity identity = mock(ReviewTaskWorkerIdentity.class);
        ReviewResourceBudgetService budgetService = mock(ReviewResourceBudgetService.class);
        ReviewRuntimeAlertSettings alertSettings = mock(ReviewRuntimeAlertSettings.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<IReviewTaskExecutionService> provider = mock(ObjectProvider.class);
        IReviewTaskExecutionService executionService = mock(IReviewTaskExecutionService.class);
        when(provider.getObject()).thenReturn(executionService);
        when(settings.maxRetries()).thenReturn(3);
        when(settings.retryBaseDelaySeconds()).thenReturn(30);
        when(settings.budgetBackoffSeconds()).thenReturn(30);
        when(settings.scanBatchSize()).thenReturn(1);
        when(budgetService.tryAcquireProject(10L)).thenReturn(true);
        when(mapper.selectDispatchableTasks(4)).thenReturn(List.of(candidate(9L, 10L)));
        when(mapper.deferDispatchableTask(eq(9L), eq(30), any())).thenReturn(1);
        ThreadPoolExecutor rejecting = mock(ThreadPoolExecutor.class);
        doThrow(new RejectedExecutionException("full")).when(rejecting).execute(any());

        ReviewTaskDispatcher dispatcher = new ReviewTaskDispatcher(
            mapper, settings, identity, budgetService, alertSettings, provider,
            mock(ScheduledExecutorService.class), rejecting);
        dispatcher.scanOnce();

        verify(executionService, never()).executeTask(anyLong());
        verify(mapper).deferDispatchableTask(eq(9L), eq(30), any());
        verify(budgetService).releaseProject(10L);
    }

    @Test
    void drainStopsDispatchAndExpiresLeasesWhenActiveTasksRemain()
    {
        ReviewTaskMapper mapper = mock(ReviewTaskMapper.class);
        ReviewTaskRuntimeSettings settings = mock(ReviewTaskRuntimeSettings.class);
        ReviewTaskWorkerIdentity identity = mock(ReviewTaskWorkerIdentity.class);
        ReviewResourceBudgetService budgetService = mock(ReviewResourceBudgetService.class);
        ReviewRuntimeAlertSettings alertSettings = mock(ReviewRuntimeAlertSettings.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<IReviewTaskExecutionService> provider = mock(ObjectProvider.class);
        ScheduledExecutorService scheduler = mock(ScheduledExecutorService.class);
        @SuppressWarnings("unchecked")
        ScheduledFuture<?> future = mock(ScheduledFuture.class);
        doReturn(future).when(scheduler).schedule(
            org.mockito.ArgumentMatchers.any(Runnable.class), anyLong(), org.mockito.ArgumentMatchers.any());
        when(identity.owner()).thenReturn("worker-a");
        when(alertSettings.drainTimeoutSeconds()).thenReturn(0);
        when(mapper.expireWorkerLeases("worker-a")).thenReturn(2);

        AtomicInteger active = new AtomicInteger(1);
        ThreadPoolExecutor busy = new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS, new SynchronousQueue<>())
        {
            @Override
            public int getActiveCount()
            {
                return active.get();
            }
        };

        ReviewTaskDispatcher dispatcher = new ReviewTaskDispatcher(
            mapper, settings, identity, budgetService, alertSettings, provider, scheduler, busy);
        dispatcher.start();
        assertEquals(true, dispatcher.isRunning());

        dispatcher.stop();
        assertFalse(dispatcher.isRunning());
        verify(future).cancel(false);
        verify(mapper).expireWorkerLeases("worker-a");
        verify(mapper, never()).releaseWorkerLeases("worker-a");

        dispatcher.wake(99L);
        verify(mapper, never()).selectReviewTaskById(99L);
        active.set(0);
    }

    private static ThreadPoolExecutor directExecutor()
    {
        return new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS, new SynchronousQueue<>())
        {
            @Override
            public void execute(Runnable command)
            {
                command.run();
            }
        };
    }

    private static ReviewTask candidate(long taskId, long projectId)
    {
        ReviewTask task = new ReviewTask();
        task.setTaskId(taskId);
        task.setProjectId(projectId);
        return task;
    }
}
