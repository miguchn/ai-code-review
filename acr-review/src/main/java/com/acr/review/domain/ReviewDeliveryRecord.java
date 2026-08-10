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
    /** 行内评论关联的问题 ID；总结/IM 为 null。 */
    private Long issueId;
    private String idempotencyKey;
    private String externalId;
    private String deliveryStatus;
    private String failureMessage;
    private Integer attemptCount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date nextAttemptAt;

    /** 稳定错误码，供自动重试策略与运维检索使用。 */
    private String lastErrorCode;

    /** 当前投递执行租约持有者；为空表示未被工作节点领取。 */
    private String leaseOwner;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date leaseUntil;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastAttemptTime;

    /** 触发来源：TASK_SUCCESS / ISSUE_DISPOSITION / MANUAL_RETRY。 */
    private String triggerSource;

    /**
     * 实际发出正文快照 JSON：
     * {"kind":"IM"|"SUMMARY_COMMENT","channelType":...,"title":...,"body":...}。
     * 列表查询不拉取此列。
     */
    private String contentSnapshot;

    /** 列表展示：是否已保留正文快照（不返回快照正文本身）。 */
    private Boolean hasContentSnapshot;

    /** 列表展示：项目名称。 */
    private String projectName;
    /** 列表展示：业务系统名称。 */
    private String businessSystemName;
    /** 查询条件：开始时间。 */
    private String beginTime;
    /** 查询条件：结束时间。 */
    private String endTime;

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

    public Long getIssueId()
    {
        return issueId;
    }

    public void setIssueId(Long issueId)
    {
        this.issueId = issueId;
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

    public Date getNextAttemptAt()
    {
        return nextAttemptAt;
    }

    public void setNextAttemptAt(Date nextAttemptAt)
    {
        this.nextAttemptAt = nextAttemptAt;
    }

    public String getLastErrorCode()
    {
        return lastErrorCode;
    }

    public void setLastErrorCode(String lastErrorCode)
    {
        this.lastErrorCode = lastErrorCode;
    }

    public String getLeaseOwner()
    {
        return leaseOwner;
    }

    public void setLeaseOwner(String leaseOwner)
    {
        this.leaseOwner = leaseOwner;
    }

    public Date getLeaseUntil()
    {
        return leaseUntil;
    }

    public void setLeaseUntil(Date leaseUntil)
    {
        this.leaseUntil = leaseUntil;
    }

    public Date getLastAttemptTime()
    {
        return lastAttemptTime;
    }

    public void setLastAttemptTime(Date lastAttemptTime)
    {
        this.lastAttemptTime = lastAttemptTime;
    }

    public String getTriggerSource()
    {
        return triggerSource;
    }

    public void setTriggerSource(String triggerSource)
    {
        this.triggerSource = triggerSource;
    }

    public String getContentSnapshot()
    {
        return contentSnapshot;
    }

    public void setContentSnapshot(String contentSnapshot)
    {
        this.contentSnapshot = contentSnapshot;
    }

    public Boolean getHasContentSnapshot()
    {
        return hasContentSnapshot;
    }

    public void setHasContentSnapshot(Boolean hasContentSnapshot)
    {
        this.hasContentSnapshot = hasContentSnapshot;
    }

    public String getProjectName()
    {
        return projectName;
    }

    public void setProjectName(String projectName)
    {
        this.projectName = projectName;
    }

    public String getBusinessSystemName()
    {
        return businessSystemName;
    }

    public void setBusinessSystemName(String businessSystemName)
    {
        this.businessSystemName = businessSystemName;
    }

    public String getBeginTime()
    {
        return beginTime;
    }

    public void setBeginTime(String beginTime)
    {
        this.beginTime = beginTime;
    }

    public String getEndTime()
    {
        return endTime;
    }

    public void setEndTime(String endTime)
    {
        this.endTime = endTime;
    }
}
