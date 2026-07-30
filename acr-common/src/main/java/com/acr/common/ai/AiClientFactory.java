package com.acr.common.ai;

import java.util.HashMap;
import java.util.Map;

/**
 * AI 客户端工厂 — 根据 providerCode 创建对应客户端
 */
public class AiClientFactory
{
    private static final Map<String, AiClient> CLIENTS = new HashMap<>();

    static
    {
        // 注册 OpenAI 兼容协议客户端（OpenAI / Qwen / DeepSeek 等）
        register("openai", new OpenAiCompatibleClient());
        register("qwen", new OpenAiCompatibleClient());
        register("deepseek", new OpenAiCompatibleClient());
        // Claude 使用独立实现
        register("claude", new ClaudeClient());
        // 其他未知编码默认使用 OpenAI 兼容协议
        register("default", new OpenAiCompatibleClient());
    }

    /**
     * 注册客户端
     */
    public static void register(String providerCode, AiClient client)
    {
        CLIENTS.put(providerCode.toLowerCase(), client);
    }

    /**
     * 根据提供商编码获取客户端
     */
    public static AiClient getClient(String providerCode)
    {
        if (providerCode == null)
        {
            return CLIENTS.get("default");
        }
        AiClient client = CLIENTS.get(providerCode.toLowerCase());
        return client != null ? client : CLIENTS.get("default");
    }
}
