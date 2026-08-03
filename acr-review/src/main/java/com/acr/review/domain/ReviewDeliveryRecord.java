package com.acr.review.domain;

import java.util.Date;
import com.acr.common.core.domain.BaseEntity;
import com.fasterxml.jackson.annotation.JsonFormat;

/** 审查结果投递记录 review_delivery_record：外部评论副作用事实。 */
public class ReviewDeliveryRecord extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long deliveryId;
    private Long taskId;
    private Long runId;
    private Long projectId;
    private String provider;
    private String channel;
    private Integer prNumber;
    private String idempotencyKey;
    private String externalId;
    private String deliveryStatus;
    private String failureMessage;
    private Integer attemptCount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastAttemptTime;

    public Long getDeliveryId()
    {
        return deliveryId;
    }

    public void setDeliveryId(Long deliveryId)
    {
        this.deliveryId = deliveryId;
    }

    public Long getTaskId()
    {
        return taskId;
    }

    public void setTaskId(Long taskId)
    {
        this.taskId = taskId;
    }

    public Long getRunId()
    {
        return runId;
    }

    public void setRunId(Long runId)
    {
        this.runId = runId;
    }

    public Long getProjectId()
    {
        return projectId;
    }

    public void setProjectId(Long projectId)
    {
        this.projectId = projectId;
    }

    public String getProvider()
    {
        return provider;
    }

    public void setProvider(String provider)
    {
        this.provider = provider;
    }

    public String getChannel()
    {
        return channel;
    }

    public void setChannel(String channel)
    {
        this.channel = channel;
    }

    public Integer getPrNumber()
    {
        return prNumber;
    }

    public void setPrNumber(Integer prNumber)
    {
        this.prNumber = prNumber;
    }

    public String getIdempotencyKey()
    {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey)
    {
        this.idempotencyKey = idempotencyKey;
    }

    public String getExternalId()
    {
        return externalId;
    }

    public void setExternalId(String externalId)
    {
        this.externalId = externalId;
    }

    public String getDeliveryStatus()
    {
        return deliveryStatus;
    }

    public void setDeliveryStatus(String deliveryStatus)
    {
        this.deliveryStatus = deliveryStatus;
    }

    public String getFailureMessage()
    {
        return failureMessage;
    }

    public void setFailureMessage(String failureMessage)
    {
        this.failureMessage = failureMessage;
    }

    public Integer getAttemptCount()
    {
        return attemptCount;
    }

    public void setAttemptCount(Integer attemptCount)
    {
        this.attemptCount = attemptCount;
    }

    public Date getLastAttemptTime()
    {
        return lastAttemptTime;
    }

    public void setLastAttemptTime(Date lastAttemptTime)
    {
        this.lastAttemptTime = lastAttemptTime;
    }
}
