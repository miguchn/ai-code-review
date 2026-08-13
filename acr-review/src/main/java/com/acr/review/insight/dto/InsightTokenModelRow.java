package com.acr.review.insight.dto;

public class InsightTokenModelRow
{
    private Long modelId;
    private String modelName;
    private String provider;
    private Long callCount;
    private Long inputTokens;
    private Long outputTokens;
    private Long totalTokens;
    private Double estimatedCost;
    private Double share;

    public Long getModelId()
    {
        return modelId;
    }

    public void setModelId(Long modelId)
    {
        this.modelId = modelId;
    }

    public String getModelName()
    {
        return modelName;
    }

    public void setModelName(String modelName)
    {
        this.modelName = modelName;
    }

    public String getProvider()
    {
        return provider;
    }

    public void setProvider(String provider)
    {
        this.provider = provider;
    }

    public Long getCallCount()
    {
        return callCount;
    }

    public void setCallCount(Long callCount)
    {
        this.callCount = callCount;
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

    public Long getTotalTokens()
    {
        return totalTokens;
    }

    public void setTotalTokens(Long totalTokens)
    {
        this.totalTokens = totalTokens;
    }

    public Double getEstimatedCost()
    {
        return estimatedCost;
    }

    public void setEstimatedCost(Double estimatedCost)
    {
        this.estimatedCost = estimatedCost;
    }

    public Double getShare()
    {
        return share;
    }

    public void setShare(Double share)
    {
        this.share = share;
    }
}
