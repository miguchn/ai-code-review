package com.acr.review.insight.dto;

/** 处置漏斗：新增 → 确认 → 关闭。 */
public class InsightDispositionFunnel
{
    private Integer issueNew;
    private Integer confirmed;
    private Integer closed;

    public Integer getIssueNew()
    {
        return issueNew;
    }

    public void setIssueNew(Integer issueNew)
    {
        this.issueNew = issueNew;
    }

    public Integer getConfirmed()
    {
        return confirmed;
    }

    public void setConfirmed(Integer confirmed)
    {
        this.confirmed = confirmed;
    }

    public Integer getClosed()
    {
        return closed;
    }

    public void setClosed(Integer closed)
    {
        this.closed = closed;
    }
}
