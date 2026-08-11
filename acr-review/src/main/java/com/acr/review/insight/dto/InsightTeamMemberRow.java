package com.acr.review.insight.dto;

import java.util.ArrayList;
import java.util.List;

public class InsightTeamMemberRow
{
    private Long userId;
    private String authorKey;
    private String authorName;
    private List<String> identities = new ArrayList<>();
    private Integer commitCount;
    private Integer tasksReviewed;
    private Integer additionsSum;
    private Integer deletionsSum;
    private Integer issuesNew;
    private Integer issuesOpen;
    private List<InsightCommitTrendPoint> commitTrend = new ArrayList<>();

    public Long getUserId()
    {
        return userId;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    public String getAuthorKey()
    {
        return authorKey;
    }

    public void setAuthorKey(String authorKey)
    {
        this.authorKey = authorKey;
    }

    public String getAuthorName()
    {
        return authorName;
    }

    public void setAuthorName(String authorName)
    {
        this.authorName = authorName;
    }

    public List<String> getIdentities()
    {
        return identities;
    }

    public void setIdentities(List<String> identities)
    {
        this.identities = identities;
    }

    public Integer getCommitCount()
    {
        return commitCount;
    }

    public void setCommitCount(Integer commitCount)
    {
        this.commitCount = commitCount;
    }

    public Integer getTasksReviewed()
    {
        return tasksReviewed;
    }

    public void setTasksReviewed(Integer tasksReviewed)
    {
        this.tasksReviewed = tasksReviewed;
    }

    public Integer getAdditionsSum()
    {
        return additionsSum;
    }

    public void setAdditionsSum(Integer additionsSum)
    {
        this.additionsSum = additionsSum;
    }

    public Integer getDeletionsSum()
    {
        return deletionsSum;
    }

    public void setDeletionsSum(Integer deletionsSum)
    {
        this.deletionsSum = deletionsSum;
    }

    public Integer getIssuesNew()
    {
        return issuesNew;
    }

    public void setIssuesNew(Integer issuesNew)
    {
        this.issuesNew = issuesNew;
    }

    public Integer getIssuesOpen()
    {
        return issuesOpen;
    }

    public void setIssuesOpen(Integer issuesOpen)
    {
        this.issuesOpen = issuesOpen;
    }

    public List<InsightCommitTrendPoint> getCommitTrend()
    {
        return commitTrend;
    }

    public void setCommitTrend(List<InsightCommitTrendPoint> commitTrend)
    {
        this.commitTrend = commitTrend;
    }
}
