package com.acr.review.domain.result;

import java.util.ArrayList;
import java.util.List;

/** 统一审查评分结果协议 DTO（protocolVersion 1.0）。 */
public class ReviewScoreResult
{
    private String protocolVersion;
    private List<ReviewScoreDimension> scores = new ArrayList<>();
    private Integer totalScore;
    private String summary;
    private List<ReviewTopIssue> topIssues = new ArrayList<>();
    private Integer focusIssueCount;
    private Boolean hasCriticalSecurityIssue;

    public String getProtocolVersion()
    {
        return protocolVersion;
    }

    public void setProtocolVersion(String protocolVersion)
    {
        this.protocolVersion = protocolVersion;
    }

    public List<ReviewScoreDimension> getScores()
    {
        return scores;
    }

    public void setScores(List<ReviewScoreDimension> scores)
    {
        this.scores = scores == null ? new ArrayList<>() : scores;
    }

    public Integer getTotalScore()
    {
        return totalScore;
    }

    public void setTotalScore(Integer totalScore)
    {
        this.totalScore = totalScore;
    }

    public String getSummary()
    {
        return summary;
    }

    public void setSummary(String summary)
    {
        this.summary = summary;
    }

    public List<ReviewTopIssue> getTopIssues()
    {
        return topIssues;
    }

    public void setTopIssues(List<ReviewTopIssue> topIssues)
    {
        this.topIssues = topIssues == null ? new ArrayList<>() : topIssues;
    }

    public Integer getFocusIssueCount()
    {
        return focusIssueCount;
    }

    public void setFocusIssueCount(Integer focusIssueCount)
    {
        this.focusIssueCount = focusIssueCount;
    }

    public Boolean getHasCriticalSecurityIssue()
    {
        return hasCriticalSecurityIssue;
    }

    public void setHasCriticalSecurityIssue(Boolean hasCriticalSecurityIssue)
    {
        this.hasCriticalSecurityIssue = hasCriticalSecurityIssue;
    }
}
