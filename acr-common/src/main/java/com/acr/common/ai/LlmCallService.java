package com.acr.common.ai;

/**
 * 统一 LLM 调用服务接口。业务层仅通过 modelConfigId 或默认模型发起调用。
 */
public interface LlmCallService
{
    /**
     * 使用指定模型配置发起对话。
     */
    LlmCallResult chat(Long modelConfigId, String prompt);

    /**
     * 使用指定模型配置发起对话，并将业务任务剩余预算作为调用级硬超时。
     */
    LlmCallResult chat(Long modelConfigId, String prompt, int timeoutMs);

    /**
     * 使用默认启用模型发起对话。
     */
    LlmCallResult chatWithDefault(String prompt);

    /**
     * 连接测试（最小请求验证可达性）。
     */
    LlmCallResult testConnection(Long modelConfigId, String apiUrl, String apiKey, String model, Integer timeout);

    /**
     * 模型调用测试（返回时延与原始响应摘要）。
     */
    LlmCallResult testModelCall(Long modelConfigId, String apiUrl, String apiKey, String model,
        Integer timeout, Integer maxTokens, Double temperature);
}
