package com.acr.common.security;

import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** LLM API Key AES-256-GCM 加解密。 */
@Component
public class LlmApiKeyCryptoService
{
    private static final byte[] AAD = "acr-system:llm-api-key:v1".getBytes(StandardCharsets.UTF_8);

    private final AesGcmStringCrypto crypto;

    public LlmApiKeyCryptoService(@Value("${review.credential.master-key:}") String masterKeyBase64)
    {
        this.crypto = new AesGcmStringCrypto(AesGcmStringCrypto.decodeKey(masterKeyBase64), AAD);
    }

    public String encrypt(String plaintext)
    {
        return crypto.encrypt(plaintext, "API Key 不能为空");
    }

    public String decrypt(String ciphertext)
    {
        return crypto.decrypt(ciphertext);
    }

    public boolean isEncrypted(String value)
    {
        return AesGcmStringCrypto.looksEncrypted(value);
    }
}
