package com.acr.common.enums;

/**
 * LLM 服务厂商标准编码（仅用于 UI 展示与未来扩展，调用协议统一 OpenAI Compatible）。
 */
public enum LlmProviderCode
{
    DEEPSEEK("deepseek", "DeepSeek", true),
    KIMI("kimi", "Kimi", true),
    QWEN("qwen", "通义千问", true),
    BAILIAN("bailian", "百炼", true),
    DOUBAO("doubao", "豆包", true),
    OPENAI("openai", "OpenAI", false),
    CLAUDE("claude", "Claude", false);

    private final String code;
    private final String label;
    private final boolean domestic;

    LlmProviderCode(String code, String label, boolean domestic)
    {
        this.code = code;
        this.label = label;
        this.domestic = domestic;
    }

    public String getCode()
    {
        return code;
    }

    public String getLabel()
    {
        return label;
    }

    public boolean isDomestic()
    {
        return domestic;
    }

    public static LlmProviderCode fromCode(String code)
    {
        if (code == null || code.isBlank())
        {
            return null;
        }
        for (LlmProviderCode provider : values())
        {
            if (provider.code.equalsIgnoreCase(code.trim()))
            {
                return provider;
            }
        }
        return null;
    }

    public static boolean isValid(String code)
    {
        return fromCode(code) != null;
    }
}
