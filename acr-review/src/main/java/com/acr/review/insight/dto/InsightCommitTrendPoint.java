package com.acr.review.insight.dto;

public class InsightCommitTrendPoint
{
    private String date;
    private Integer commitCount;
    private String authorKey;

    public String getDate()
    {
        return date;
    }

    public void setDate(String date)
    {
        this.date = date;
    }

    public Integer getCommitCount()
    {
        return commitCount;
    }

    public void setCommitCount(Integer commitCount)
    {
        this.commitCount = commitCount;
    }

    public String getAuthorKey()
    {
        return authorKey;
    }

    public void setAuthorKey(String authorKey)
    {
        this.authorKey = authorKey;
    }
}
