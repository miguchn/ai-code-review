package com.acr.review.git.gitea;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;
import com.acr.review.git.GitPullRequestEvent;
import com.acr.review.git.GitRepositoryCoordinates;
import com.acr.review.git.GitWebhookAdapter;
import com.acr.review.git.WebhookRequestHeaders;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;

/** Gitea Webhook 验签与载荷解析。 */
@Component
public class GiteaWebhookAdapter implements GitWebhookAdapter
{
    private static final String PULL_REQUEST_EVENT = "pull_request";
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String GITHUB_SIGNATURE_PREFIX = "sha256=";

    private static final String HEADER_EVENT = "X-Gitea-Event";
    private static final String HEADER_DELIVERY = "X-Gitea-Delivery";
    private static final String HEADER_SIGNATURE = "X-Gitea-Signature";
    private static final String HEADER_GITHUB_SIGNATURE = "X-Hub-Signature-256";

    @Override
    public String providerCode()
    {
        return "GITEA";
    }

    @Override
    public String resolveDeliveryId(WebhookRequestHeaders headers, byte[] payload)
    {
        if (headers != null)
        {
            String deliveryId = headers.get(HEADER_DELIVERY);
            if (deliveryId != null && !deliveryId.isBlank())
            {
                return deliveryId;
            }
        }
        return null;
    }

    @Override
    public String resolveEventType(WebhookRequestHeaders headers)
    {
        return headers == null ? null : headers.get(HEADER_EVENT);
    }

    @Override
    public boolean verify(String secret, byte[] payload, WebhookRequestHeaders headers)
    {
        if (secret == null || secret.isBlank() || payload == null || headers == null)
        {
            return false;
        }
        String giteaSignature = headers.get(HEADER_SIGNATURE);
        if (giteaSignature != null && !giteaSignature.isBlank())
        {
            return verifyGiteaSignature(secret, payload, giteaSignature);
        }
        String githubSignature = headers.get(HEADER_GITHUB_SIGNATURE);
        if (githubSignature != null && !githubSignature.isBlank())
        {
            return verifyGithubCompatSignature(secret, payload, githubSignature);
        }
        return false;
    }

    @Override
    public boolean isPullRequestEventType(String eventType)
    {
        return PULL_REQUEST_EVENT.equals(eventType);
    }

    @Override
    public GitRepositoryCoordinates parseRepository(byte[] payload)
    {
        JSONObject root = parseObject(payload);
        if (root == null)
        {
            return null;
        }
        JSONObject repository = root.getJSONObject("repository");
        if (repository == null)
        {
            return null;
        }
        String fullName = repository.getString("full_name");
        if (fullName == null || fullName.isBlank())
        {
            return null;
        }
        int lastSlash = fullName.lastIndexOf('/');
        if (lastSlash <= 0)
        {
            return null;
        }
        String owner = fullName.substring(0, lastSlash);
        String name = fullName.substring(lastSlash + 1);
        String htmlUrl = repository.getString("html_url");
        String canonicalUrl = htmlUrl != null && !htmlUrl.isBlank() ? htmlUrl : fullName;
        return new GitRepositoryCoordinates(owner, name, fullName, canonicalUrl);
    }

    @Override
    public GitPullRequestEvent parsePullRequestEvent(String eventType, String deliveryId, byte[] payload)
    {
        if (!PULL_REQUEST_EVENT.equals(eventType))
        {
            return null;
        }
        JSONObject root = parseObject(payload);
        if (root == null)
        {
            return null;
        }
        JSONObject pr = root.getJSONObject("pull_request");
        JSONObject repository = root.getJSONObject("repository");
        if (pr == null || repository == null)
        {
            return null;
        }

        String fullName = repository.getString("full_name");
        if (fullName == null || fullName.isBlank())
        {
            return null;
        }
        int lastSlash = fullName.lastIndexOf('/');
        if (lastSlash <= 0)
        {
            return null;
        }
        String ownerLogin = fullName.substring(0, lastSlash);
        String repoName = fullName.substring(lastSlash + 1);

        JSONObject base = pr.getJSONObject("base");
        JSONObject head = pr.getJSONObject("head");
        JSONObject user = pr.getJSONObject("user");
        Integer prNumber = pr.getInteger("number") != null ? pr.getInteger("number") : root.getInteger("number");
        String baseRef = base == null ? null : base.getString("ref");
        String baseSha = base == null ? null : base.getString("sha");
        String headRef = head == null ? null : head.getString("ref");
        String headSha = head == null ? null : head.getString("sha");
        String prAuthor = user == null ? null : user.getString("login");
        Integer additions = pr.getInteger("additions");
        Integer deletions = pr.getInteger("deletions");
        Integer changedFiles = pr.getInteger("changed_files");
        String action = mapAction(root.getString("action"));

        if (prNumber == null || baseRef == null || baseSha == null || headRef == null || headSha == null
            || repoName.isBlank() || ownerLogin.isBlank())
        {
            return null;
        }

        boolean merged = Boolean.TRUE.equals(pr.getBoolean("merged")) || "merged".equals(action);
        return new GitPullRequestEvent(deliveryId, action, ownerLogin, repoName, fullName, prNumber,
            pr.getString("title"), headRef, baseRef, baseSha, headSha, prAuthor, additions, deletions, changedFiles,
            merged);
    }

    /** 供单测复用：校验 X-Gitea-Signature（HMAC-SHA256 hex）。 */
    boolean verifyGiteaSignature(String secret, byte[] payload, String signatureHeader)
    {
        if (secret == null || secret.isBlank() || payload == null
            || signatureHeader == null || signatureHeader.isBlank())
        {
            return false;
        }
        byte[] actual = hexToBytes(signatureHeader);
        if (actual == null)
        {
            return false;
        }
        return hmacEquals(secret, payload, actual);
    }

    /** 供单测复用：校验 X-Hub-Signature-256（sha256= 前缀）。 */
    boolean verifyGithubCompatSignature(String secret, byte[] payload, String signatureHeader)
    {
        if (secret == null || secret.isBlank() || payload == null
            || signatureHeader == null || !signatureHeader.startsWith(GITHUB_SIGNATURE_PREFIX))
        {
            return false;
        }
        byte[] actual = hexToBytes(signatureHeader.substring(GITHUB_SIGNATURE_PREFIX.length()));
        if (actual == null)
        {
            return false;
        }
        return hmacEquals(secret, payload, actual);
    }

    static String mapAction(String rawAction)
    {
        if (rawAction == null)
        {
            return null;
        }
        if ("synchronized".equals(rawAction))
        {
            return "synchronize";
        }
        return rawAction;
    }

    private static boolean hmacEquals(String secret, byte[] payload, byte[] actual)
    {
        try
        {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return MessageDigest.isEqual(mac.doFinal(payload), actual);
        }
        catch (GeneralSecurityException e)
        {
            return false;
        }
    }

    private static JSONObject parseObject(byte[] payload)
    {
        if (payload == null || payload.length == 0)
        {
            return null;
        }
        try
        {
            return JSON.parseObject(new String(payload, StandardCharsets.UTF_8));
        }
        catch (RuntimeException e)
        {
            return null;
        }
    }

    private static byte[] hexToBytes(String hex)
    {
        if (hex == null || hex.length() % 2 != 0)
        {
            return null;
        }
        try
        {
            byte[] bytes = new byte[hex.length() / 2];
            for (int i = 0; i < bytes.length; i++)
            {
                bytes[i] = (byte) Integer.parseInt(hex.substring(i * 2, i * 2 + 2), 16);
            }
            return bytes;
        }
        catch (NumberFormatException e)
        {
            return null;
        }
    }
}
