package com.acr.review.insight.dto;

public class InsightTokenProjectRow
{
    private Long projectId;
    private String projectName;
    private String businessSystemName;
    private String ownerName;
    private Long callCount;
    private Long totalTokens;
    private Long inputTokens;
    private Long outputTokens;
    private Double estimatedCost;

    public Long getProjectId()
    {
        return projectId;
    }

    public void setProjectId(Long projectId)
    {
        this.projectId = projectId;
    }

    public String getProjectName()
    {
        return projectName;
    }

    public void setProjectName(String projectName)
    {
        this.projectName = projectName;
    }

    public String getBusinessSystemName()
    {
        return businessSystemName;
    }

    public void setBusinessSystemName(String businessSystemName)
    {
        this.businessSystemName = businessSystemName;
    }

    public String getOwnerName()
    {
        return ownerName;
    }

    public void setOwnerName(String ownerName)
    {
        this.ownerName = ownerName;
    }

    public Long getCallCount()
    {
        return callCount;
    }

    public void setCallCount(Long callCount)
    {
        this.callCount = callCount;
    }

    public Long getTotalTokens()
    {
        return totalTokens;
    }

    public void setTotalTokens(Long totalTokens)
    {
        this.totalTokens = totalTokens;
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

    public Double getEstimatedCost()
    {
        return estimatedCost;
    }

    public void setEstimatedCost(Double estimatedCost)
    {
        this.estimatedCost = estimatedCost;
    }
}
