package com.acr.review.git.gitee;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Locale;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;
import com.acr.review.git.GitProviderCodes;
import com.acr.review.git.GitPullRequestEvent;
import com.acr.review.git.GitRepositoryCoordinates;
import com.acr.review.git.GitWebhookAdapter;
import com.acr.review.git.WebhookRequestHeaders;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;

/** Gitee Webhook 验签与载荷解析。Secret、签名与载荷结构不越出本包。 */
@Component
public class GiteeWebhookAdapter implements GitWebhookAdapter
{
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String HEADER_EVENT = "X-Gitee-Event";
    private static final String HEADER_TOKEN = "X-Gitee-Token";
    private static final String HEADER_TIMESTAMP = "X-Gitee-Timestamp";
    private static final String MERGE_REQUEST_HOOK = "Merge Request Hook";
    private static final String PULL_REQUEST_HOOK = "Pull Request Hook";
    private static final Duration MAX_TIMESTAMP_SKEW = Duration.ofHours(1);

    @Override
    public String providerCode()
    {
        return GitProviderCodes.GITEE;
    }

    @Override
    public String resolveDeliveryId(WebhookRequestHeaders headers, byte[] payload)
    {
        JSONObject root = parseObject(payload);
        if (root == null)
        {
            return null;
        }
        String eventType = headers == null ? null : headers.get(HEADER_EVENT);
        if (eventType == null)
        {
            eventType = "";
        }

        GitRepositoryCoordinates repository = parseRepository(payload);
        if (repository == null)
        {
            return null;
        }

        JSONObject pullRequest = root.getJSONObject("pull_request");
        Integer prNumber = pullRequest == null ? null : pullRequest.getInteger("number");
        if (prNumber == null)
        {
            prNumber = root.getInteger("number");
        }
        String action = root.getString("action");
        if (action == null)
        {
            action = root.getString("hook_name");
        }
        if (action == null)
        {
            action = "";
        }

        JSONObject head = pullRequest == null ? null : pullRequest.getJSONObject("head");
        String headSha = head == null ? null : head.getString("sha");
        if (headSha == null)
        {
            headSha = "";
        }
        if (prNumber == null)
        {
            return null;
        }

        String material = eventType + "|" + repository.fullPath() + "|" + prNumber + "|" + action + "|" + headSha;
        return sha256Hex(material);
    }

    @Override
    public String resolveEventType(WebhookRequestHeaders headers)
    {
        return headers == null ? null : headers.get(HEADER_EVENT);
    }

    @Override
    public boolean verify(String secret, byte[] payload, WebhookRequestHeaders headers)
    {
        if (secret == null || secret.isBlank() || headers == null)
        {
            return false;
        }
        String tokenHeader = headers.get(HEADER_TOKEN);
        if (tokenHeader == null || tokenHeader.isBlank())
        {
            return false;
        }

        String timestamp = headers.get(HEADER_TIMESTAMP);
        if (timestamp != null && !timestamp.isBlank())
        {
            if (!isTimestampWithinSkew(timestamp))
            {
                return false;
            }
            String expectedUrlEncoded = computeSignToken(timestamp, secret, true);
            String expectedRaw = computeSignToken(timestamp, secret, false);
            return constantTimeEquals(tokenHeader, expectedUrlEncoded)
                || constantTimeEquals(tokenHeader, expectedRaw);
        }

        return constantTimeEquals(tokenHeader, secret);
    }

    @Override
    public boolean isPullRequestEventType(String eventType)
    {
        if (eventType == null)
        {
            return false;
        }
        return MERGE_REQUEST_HOOK.equalsIgnoreCase(eventType)
            || PULL_REQUEST_HOOK.equalsIgnoreCase(eventType);
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
            fullName = repository.getString("path");
        }
        if (fullName == null || fullName.isBlank())
        {
            return null;
        }

        int slash = fullName.indexOf('/');
        if (slash <= 0 || slash >= fullName.length() - 1)
        {
            return null;
        }
        String owner = fullName.substring(0, slash);
        String name = fullName.substring(slash + 1);
        return new GitRepositoryCoordinates(owner, name, GitProviderCodes.DEFAULT_GITEE_SERVER + "/" + owner + "/" + name);
    }

    @Override
    public GitPullRequestEvent parsePullRequestEvent(String eventType, String deliveryId, byte[] payload)
    {
        if (!isPullRequestEventType(eventType))
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

        String rawAction = root.getString("action");
        if (rawAction == null)
        {
            rawAction = root.getString("hook_name");
        }
        String mappedAction = mapAction(rawAction);
        if (mappedAction == null)
        {
            return null;
        }

        JSONObject base = pr.getJSONObject("base");
        JSONObject head = pr.getJSONObject("head");
        JSONObject user = pr.getJSONObject("user");
        Integer prNumber = pr.getInteger("number") != null ? pr.getInteger("number") : root.getInteger("number");
        String baseRef = base == null ? null : base.getString("ref");
        String baseSha = base == null ? null : base.getString("sha");
        String headRef = head == null ? null : head.getString("ref");
        String headSha = head == null ? null : head.getString("sha");
        String repoName = repository.getString("name");
        String ownerLogin = parseOwner(repository);
        String prAuthor = user == null ? null : user.getString("login");
        Integer additions = pr.getInteger("additions");
        Integer deletions = pr.getInteger("deletions");
        Integer changedFiles = pr.getInteger("changed_files");
        if (prNumber == null || baseRef == null || baseSha == null || headRef == null || headSha == null
            || repoName == null || repoName.isBlank() || ownerLogin == null || ownerLogin.isBlank())
        {
            return null;
        }
        String repositoryFullPath = ownerLogin + "/" + repoName;
        return new GitPullRequestEvent(deliveryId, mappedAction, ownerLogin, repoName,
            repositoryFullPath, prNumber, pr.getString("title"), headRef, baseRef, baseSha, headSha,
            prAuthor, additions, deletions, changedFiles);
    }

    /** 供 verify 与单测复用：校验 X-Gitee-Token 头（密码或签名模式）。 */
    boolean verifyToken(String secret, String tokenHeader, String timestamp)
    {
        if (secret == null || secret.isBlank() || tokenHeader == null || tokenHeader.isBlank())
        {
            return false;
        }
        if (timestamp != null && !timestamp.isBlank())
        {
            if (!isTimestampWithinSkew(timestamp))
            {
                return false;
            }
            String expectedUrlEncoded = computeSignToken(timestamp, secret, true);
            String expectedRaw = computeSignToken(timestamp, secret, false);
            return constantTimeEquals(tokenHeader, expectedUrlEncoded)
                || constantTimeEquals(tokenHeader, expectedRaw);
        }
        return constantTimeEquals(tokenHeader, secret);
    }

    static String mapAction(String rawAction)
    {
        if (rawAction == null || rawAction.isBlank())
        {
            return null;
        }
        String normalized = rawAction.toLowerCase(Locale.ROOT);
        return switch (normalized)
        {
            case "open", "opened" -> "opened";
            case "reopen", "reopened" -> "reopened";
            case "update", "push_update", "synchronize" -> "synchronize";
            // 无法映射的动作原样透传：由服务端白名单判为 IGNORED，而非按载荷解析失败记 FAILED
            default -> normalized;
        };
    }

    static String computeSignToken(String timestamp, String secret, boolean urlEncode)
    {
        try
        {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            byte[] digest = mac.doFinal((timestamp + "\n" + secret).getBytes(StandardCharsets.UTF_8));
            String base64 = Base64.getEncoder().encodeToString(digest);
            if (!urlEncode)
            {
                return base64;
            }
            return URLEncoder.encode(base64, StandardCharsets.UTF_8);
        }
        catch (GeneralSecurityException e)
        {
            return "";
        }
    }

    private static String parseOwner(JSONObject repository)
    {
        JSONObject owner = repository.getJSONObject("owner");
        if (owner != null)
        {
            String login = owner.getString("login");
            if (login != null && !login.isBlank())
            {
                return login;
            }
        }
        String fullName = repository.getString("full_name");
        if (fullName == null)
        {
            fullName = repository.getString("path");
        }
        if (fullName == null)
        {
            return null;
        }
        int slash = fullName.indexOf('/');
        return slash > 0 ? fullName.substring(0, slash) : null;
    }

    private static boolean isTimestampWithinSkew(String timestamp)
    {
        try
        {
            long epochSeconds = Long.parseLong(timestamp.trim());
            Instant eventTime = Instant.ofEpochSecond(epochSeconds);
            Instant now = Instant.now();
            return !eventTime.isBefore(now.minus(MAX_TIMESTAMP_SKEW))
                && !eventTime.isAfter(now.plus(MAX_TIMESTAMP_SKEW));
        }
        catch (NumberFormatException e)
        {
            return false;
        }
    }

    private static boolean constantTimeEquals(String left, String right)
    {
        if (left == null || right == null)
        {
            return false;
        }
        return MessageDigest.isEqual(left.getBytes(StandardCharsets.UTF_8), right.getBytes(StandardCharsets.UTF_8));
    }

    private static String sha256Hex(String material)
    {
        try
        {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(material.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash)
            {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        }
        catch (GeneralSecurityException e)
        {
            return null;
        }
    }

    private JSONObject parseObject(byte[] payload)
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
}
