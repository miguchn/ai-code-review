package com.acr.common.enums;

/**
 * LLM 调用错误分类。
 */
public enum LlmCallErrorType
{
    AUTH("认证失败"),
    ADDRESS_ERROR("地址错误"),
    TIMEOUT("请求超时"),
    RATE_LIMIT("速率限制"),
    MODEL_NOT_FOUND("模型不存在"),
    NETWORK_ERROR("网络错误"),
    UNKNOWN("未知错误");

    private final String label;

    LlmCallErrorType(String label)
    {
        this.label = label;
    }

    public String getLabel()
    {
        return label;
    }
}
