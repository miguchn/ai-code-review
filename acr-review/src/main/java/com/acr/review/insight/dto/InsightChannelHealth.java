package com.acr.review.insight.dto;

public class InsightChannelHealth
{
    private String channel;
    private Integer total;
    private Integer success;
    private Double successRate;

    public String getChannel()
    {
        return channel;
    }

    public void setChannel(String channel)
    {
        this.channel = channel;
    }

    public Integer getTotal()
    {
        return total;
    }

    public void setTotal(Integer total)
    {
        this.total = total;
    }

    public Integer getSuccess()
    {
        return success;
    }

    public void setSuccess(Integer success)
    {
        this.success = success;
    }

    public Double getSuccessRate()
    {
        return successRate;
    }

    public void setSuccessRate(Double successRate)
    {
        this.successRate = successRate;
    }
}
