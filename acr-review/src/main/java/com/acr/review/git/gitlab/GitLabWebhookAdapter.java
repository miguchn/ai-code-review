package com.acr.review.git.gitlab;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Base64;
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

/** GitLab Webhook 验签与载荷解析。 */
@Component
public class GitLabWebhookAdapter implements GitWebhookAdapter
{
    private static final String MERGE_REQUEST_HOOK = "Merge Request Hook";
    private static final String PUSH_HOOK = "Push Hook";
    private static final String OBJECT_KIND_MERGE_REQUEST = "merge_request";
    private static final String OBJECT_KIND_PUSH = "push";
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private static final String HEADER_EVENT = "X-Gitlab-Event";
    private static final String HEADER_DELIVERY = "X-Gitlab-Event-UUID";
    private static final String HEADER_TOKEN = "X-Gitlab-Token";
    private static final String HEADER_WEBHOOK_SIGNATURE = "Webhook-Signature";
    private static final String HEADER_GITLAB_WEBHOOK_SIGNATURE = "X-Gitlab-Webhook-Signature";

    @Override
    public String providerCode()
    {
        return "GITLAB";
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
        String eventType = headers == null ? null : headers.get(HEADER_EVENT);
        return synthesizeDeliveryId(eventType, payload);
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
        String signatureHeader = firstNonBlank(
            headers.get(HEADER_WEBHOOK_SIGNATURE),
            headers.get(HEADER_GITLAB_WEBHOOK_SIGNATURE));
        if (signatureHeader != null && !signatureHeader.isBlank())
        {
            return verifyWebhookSignature(secret, payload, signatureHeader);
        }
        String tokenHeader = headers.get(HEADER_TOKEN);
        if (tokenHeader != null)
        {
            return MessageDigest.isEqual(
                secret.getBytes(StandardCharsets.UTF_8),
                tokenHeader.getBytes(StandardCharsets.UTF_8));
        }
        return false;
    }

    @Override
    public boolean isPullRequestEventType(String eventType)
    {
        return MERGE_REQUEST_HOOK.equals(eventType);
    }

    @Override
    public boolean isPushEventType(String eventType)
    {
        return PUSH_HOOK.equals(eventType);
    }

    @Override
    public GitPushEvent parsePushEvent(String eventType, String deliveryId, byte[] payload)
    {
        if (!shouldParsePush(eventType, payload))
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
        JSONObject project = root.getJSONObject("project");
        String pathWithNamespace = project == null ? null : project.getString("path_with_namespace");
        if (pathWithNamespace == null || pathWithNamespace.isBlank())
        {
            return null;
        }
        int lastSlash = pathWithNamespace.lastIndexOf('/');
        if (lastSlash <= 0)
        {
            return null;
        }
        String owner = pathWithNamespace.substring(0, lastSlash);
        String repository = pathWithNamespace.substring(lastSlash + 1);
        boolean created = isZeroSha(before);
        boolean deleted = isZeroSha(after);
        return new GitPushEvent(deliveryId, owner, repository, pathWithNamespace, branch,
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
        JSONObject project = root.getJSONObject("project");
        String pathWithNamespace = project == null ? null : project.getString("path_with_namespace");
        if (pathWithNamespace == null || pathWithNamespace.isBlank())
        {
            return null;
        }
        int lastSlash = pathWithNamespace.lastIndexOf('/');
        if (lastSlash <= 0)
        {
            return null;
        }
        String owner = pathWithNamespace.substring(0, lastSlash);
        String repository = pathWithNamespace.substring(lastSlash + 1);
        String webUrl = project.getString("web_url");
        String canonicalUrl = webUrl;
        if (canonicalUrl == null || canonicalUrl.isBlank())
        {
            String homepage = project.getString("homepage");
            canonicalUrl = homepage != null && !homepage.isBlank() ? homepage
                : pathWithNamespace;
        }
        return new GitRepositoryCoordinates(owner, repository, pathWithNamespace, canonicalUrl);
    }

    @Override
    public GitPullRequestEvent parsePullRequestEvent(String eventType, String deliveryId, byte[] payload)
    {
        if (!shouldParseMergeRequest(eventType, payload))
        {
            return null;
        }
        JSONObject root = parseObject(payload);
        if (root == null)
        {
            return null;
        }
        JSONObject attrs = root.getJSONObject("object_attributes");
        JSONObject project = root.getJSONObject("project");
        if (attrs == null || project == null)
        {
            return null;
        }

        String pathWithNamespace = project.getString("path_with_namespace");
        Integer iid = attrs.getInteger("iid");
        String title = attrs.getString("title");
        String sourceBranch = attrs.getString("source_branch");
        String targetBranch = attrs.getString("target_branch");
        String action = mapAction(attrs.getString("action"), attrs.getString("oldrev"));
        String headSha = resolveHeadSha(attrs);
        String baseSha = resolveBaseSha(attrs);

        int lastSlash = pathWithNamespace == null ? -1 : pathWithNamespace.lastIndexOf('/');
        String owner = lastSlash > 0 ? pathWithNamespace.substring(0, lastSlash) : null;
        String repository = lastSlash > 0 ? pathWithNamespace.substring(lastSlash + 1) : null;

        JSONObject user = root.getJSONObject("user");
        String author = user == null ? null : user.getString("username");
        if (author == null || author.isBlank())
        {
            JSONObject authorObj = attrs.getJSONObject("author");
            author = authorObj == null ? null : authorObj.getString("username");
        }

        if (iid == null || sourceBranch == null || targetBranch == null || headSha == null || baseSha == null
            || pathWithNamespace == null || pathWithNamespace.isBlank()
            || owner == null || repository == null)
        {
            return null;
        }

        boolean merged = "merge".equals(action);
        return new GitPullRequestEvent(deliveryId, action, owner, repository, pathWithNamespace, iid, title,
            sourceBranch, targetBranch, baseSha, headSha, author, null, null, null, merged);
    }

    /** 供单测复用：判断 object_kind 形式的 push 事件。 */
    boolean isPushPayload(String eventType, byte[] payload)
    {
        return shouldParsePush(eventType, payload);
    }

    private static boolean shouldParsePush(String eventType, byte[] payload)
    {
        if (PUSH_HOOK.equals(eventType))
        {
            return true;
        }
        if (eventType != null && !eventType.isBlank())
        {
            return false;
        }
        JSONObject root = parseObject(payload);
        return root != null && OBJECT_KIND_PUSH.equals(root.getString("object_kind"));
    }

    /** 供单测复用：判断 object_kind 形式的 MR 事件。 */
    boolean isMergeRequestPayload(String eventType, byte[] payload)
    {
        return shouldParseMergeRequest(eventType, payload);
    }

    private static boolean shouldParseMergeRequest(String eventType, byte[] payload)
    {
        if (MERGE_REQUEST_HOOK.equals(eventType))
        {
            return true;
        }
        if (eventType != null && !eventType.isBlank())
        {
            return false;
        }
        JSONObject root = parseObject(payload);
        return root != null && OBJECT_KIND_MERGE_REQUEST.equals(root.getString("object_kind"));
    }

    /** 供单测复用：校验 Webhook-Signature 头（v1,base64）。 */
    boolean verifyWebhookSignature(String secret, byte[] payload, String signatureHeader)
    {
        if (secret == null || secret.isBlank() || payload == null
            || signatureHeader == null || !signatureHeader.startsWith("v1,"))
        {
            return false;
        }
        String encoded = signatureHeader.substring(3);
        byte[] expected;
        try
        {
            expected = Base64.getDecoder().decode(encoded);
        }
        catch (IllegalArgumentException ex)
        {
            return false;
        }
        try
        {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return MessageDigest.isEqual(mac.doFinal(payload), expected);
        }
        catch (GeneralSecurityException e)
        {
            return false;
        }
    }

    static String mapAction(String rawAction, String oldrev)
    {
        if (rawAction == null)
        {
            return null;
        }
        return switch (rawAction)
        {
            case "open" -> "opened";
            case "reopen" -> "reopened";
            case "update" -> (oldrev != null && !oldrev.isBlank()) ? "synchronize" : "update";
            default -> rawAction;
        };
    }

    static String synthesizeDeliveryId(String eventType, byte[] payload)
    {
        JSONObject root = parseObject(payload);
        if (root != null)
        {
            JSONObject project = root.getJSONObject("project");
            String fullPath = project == null ? "" : nullToEmpty(project.getString("path_with_namespace"));
            JSONObject attrs = root.getJSONObject("object_attributes");
            String iid = attrs == null ? "" : nullToEmpty(String.valueOf(attrs.get("iid")));
            String action = attrs == null ? nullToEmpty(root.getString("action")) : nullToEmpty(attrs.getString("action"));
            String headSha = attrs == null ? "" : nullToEmpty(resolveHeadSha(attrs));
            String material = nullToEmpty(eventType) + "|" + fullPath + "|" + iid + "|" + action + "|" + headSha;
            return sha256Hex(material.getBytes(StandardCharsets.UTF_8));
        }
        if (payload == null || payload.length == 0)
        {
            return sha256Hex(new byte[0]);
        }
        return sha256Hex(payload);
    }

    private static String resolveHeadSha(JSONObject attrs)
    {
        JSONObject diffRefs = attrs.getJSONObject("diff_refs");
        if (diffRefs != null)
        {
            String head = diffRefs.getString("head_sha");
            if (head != null && !head.isBlank())
            {
                return head;
            }
        }
        JSONObject lastCommit = attrs.getJSONObject("last_commit");
        if (lastCommit != null)
        {
            String id = lastCommit.getString("id");
            if (id != null && !id.isBlank())
            {
                return id;
            }
        }
        return attrs.getString("sha");
    }

    private static String resolveBaseSha(JSONObject attrs)
    {
        JSONObject diffRefs = attrs.getJSONObject("diff_refs");
        if (diffRefs != null)
        {
            String base = diffRefs.getString("base_sha");
            if (base != null && !base.isBlank())
            {
                return base;
            }
        }
        return attrs.getString("target_branch_sha");
    }

    private static String sha256Hex(byte[] input)
    {
        try
        {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(input);
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest)
            {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        }
        catch (GeneralSecurityException e)
        {
            throw new IllegalStateException("SHA-256 不可用", e);
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
        String username = root.getString("user_username");
        if (username != null && !username.isBlank())
        {
            return username;
        }
        String name = root.getString("user_name");
        return name == null || name.isBlank() ? null : name;
    }

    private static Integer resolveCommitCount(JSONObject root)
    {
        Integer total = root.getInteger("total_commits_count");
        if (total != null)
        {
            return total;
        }
        JSONArray commits = root.getJSONArray("commits");
        return commits == null ? null : commits.size();
    }

    private static String resolveHeadCommitMessage(JSONObject root)
    {
        JSONArray commits = root.getJSONArray("commits");
        if (commits == null || commits.isEmpty())
        {
            return null;
        }
        JSONObject last = commits.getJSONObject(commits.size() - 1);
        return last == null ? null : last.getString("message");
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

    private static String nullToEmpty(String value)
    {
        return value == null ? "" : value;
    }

    private static String firstNonBlank(String first, String second)
    {
        if (first != null && !first.isBlank())
        {
            return first;
        }
        if (second != null && !second.isBlank())
        {
            return second;
        }
        return null;
    }
}
