package com.acr.review.domain;

import com.acr.common.core.domain.BaseEntity;

/** 审查任务 review_task：一次由事件触发的执行实例。 */
public class ReviewTask extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long taskId;
    private Long projectId;
    private Long eventId;
    private String provider;
    private Integer prNumber;
    private String prTitle;
    private String sourceBranch;
    private String targetBranch;
    private String baseSha;
    private String headSha;
    private String triggerType;
    private String taskStatus;
    private String failureMessage;

    /** 列表展示用项目名称（join review_project，不持久化）。 */
    private String projectName;

    public Long getTaskId()
    {
        return taskId;
    }

    public void setTaskId(Long taskId)
    {
        this.taskId = taskId;
    }

    public Long getProjectId()
    {
        return projectId;
    }

    public void setProjectId(Long projectId)
    {
        this.projectId = projectId;
    }

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

    public String getTriggerType()
    {
        return triggerType;
    }

    public void setTriggerType(String triggerType)
    {
        this.triggerType = triggerType;
    }

    public String getTaskStatus()
    {
        return taskStatus;
    }

    public void setTaskStatus(String taskStatus)
    {
        this.taskStatus = taskStatus;
    }

    public String getFailureMessage()
    {
        return failureMessage;
    }

    public void setFailureMessage(String failureMessage)
    {
        this.failureMessage = failureMessage;
    }

    public String getProjectName()
    {
        return projectName;
    }

    public void setProjectName(String projectName)
    {
        this.projectName = projectName;
    }
}
