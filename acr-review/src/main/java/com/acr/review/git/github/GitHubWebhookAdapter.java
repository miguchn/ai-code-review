package com.acr.review.git.github;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;
import com.acr.review.git.GitPullRequestEvent;
import com.acr.review.git.GitPushCommitParser;
import com.acr.review.git.GitPushEvent;
import com.acr.review.git.GitRepositoryCoordinates;
import com.acr.review.git.GitWebhookAdapter;
import com.acr.review.git.WebhookRequestHeaders;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;

/** GitHub Webhook 验签与载荷解析。Secret、签名与载荷结构不越出本包。 */
@Component
public class GitHubWebhookAdapter implements GitWebhookAdapter
{
    private static final String SIGNATURE_PREFIX = "sha256=";
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String PULL_REQUEST_EVENT = "pull_request";
    private static final String PUSH_EVENT = "push";
    private static final String HEADER_SIGNATURE = "X-Hub-Signature-256";
    private static final String HEADER_DELIVERY = "X-GitHub-Delivery";
    private static final String HEADER_EVENT = "X-GitHub-Event";

    @Override
    public String providerCode()
    {
        return "GITHUB";
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
        String signatureHeader = headers == null ? null : headers.get(HEADER_SIGNATURE);
        return verifySignature(secret, payload, signatureHeader);
    }

    /**
     * GitHub 对 push/ping 等事件可能发送 {@code application/x-www-form-urlencoded}
     *（body 为 {@code payload=<URL编码JSON>}）。仅当整体以 {@code payload=} 开头时解包；
     * 纯 JSON（以 '{' 开头）原样返回。验签仍使用原始请求字节。
     */
    @Override
    public byte[] unwrapPayload(byte[] payload)
    {
        if (payload == null || payload.length == 0)
        {
            return payload;
        }
        // 纯 JSON 字节流必须原样返回；仅整体系 form（以 payload= 开头）才解包。
        if (payload[0] != 'p')
        {
            return payload;
        }
        String body = new String(payload, StandardCharsets.UTF_8);
        if (!body.startsWith("payload="))
        {
            return payload;
        }
        try
        {
            int valueStart = "payload=".length();
            int amp = body.indexOf('&', valueStart);
            String encoded = amp < 0 ? body.substring(valueStart) : body.substring(valueStart, amp);
            if (encoded.isEmpty())
            {
                return payload;
            }
            String decoded = URLDecoder.decode(encoded, StandardCharsets.UTF_8);
            if (decoded == null || decoded.isEmpty())
            {
                return payload;
            }
            return decoded.getBytes(StandardCharsets.UTF_8);
        }
        catch (RuntimeException ex)
        {
            return payload;
        }
    }

    @Override
    public boolean isPullRequestEventType(String eventType)
    {
        return PULL_REQUEST_EVENT.equals(eventType);
    }

    @Override
    public boolean isPushEventType(String eventType)
    {
        return PUSH_EVENT.equals(eventType);
    }

    @Override
    public GitPushEvent parsePushEvent(String eventType, String deliveryId, byte[] payload)
    {
        if (!PUSH_EVENT.equals(eventType))
        {
            return null;
        }
        JSONObject root = parseObject(payload);
        if (root == null)
        {
            return null;
        }
        String branch = stripHeadsBranchRef(root.getString("ref"));
        if (branch == null)
        {
            return null;
        }
        String before = root.getString("before");
        String after = root.getString("after");
        if (before == null || after == null)
        {
            return null;
        }
        JSONObject repository = root.getJSONObject("repository");
        if (repository == null)
        {
            return null;
        }
        JSONObject owner = repository.getJSONObject("owner");
        String repoName = repository.getString("name");
        String ownerLogin = owner == null ? null : owner.getString("login");
        if (repoName == null || repoName.isBlank() || ownerLogin == null || ownerLogin.isBlank())
        {
            return null;
        }
        boolean created = Boolean.TRUE.equals(root.getBoolean("created")) || isZeroSha(before);
        boolean deleted = Boolean.TRUE.equals(root.getBoolean("deleted")) || isZeroSha(after);
        return new GitPushEvent(deliveryId, ownerLogin, repoName, ownerLogin + "/" + repoName, branch,
            before, after, resolvePusher(root), resolveCommitCount(root), resolveHeadCommitMessage(root),
            created, deleted, GitPushCommitParser.parseCommits(root));
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
        JSONObject owner = repository == null ? null : repository.getJSONObject("owner");
        String name = repository == null ? null : repository.getString("name");
        String ownerLogin = owner == null ? null : owner.getString("login");
        if (name == null || name.isBlank() || ownerLogin == null || ownerLogin.isBlank())
        {
            return null;
        }
        return new GitRepositoryCoordinates(ownerLogin, name, "https://github.com/" + ownerLogin + "/" + name);
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
        JSONObject base = pr.getJSONObject("base");
        JSONObject head = pr.getJSONObject("head");
        JSONObject owner = repository.getJSONObject("owner");
        JSONObject user = pr.getJSONObject("user");
        Integer prNumber = pr.getInteger("number") != null ? pr.getInteger("number") : root.getInteger("number");
        String baseRef = base == null ? null : base.getString("ref");
        String baseSha = base == null ? null : base.getString("sha");
        String headRef = head == null ? null : head.getString("ref");
        String headSha = head == null ? null : head.getString("sha");
        String repoName = repository.getString("name");
        String ownerLogin = owner == null ? null : owner.getString("login");
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
        boolean merged = Boolean.TRUE.equals(pr.getBoolean("merged"));
        return new GitPullRequestEvent(deliveryId, root.getString("action"), ownerLogin, repoName,
            repositoryFullPath, prNumber, pr.getString("title"), headRef, baseRef, baseSha, headSha,
            prAuthor, additions, deletions, changedFiles, merged);
    }

    /** 供 verify 与单测复用：校验 X-Hub-Signature-256 头。 */
    boolean verifySignature(String secret, byte[] payload, String signatureHeader)
    {
        if (secret == null || secret.isBlank() || payload == null
            || signatureHeader == null || !signatureHeader.startsWith(SIGNATURE_PREFIX))
        {
            return false;
        }
        byte[] actual = hexToBytes(signatureHeader.substring(SIGNATURE_PREFIX.length()));
        if (actual == null)
        {
            return false;
        }
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

    private static String stripHeadsBranchRef(String ref)
    {
        if (ref == null || ref.isBlank())
        {
            return null;
        }
        String prefix = "refs/heads/";
        if (!ref.startsWith(prefix))
        {
            return null;
        }
        String branch = ref.substring(prefix.length());
        return branch.isBlank() ? null : branch;
    }

    private static boolean isZeroSha(String sha)
    {
        return sha == null || sha.isBlank() || sha.matches("^0+$");
    }

    private static String resolvePusher(JSONObject root)
    {
        JSONObject pusher = root.getJSONObject("pusher");
        if (pusher != null)
        {
            String name = pusher.getString("name");
            if (name != null && !name.isBlank())
            {
                return name;
            }
            String login = pusher.getString("login");
            if (login != null && !login.isBlank())
            {
                return login;
            }
        }
        JSONObject sender = root.getJSONObject("sender");
        if (sender != null)
        {
            String login = sender.getString("login");
            if (login != null && !login.isBlank())
            {
                return login;
            }
        }
        return null;
    }

    private static Integer resolveCommitCount(JSONObject root)
    {
        JSONArray commits = root.getJSONArray("commits");
        return commits == null ? null : commits.size();
    }

    private static String resolveHeadCommitMessage(JSONObject root)
    {
        JSONObject headCommit = root.getJSONObject("head_commit");
        if (headCommit != null)
        {
            String message = headCommit.getString("message");
            if (message != null && !message.isBlank())
            {
                return message;
            }
        }
        JSONArray commits = root.getJSONArray("commits");
        if (commits != null && !commits.isEmpty())
        {
            JSONObject last = commits.getJSONObject(commits.size() - 1);
            return last == null ? null : last.getString("message");
        }
        return null;
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
