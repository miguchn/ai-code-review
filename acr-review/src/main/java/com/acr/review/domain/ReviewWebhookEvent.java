package com.acr.review.domain;

import java.util.Date;
import com.acr.common.core.domain.BaseEntity;
import com.fasterxml.jackson.annotation.JsonFormat;

/** Webhook 事件 review_webhook_event：一次可信投递的不可变接入事实。 */
public class ReviewWebhookEvent extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long eventId;
    private String provider;
    private String deliveryId;
    private String eventType;
    private String action;
    private String repositoryOwner;
    private String repositoryName;
    private String repositoryFullPath;
    private Long projectId;
    private Integer prNumber;
    private String prTitle;
    private String sourceBranch;
    private String targetBranch;
    private String baseSha;
    private String headSha;
    private String processStatus;
    private String processMessage;
    private Integer duplicateCount;
    private Long taskId;
    private Integer payloadSize;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date receiveTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date processTime;

    public Long getEventId()
    {
        return eventId;
    }

    public void setEventId(Long eventId)
    {
        this.eventId = eventId;
    }

    public String getProvider()
    {
        return provider;
    }

    public void setProvider(String provider)
    {
        this.provider = provider;
    }

    public String getDeliveryId()
    {
        return deliveryId;
    }

    public void setDeliveryId(String deliveryId)
    {
        this.deliveryId = deliveryId;
    }

    public String getEventType()
    {
        return eventType;
    }

    public void setEventType(String eventType)
    {
        this.eventType = eventType;
    }

    public String getAction()
    {
        return action;
    }

    public void setAction(String action)
    {
        this.action = action;
    }

    public String getRepositoryOwner()
    {
        return repositoryOwner;
    }

    public void setRepositoryOwner(String repositoryOwner)
    {
        this.repositoryOwner = repositoryOwner;
    }

    public String getRepositoryName()
    {
        return repositoryName;
    }

    public void setRepositoryName(String repositoryName)
    {
        this.repositoryName = repositoryName;
    }

    public String getRepositoryFullPath()
    {
        return repositoryFullPath;
    }

    public void setRepositoryFullPath(String repositoryFullPath)
    {
        this.repositoryFullPath = repositoryFullPath;
    }

    public Long getProjectId()
    {
        return projectId;
    }

    public void setProjectId(Long projectId)
    {
        this.projectId = projectId;
    }

    public Integer getPrNumber()
    {
        return prNumber;
    }

    public void setPrNumber(Integer prNumber)
    {
        this.prNumber = prNumber;
    }

    public String getPrTitle()
    {
        return prTitle;
    }

    public void setPrTitle(String prTitle)
    {
        this.prTitle = prTitle;
    }

    public String getSourceBranch()
    {
        return sourceBranch;
    }

    public void setSourceBranch(String sourceBranch)
    {
        this.sourceBranch = sourceBranch;
    }

    public String getTargetBranch()
    {
        return targetBranch;
    }

    public void setTargetBranch(String targetBranch)
    {
        this.targetBranch = targetBranch;
    }

    public String getBaseSha()
    {
        return baseSha;
    }

    public void setBaseSha(String baseSha)
    {
        this.baseSha = baseSha;
    }

    public String getHeadSha()
    {
        return headSha;
    }

    public void setHeadSha(String headSha)
    {
        this.headSha = headSha;
    }

    public String getProcessStatus()
    {
        return processStatus;
    }

    public void setProcessStatus(String processStatus)
    {
        this.processStatus = processStatus;
    }

    public String getProcessMessage()
    {
        return processMessage;
    }

    public void setProcessMessage(String processMessage)
    {
        this.processMessage = processMessage;
    }

    public Integer getDuplicateCount()
    {
        return duplicateCount;
    }

    public void setDuplicateCount(Integer duplicateCount)
    {
        this.duplicateCount = duplicateCount;
    }

    public Long getTaskId()
    {
        return taskId;
    }

    public void setTaskId(Long taskId)
    {
        this.taskId = taskId;
    }

    public Integer getPayloadSize()
    {
        return payloadSize;
    }

    public void setPayloadSize(Integer payloadSize)
    {
        this.payloadSize = payloadSize;
    }

    public Date getReceiveTime()
    {
        return receiveTime;
    }

    public void setReceiveTime(Date receiveTime)
    {
        this.receiveTime = receiveTime;
    }

    public Date getProcessTime()
    {
        return processTime;
    }

    public void setProcessTime(Date processTime)
    {
        this.processTime = processTime;
    }
}
