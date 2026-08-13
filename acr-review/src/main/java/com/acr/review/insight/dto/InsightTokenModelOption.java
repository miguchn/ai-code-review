package com.acr.review.insight.dto;

public class InsightTokenModelOption
{
    private Long modelId;
    private String modelName;
    private String provider;

    public InsightTokenModelOption()
    {
    }

    public InsightTokenModelOption(Long modelId, String modelName, String provider)
    {
        this.modelId = modelId;
        this.modelName = modelName;
        this.provider = provider;
    }

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
}
