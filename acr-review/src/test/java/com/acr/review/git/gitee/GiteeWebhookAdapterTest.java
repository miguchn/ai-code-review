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

    private static WebhookRequestHeaders headers(String name, String value)
    {
        return WebhookRequestHeaders.of(Map.of(name, value));
    }
}
