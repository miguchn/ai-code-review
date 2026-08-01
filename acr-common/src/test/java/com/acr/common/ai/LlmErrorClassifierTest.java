package com.acr.common.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import java.net.SocketTimeoutException;
import org.junit.jupiter.api.Test;
import com.acr.common.enums.LlmCallErrorType;

class LlmErrorClassifierTest
{
    @Test
    void classifiesAuthErrors()
    {
        assertEquals(LlmCallErrorType.AUTH, LlmErrorClassifier.classify(401, null, null));
        assertEquals(LlmCallErrorType.AUTH, LlmErrorClassifier.classify(403, null, null));
    }

    @Test
    void classifiesTimeoutFromException()
    {
        assertEquals(LlmCallErrorType.TIMEOUT,
            LlmErrorClassifier.classify(0, null, new SocketTimeoutException("read timed out")));
    }

    @Test
    void classifiesModelNotFound()
    {
        assertEquals(LlmCallErrorType.MODEL_NOT_FOUND,
            LlmErrorClassifier.classify(404, "{\"error\":{\"message\":\"model not found\"}}", null));
    }

    @Test
    void snippetRedactsSecrets()
    {
        String snippet = LlmErrorClassifier.snippet(
            "{\"api_key\":\"plain-secret\",\"authorization\":\"Bearer opaque-token-value\"} sk-secret1234567890", 200);
        assertNotNull(snippet);
        assertFalse(snippet.contains("plain-secret"));
        assertFalse(snippet.contains("opaque-token-value"));
        assertFalse(snippet.contains("sk-secret1234567890"));
    }
}
