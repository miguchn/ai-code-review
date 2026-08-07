package com.acr.review.git.gitee;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import com.acr.review.git.GitPullRequestEvent;
import com.acr.review.git.GitPushEvent;
import com.acr.review.git.GitRepositoryCoordinates;
import com.acr.review.git.WebhookRequestHeaders;

class GiteeWebhookAdapterTest
{
    private static final String SECRET = "test-webhook-secret";

    private static final String PR_PAYLOAD = """
        {
          "action": "open",
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
            "full_name": "miguchn/demo-repo",
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
            "full_name": "miguchn/demo-repo",
            "owner": { "login": "miguchn" }
          }
        }
        """;

    private final GiteeWebhookAdapter adapter = new GiteeWebhookAdapter();

    @Test
    void verifiesPasswordModeToken()
    {
        byte[] payload = PR_PAYLOAD.getBytes(StandardCharsets.UTF_8);

        assertTrue(adapter.verify(SECRET, payload, headers("X-Gitee-Token", SECRET)));
    }

    @Test
    void verifiesSignModeWithUrlEncodedToken()
    {
        byte[] payload = PR_PAYLOAD.getBytes(StandardCharsets.UTF_8);
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String token = GiteeWebhookAdapter.computeSignToken(timestamp, SECRET, true);

        WebhookRequestHeaders headers = WebhookRequestHeaders.of(Map.of(
            "X-Gitee-Token", token,
            "X-Gitee-Timestamp", timestamp));

        assertTrue(adapter.verify(SECRET, payload, headers));
    }

    @Test
    void verifiesSignModeWithRawBase64Token()
    {
        byte[] payload = PR_PAYLOAD.getBytes(StandardCharsets.UTF_8);
        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String token = GiteeWebhookAdapter.computeSignToken(timestamp, SECRET, false);

        WebhookRequestHeaders headers = WebhookRequestHeaders.of(Map.of(
            "X-Gitee-Token", token,
            "X-Gitee-Timestamp", timestamp));

        assertTrue(adapter.verify(SECRET, payload, headers));
    }

    @Test
    void rejectsWrongSecretAndBadSignature()
    {
        byte[] payload = PR_PAYLOAD.getBytes(StandardCharsets.UTF_8);

        assertFalse(adapter.verify(SECRET, payload, headers("X-Gitee-Token", "wrong-secret")));
        assertFalse(adapter.verify(SECRET, payload, WebhookRequestHeaders.empty()));

        String timestamp = String.valueOf(Instant.now().getEpochSecond());
        String badToken = GiteeWebhookAdapter.computeSignToken(timestamp, "other-secret", true);
        WebhookRequestHeaders headers = WebhookRequestHeaders.of(Map.of(
            "X-Gitee-Token", badToken,
            "X-Gitee-Timestamp", timestamp));
        assertFalse(adapter.verify(SECRET, payload, headers));
    }

    @Test
    void rejectsTimestampSkewBeyondOneHour()
    {
        byte[] payload = PR_PAYLOAD.getBytes(StandardCharsets.UTF_8);
        String timestamp = String.valueOf(Instant.now().minusSeconds(7200).getEpochSecond());
        String token = GiteeWebhookAdapter.computeSignToken(timestamp, SECRET, true);

        WebhookRequestHeaders headers = WebhookRequestHeaders.of(Map.of(
            "X-Gitee-Token", token,
            "X-Gitee-Timestamp", timestamp));

        assertFalse(adapter.verify(SECRET, payload, headers));
    }

    @Test
    void synthesizesDeliveryIdAs64Hex()
    {
        byte[] payload = PR_PAYLOAD.getBytes(StandardCharsets.UTF_8);
        WebhookRequestHeaders headers = WebhookRequestHeaders.of(Map.of("X-Gitee-Event", "Merge Request Hook"));

        String deliveryId = adapter.resolveDeliveryId(headers, payload);

        assertNotNull(deliveryId);
        assertEquals(64, deliveryId.length());
        assertTrue(deliveryId.matches("[0-9a-f]{64}"));

        String deliveryIdAgain = adapter.resolveDeliveryId(headers, payload);
        assertEquals(deliveryId, deliveryIdAgain);
    }

    @Test
    void resolvesEventTypeAndPullRequestHook()
    {
        WebhookRequestHeaders mergeHook = WebhookRequestHeaders.of(Map.of("X-Gitee-Event", "Merge Request Hook"));
        WebhookRequestHeaders pullHook = WebhookRequestHeaders.of(Map.of("X-Gitee-Event", "Pull Request Hook"));

        assertEquals("Merge Request Hook", adapter.resolveEventType(mergeHook));
        assertTrue(adapter.isPullRequestEventType("Merge Request Hook"));
        assertTrue(adapter.isPullRequestEventType("Pull Request Hook"));
        assertFalse(adapter.isPullRequestEventType("Push Hook"));
        assertTrue(adapter.isPushEventType("Push Hook"));
        assertTrue(adapter.isPushEventType("push hook"));
        assertFalse(adapter.isPushEventType("Merge Request Hook"));
    }

    @Test
    void parsesRepositoryFromFullNameOrPath()
    {
        GitRepositoryCoordinates fromFullName = adapter.parseRepository(PR_PAYLOAD.getBytes(StandardCharsets.UTF_8));

        assertNotNull(fromFullName);
        assertEquals("miguchn", fromFullName.owner());
        assertEquals("demo-repo", fromFullName.repository());
        assertEquals("miguchn/demo-repo", fromFullName.fullPath());

        String pathPayload = """
            {"repository":{"name":"demo-repo","path":"acme/demo-repo"}}
            """;
        GitRepositoryCoordinates fromPath = adapter.parseRepository(pathPayload.getBytes(StandardCharsets.UTF_8));
        assertNotNull(fromPath);
        assertEquals("acme", fromPath.owner());
        assertEquals("demo-repo", fromPath.repository());
    }

    @Test
    void mapsActionsToUnifiedValues()
    {
        assertEquals("opened", GiteeWebhookAdapter.mapAction("open"));
        assertEquals("opened", GiteeWebhookAdapter.mapAction("opened"));
        assertEquals("reopened", GiteeWebhookAdapter.mapAction("reopen"));
        assertEquals("reopened", GiteeWebhookAdapter.mapAction("reopened"));
        assertEquals("synchronize", GiteeWebhookAdapter.mapAction("update"));
        assertEquals("synchronize", GiteeWebhookAdapter.mapAction("push_update"));
        assertEquals("synchronize", GiteeWebhookAdapter.mapAction("synchronize"));
        assertEquals("merge", GiteeWebhookAdapter.mapAction("merge"));
        assertEquals("close", GiteeWebhookAdapter.mapAction("close"));
    }

    @Test
    void parsesPullRequestEventWithMappedAction()
    {
        byte[] payload = PR_PAYLOAD.getBytes(StandardCharsets.UTF_8);
        String deliveryId = adapter.resolveDeliveryId(
            WebhookRequestHeaders.of(Map.of("X-Gitee-Event", "Merge Request Hook")), payload);

        GitPullRequestEvent event = adapter.parsePullRequestEvent(
            "Merge Request Hook", deliveryId, payload);

        assertNotNull(event);
        assertEquals(deliveryId, event.deliveryId());
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
    void passesThroughUnmappedActionForWhitelistToIgnore()
    {
        String mergePayload = PR_PAYLOAD.replace("\"action\": \"open\"", "\"action\": \"merge\"");
        GitPullRequestEvent event = adapter.parsePullRequestEvent(
            "Merge Request Hook", "d-1", mergePayload.getBytes(StandardCharsets.UTF_8));
        assertNotNull(event);
        assertEquals("merge", event.action());
        assertTrue(event.merged());
        assertTrue(event.isCloseLifecycle());
    }

    @Test
    void parsesCloseActionAsCloseLifecycle()
    {
        String closePayload = PR_PAYLOAD.replace("\"action\": \"open\"", "\"action\": \"close\"");
        GitPullRequestEvent event = adapter.parsePullRequestEvent(
            "Merge Request Hook", "d-1", closePayload.getBytes(StandardCharsets.UTF_8));
        assertNotNull(event);
        assertEquals("close", event.action());
        assertFalse(event.merged());
        assertTrue(event.isCloseLifecycle());
    }

    @Test
    void returnsNullForInvalidPayload()
    {
        assertNull(adapter.parseRepository("not-json".getBytes(StandardCharsets.UTF_8)));
        assertNull(adapter.parsePullRequestEvent("Merge Request Hook", "d-1", "broken".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void parsesNormalPushEvent()
    {
        GitPushEvent event = adapter.parsePushEvent(
            "Push Hook", "delivery-push-1", PUSH_PAYLOAD.getBytes(StandardCharsets.UTF_8));

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
            "Push Hook", "delivery-del", deletePayload.getBytes(StandardCharsets.UTF_8));

        assertNotNull(event);
        assertTrue(event.deleted());
        assertFalse(event.created());
    }

    @Test
    void parsesNewBranchPushEvent()
    {
        String createPayload = PUSH_PAYLOAD
            .replace("\"created\": false", "\"created\": true")
            .replace("\"before\": \"aaaabbbbccccddddeeeeffff0000111122223333\"",
                "\"before\": \"0000000000000000000000000000000000000000\"");
        GitPushEvent event = adapter.parsePushEvent(
            "Push Hook", "delivery-new", createPayload.getBytes(StandardCharsets.UTF_8));

        assertNotNull(event);
        assertTrue(event.created());
        assertFalse(event.deleted());
    }

    @Test
    void returnsNullPushEventForNonPushOrBrokenPayload()
    {
        assertNull(adapter.parsePushEvent("Merge Request Hook", "d-1", PUSH_PAYLOAD.getBytes(StandardCharsets.UTF_8)));
        assertNull(adapter.parsePushEvent("Push Hook", "d-1", "broken".getBytes(StandardCharsets.UTF_8)));
        assertNull(adapter.parsePushEvent("Push Hook", "d-1",
            "{\"ref\":\"refs/tags/v1.0\",\"before\":\"aaa\",\"after\":\"bbb\",\"repository\":{\"name\":\"r\",\"full_name\":\"o/r\"}}"
                .getBytes(StandardCharsets.UTF_8)));
    }

    private static WebhookRequestHeaders headers(String name, String value)
    {
        return WebhookRequestHeaders.of(Map.of(name, value));
    }
}
