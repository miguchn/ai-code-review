package com.acr.review.security;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.acr.common.exception.ServiceException;

/** Git 凭据专用 AES-256-GCM 加解密。 */
@Component
public class CredentialCryptoService
{
    private static final String PREFIX = "v1:";
    private static final byte[] AAD = "acr-review:github-pat:v1".getBytes(StandardCharsets.UTF_8);
    private static final byte[] WEBHOOK_SECRET_AAD = "acr-review:github-webhook-secret:v1".getBytes(StandardCharsets.UTF_8);
    private static final byte[] NOTIFY_WEBHOOK_URL_AAD = "acr-review:notify-webhook-url:v1".getBytes(StandardCharsets.UTF_8);
    private static final byte[] NOTIFY_WEBHOOK_SECRET_AAD = "acr-review:notify-webhook-secret:v1".getBytes(StandardCharsets.UTF_8);
    private static final int IV_LENGTH = 12;
    private static final int TAG_LENGTH_BITS = 128;

    private final SecretKeySpec key;
    private final SecureRandom secureRandom = new SecureRandom();

    public CredentialCryptoService(@Value("${review.credential.master-key:}") String masterKeyBase64)
    {
        this.key = decodeKey(masterKeyBase64);
    }

    public String encrypt(String plaintext)
    {
        return encrypt(plaintext, AAD, "GitHub Token 不能为空");
    }

    public String decrypt(String ciphertext)
    {
        return decrypt(ciphertext, AAD);
    }

    /** Webhook Secret 加密，使用独立 AAD 与 PAT 密文隔离。 */
    public String encryptWebhookSecret(String plaintext)
    {
        return encrypt(plaintext, WEBHOOK_SECRET_AAD, "Webhook Secret 不能为空");
    }

    /** Webhook Secret 解密，仅供服务端验签使用。 */
    public String decryptWebhookSecret(String ciphertext)
    {
        return decrypt(ciphertext, WEBHOOK_SECRET_AAD);
    }

    /** 通知渠道 Webhook URL 加密（URL 含 access_token 等敏感参数）。 */
    public String encryptNotifyWebhookUrl(String plaintext)
    {
        return encrypt(plaintext, NOTIFY_WEBHOOK_URL_AAD, "通知 Webhook URL 不能为空");
    }

    /** 通知渠道 Webhook URL 解密，仅供服务端发送使用。 */
    public String decryptNotifyWebhookUrl(String ciphertext)
    {
        return decrypt(ciphertext, NOTIFY_WEBHOOK_URL_AAD);
    }

    /** 通知渠道加签 Secret 加密。 */
    public String encryptNotifyWebhookSecret(String plaintext)
    {
        return encrypt(plaintext, NOTIFY_WEBHOOK_SECRET_AAD, "通知加签 Secret 不能为空");
    }

    /** 通知渠道加签 Secret 解密，仅供服务端发送使用。 */
    public String decryptNotifyWebhookSecret(String ciphertext)
    {
        return decrypt(ciphertext, NOTIFY_WEBHOOK_SECRET_AAD);
    }

    private String encrypt(String plaintext, byte[] aad, String emptyMessage)
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
            throw new ServiceException("Git 凭据加密失败");
        }
    }

    private String decrypt(String ciphertext, byte[] aad)
    {
        requireConfiguredKey();
        if (ciphertext == null || !ciphertext.startsWith(PREFIX))
        {
            throw new ServiceException("Git 凭据密文格式无效");
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
            throw new ServiceException("Git 凭据无法解密，请检查主密钥配置");
        }
    }

    private void requireConfiguredKey()
    {
        if (key == null)
        {
            throw new ServiceException("未配置 Git 凭据主密钥 ACR_CREDENTIAL_MASTER_KEY");
        }
    }

    private static SecretKeySpec decodeKey(String masterKeyBase64)
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
}
