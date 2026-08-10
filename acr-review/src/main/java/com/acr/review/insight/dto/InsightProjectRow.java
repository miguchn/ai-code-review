package com.acr.review.insight.dto;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;

/** 项目分析矩阵行。 */
public class InsightProjectRow
{
    private Long projectId;
    private String projectName;
    private String businessSystemName;
    private String ownerName;
    private Integer taskTotal;
    private Double successRate;
    private Integer issueNew;
    private Integer openFocusIssues;
    private Double dispositionRate;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastReviewTime;

    public Long getProjectId()
    {
        return projectId;
    }

    public void setProjectId(Long projectId)
    {
        this.projectId = projectId;
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

    public String getOwnerName()
    {
        return ownerName;
    }

    public void setOwnerName(String ownerName)
    {
        this.ownerName = ownerName;
    }

    public Integer getTaskTotal()
    {
        return taskTotal;
    }

    public void setTaskTotal(Integer taskTotal)
    {
        this.taskTotal = taskTotal;
    }

    public Double getSuccessRate()
    {
        return successRate;
    }

    public void setSuccessRate(Double successRate)
    {
        this.successRate = successRate;
    }

    public Integer getIssueNew()
    {
        return issueNew;
    }

    public void setIssueNew(Integer issueNew)
    {
        this.issueNew = issueNew;
    }

    public Integer getOpenFocusIssues()
    {
        return openFocusIssues;
    }

    public void setOpenFocusIssues(Integer openFocusIssues)
    {
        this.openFocusIssues = openFocusIssues;
    }

    public Double getDispositionRate()
    {
        return dispositionRate;
    }

    public void setDispositionRate(Double dispositionRate)
    {
        this.dispositionRate = dispositionRate;
    }

    public Date getLastReviewTime()
    {
        return lastReviewTime;
    }

    public void setLastReviewTime(Date lastReviewTime)
    {
        this.lastReviewTime = lastReviewTime;
    }
}
