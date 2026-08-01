package com.acr.common.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.Base64;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;

class ApiKeyMaskUtilsTest
{
    @Test
    void masksSkPrefixKeys()
    {
        assertEquals("sk-****cdef", ApiKeyMaskUtils.mask("sk-abcdefghijklmnopcdef"));
    }

    @Test
    void masksGenericKeys()
    {
        assertEquals("abcd****wxyz", ApiKeyMaskUtils.mask("abcdefghijklmnopwxyz"));
    }

    @Test
    void detectsMaskedOrBlank()
    {
        assertTrue(ApiKeyMaskUtils.isMaskedOrBlank(""));
        assertTrue(ApiKeyMaskUtils.isMaskedOrBlank("sk-****abcd"));
        assertFalse(ApiKeyMaskUtils.isMaskedOrBlank("sk-live-real-key"));
    }
}
