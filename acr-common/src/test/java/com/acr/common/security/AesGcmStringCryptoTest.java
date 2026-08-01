package com.acr.common.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;

class AesGcmStringCryptoTest
{
    private static final byte[] AAD = "test:aad:v1".getBytes(StandardCharsets.UTF_8);

    @Test
    void encryptsAndDecryptsRoundTrip()
    {
        byte[] raw = new byte[32];
        for (int i = 0; i < raw.length; i++)
        {
            raw[i] = (byte) i;
        }
        SecretKeySpec key = new SecretKeySpec(raw, "AES");
        AesGcmStringCrypto crypto = new AesGcmStringCrypto(key, AAD);
        String encrypted = crypto.encrypt("sk-test-secret-key", "empty");
        assertTrue(AesGcmStringCrypto.looksEncrypted(encrypted));
        assertEquals("sk-test-secret-key", crypto.decrypt(encrypted));
    }
}
