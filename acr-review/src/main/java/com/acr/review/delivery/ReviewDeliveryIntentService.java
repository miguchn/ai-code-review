package com.acr.review.delivery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import com.acr.common.exception.ServiceException;
import com.acr.common.utils.StringUtils;
import com.acr.review.domain.ReviewDeliveryRecord;
import com.acr.review.domain.ReviewNotifyChannel;
import com.acr.review.domain.ReviewPipelineConstants;
import com.acr.review.domain.ReviewProject;
import com.acr.review.domain.ReviewTask;
import com.acr.review.domain.ReviewTaskRun;
import com.acr.review.mapper.ReviewDeliveryRecordMapper;
import com.acr.review.mapper.ReviewProjectMapper;
import com.acr.review.service.IReviewNotifyChannelService;

/** 只负责写入可恢复投递意图，不执行任何外部副作用。 */
@Service
public class ReviewDeliveryIntentService
{
    private static final Logger log = LoggerFactory.getLogger(ReviewDeliveryIntentService.class);

    private final ReviewDeliveryRecordMapper deliveryMapper;
    private final ReviewProjectMapper projectMapper;
    private final IReviewNotifyChannelService notifyChannelService;
    private final ApplicationEventPublisher eventPublisher;

    public ReviewDeliveryIntentService(ReviewDeliveryRecordMapper deliveryMapper,
                                       ReviewProjectMapper projectMapper,
                                       IReviewNotifyChannelService notifyChannelService,
                                       ApplicationEventPublisher eventPublisher)
    {
        this.deliveryMapper = deliveryMapper;
        this.projectMapper = projectMapper;
        this.notifyChannelService = notifyChannelService;
        this.eventPublisher = eventPublisher;
    }

    public void enqueueAfterSuccess(ReviewTask task, ReviewTaskRun run)
    {
        enqueueSummary(task, run, ReviewDeliveryConstants.TRIGGER_TASK_SUCCESS, "system");
        enqueueInlineComments(task, run, ReviewDeliveryConstants.TRIGGER_TASK_SUCCESS, "system");
        enqueueTerminalNotification(task, run, "system");
    }

    public ReviewDeliveryRecord enqueueSummary(ReviewTask task, ReviewTaskRun run,
                                                String triggerSource, String operator)
    {
        if (task == null || !ReviewPipelineConstants.TASK_SUCCESS.equals(task.getTaskStatus())
            || ReviewPipelineConstants.EVENT_SOURCE_PUSH.equals(task.getEventSource())
            || task.getPrNumber() == null || task.getPrNumber() <= 0)
        {
            return null;
        }
        ReviewProject project = projectMapper.selectReviewProjectById(task.getProjectId());
        String provider = project != null && StringUtils.isNotEmpty(project.getProvider())
            ? project.getProvider() : StringUtils.defaultIfEmpty(task.getProvider(), ReviewDeliveryConstants.PROVIDER_GITHUB);
        String channel = ReviewDeliveryConstants.channelForProvider(provider);
        String key = ReviewDeliveryConstants.idempotencyKey(provider, task.getProjectId(), task.getPrNumber());
        return upsert(task, run, provider, channel, key, null, ReviewDeliveryConstants.STATUS_PENDING,
            null, null, triggerSource, operator);
    }

    /**
     * 为达到严重度门槛的问题各登记一条行内评论意图；返回新入队（或刷新为 PENDING）的条数。
     * 已 SUCCESS/SKIPPED 的问题全生命周期不再入队。
     */
    public int enqueueInlineComments(ReviewTask task, ReviewTaskRun run,
                                     String triggerSource, String operator)
    {
        if (task == null || !ReviewPipelineConstants.TASK_SUCCESS.equals(task.getTaskStatus())
            || ReviewPipelineConstants.EVENT_SOURCE_PUSH.equals(task.getEventSource())
            || task.getPrNumber() == null || task.getPrNumber() <= 0)
        {
            return 0;
        }
        ReviewProject project = projectMapper.selectReviewProjectById(task.getProjectId());
        if (project == null || !"0".equals(project.getInlineCommentEnabled()))
        {
            return 0;
        }
        String provider = StringUtils.isNotEmpty(project.getProvider())
            ? project.getProvider()
            : StringUtils.defaultIfEmpty(task.getProvider(), ReviewDeliveryConstants.PROVIDER_GITHUB);
        String channel = ReviewDeliveryConstants.inlineChannelForProvider(provider);
        java.util.Set<String> allowed = ReviewInlineCommentRenderer.parseSeverities(project.getInlineSeverities());
        int enqueued = 0;
        for (com.acr.review.domain.result.ReviewTopIssue issue
            : ReviewSummaryContentFactory.resolveTopIssues(run))
        {
            if (issue == null || issue.getIssueId() == null)
            {
                continue;
            }
            if (!ReviewInlineCommentRenderer.severityAllowed(issue.getSeverity(), allowed))
            {
                continue;
            }
            if (StringUtils.isEmpty(issue.getFilePath()) || effectiveEndLine(issue) == null)
            {
                continue;
            }
            String key = ReviewDeliveryConstants.inlineIdempotencyKey(
                provider, task.getProjectId(), issue.getIssueId());
            ReviewDeliveryRecord existing = deliveryMapper.selectByIdempotencyKey(key);
            if (existing != null
                && (ReviewDeliveryConstants.STATUS_SUCCESS.equals(existing.getDeliveryStatus())
                    || ReviewDeliveryConstants.STATUS_SKIPPED.equals(existing.getDeliveryStatus())))
            {
                continue;
            }
            upsert(task, run, provider, channel, key, issue.getIssueId(),
                ReviewDeliveryConstants.STATUS_PENDING, null, null, triggerSource, operator);
            enqueued++;
        }
        return enqueued;
    }

    private static Integer effectiveEndLine(com.acr.review.domain.result.ReviewTopIssue issue)
    {
        if (issue.getEndLine() != null)
        {
            return issue.getEndLine();
        }
        return issue.getStartLine();
    }

    public ReviewDeliveryRecord enqueueTerminalNotification(ReviewTask task, ReviewTaskRun run, String operator)
    {
        if (task == null || task.getTaskId() == null)
        {
            return null;
        }
        boolean success = ReviewPipelineConstants.TASK_SUCCESS.equals(task.getTaskStatus());
        boolean failed = ReviewPipelineConstants.TASK_FAILED.equals(task.getTaskStatus());
        if (!success && !failed)
        {
            return null;
        }
        ReviewProject project = projectMapper.selectReviewProjectById(task.getProjectId());
        if (project == null || !"Y".equals(project.getNotifyEnabled())
            || (failed && !"Y".equals(project.getNotifyOnFailure())))
        {
            return null;
        }

        String provider = StringUtils.defaultIfEmpty(project.getProvider(), ReviewDeliveryConstants.PROVIDER_GITHUB);
        String channelType = resolveChannelType(project);
        boolean resolvable = ReviewDeliveryConstants.isSupportedNotifyChannelType(channelType);
        String channel = resolvable ? channelType : ReviewDeliveryConstants.CHANNEL_IM_NOTIFICATION;
        String status = resolvable ? ReviewDeliveryConstants.STATUS_PENDING : ReviewDeliveryConstants.STATUS_MANUAL;
        String errorCode = resolvable ? null : ReviewDeliveryConstants.ERROR_CONFIGURATION;
        String failure = resolvable ? null : "项目已启用通知，但通知渠道缺失、停用或类型不受支持，请修复配置后人工补发";
        return upsert(task, run, provider, channel,
            ReviewDeliveryConstants.imIdempotencyKey(channel, task.getTaskId()),
            null, status, errorCode, failure, ReviewDeliveryConstants.TRIGGER_TASK_SUCCESS, operator);
    }

    public void requeue(Long deliveryId, String operator)
    {
        if (deliveryMapper.requeueDelivery(deliveryId, ReviewDeliveryConstants.TRIGGER_MANUAL_RETRY,
            StringUtils.defaultIfEmpty(operator, "system")) != 1)
        {
            throw new ServiceException("投递记录正在执行或状态已变化，请刷新后重试");
        }
        eventPublisher.publishEvent(new ReviewDeliveryPendingEvent(this, deliveryId));
    }

    private ReviewDeliveryRecord upsert(ReviewTask task, ReviewTaskRun run, String provider, String channel,
                                        String key, Long issueId, String status, String errorCode, String failureMessage,
                                        String triggerSource, String operator)
    {
        ReviewDeliveryRecord record = new ReviewDeliveryRecord();
        record.setTaskId(task.getTaskId());
        record.setRunId(run == null ? null : run.getRunId());
        record.setProjectId(task.getProjectId());
        record.setProvider(provider);
        record.setChannel(channel);
        record.setPrNumber(task.getPrNumber());
        record.setIssueId(issueId);
        record.setIdempotencyKey(key);
        record.setDeliveryStatus(status);
        record.setLastErrorCode(errorCode);
        record.setFailureMessage(failureMessage);
        record.setTriggerSource(triggerSource);
        record.setCreateBy(StringUtils.defaultIfEmpty(operator, "system"));
        record.setUpdateBy(StringUtils.defaultIfEmpty(operator, "system"));
        deliveryMapper.upsertDeliveryIntent(record);
        if (record.getDeliveryId() == null)
        {
            ReviewDeliveryRecord persisted = deliveryMapper.selectByIdempotencyKey(key);
            record.setDeliveryId(persisted == null ? null : persisted.getDeliveryId());
        }
        if (ReviewDeliveryConstants.STATUS_PENDING.equals(status) && record.getDeliveryId() != null)
        {
            eventPublisher.publishEvent(new ReviewDeliveryPendingEvent(this, record.getDeliveryId()));
        }
        else if (ReviewDeliveryConstants.STATUS_MANUAL.equals(status))
        {
            log.warn("投递意图因配置问题进入人工处置, taskId={}, channel={}", task.getTaskId(), channel);
        }
        return record;
    }

    private String resolveChannelType(ReviewProject project)
    {
        if (project.getNotifyChannelId() == null)
        {
            return null;
        }
        try
        {
            ReviewNotifyChannel channel = notifyChannelService.selectReviewNotifyChannelById(project.getNotifyChannelId());
            return channel == null || !"0".equals(channel.getStatus()) ? null : channel.getChannelType();
        }
        catch (RuntimeException ex)
        {
            return null;
        }
    }
}
