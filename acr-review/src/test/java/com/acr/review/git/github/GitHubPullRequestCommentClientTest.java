package com.acr.review.git.github;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.IOException;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.acr.review.git.GitAccessContext;
import com.acr.review.delivery.ReviewDeliveryConstants;
import com.acr.review.git.GitPullRequestComment;
import com.acr.review.git.GitPullRequestCommentException;
import com.acr.review.git.GitRepositoryCoordinates;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

class GitHubPullRequestCommentClientTest
{
    private static final GitAccessContext ACCESS = GitAccessContext.of("test-token", "https://github.com");
    private static final GitAccessContext TOK_ACCESS = GitAccessContext.of("tok", "https://github.com");
    private static final GitAccessContext SECRET_ACCESS = GitAccessContext.of("ghp_secrettokenvalue", "https://github.com");

    private MockWebServer server;
    private GitHubPullRequestCommentClient client;
    private GitRepositoryCoordinates repo;

    @BeforeEach
    void setUp() throws IOException
    {
        server = new MockWebServer();
        server.start();
        client = new GitHubPullRequestCommentClient(server.url("/").toString(), 1000, 1000);
        repo = new GitRepositoryCoordinates("acme", "demo", "https://github.com/acme/demo");
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
            repo, ACCESS, 7, ReviewDeliveryConstants.COMMENT_MARKER);

        assertTrue(found.isPresent());
        assertEquals("99", found.get().id());
        RecordedRequest request = server.takeRequest();
        assertEquals("Bearer test-token", request.getHeader("Authorization"));
        assertTrue(request.getPath().contains("/repos/acme/demo/issues/7/comments"));
    }

    @Test
    void createsAndUpdatesComment() throws InterruptedException
    {
        server.enqueue(json(201, "{\"id\":55,\"body\":\"new\"}"));
        GitPullRequestComment created = client.createIssueComment(repo, TOK_ACCESS, 3, "body-a");
        assertEquals("55", created.id());
        RecordedRequest createReq = server.takeRequest();
        assertEquals("POST", createReq.getMethod());
        assertTrue(createReq.getBody().readUtf8().contains("body-a"));

        server.enqueue(json(200, "{\"id\":55,\"body\":\"updated\"}"));
        GitPullRequestComment updated = client.updateIssueComment(repo, TOK_ACCESS, "55", "body-b");
        assertEquals("55", updated.id());
        RecordedRequest updateReq = server.takeRequest();
        assertEquals("PATCH", updateReq.getMethod());
        assertTrue(updateReq.getPath().endsWith("/issues/comments/55"));
    }

    @Test
    void mapsAuthAndRateLimitErrorsWithoutLeakingToken()
    {
        server.enqueue(json(401, "{\"message\":\"bad\"}"));
        GitPullRequestCommentException auth = assertThrows(GitPullRequestCommentException.class,
            () -> client.createIssueComment(repo, SECRET_ACCESS, 1, "x"));
        assertTrue(auth.getMessage().contains("凭据"));
        assertFalse(auth.getMessage().contains("ghp_secrettokenvalue"));

        server.enqueue(new MockResponse().setResponseCode(403)
            .addHeader("X-RateLimit-Remaining", "0")
            .setBody("{\"message\":\"rate\"}"));
        GitPullRequestCommentException rate = assertThrows(GitPullRequestCommentException.class,
            () -> client.createIssueComment(repo, TOK_ACCESS, 1, "x"));
        assertTrue(rate.getMessage().contains("限流"));
    }

    private static MockResponse json(int code, String body)
    {
        return new MockResponse()
            .setResponseCode(code)
            .addHeader("Content-Type", "application/json")
            .setBody(body);
    }
}
