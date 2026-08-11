package com.acr.review.delivery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import com.acr.review.domain.ReviewDeliveryRecord;
import com.acr.review.domain.ReviewNotifyChannel;
import com.acr.review.domain.ReviewPipelineConstants;
import com.acr.review.domain.ReviewProject;
import com.acr.review.domain.ReviewTask;
import com.acr.review.domain.ReviewTaskRun;
import com.acr.review.mapper.ReviewDeliveryRecordMapper;
import com.acr.review.mapper.ReviewProjectMapper;
import com.acr.review.service.IReviewNotifyChannelService;

class ReviewDeliveryIntentServiceTest
{
    private final ReviewDeliveryRecordMapper mapper = mock(ReviewDeliveryRecordMapper.class);
    private final ReviewProjectMapper projectMapper = mock(ReviewProjectMapper.class);
    private final IReviewNotifyChannelService channelService = mock(IReviewNotifyChannelService.class);
    private final ApplicationEventPublisher publisher = mock(ApplicationEventPublisher.class);
    private final ReviewDeliveryIntentService service = new ReviewDeliveryIntentService(
        mapper, projectMapper, channelService, publisher);

    @Test
    void createsPendingSummaryIntentWithStableKey()
    {
        ReviewTask task = task(10L, ReviewPipelineConstants.TASK_SUCCESS);
        ReviewTaskRun run = run(100L);
        ReviewProject project = project();
        when(projectMapper.selectReviewProjectById(3L)).thenReturn(project);
        when(mapper.upsertDeliveryIntent(any())).thenAnswer(invocation -> {
            invocation.<ReviewDeliveryRecord>getArgument(0).setDeliveryId(55L);
            return 1;
        });

        ReviewDeliveryRecord result = service.enqueueSummary(task, run,
            ReviewDeliveryConstants.TRIGGER_TASK_SUCCESS, "system");

        ArgumentCaptor<ReviewDeliveryRecord> captor = ArgumentCaptor.forClass(ReviewDeliveryRecord.class);
        verify(mapper).upsertDeliveryIntent(captor.capture());
        assertEquals(ReviewDeliveryConstants.STATUS_PENDING, captor.getValue().getDeliveryStatus());
        assertEquals("GITHUB:3:8:SUMMARY_COMMENT", captor.getValue().getIdempotencyKey());
        assertEquals(55L, result.getDeliveryId());
        verify(publisher).publishEvent(any(ReviewDeliveryPendingEvent.class));
    }

    @Test
    void disabledNotificationDoesNotCreateIntent()
    {
        ReviewProject project = project();
        project.setNotifyEnabled("N");
        when(projectMapper.selectReviewProjectById(3L)).thenReturn(project);

        ReviewDeliveryRecord result = service.enqueueTerminalNotification(
            task(10L, ReviewPipelineConstants.TASK_SUCCESS), run(100L), "system");

        assertNull(result);
        verify(mapper, never()).upsertDeliveryIntent(any());
    }

    @Test
    void invalidNotificationConfigurationIsVisibleAsManualWork()
    {
        ReviewProject project = project();
        project.setNotifyEnabled("Y");
        project.setNotifyChannelId(7L);
        ReviewNotifyChannel channel = new ReviewNotifyChannel();
        channel.setChannelType(ReviewDeliveryConstants.CHANNEL_FEISHU_BOT);
        channel.setStatus("1");
        when(projectMapper.selectReviewProjectById(3L)).thenReturn(project);
        when(channelService.selectReviewNotifyChannelById(7L)).thenReturn(channel);

        service.enqueueTerminalNotification(task(10L, ReviewPipelineConstants.TASK_SUCCESS), run(100L), "system");

        ArgumentCaptor<ReviewDeliveryRecord> captor = ArgumentCaptor.forClass(ReviewDeliveryRecord.class);
        verify(mapper).upsertDeliveryIntent(captor.capture());
        assertEquals(ReviewDeliveryConstants.STATUS_MANUAL, captor.getValue().getDeliveryStatus());
        assertEquals(ReviewDeliveryConstants.CHANNEL_IM_NOTIFICATION, captor.getValue().getChannel());
        assertEquals(ReviewDeliveryConstants.ERROR_CONFIGURATION, captor.getValue().getLastErrorCode());
        verify(publisher, never()).publishEvent(any(ReviewDeliveryPendingEvent.class));
    }

    @Test
    void successTaskNotificationUsesTaskSuccessTrigger()
    {
        ReviewProject project = notifyEnabledProject();
        when(projectMapper.selectReviewProjectById(3L)).thenReturn(project);
        when(channelService.selectReviewNotifyChannelById(7L)).thenReturn(activeDingTalkChannel());
        when(mapper.upsertDeliveryIntent(any())).thenReturn(1);

        service.enqueueTerminalNotification(task(10L, ReviewPipelineConstants.TASK_SUCCESS), run(100L), "system");

        ArgumentCaptor<ReviewDeliveryRecord> captor = ArgumentCaptor.forClass(ReviewDeliveryRecord.class);
        verify(mapper).upsertDeliveryIntent(captor.capture());
        assertEquals(ReviewDeliveryConstants.TRIGGER_TASK_SUCCESS, captor.getValue().getTriggerSource());
    }

    @Test
    void failedTaskNotificationUsesTaskFailedTrigger()
    {
        ReviewProject project = notifyEnabledProject();
        project.setNotifyOnFailure("Y");
        when(projectMapper.selectReviewProjectById(3L)).thenReturn(project);
        when(channelService.selectReviewNotifyChannelById(7L)).thenReturn(activeDingTalkChannel());
        when(mapper.upsertDeliveryIntent(any())).thenReturn(1);

        service.enqueueTerminalNotification(task(10L, ReviewPipelineConstants.TASK_FAILED), run(100L), "system");

        ArgumentCaptor<ReviewDeliveryRecord> captor = ArgumentCaptor.forClass(ReviewDeliveryRecord.class);
        verify(mapper).upsertDeliveryIntent(captor.capture());
        assertEquals(ReviewDeliveryConstants.TRIGGER_TASK_FAILED, captor.getValue().getTriggerSource());
    }

    private static ReviewTask task(Long id, String status)
    {
        ReviewTask task = new ReviewTask();
        task.setTaskId(id);
        task.setProjectId(3L);
        task.setProvider("GITHUB");
        task.setPrNumber(8);
        task.setTaskStatus(status);
        return task;
    }

    private static ReviewTaskRun run(Long id)
    {
        ReviewTaskRun run = new ReviewTaskRun();
        run.setRunId(id);
        return run;
    }

    private static ReviewProject project()
    {
        ReviewProject project = new ReviewProject();
        project.setProjectId(3L);
        project.setProvider("GITHUB");
        project.setNotifyEnabled("N");
        return project;
    }

    private static ReviewProject notifyEnabledProject()
    {
        ReviewProject project = project();
        project.setNotifyEnabled("Y");
        project.setNotifyChannelId(7L);
        return project;
    }

    private static ReviewNotifyChannel activeDingTalkChannel()
    {
        ReviewNotifyChannel channel = new ReviewNotifyChannel();
        channel.setChannelType(ReviewDeliveryConstants.CHANNEL_DINGTALK_ROBOT);
        channel.setStatus("0");
        return channel;
    }
}
