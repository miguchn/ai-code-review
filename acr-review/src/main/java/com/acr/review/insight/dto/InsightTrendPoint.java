package com.acr.review.insight.dto;

/** 趋势点。 */
public class InsightTrendPoint
{
    private String date;
    private Integer success;
    private Integer failed;
    private Integer critical;
    private Integer high;
    private Integer medium;
    private Integer low;
    private Integer issueNew;

    public String getDate()
    {
        return date;
    }

    public void setDate(String date)
    {
        this.date = date;
    }

    public Integer getSuccess()
    {
        return success;
    }

    public void setSuccess(Integer success)
    {
        this.success = success;
    }

    public Integer getFailed()
    {
        return failed;
    }

    public void setFailed(Integer failed)
    {
        this.failed = failed;
    }

    public Integer getCritical()
    {
        return critical;
    }

    public void setCritical(Integer critical)
    {
        this.critical = critical;
    }

    public Integer getHigh()
    {
        return high;
    }

    public void setHigh(Integer high)
    {
        this.high = high;
    }

    public Integer getMedium()
    {
        return medium;
    }

    public void setMedium(Integer medium)
    {
        this.medium = medium;
    }

    public Integer getLow()
    {
        return low;
    }

    public void setLow(Integer low)
    {
        this.low = low;
    }

    public Integer getIssueNew()
    {
        return issueNew;
    }

    public void setIssueNew(Integer issueNew)
    {
        this.issueNew = issueNew;
    }
}
