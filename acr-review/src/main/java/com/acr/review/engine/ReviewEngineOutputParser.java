package com.acr.review.engine;

import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/** OCR CLI 输出最小结构化解析。 */
@Component
public class ReviewEngineOutputParser
{
    private final ObjectMapper objectMapper = new ObjectMapper();

    public Map<String, Object> parse(String stdout, ReviewEngineInvocationType invocationType)
    {
        String cleaned = AnsiTextCleaner.strip(stdout);
        if (cleaned == null || cleaned.isBlank())
        {
            return Map.of();
        }

        if (invocationType == ReviewEngineInvocationType.VERSION)
        {
            Map<String, Object> version = new HashMap<>();
            version.put("rawVersionLine", cleaned.lines().findFirst().orElse(cleaned).trim());
            return version;
        }

        String trimmed = cleaned.trim();
        if (trimmed.startsWith("{") || trimmed.startsWith("["))
        {
            try
            {
                return objectMapper.readValue(trimmed, new TypeReference<Map<String, Object>>() { });
            }
            catch (Exception ex)
            {
                throw new IllegalArgumentException("无法解析 JSON 输出: " + ex.getMessage(), ex);
            }
        }

        Map<String, Object> text = new HashMap<>();
        text.put("message", trimmed);
        return text;
    }
}
