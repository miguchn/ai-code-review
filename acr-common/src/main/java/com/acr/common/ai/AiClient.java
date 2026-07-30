package com.acr.common.ai;

/**
 * AI 客户端统一接口
 */
public interface AiClient
{
    /**
     * 对话请求（返回纯文本）
     *
     * @param config AI Provider 配置
     * @param prompt 提示词
     * @return AI 回答文本
     */
    String chat(AiProviderConfig config, String prompt);

    /**
     * 对话请求（返回 JSON 格式响应）
     *
     * @param config AI Provider 配置
     * @param prompt 提示词
     * @return AI 返回的 JSON 字符串
     */
    String chatJson(AiProviderConfig config, String prompt);

    /**
     * 测试连接
     *
     * @param config AI Provider 配置
     * @return 连接测试结果
     */
    String testConnection(AiProviderConfig config);

    /**
     * 文本向量（OpenAI 兼容 {@code /v1/embeddings}）。不支持或失败时返回 {@code null}。
     *
     * @param config 须已设置 {@link AiProviderConfig#getEmbeddingModel()} 等
     * @param input  待编码文本
     * @return JSON 数组字符串，如 {@code [0.01,-0.02,...]}，与库存向量格式一致
     */
    default String embedAsJsonArray(AiProviderConfig config, String input)
    {
        return null;
    }
}
