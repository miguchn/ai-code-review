package com.acr.review.git.gitee;

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
import com.acr.review.git.GitInlineCommentUnsupportedException;
import com.acr.review.git.GitPullRequestComment;
import com.acr.review.git.GitPullRequestCommentException;
import com.acr.review.git.GitRepositoryCoordinates;
import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

class GiteePullRequestCommentClientTest
{
    private static final GitAccessContext ACCESS = GitAccessContext.of("test-token", "https://gitee.com");
    private static final GitAccessContext SECRET_ACCESS = GitAccessContext.of("gitee_secrettokenvalue", "https://gitee.com");

    private MockWebServer server;
    private GiteePullRequestCommentClient client;
    private GitRepositoryCoordinates repo;

    @BeforeEach
    void setUp() throws IOException
    {
        server = new MockWebServer();
        server.start();
        client = new GiteePullRequestCommentClient(HttpUrl.get(server.url("/").toString()), 1000, 1000);
        repo = new GitRepositoryCoordinates("acme", "demo", "https://gitee.com/acme/demo");
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
        assertEquals("test-token", request.getRequestUrl().queryParameter("access_token"));
        assertTrue(request.getPath().contains("/issues/7/comments"));
    }

    @Test
    void createsAndUpdatesComment() throws InterruptedException
    {
        server.enqueue(json(201, "{\"id\":55,\"body\":\"new\"}"));
        GitPullRequestComment created = client.createIssueComment(repo, ACCESS, 3, "body-a");
        assertEquals("55", created.id());
        RecordedRequest createReq = server.takeRequest();
        assertEquals("POST", createReq.getMethod());
        assertTrue(createReq.getBody().readUtf8().contains("body-a"));

        server.enqueue(json(200, "{\"id\":55,\"body\":\"updated\"}"));
        GitPullRequestComment updated = client.updateIssueComment(repo, ACCESS, "55", "body-b");
        assertEquals("55", updated.id());
        RecordedRequest updateReq = server.takeRequest();
        assertEquals("PATCH", updateReq.getMethod());
        assertTrue(updateReq.getPath().contains("/issues/comments/55"));
    }

    @Test
    void createsInlineCommentWithLineField() throws InterruptedException
    {
        server.enqueue(json(201, "{\"id\":55,\"body\":\"inline body\"}"));
        GitInlineCommentRequest request = new GitInlineCommentRequest(
            "src/Main.java", null, 15, "inline body", "commitSha123");
        GitPullRequestComment created = client.createInlineComment(repo, ACCESS, 3, request);
        assertEquals("55", created.id());
        RecordedRequest recorded = server.takeRequest();
        assertEquals("POST", recorded.getMethod());
        assertTrue(recorded.getPath().contains("/pulls/3/comments"));
        String payload = recorded.getBody().readUtf8();
        assertTrue(payload.contains("\"path\":\"src/Main.java\""));
        assertTrue(payload.contains("\"commit_id\":\"commitSha123\""));
        assertTrue(payload.contains("\"line\":15"));
    }

    @Test
    void throwsUnsupportedWhenServerReturns422()
    {
        server.enqueue(json(422, "{\"message\":\"line not supported\"}"));
        GitInlineCommentRequest request = new GitInlineCommentRequest(
            "src/Main.java", null, 15, "inline body", "commitSha123");
        GitInlineCommentUnsupportedException ex = assertThrows(GitInlineCommentUnsupportedException.class,
            () -> client.createInlineComment(repo, ACCESS, 3, request));
        assertEquals("Gitee 不支持行内评论", ex.getMessage());
    }

    @Test
    void findsInlineCommentWithMarker() throws InterruptedException
    {
        server.enqueue(json(200, "[{\"id\":1,\"body\":\"other\"},"
            + "{\"id\":99,\"body\":\"hello\\n" + ReviewDeliveryConstants.COMMENT_MARKER + "\"}]"));

        Optional<GitPullRequestComment> found = client.findInlineCommentWithMarker(
            repo, ACCESS, 7, ReviewDeliveryConstants.COMMENT_MARKER);

        assertTrue(found.isPresent());
        assertEquals("99", found.get().id());
        RecordedRequest request = server.takeRequest();
        assertTrue(request.getPath().contains("/pulls/7/comments"));
    }

    @Test
    void sanitizesTokenInFailureMessages()
    {
        server.enqueue(new MockResponse().setResponseCode(500).setBody("error"));
        GitPullRequestCommentException ex = assertThrows(GitPullRequestCommentException.class,
            () -> client.createIssueComment(repo, SECRET_ACCESS, 1, "body"));
        assertFalse(ex.getMessage().contains("gitee_secrettokenvalue"));
    }

    private MockResponse json(int status, String body)
    {
        return new MockResponse().setResponseCode(status)
            .setHeader("Content-Type", "application/json")
            .setBody(body);
    }
}
