package com.acr.common.ai;

/**
 * AI Provider 配置信息
 */
public class AiProviderConfig
{
    /** 提供商名称 */
    private String providerName;
    /** 提供商编码 */
    private String providerCode;
    /** API 地址 */
    private String apiUrl;
    /** API 密钥 */
    private String apiKey;
    /** 模型名称 */
    private String model;
    /** Embedding 模型名称（OpenAI 兼容 embeddings 接口） */
    private String embeddingModel;
    /** Embedding 接口完整 URL；空则从 apiUrl 推导 */
    private String embeddingApiUrl;
    /** 超时时间(ms) */
    private int timeout;
    /** 最大 token 数 */
    private int maxTokens;
    /** Temperature */
    private Double temperature;
    /** 上下文长度 */
    private Integer contextLength;

    public String getProviderName() { return providerName; }
    public void setProviderName(String providerName) { this.providerName = providerName; }

    public String getProviderCode() { return providerCode; }
    public void setProviderCode(String providerCode) { this.providerCode = providerCode; }

    public String getApiUrl() { return apiUrl; }
    public void setApiUrl(String apiUrl) { this.apiUrl = apiUrl; }

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public String getEmbeddingModel() { return embeddingModel; }
    public void setEmbeddingModel(String embeddingModel) { this.embeddingModel = embeddingModel; }

    public String getEmbeddingApiUrl() { return embeddingApiUrl; }
    public void setEmbeddingApiUrl(String embeddingApiUrl) { this.embeddingApiUrl = embeddingApiUrl; }

    public int getTimeout() { return timeout; }
    public void setTimeout(int timeout) { this.timeout = timeout; }

    public int getMaxTokens() { return maxTokens; }
    public void setMaxTokens(int maxTokens) { this.maxTokens = maxTokens; }

    public Double getTemperature() { return temperature; }
    public void setTemperature(Double temperature) { this.temperature = temperature; }

    public Integer getContextLength() { return contextLength; }
    public void setContextLength(Integer contextLength) { this.contextLength = contextLength; }
}
