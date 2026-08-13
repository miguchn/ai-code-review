package com.acr.common.ai;

import com.acr.common.enums.LlmCallErrorType;

/**
 * LLM 调用结果（连接测试 / 模型调用测试 / 业务调用）。
 */
public class LlmCallResult
{
    private boolean success;
    private long latencyMs;
    private String content;
    private String rawSnippet;
    private LlmCallErrorType errorType;
    private String errorMessage;
    private Integer promptTokens;
    private Integer completionTokens;
    private Integer totalTokens;

    public static LlmCallResult success(long latencyMs, String content, String rawSnippet)
    {
        LlmCallResult result = new LlmCallResult();
        result.success = true;
        result.latencyMs = latencyMs;
        result.content = content;
        result.rawSnippet = rawSnippet;
        return result;
    }

    public static LlmCallResult failure(LlmCallErrorType errorType, String errorMessage, long latencyMs, String rawSnippet)
    {
        LlmCallResult result = new LlmCallResult();
        result.success = false;
        result.errorType = errorType;
        result.errorMessage = errorMessage;
        result.latencyMs = latencyMs;
        result.rawSnippet = rawSnippet;
        return result;
    }

    public boolean isSuccess()
    {
        return success;
    }

    public long getLatencyMs()
    {
        return latencyMs;
    }

    public String getContent()
    {
        return content;
    }

    public String getRawSnippet()
    {
        return rawSnippet;
    }

    public LlmCallErrorType getErrorType()
    {
        return errorType;
    }

    public String getErrorMessage()
    {
        return errorMessage;
    }

    public Integer getPromptTokens()
    {
        return promptTokens;
    }

    public void setPromptTokens(Integer promptTokens)
    {
        this.promptTokens = promptTokens;
    }

    public Integer getCompletionTokens()
    {
        return completionTokens;
    }

    public void setCompletionTokens(Integer completionTokens)
    {
        this.completionTokens = completionTokens;
    }

    public Integer getTotalTokens()
    {
        return totalTokens;
    }

    public void setTotalTokens(Integer totalTokens)
    {
        this.totalTokens = totalTokens;
    }
}
