package com.acr.review.insight.dto;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;

public class InsightTokenRunRow
{
    private Long runId;
    private Long taskId;
    private String projectName;
    private Long projectId;
    private Long modelId;
    private String modelName;
    private String reviewMode;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date triggerTime;
    private Integer inputTokens;
    private Integer outputTokens;
    private Integer totalTokens;
    private Double estimatedCost;

    public Long getRunId()
    {
        return runId;
    }

    public void setRunId(Long runId)
    {
        this.runId = runId;
    }

    public Long getTaskId()
    {
        return taskId;
    }

    public void setTaskId(Long taskId)
    {
        this.taskId = taskId;
    }

    public String getProjectName()
    {
        return projectName;
    }

    public void setProjectName(String projectName)
    {
        this.projectName = projectName;
    }

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

    public String getModelName()
    {
        return modelName;
    }

    public void setModelName(String modelName)
    {
        this.modelName = modelName;
    }

    public String getReviewMode()
    {
        return reviewMode;
    }

    public void setReviewMode(String reviewMode)
    {
        this.reviewMode = reviewMode;
    }

    public Date getTriggerTime()
    {
        return triggerTime;
    }

    public void setTriggerTime(Date triggerTime)
    {
        this.triggerTime = triggerTime;
    }

    public Integer getInputTokens()
    {
        return inputTokens;
    }

    public void setInputTokens(Integer inputTokens)
    {
        this.inputTokens = inputTokens;
    }

    public Integer getOutputTokens()
    {
        return outputTokens;
    }

    public void setOutputTokens(Integer outputTokens)
    {
        this.outputTokens = outputTokens;
    }

    public Integer getTotalTokens()
    {
        return totalTokens;
    }

    public void setTotalTokens(Integer totalTokens)
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
}
