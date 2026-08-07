package com.acr.review.git.github;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import com.acr.review.git.GitPullRequestEvent;
import com.acr.review.git.GitPushEvent;
import com.acr.review.git.GitRepositoryCoordinates;
import com.acr.review.git.WebhookRequestHeaders;

class GitHubWebhookAdapterTest
{
    private static final String SECRET = "test-webhook-secret";

    private static final String PR_PAYLOAD = """
        {
          "action": "opened",
          "number": 12,
          "pull_request": {
            "number": 12,
            "title": "feat: add login page",
            "user": { "login": "alice" },
            "additions": 20,
            "deletions": 4,
            "changed_files": 3,
            "base": { "ref": "dev", "sha": "aaaabbbbccccddddeeeeffff0000111122223333" },
            "head": { "ref": "feature/login", "sha": "ffffeeeeddddccccbbbbaaaa3333222211110000" }
          },
          "repository": {
            "name": "demo-repo",
            "owner": { "login": "miguchn" }
          }
        }
        """;

    private static final String PUSH_PAYLOAD = """
        {
          "ref": "refs/heads/main",
          "before": "aaaabbbbccccddddeeeeffff0000111122223333",
          "after": "ffffeeeeddddccccbbbbaaaa3333222211110000",
          "created": false,
          "deleted": false,
          "pusher": { "name": "alice", "email": "a@x.com" },
          "sender": { "login": "alice" },
          "commits": [
            { "id": "aaaabbbbccccddddeeeeffff0000111122223333", "message": "first", "distinct": true },
            { "id": "ffffeeeeddddccccbbbbaaaa3333222211110000", "message": "second fix", "distinct": true }
          ],
          "head_commit": { "id": "ffffeeeeddddccccbbbbaaaa3333222211110000", "message": "second fix" },
          "repository": {
            "name": "demo-repo",
            "owner": { "login": "miguchn", "name": "miguchn" },
            "full_name": "miguchn/demo-repo"
          }
        }
        """;

    private final GitHubWebhookAdapter adapter = new GitHubWebhookAdapter();

    @Test
    void verifiesValidSignature() throws Exception
    {
        byte[] payload = PR_PAYLOAD.getBytes(StandardCharsets.UTF_8);
        String signature = "sha256=" + hmacHex(SECRET, payload);

        assertTrue(adapter.verify(SECRET, payload, headers("X-Hub-Signature-256", signature)));
    }

    @Test
    void rejectsWrongSecret() throws Exception
    {
        byte[] payload = PR_PAYLOAD.getBytes(StandardCharsets.UTF_8);
        String signature = "sha256=" + hmacHex("other-secret", payload);

        assertFalse(adapter.verify(SECRET, payload, headers("X-Hub-Signature-256", signature)));
    }

    @Test
    void rejectsForgedAndMalformedSignature() throws Exception
    {
        byte[] payload = PR_PAYLOAD.getBytes(StandardCharsets.UTF_8);

        assertFalse(adapter.verify(SECRET, payload,
            headers("X-Hub-Signature-256", "sha256=0000000000000000000000000000000000000000000000000000000000000000")));
        assertFalse(adapter.verify(SECRET, payload, headers("X-Hub-Signature-256", "sha1=abc")));
        assertFalse(adapter.verify(SECRET, payload, WebhookRequestHeaders.empty()));
        assertFalse(adapter.verify(SECRET, payload, headers("X-Hub-Signature-256", "sha256=not-hex")));
        assertFalse(adapter.verify(null, payload, headers("X-Hub-Signature-256", "sha256=abc")));
    }

    @Test
    void resolvesDeliveryIdAndEventTypeFromHeaders()
    {
        WebhookRequestHeaders headers = WebhookRequestHeaders.of(Map.of(
            "X-GitHub-Delivery", "delivery-42",
            "X-GitHub-Event", "pull_request"));

        assertEquals("delivery-42", adapter.resolveDeliveryId(headers, PR_PAYLOAD.getBytes(StandardCharsets.UTF_8)));
        assertEquals("pull_request", adapter.resolveEventType(headers));
        assertTrue(adapter.isPullRequestEventType("pull_request"));
        assertFalse(adapter.isPullRequestEventType("push"));
        assertTrue(adapter.isPushEventType("push"));
        assertFalse(adapter.isPushEventType("pull_request"));
    }

    @Test
    void parsesRepositoryFromPayload()
    {
        GitRepositoryCoordinates repository = adapter.parseRepository(PR_PAYLOAD.getBytes(StandardCharsets.UTF_8));

        assertNotNull(repository);
        assertEquals("miguchn", repository.owner());
        assertEquals("demo-repo", repository.repository());
        assertEquals("miguchn/demo-repo", repository.fullPath());
    }

    @Test
    void returnsNullRepositoryForInvalidPayload()
    {
        assertNull(adapter.parseRepository("not-json".getBytes(StandardCharsets.UTF_8)));
        assertNull(adapter.parseRepository("{\"hook\":{}}".getBytes(StandardCharsets.UTF_8)));
        assertNull(adapter.parseRepository(new byte[0]));
    }

    @Test
    void parsesPullRequestEvent()
    {
        GitPullRequestEvent event = adapter.parsePullRequestEvent(
            "pull_request", "delivery-1", PR_PAYLOAD.getBytes(StandardCharsets.UTF_8));

        assertNotNull(event);
        assertEquals("delivery-1", event.deliveryId());
        assertEquals("opened", event.action());
        assertEquals("miguchn", event.repositoryOwner());
        assertEquals("demo-repo", event.repositoryName());
        assertEquals("miguchn/demo-repo", event.repositoryFullPath());
        assertEquals(12, event.prNumber());
        assertEquals("feat: add login page", event.prTitle());
        assertEquals("feature/login", event.sourceBranch());
        assertEquals("dev", event.targetBranch());
        assertEquals("aaaabbbbccccddddeeeeffff0000111122223333", event.baseSha());
        assertEquals(40, event.headSha().length());
        assertEquals("alice", event.prAuthor());
        assertEquals(20, event.additions());
        assertEquals(4, event.deletions());
        assertEquals(3, event.changedFiles());
    }

    @Test
    void returnsNullEventForNonPullRequestOrBrokenPayload()
    {
        assertNull(adapter.parsePullRequestEvent("ping", "d-1", PR_PAYLOAD.getBytes(StandardCharsets.UTF_8)));
        assertNull(adapter.parsePullRequestEvent("pull_request", "d-1", "broken".getBytes(StandardCharsets.UTF_8)));
        assertNull(adapter.parsePullRequestEvent("pull_request", "d-1",
            "{\"action\":\"opened\",\"repository\":{\"name\":\"r\",\"owner\":{\"login\":\"o\"}}}".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void parsesClosedEventWithMergedFlag()
    {
        String closedMerged = PR_PAYLOAD
            .replace("\"action\": \"opened\"", "\"action\": \"closed\"")
            .replace("\"changed_files\": 3,", "\"changed_files\": 3,\n            \"merged\": true,");
        GitPullRequestEvent mergedEvent = adapter.parsePullRequestEvent(
            "pull_request", "d-close", closedMerged.getBytes(StandardCharsets.UTF_8));
        assertNotNull(mergedEvent);
        assertEquals("closed", mergedEvent.action());
        assertTrue(mergedEvent.merged());
        assertTrue(mergedEvent.isCloseLifecycle());

        String closedOnly = PR_PAYLOAD
            .replace("\"action\": \"opened\"", "\"action\": \"closed\"")
            .replace("\"changed_files\": 3,", "\"changed_files\": 3,\n            \"merged\": false,");
        GitPullRequestEvent closedEvent = adapter.parsePullRequestEvent(
            "pull_request", "d-close2", closedOnly.getBytes(StandardCharsets.UTF_8));
        assertNotNull(closedEvent);
        assertEquals("closed", closedEvent.action());
        assertFalse(closedEvent.merged());
        assertTrue(closedEvent.isCloseLifecycle());
    }

    @Test
    void parsesNormalPushEvent()
    {
        GitPushEvent event = adapter.parsePushEvent(
            "push", "delivery-push-1", PUSH_PAYLOAD.getBytes(StandardCharsets.UTF_8));

        assertNotNull(event);
        assertEquals("delivery-push-1", event.deliveryId());
        assertEquals("miguchn", event.repositoryOwner());
        assertEquals("demo-repo", event.repositoryName());
        assertEquals("miguchn/demo-repo", event.repositoryFullPath());
        assertEquals("main", event.branch());
        assertEquals("aaaabbbbccccddddeeeeffff0000111122223333", event.beforeSha());
        assertEquals("ffffeeeeddddccccbbbbaaaa3333222211110000", event.afterSha());
        assertEquals("alice", event.pusher());
        assertEquals(2, event.commitCount());
        assertEquals("second fix", event.headCommitMessage());
        assertFalse(event.created());
        assertFalse(event.deleted());
    }

    @Test
    void parsesBranchDeletePushEvent()
    {
        String deletePayload = PUSH_PAYLOAD
            .replace("\"deleted\": false", "\"deleted\": true")
            .replace("\"after\": \"ffffeeeeddddccccbbbbaaaa3333222211110000\"",
                "\"after\": \"0000000000000000000000000000000000000000\"");
        GitPushEvent event = adapter.parsePushEvent(
            "push", "delivery-del", deletePayload.getBytes(StandardCharsets.UTF_8));

        assertNotNull(event);
        assertTrue(event.deleted());
        assertFalse(event.created());
        assertEquals("0000000000000000000000000000000000000000", event.afterSha());
    }

    @Test
    void parsesNewBranchPushEvent()
    {
        String createPayload = PUSH_PAYLOAD
            .replace("\"created\": false", "\"created\": true")
            .replace("\"before\": \"aaaabbbbccccddddeeeeffff0000111122223333\"",
                "\"before\": \"0000000000000000000000000000000000000000\"");
        GitPushEvent event = adapter.parsePushEvent(
            "push", "delivery-new", createPayload.getBytes(StandardCharsets.UTF_8));

        assertNotNull(event);
        assertTrue(event.created());
        assertFalse(event.deleted());
        assertEquals("0000000000000000000000000000000000000000", event.beforeSha());
    }

    @Test
    void returnsNullPushEventForNonPushOrBrokenPayload()
    {
        assertNull(adapter.parsePushEvent("pull_request", "d-1", PUSH_PAYLOAD.getBytes(StandardCharsets.UTF_8)));
        assertNull(adapter.parsePushEvent("push", "d-1", "broken".getBytes(StandardCharsets.UTF_8)));
        assertNull(adapter.parsePushEvent("push", "d-1",
            "{\"ref\":\"refs/tags/v1.0\",\"before\":\"aaa\",\"after\":\"bbb\",\"repository\":{\"name\":\"r\",\"owner\":{\"login\":\"o\"}}}"
                .getBytes(StandardCharsets.UTF_8)));
    }

    private static WebhookRequestHeaders headers(String name, String value)
    {
        return WebhookRequestHeaders.of(Map.of(name, value));
    }

    private static String hmacHex(String secret, byte[] payload) throws Exception
    {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] bytes = mac.doFinal(payload);
        StringBuilder hex = new StringBuilder(bytes.length * 2);
        for (byte b : bytes)
        {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }
}
