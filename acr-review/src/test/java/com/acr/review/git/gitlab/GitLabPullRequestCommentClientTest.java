package com.acr.review.git.gitlab;

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
import com.acr.review.git.GitPullRequestComment;
import com.acr.review.git.GitPullRequestCommentException;
import com.acr.review.git.GitRepositoryCoordinates;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

class GitLabPullRequestCommentClientTest
{
    private MockWebServer server;
    private GitLabPullRequestCommentClient client;
    private GitRepositoryCoordinates repo;
    private GitAccessContext access;

    @BeforeEach
    void setUp() throws IOException
    {
        server = new MockWebServer();
        server.start();
        String serverUrl = server.url("/").toString();
        if (serverUrl.endsWith("/"))
        {
            serverUrl = serverUrl.substring(0, serverUrl.length() - 1);
        }
        access = GitAccessContext.of("test-token", serverUrl);
        client = new GitLabPullRequestCommentClient(1000, 1000);
        repo = new GitRepositoryCoordinates("acme", "demo", "acme/demo",
            serverUrl + "/acme/demo");
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
        assertEquals("7:99", found.get().id());
        RecordedRequest request = server.takeRequest();
        assertEquals("test-token", request.getHeader("PRIVATE-TOKEN"));
        assertTrue(request.getPath().contains("/merge_requests/7/notes"));
    }

    @Test
    void createsAndUpdatesComment() throws InterruptedException
    {
        server.enqueue(json(201, "{\"id\":55,\"body\":\"new\"}"));
        GitPullRequestComment created = client.createIssueComment(repo, access, 3, "body-a");
        assertEquals("3:55", created.id());
        RecordedRequest createReq = server.takeRequest();
        assertEquals("POST", createReq.getMethod());
        assertTrue(createReq.getBody().readUtf8().contains("body-a"));

        server.enqueue(json(200, "{\"id\":55,\"body\":\"updated\"}"));
        GitPullRequestComment updated = client.updateIssueComment(repo, access, "3:55", "body-b");
        assertEquals("3:55", updated.id());
        RecordedRequest updateReq = server.takeRequest();
        assertEquals("PUT", updateReq.getMethod());
        assertTrue(updateReq.getPath().contains("/merge_requests/3/notes/55"));
    }

    @Test
    void mapsAuthErrorsWithoutLeakingToken()
    {
        server.enqueue(json(401, "{\"message\":\"bad\"}"));
        GitPullRequestCommentException auth = assertThrows(GitPullRequestCommentException.class,
            () -> client.createIssueComment(repo, GitAccessContext.of("glpat-secrettoken", access.serverUrl()), 1, "x"));
        assertTrue(auth.getMessage().contains("凭据"));
        assertFalse(auth.getMessage().contains("glpat-secrettoken"));
    }

    private static MockResponse json(int code, String body)
    {
        return new MockResponse()
            .setResponseCode(code)
            .addHeader("Content-Type", "application/json")
            .setBody(body);
    }
}
