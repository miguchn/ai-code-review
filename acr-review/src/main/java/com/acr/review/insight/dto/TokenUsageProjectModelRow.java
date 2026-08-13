package com.acr.review.insight.dto;

/** 项目 × 模型用量，用于按当前单价汇总项目成本。 */
public class TokenUsageProjectModelRow
{
    private Long projectId;
    private Long modelId;
    private Long inputTokens;
    private Long outputTokens;

    public Long getProjectId()
    {
        return projectId;
    }

    public void setProjectId(Long projectId)
    {
        this.projectId = projectId;
    }

    public Long getModelId()
    {
        return modelId;
    }

    public void setModelId(Long modelId)
    {
        this.modelId = modelId;
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
