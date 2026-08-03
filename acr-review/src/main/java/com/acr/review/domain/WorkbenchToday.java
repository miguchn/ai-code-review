package com.acr.review.domain;

/** 今日摘要；无对应 list 权限的字段为 null。 */
public class WorkbenchToday
{
    private Integer newTasks;
    private Integer successTasks;
    private Integer failedTasks;
    private Integer closedIssues;

    public Integer getNewTasks()
    {
        return newTasks;
    }

    public void setNewTasks(Integer newTasks)
    {
        this.newTasks = newTasks;
    }

    public Integer getSuccessTasks()
    {
        return successTasks;
    }

    public void setSuccessTasks(Integer successTasks)
    {
        this.successTasks = successTasks;
    }

    public Integer getFailedTasks()
    {
        return failedTasks;
    }

    public void setFailedTasks(Integer failedTasks)
    {
        this.failedTasks = failedTasks;
    }

    public Integer getClosedIssues()
    {
        return closedIssues;
    }

    public void setClosedIssues(Integer closedIssues)
    {
        this.closedIssues = closedIssues;
    }
}
