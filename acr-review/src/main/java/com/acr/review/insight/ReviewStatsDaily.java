package com.acr.review.insight;

import java.util.Date;
import com.acr.common.core.domain.BaseEntity;

/** 项目 × 日审查聚合行。 */
public class ReviewStatsDaily extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long projectId;
    private Date statDate;
    private Integer taskTotal;
    private Integer taskSuccess;
    private Integer taskFailed;
    private Integer taskPush;
    private Integer taskCovered;
    private Long durationP95Ms;
    private Integer issueNew;
    private Integer issueCritical;
    private Integer issueHigh;
    private Integer issueMedium;
    private Integer issueLow;
    private Integer issueClosed;
    private Integer issueConfirmed;
    private Integer issueFalsePositive;
    private Integer deliveryTotal;
    private Integer deliverySuccess;
    private Integer eventAccepted;
    private Integer eventIgnored;
    private Date createTime;
    private Date updateTime;

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public Long getProjectId()
    {
        return projectId;
    }

    public void setProjectId(Long projectId)
    {
        this.projectId = projectId;
    }

    public Date getStatDate()
    {
        return statDate;
    }

    public void setStatDate(Date statDate)
    {
        this.statDate = statDate;
    }

    public Integer getTaskTotal()
    {
        return taskTotal;
    }

    public void setTaskTotal(Integer taskTotal)
    {
        this.taskTotal = taskTotal;
    }

    public Integer getTaskSuccess()
    {
        return taskSuccess;
    }

    public void setTaskSuccess(Integer taskSuccess)
    {
        this.taskSuccess = taskSuccess;
    }

    public Integer getTaskFailed()
    {
        return taskFailed;
    }

    public void setTaskFailed(Integer taskFailed)
    {
        this.taskFailed = taskFailed;
    }

    public Integer getTaskPush()
    {
        return taskPush;
    }

    public void setTaskPush(Integer taskPush)
    {
        this.taskPush = taskPush;
    }

    public Integer getTaskCovered()
    {
        return taskCovered;
    }

    public void setTaskCovered(Integer taskCovered)
    {
        this.taskCovered = taskCovered;
    }

    public Long getDurationP95Ms()
    {
        return durationP95Ms;
    }

    public void setDurationP95Ms(Long durationP95Ms)
    {
        this.durationP95Ms = durationP95Ms;
    }

    public Integer getIssueNew()
    {
        return issueNew;
    }

    public void setIssueNew(Integer issueNew)
    {
        this.issueNew = issueNew;
    }

    public Integer getIssueCritical()
    {
        return issueCritical;
    }

    public void setIssueCritical(Integer issueCritical)
    {
        this.issueCritical = issueCritical;
    }

    public Integer getIssueHigh()
    {
        return issueHigh;
    }

    public void setIssueHigh(Integer issueHigh)
    {
        this.issueHigh = issueHigh;
    }

    public Integer getIssueMedium()
    {
        return issueMedium;
    }

    public void setIssueMedium(Integer issueMedium)
    {
        this.issueMedium = issueMedium;
    }

    public Integer getIssueLow()
    {
        return issueLow;
    }

    public void setIssueLow(Integer issueLow)
    {
        this.issueLow = issueLow;
    }

    public Integer getIssueClosed()
    {
        return issueClosed;
    }

    public void setIssueClosed(Integer issueClosed)
    {
        this.issueClosed = issueClosed;
    }

    public Integer getIssueConfirmed()
    {
        return issueConfirmed;
    }

    public void setIssueConfirmed(Integer issueConfirmed)
    {
        this.issueConfirmed = issueConfirmed;
    }

    public Integer getIssueFalsePositive()
    {
        return issueFalsePositive;
    }

    public void setIssueFalsePositive(Integer issueFalsePositive)
    {
        this.issueFalsePositive = issueFalsePositive;
    }

    public Integer getDeliveryTotal()
    {
        return deliveryTotal;
    }

    public void setDeliveryTotal(Integer deliveryTotal)
    {
        this.deliveryTotal = deliveryTotal;
    }

    public Integer getDeliverySuccess()
    {
        return deliverySuccess;
    }

    public void setDeliverySuccess(Integer deliverySuccess)
    {
        this.deliverySuccess = deliverySuccess;
    }

    public Integer getEventAccepted()
    {
        return eventAccepted;
    }

    public void setEventAccepted(Integer eventAccepted)
    {
        this.eventAccepted = eventAccepted;
    }

    public Integer getEventIgnored()
    {
        return eventIgnored;
    }

    public void setEventIgnored(Integer eventIgnored)
    {
        this.eventIgnored = eventIgnored;
    }

    public Date getCreateTime()
    {
        return createTime;
    }

    public void setCreateTime(Date createTime)
    {
        this.createTime = createTime;
    }

    public Date getUpdateTime()
    {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime)
    {
        this.updateTime = updateTime;
    }
}
