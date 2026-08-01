package com.acr.common.security;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import com.acr.common.exception.ServiceException;

/**
 * AES-256-GCM 字符串加解密（v1 前缀 + Base64 载荷）。
 */
public final class AesGcmStringCrypto
{
    public static final String PREFIX = "v1:";
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH_BITS = 128;

    private final SecretKeySpec key;
    private final byte[] aad;
    private final SecureRandom secureRandom = new SecureRandom();

    public AesGcmStringCrypto(SecretKeySpec key, byte[] aad)
    {
        this.key = key;
        this.aad = aad;
    }

    public String encrypt(String plaintext, String emptyMessage)
    {
        requireConfiguredKey();
        if (plaintext == null || plaintext.isBlank())
        {
            throw new ServiceException(emptyMessage);
        }
        try
        {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            cipher.updateAAD(aad);
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] payload = ByteBuffer.allocate(iv.length + encrypted.length).put(iv).put(encrypted).array();
            return PREFIX + Base64.getEncoder().encodeToString(payload);
        }
        catch (GeneralSecurityException e)
        {
            throw new ServiceException("凭据加密失败");
        }
    }

    public String decrypt(String ciphertext)
    {
        requireConfiguredKey();
        if (ciphertext == null || !ciphertext.startsWith(PREFIX))
        {
            throw new ServiceException("凭据密文格式无效");
        }
        try
        {
            byte[] payload = Base64.getDecoder().decode(ciphertext.substring(PREFIX.length()));
            if (payload.length <= IV_LENGTH)
            {
                throw new GeneralSecurityException("invalid payload");
            }
            byte[] iv = new byte[IV_LENGTH];
            byte[] encrypted = new byte[payload.length - IV_LENGTH];
            System.arraycopy(payload, 0, iv, 0, IV_LENGTH);
            System.arraycopy(payload, IV_LENGTH, encrypted, 0, encrypted.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            cipher.updateAAD(aad);
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        }
        catch (GeneralSecurityException | IllegalArgumentException e)
        {
            throw new ServiceException("凭据无法解密，请检查主密钥配置");
        }
    }

    public static boolean looksEncrypted(String value)
    {
        return value != null && value.startsWith(PREFIX);
    }

    public static SecretKeySpec decodeKey(String masterKeyBase64)
    {
        if (masterKeyBase64 == null || masterKeyBase64.isBlank())
        {
            return null;
        }
        try
        {
            byte[] decoded = Base64.getDecoder().decode(masterKeyBase64.trim());
            if (decoded.length != 32)
            {
                throw new IllegalArgumentException("length");
            }
            return new SecretKeySpec(decoded, "AES");
        }
        catch (IllegalArgumentException e)
        {
            throw new IllegalStateException("ACR_CREDENTIAL_MASTER_KEY 必须是 Base64 编码的 32 字节密钥");
        }
    }

    private void requireConfiguredKey()
    {
        if (key == null)
        {
            throw new ServiceException("未配置凭据主密钥 ACR_CREDENTIAL_MASTER_KEY");
        }
    }
}
