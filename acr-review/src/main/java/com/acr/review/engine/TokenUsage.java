package com.acr.review.engine;

/** OCR / LLM 输出中尽力解析到的 token 用量；字段均可空。 */
public record TokenUsage(Integer inputTokens, Integer outputTokens, Integer totalTokens)
{
    public boolean isPresent()
    {
        return inputTokens != null || outputTokens != null || totalTokens != null;
    }
}
