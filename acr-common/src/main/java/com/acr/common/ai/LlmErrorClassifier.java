package com.acr.common.ai;

import java.net.SocketTimeoutException;
import java.util.Locale;
import com.acr.common.enums.LlmCallErrorType;

/**
 * LLM HTTP 调用错误分类器。
 */
public final class LlmErrorClassifier
{
    private LlmErrorClassifier()
    {
    }

    public static LlmCallErrorType classify(int httpStatus, String body, Exception exception)
    {
        if (exception != null)
        {
            if (exception instanceof SocketTimeoutException
                || containsAny(exception.getMessage(), "timeout", "timed out"))
            {
                return LlmCallErrorType.TIMEOUT;
            }
            if (containsAny(exception.getMessage(), "invalid url", "unexpected url", "expected url", "unsupported scheme"))
            {
                return LlmCallErrorType.ADDRESS_ERROR;
            }
            if (containsAny(exception.getMessage(), "unknown host", "connection refused", "connect timed out", "network"))
            {
                return LlmCallErrorType.NETWORK_ERROR;
            }
        }
        if (httpStatus == 401 || httpStatus == 403)
        {
            return LlmCallErrorType.AUTH;
        }
        if (httpStatus == 429)
        {
            return LlmCallErrorType.RATE_LIMIT;
        }
        if (httpStatus >= 400 && containsAny(body, "model")
            && containsAny(body, "not found", "does not exist", "invalid model", "unknown model"))
        {
            return LlmCallErrorType.MODEL_NOT_FOUND;
        }
        if (httpStatus == 404 || httpStatus == 502 || httpStatus == 503 || httpStatus == 504)
        {
            return LlmCallErrorType.ADDRESS_ERROR;
        }
        if (httpStatus >= 500)
        {
            return LlmCallErrorType.NETWORK_ERROR;
        }
        if (httpStatus >= 400)
        {
            return LlmCallErrorType.UNKNOWN;
        }
        return LlmCallErrorType.UNKNOWN;
    }

    public static String message(LlmCallErrorType errorType, int httpStatus, String body)
    {
        String base = errorType != null ? errorType.getLabel() : LlmCallErrorType.UNKNOWN.getLabel();
        if (httpStatus > 0)
        {
            base = base + " (HTTP " + httpStatus + ")";
        }
        String snippet = snippet(body, 120);
        if (snippet != null && !snippet.isBlank())
        {
            return base + ": " + snippet;
        }
        return base;
    }

    public static String snippet(String raw, int maxLen)
    {
        if (raw == null || raw.isBlank())
        {
            return null;
        }
        String sanitized = raw
            .replaceAll("(?i)bearer\\s+[a-zA-Z0-9._~+/=-]+", "Bearer ***")
            .replaceAll("(?i)sk-[a-zA-Z0-9_-]+", "sk-****")
            .replaceAll("(?i)(\"?(?:api[_-]?key|authorization|access[_-]?token|token)\"?\\s*[:=]\\s*\")([^\"]+)(\")",
                "$1***$3")
            .replaceAll("(?i)((?:api[_-]?key|authorization|access[_-]?token|token)\\s*[:=]\\s*)([^\\s,;}]+)",
                "$1***");
        return sanitized.length() <= maxLen ? sanitized : sanitized.substring(0, maxLen) + "...";
    }

    private static boolean containsAny(String value, String... needles)
    {
        if (value == null)
        {
            return false;
        }
        String lower = value.toLowerCase(Locale.ROOT);
        for (String needle : needles)
        {
            if (lower.contains(needle))
            {
                return true;
            }
        }
        return false;
    }
}
