package com.acr.review.scheduling;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doReturn;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import com.acr.review.domain.ReviewTask;
import com.acr.review.mapper.ReviewTaskMapper;

class ReviewTaskLeaseManagerTest
{
    @Test
    void renewsByOwnerAndEpochAndMarksLostLease()
    {
        ReviewTaskMapper mapper = mock(ReviewTaskMapper.class);
        ReviewTaskRuntimeSettings settings = mock(ReviewTaskRuntimeSettings.class);
        ScheduledExecutorService scheduler = mock(ScheduledExecutorService.class);
        @SuppressWarnings("unchecked")
        ScheduledFuture<?> future = mock(ScheduledFuture.class);
        when(settings.heartbeatSeconds()).thenReturn(5);
        when(settings.leaseSeconds()).thenReturn(60);
        doReturn(future).when(scheduler)
            .scheduleAtFixedRate(any(Runnable.class), eq(5L), eq(5L), eq(TimeUnit.SECONDS));

        ReviewTask task = new ReviewTask();
        task.setTaskId(7L);
        task.setExecutionEpoch(4L);
        task.setLeaseOwner("worker-a");
        when(mapper.renewTaskLease(7L, 4L, "worker-a", 60)).thenReturn(0);

        ReviewTaskLeaseManager.LeaseHandle handle =
            new ReviewTaskLeaseManager(mapper, settings, scheduler).start(task);
        ArgumentCaptor<Runnable> heartbeat = ArgumentCaptor.forClass(Runnable.class);
        verify(scheduler).scheduleAtFixedRate(heartbeat.capture(), eq(5L), eq(5L), eq(TimeUnit.SECONDS));
        heartbeat.getValue().run();

        verify(mapper).renewTaskLease(7L, 4L, "worker-a", 60);
        assertTrue(handle.isLost());
        handle.close();
        verify(future).cancel(false);
    }
}
