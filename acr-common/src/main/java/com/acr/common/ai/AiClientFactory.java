package com.acr.common.ai;

/**
 * AI 客户端工厂 — 统一使用 OpenAI Compatible 协议，Provider 编码仅用于 UI 展示。
 */
public final class AiClientFactory
{
    private static final AiClient OPENAI_COMPATIBLE_CLIENT = new OpenAiCompatibleClient();

    private AiClientFactory()
    {
    }

    /**
     * 获取统一 OpenAI 兼容客户端。
     */
    public static AiClient getClient()
    {
        return OPENAI_COMPATIBLE_CLIENT;
    }
}
