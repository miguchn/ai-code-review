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
