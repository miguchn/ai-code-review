package com.acr.review.insight.dto;

public class InsightTokenTrendPoint
{
    private String date;
    private Long inputTokens;
    private Long outputTokens;

    public String getDate()
    {
        return date;
    }

    public void setDate(String date)
    {
        this.date = date;
    }

    public Long getInputTokens()
    {
        return inputTokens;
    }

    public void setInputTokens(Long inputTokens)
    {
        this.inputTokens = inputTokens;
    }

    public Long getOutputTokens()
    {
        return outputTokens;
    }

    public void setOutputTokens(Long outputTokens)
    {
        this.outputTokens = outputTokens;
    }
}
