package com.acr.review.engine;

import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;
import com.acr.common.enums.LlmProviderCode;
import com.acr.common.utils.StringUtils;
import com.acr.system.domain.SysAiModelConfig;

/** 将平台模型配置映射为 OCR CLI 环境变量（不落日志）。 */
@Component
public class OcrModelConfigMapper
{
    public Map<String, String> toEnvironment(SysAiModelConfig config)
    {
        if (config == null)
        {
            throw new IllegalArgumentException("模型配置不能为空");
        }
        if (StringUtils.isEmpty(config.getApiUrl()) || StringUtils.isEmpty(config.getApiKey())
            || StringUtils.isEmpty(config.getModel()))
        {
            throw new IllegalArgumentException("模型地址、密钥和 Model 名称均不能为空");
        }

        LlmProviderCode provider = LlmProviderCode.fromCode(config.getProvider());
        boolean useAnthropic = provider == LlmProviderCode.CLAUDE;
        Map<String, String> env = new HashMap<>();
        env.put("OCR_LLM_URL", normalizeUrl(config.getApiUrl(), useAnthropic));
        env.put("OCR_LLM_TOKEN", config.getApiKey());
        env.put("OCR_LLM_MODEL", config.getModel());
        env.put("OCR_USE_ANTHROPIC", useAnthropic ? "true" : "false");
        return env;
    }

    String normalizeUrl(String apiUrl, boolean useAnthropic)
    {
        String trimmed = apiUrl.trim();
        if (useAnthropic)
        {
            return normalizeAnthropicUrl(trimmed);
        }
        if (trimmed.endsWith("/v1/chat/completions") || trimmed.endsWith("/chat/completions"))
        {
            return trimmed;
        }
        if (trimmed.endsWith("/"))
        {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        if (trimmed.endsWith("/v1"))
        {
            return trimmed + "/chat/completions";
        }
        return trimmed + "/v1/chat/completions";
    }

    /** 兼容用户误填 OpenAI Compatible 后缀的 Claude 地址。 */
    private String normalizeAnthropicUrl(String trimmed)
    {
        if (trimmed.endsWith("/v1/messages"))
        {
            return trimmed;
        }
        if (trimmed.endsWith("/v1/chat/completions"))
        {
            trimmed = trimmed.substring(0, trimmed.length() - "/v1/chat/completions".length());
        }
        else if (trimmed.endsWith("/chat/completions"))
        {
            trimmed = trimmed.substring(0, trimmed.length() - "/chat/completions".length());
        }
        if (trimmed.endsWith("/"))
        {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        if (trimmed.endsWith("/v1"))
        {
            return trimmed + "/messages";
        }
        return trimmed + "/v1/messages";
    }
}
