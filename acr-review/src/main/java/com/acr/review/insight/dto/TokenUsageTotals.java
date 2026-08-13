package com.acr.review.insight.dto;

/** Token 用量 KPI 汇总（SQL 聚合）。 */
public class TokenUsageTotals
{
    private Long inputTokens;
    private Long outputTokens;
    private Long totalTokens;
    private Long callCount;
    private Long successCount;
    private Long successMissingTokens;

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

    public Long getCallCount()
    {
        return callCount;
    }

    public void setCallCount(Long callCount)
    {
        this.callCount = callCount;
    }

    public Long getSuccessCount()
    {
        return successCount;
    }

    public void setSuccessCount(Long successCount)
    {
        this.successCount = successCount;
    }

    public Long getSuccessMissingTokens()
    {
        return successMissingTokens;
    }

    public void setSuccessMissingTokens(Long successMissingTokens)
    {
        this.successMissingTokens = successMissingTokens;
    }
}
