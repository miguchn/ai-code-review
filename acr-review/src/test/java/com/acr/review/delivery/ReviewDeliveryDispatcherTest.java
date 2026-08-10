package com.acr.review.delivery;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import com.acr.review.mapper.ReviewDeliveryRecordMapper;
import com.acr.review.runtime.ReviewRuntimeAlertSettings;
import com.acr.review.scheduling.ReviewTaskWorkerIdentity;
import com.acr.review.service.IReviewDeliveryService;

class ReviewDeliveryDispatcherTest
{
    @Test
    void claimsBeforeSubmittingAndExecutesWithSameLeaseOwner()
    {
        ReviewDeliveryRecordMapper mapper = mock(ReviewDeliveryRecordMapper.class);
        ReviewDeliveryRuntimeSettings settings = mock(ReviewDeliveryRuntimeSettings.class);
        ReviewRuntimeAlertSettings alertSettings = mock(ReviewRuntimeAlertSettings.class);
        IReviewDeliveryService service = mock(IReviewDeliveryService.class);
        ReviewTaskWorkerIdentity identity = mock(ReviewTaskWorkerIdentity.class);
        ScheduledExecutorService scheduler = mock(ScheduledExecutorService.class);
        @SuppressWarnings({"unchecked", "rawtypes"})
        ScheduledFuture future = mock(ScheduledFuture.class);
        when(scheduler.schedule(any(Runnable.class), anyLong(), any())).thenReturn(future);
        when(settings.scanBatchSize()).thenReturn(10);
        when(settings.leaseSeconds()).thenReturn(120);
        when(identity.owner()).thenReturn("worker-a");
        when(mapper.selectDispatchableDeliveryIds(10)).thenReturn(List.of(7L));
        when(mapper.claimDelivery(7L, "worker-a:delivery", 120)).thenReturn(1);
        ThreadPoolExecutor direct = new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS, new SynchronousQueue<>())
        {
            @Override
            public void execute(Runnable command)
            {
                command.run();
            }
        };
        ReviewDeliveryDispatcher dispatcher = new ReviewDeliveryDispatcher(
            mapper, settings, alertSettings, service, identity, scheduler, direct);

        dispatcher.scanOnce();

        verify(mapper).claimDelivery(7L, "worker-a:delivery", 120);
        verify(service).executeClaimedDelivery(7L, "worker-a:delivery");
    }
}
