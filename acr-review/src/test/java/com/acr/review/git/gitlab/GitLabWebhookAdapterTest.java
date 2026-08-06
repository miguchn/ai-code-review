package com.acr.review.git.gitlab;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import com.acr.review.git.GitPullRequestEvent;
import com.acr.review.git.GitRepositoryCoordinates;
import com.acr.review.git.WebhookRequestHeaders;

class GitLabWebhookAdapterTest
{
    private static final String SECRET = "test-webhook-secret";

    private static final String MR_PAYLOAD = """
        {
          "object_kind": "merge_request",
          "event_type": "merge_request",
          "user": { "username": "alice" },
          "project": {
            "path_with_namespace": "miguchn/demo-repo",
            "web_url": "https://gitlab.example.com/miguchn/demo-repo"
          },
          "object_attributes": {
            "iid": 12,
            "title": "feat: add login page",
            "action": "open",
            "source_branch": "feature/login",
            "target_branch": "dev",
            "diff_refs": {
              "base_sha": "aaaabbbbccccddddeeeeffff0000111122223333",
              "head_sha": "ffffeeeeddddccccbbbbaaaa3333222211110000"
            }
          }
        }
        """;

    private final GitLabWebhookAdapter adapter = new GitLabWebhookAdapter();

    @Test
    void verifiesSecretToken() throws Exception
    {
        byte[] payload = MR_PAYLOAD.getBytes(StandardCharsets.UTF_8);
        assertTrue(adapter.verify(SECRET, payload, headers("X-Gitlab-Token", SECRET)));
        assertFalse(adapter.verify(SECRET, payload, headers("X-Gitlab-Token", "wrong")));
    }

    @Test
    void verifiesWebhookSignature() throws Exception
    {
        byte[] payload = MR_PAYLOAD.getBytes(StandardCharsets.UTF_8);
        String signature = "v1," + hmacBase64(SECRET, payload);
        assertTrue(adapter.verify(SECRET, payload, headers("Webhook-Signature", signature)));
        assertFalse(adapter.verify(SECRET, payload, headers("Webhook-Signature", "v1,AAAA")));
    }

    @Test
    void rejectsMissingVerificationHeaders()
    {
        byte[] payload = MR_PAYLOAD.getBytes(StandardCharsets.UTF_8);
        assertFalse(adapter.verify(SECRET, payload, WebhookRequestHeaders.empty()));
        assertFalse(adapter.verify(null, payload, headers("X-Gitlab-Token", SECRET)));
    }

    @Test
    void resolvesDeliveryIdFromUuidOrSynthesizes64Hex()
    {
        WebhookRequestHeaders withUuid = WebhookRequestHeaders.of(Map.of(
            "X-Gitlab-Event-UUID", "uuid-42",
            "X-Gitlab-Event", "Merge Request Hook"));
        byte[] payload = MR_PAYLOAD.getBytes(StandardCharsets.UTF_8);
        assertEquals("uuid-42", adapter.resolveDeliveryId(withUuid, payload));

        WebhookRequestHeaders withoutUuid = WebhookRequestHeaders.of(Map.of(
            "X-Gitlab-Event", "Merge Request Hook"));
        String synthesized = adapter.resolveDeliveryId(withoutUuid, payload);
        assertNotNull(synthesized);
        assertEquals(64, synthesized.length());
        assertTrue(synthesized.matches("[0-9a-f]{64}"));
    }

    @Test
    void mapsUpdateWithoutOldrevToUpdateNotSynchronize()
    {
        String payload = """
            {
              "object_kind": "merge_request",
              "project": { "path_with_namespace": "miguchn/demo-repo" },
              "object_attributes": {
                "iid": 3,
                "title": "title",
                "action": "update",
                "source_branch": "feature",
                "target_branch": "main",
                "diff_refs": {
                  "base_sha": "aaaabbbbccccddddeeeeffff0000111122223333",
                  "head_sha": "ffffeeeeddddccccbbbbaaaa3333222211110000"
                }
              }
            }
            """;
        GitPullRequestEvent event = adapter.parsePullRequestEvent(
            "Merge Request Hook", "d-1", payload.getBytes(StandardCharsets.UTF_8));
        assertNotNull(event);
        assertEquals("update", event.action());
    }

    @Test
    void mapsUpdateWithOldrevToSynchronize()
    {
        String payload = """
            {
              "object_kind": "merge_request",
              "project": { "path_with_namespace": "miguchn/demo-repo" },
              "object_attributes": {
                "iid": 3,
                "title": "title",
                "action": "update",
                "oldrev": "aaaabbbbccccddddeeeeffff0000111122223333",
                "source_branch": "feature",
                "target_branch": "main",
                "diff_refs": {
                  "base_sha": "aaaabbbbccccddddeeeeffff0000111122223333",
                  "head_sha": "ffffeeeeddddccccbbbbaaaa3333222211110000"
                }
              }
            }
            """;
        GitPullRequestEvent event = adapter.parsePullRequestEvent(
            "Merge Request Hook", "d-1", payload.getBytes(StandardCharsets.UTF_8));
        assertNotNull(event);
        assertEquals("synchronize", event.action());
    }

    @Test
    void parsesRepositoryAndMergeRequestEvent()
    {
        GitRepositoryCoordinates repository = adapter.parseRepository(MR_PAYLOAD.getBytes(StandardCharsets.UTF_8));
        assertNotNull(repository);
        assertEquals("miguchn", repository.owner());
        assertEquals("demo-repo", repository.repository());
        assertEquals("miguchn/demo-repo", repository.fullPath());

        GitPullRequestEvent event = adapter.parsePullRequestEvent(
            "Merge Request Hook", "delivery-1", MR_PAYLOAD.getBytes(StandardCharsets.UTF_8));
        assertNotNull(event);
        assertEquals("opened", event.action());
        assertEquals(12, event.prNumber());
        assertEquals("alice", event.prAuthor());
    }

    @Test
    void returnsNullForInvalidPayload()
    {
        assertNull(adapter.parseRepository("not-json".getBytes(StandardCharsets.UTF_8)));
        assertNull(adapter.parsePullRequestEvent("push", "d-1", MR_PAYLOAD.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void parsesCloseAndMergeActions()
    {
        String closePayload = MR_PAYLOAD.replace("\"action\": \"open\"", "\"action\": \"close\"");
        GitPullRequestEvent closeEvent = adapter.parsePullRequestEvent(
            "Merge Request Hook", "d-close", closePayload.getBytes(StandardCharsets.UTF_8));
        assertNotNull(closeEvent);
        assertEquals("close", closeEvent.action());
        assertFalse(closeEvent.merged());
        assertTrue(closeEvent.isCloseLifecycle());

        String mergePayload = MR_PAYLOAD.replace("\"action\": \"open\"", "\"action\": \"merge\"");
        GitPullRequestEvent mergeEvent = adapter.parsePullRequestEvent(
            "Merge Request Hook", "d-merge", mergePayload.getBytes(StandardCharsets.UTF_8));
        assertNotNull(mergeEvent);
        assertEquals("merge", mergeEvent.action());
        assertTrue(mergeEvent.merged());
        assertTrue(mergeEvent.isCloseLifecycle());
    }

    private static WebhookRequestHeaders headers(String name, String value)
    {
        return WebhookRequestHeaders.of(Map.of(name, value));
    }

    private static String hmacBase64(String secret, byte[] payload) throws Exception
    {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return Base64.getEncoder().encodeToString(mac.doFinal(payload));
    }
}
