package com.acr.review.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class OcrModelConfigMapperTest
{
    private final OcrModelConfigMapper mapper = new OcrModelConfigMapper();

    @Test
    void mapsAnthropicProviderToOcrEnvironment()
    {
        var config = new com.acr.system.domain.SysAiModelConfig();
        config.setProvider("claude");
        config.setApiUrl("https://api.anthropic.com");
        config.setApiKey("secret-key");
        config.setModel("claude-opus-4-6");

        var env = mapper.toEnvironment(config);
        assertEquals("https://api.anthropic.com/v1/messages", env.get("OCR_LLM_URL"));
        assertEquals("secret-key", env.get("OCR_LLM_TOKEN"));
        assertEquals("claude-opus-4-6", env.get("OCR_LLM_MODEL"));
        assertEquals("true", env.get("OCR_USE_ANTHROPIC"));
    }

    @Test
    void mapsOpenAiCompatibleProviderToOcrEnvironment()
    {
        var config = new com.acr.system.domain.SysAiModelConfig();
        config.setProvider("deepseek");
        config.setApiUrl("https://api.deepseek.com");
        config.setApiKey("secret-key");
        config.setModel("deepseek-chat");

        var env = mapper.toEnvironment(config);
        assertEquals("https://api.deepseek.com/v1/chat/completions", env.get("OCR_LLM_URL"));
        assertEquals("false", env.get("OCR_USE_ANTHROPIC"));
    }

    @Test
    void stripsMistakenOpenAiSuffixForAnthropicUrl()
    {
        var config = new com.acr.system.domain.SysAiModelConfig();
        config.setProvider("claude");
        config.setApiUrl("https://api.anthropic.com/v1/chat/completions");
        config.setApiKey("secret-key");
        config.setModel("claude-3-5-sonnet-latest");

        var env = mapper.toEnvironment(config);
        assertEquals("https://api.anthropic.com/v1/messages", env.get("OCR_LLM_URL"));
        assertEquals("true", env.get("OCR_USE_ANTHROPIC"));
    }
}
