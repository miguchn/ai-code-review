package com.acr.review.insight.dto;

import java.util.ArrayList;
import java.util.List;

public class InsightMemberMineResponse
{
    private boolean claimed;
    private String dataSince;
    private String metricsVersion;
    private List<InsightIdentityCandidate> candidates = new ArrayList<>();
    private List<InsightIdentityCandidate> claimedIdentities = new ArrayList<>();
    private List<InsightCommitTrendPoint> commitTrend = new ArrayList<>();
    private Integer tasksReviewed;
    private Integer issuesNew;
    private Integer issuesOpen;
    private List<InsightNamedCount> openIssueTitles = new ArrayList<>();

    public boolean isClaimed()
    {
        return claimed;
    }

    public void setClaimed(boolean claimed)
    {
        this.claimed = claimed;
    }

    public String getDataSince()
    {
        return dataSince;
    }

    public void setDataSince(String dataSince)
    {
        this.dataSince = dataSince;
    }

    public String getMetricsVersion()
    {
        return metricsVersion;
    }

    public void setMetricsVersion(String metricsVersion)
    {
        this.metricsVersion = metricsVersion;
    }

    public List<InsightIdentityCandidate> getCandidates()
    {
        return candidates;
    }

    public void setCandidates(List<InsightIdentityCandidate> candidates)
    {
        this.candidates = candidates;
    }

    public List<InsightIdentityCandidate> getClaimedIdentities()
    {
        return claimedIdentities;
    }

    public void setClaimedIdentities(List<InsightIdentityCandidate> claimedIdentities)
    {
        this.claimedIdentities = claimedIdentities;
    }

    public List<InsightCommitTrendPoint> getCommitTrend()
    {
        return commitTrend;
    }

    public void setCommitTrend(List<InsightCommitTrendPoint> commitTrend)
    {
        this.commitTrend = commitTrend;
    }

    public Integer getTasksReviewed()
    {
        return tasksReviewed;
    }

    public void setTasksReviewed(Integer tasksReviewed)
    {
        this.tasksReviewed = tasksReviewed;
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

    public List<InsightNamedCount> getOpenIssueTitles()
    {
        return openIssueTitles;
    }

    public void setOpenIssueTitles(List<InsightNamedCount> openIssueTitles)
    {
        this.openIssueTitles = openIssueTitles;
    }
}
