package com.acr.review.runtime;

import java.util.Date;

/** 积压与处置清单行。 */
public class ReviewRuntimeBacklogItem
{
    private String kind;
    private Long taskId;
    private Long deliveryId;
    private Long runId;
    private String projectName;
    private String statusLabel;
    private String summary;
    private Long ageSeconds;
    private Date createTime;
    private Date leaseUntil;
    private Date nextRunAt;

    public String getKind()
    {
        return kind;
    }

    public void setKind(String kind)
    {
        this.kind = kind;
    }

    public Long getTaskId()
    {
        return taskId;
    }

    public void setTaskId(Long taskId)
    {
        this.taskId = taskId;
    }

    public Long getDeliveryId()
    {
        return deliveryId;
    }

    public void setDeliveryId(Long deliveryId)
    {
        this.deliveryId = deliveryId;
    }

    public Long getRunId()
    {
        return runId;
    }

    public void setRunId(Long runId)
    {
        this.runId = runId;
    }

    public String getProjectName()
    {
        return projectName;
    }

    public void setProjectName(String projectName)
    {
        this.projectName = projectName;
    }

    public String getStatusLabel()
    {
        return statusLabel;
    }

    public void setStatusLabel(String statusLabel)
    {
        this.statusLabel = statusLabel;
    }

    public String getSummary()
    {
        return summary;
    }

    public void setSummary(String summary)
    {
        this.summary = summary;
    }

    public Long getAgeSeconds()
    {
        return ageSeconds;
    }

    public void setAgeSeconds(Long ageSeconds)
    {
        this.ageSeconds = ageSeconds;
    }

    public Date getCreateTime()
    {
        return createTime;
    }

    public void setCreateTime(Date createTime)
    {
        this.createTime = createTime;
    }

    public Date getLeaseUntil()
    {
        return leaseUntil;
    }

    public void setLeaseUntil(Date leaseUntil)
    {
        this.leaseUntil = leaseUntil;
    }

    public Date getNextRunAt()
    {
        return nextRunAt;
    }

    public void setNextRunAt(Date nextRunAt)
    {
        this.nextRunAt = nextRunAt;
    }
}
