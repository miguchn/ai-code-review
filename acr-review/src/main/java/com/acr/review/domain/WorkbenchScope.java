package com.acr.review.domain;

/** 工作台范围与健康；无权限字段为 null。 */
public class WorkbenchScope
{
    private Integer projectCount;
    private String latestTaskTime;

    public Integer getProjectCount()
    {
        return projectCount;
    }

    public void setProjectCount(Integer projectCount)
    {
        this.projectCount = projectCount;
    }

    public String getLatestTaskTime()
    {
        return latestTaskTime;
    }

    public void setLatestTaskTime(String latestTaskTime)
    {
        this.latestTaskTime = latestTaskTime;
    }
}
