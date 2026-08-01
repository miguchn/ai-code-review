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
        if (stdout == null || stdout.isBlank())
        {
            return Map.of();
        }

        if (invocationType == ReviewEngineInvocationType.VERSION)
        {
            Map<String, Object> version = new HashMap<>();
            version.put("rawVersionLine", stdout.lines().findFirst().orElse(stdout).trim());
            return version;
        }

        String trimmed = stdout.trim();
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
