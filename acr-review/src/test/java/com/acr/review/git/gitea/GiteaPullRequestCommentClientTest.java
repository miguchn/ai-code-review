package com.acr.review.git.gitea;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.IOException;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.acr.review.delivery.ReviewDeliveryConstants;
import com.acr.review.git.GitAccessContext;
import com.acr.review.git.GitInlineCommentRequest;
import com.acr.review.git.GitPullRequestComment;
import com.acr.review.git.GitPullRequestCommentException;
import com.acr.review.git.GitRepositoryCoordinates;
import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

class GiteaPullRequestCommentClientTest
{
    private MockWebServer server;
    private GiteaPullRequestCommentClient client;
    private GitRepositoryCoordinates repo;
    private GitAccessContext access;

    @BeforeEach
    void setUp() throws IOException
    {
        server = new MockWebServer();
        server.start();
        String serverUrl = baseServerUrl(server);
        access = GitAccessContext.of("test-token", serverUrl);
        client = new GiteaPullRequestCommentClient(1000, 1000);
        repo = new GitRepositoryCoordinates("acme", "demo", "acme/demo", serverUrl + "/acme/demo");
    }

    @AfterEach
    void tearDown() throws IOException
    {
        server.shutdown();
    }

    @Test
    void findsMarkerCommentAcrossList() throws InterruptedException
    {
        server.enqueue(json(200, "[{\"id\":1,\"body\":\"other\"},"
            + "{\"id\":99,\"body\":\"hello\\n" + ReviewDeliveryConstants.COMMENT_MARKER + "\"}]"));

        Optional<GitPullRequestComment> found = client.findCommentWithMarker(
            repo, access, 7, ReviewDeliveryConstants.COMMENT_MARKER);

        assertTrue(found.isPresent());
        assertEquals("99", found.get().id());
        RecordedRequest request = server.takeRequest();
        assertEquals("token test-token", request.getHeader("Authorization"));
        assertTrue(request.getPath().contains("/api/v1/repos/acme/demo/issues/7/comments"));
    }

    @Test
    void createsAndUpdatesComment() throws InterruptedException
    {
        server.enqueue(json(201, "{\"id\":55,\"body\":\"new\"}"));
        GitPullRequestComment created = client.createIssueComment(repo, access, 3, "body-a");
        assertEquals("55", created.id());
        RecordedRequest createReq = server.takeRequest();
        assertEquals("POST", createReq.getMethod());
        assertTrue(createReq.getBody().readUtf8().contains("body-a"));

        server.enqueue(json(200, "{\"id\":55,\"body\":\"updated\"}"));
        GitPullRequestComment updated = client.updateIssueComment(repo, access, "55", "body-b");
        assertEquals("55", updated.id());
        RecordedRequest updateReq = server.takeRequest();
        assertEquals("PATCH", updateReq.getMethod());
        assertTrue(updateReq.getPath().endsWith("/issues/comments/55"));
    }

    @Test
    void createsInlineCommentViaReview() throws InterruptedException
    {
        server.enqueue(json(201, "{\"id\":42,\"comments\":[{\"id\":77,\"body\":\"inline body\"}]}"));
        GitInlineCommentRequest request = new GitInlineCommentRequest(
            "pkg/main.go", null, 8, "inline body", "unused");
        GitPullRequestComment created = client.createInlineComment(repo, access, 4, request);
        assertEquals("77", created.id());
        RecordedRequest recorded = server.takeRequest();
        assertEquals("POST", recorded.getMethod());
        assertTrue(recorded.getPath().contains("/api/v1/repos/acme/demo/pulls/4/reviews"));
        String payload = recorded.getBody().readUtf8();
        assertTrue(payload.contains("\"event\":\"COMMENT\""));
        assertTrue(payload.contains("\"path\":\"pkg/main.go\""));
        assertTrue(payload.contains("\"new_line\":8"));
    }

    @Test
    void findsInlineCommentWithMarker() throws InterruptedException
    {
        server.enqueue(json(200, "[{\"id\":1,\"body\":\"other\"},"
            + "{\"id\":88,\"body\":\"hello\\n" + ReviewDeliveryConstants.COMMENT_MARKER + "\"}]"));

        Optional<GitPullRequestComment> found = client.findInlineCommentWithMarker(
            repo, access, 7, ReviewDeliveryConstants.COMMENT_MARKER);

        assertTrue(found.isPresent());
        assertEquals("88", found.get().id());
        RecordedRequest request = server.takeRequest();
        assertTrue(request.getPath().contains("/api/v1/repos/acme/demo/pulls/7/comments"));
    }

    @Test
    void mapsAuthAndRateLimitErrorsWithoutLeakingToken()
    {
        server.enqueue(json(401, "{\"message\":\"bad\"}"));
        GitPullRequestCommentException auth = assertThrows(GitPullRequestCommentException.class,
            () -> client.createIssueComment(repo, GitAccessContext.of("gitea_secrettokenvalue", baseServerUrl(server)), 1, "x"));
        assertTrue(auth.getMessage().contains("凭据"));
        assertFalse(auth.getMessage().contains("gitea_secrettokenvalue"));

        server.enqueue(new MockResponse().setResponseCode(429).setBody("{\"message\":\"rate\"}"));
        GitPullRequestCommentException rate = assertThrows(GitPullRequestCommentException.class,
            () -> client.createIssueComment(repo, access, 1, "x"));
        assertTrue(rate.getMessage().contains("限流"));
    }

    private static String baseServerUrl(MockWebServer server)
    {
        HttpUrl url = server.url("/");
        return url.scheme() + "://" + url.host() + ":" + url.port();
    }

    private static MockResponse json(int code, String body)
    {
        return new MockResponse()
            .setResponseCode(code)
            .addHeader("Content-Type", "application/json")
            .setBody(body);
    }
}
