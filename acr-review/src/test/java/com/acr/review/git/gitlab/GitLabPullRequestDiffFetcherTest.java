package com.acr.review.git.gitlab;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.IOException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.acr.review.domain.ReviewPipelineConstants;
import com.acr.review.git.GitAccessContext;
import com.acr.review.git.GitPullRequestDiffResult;
import com.acr.review.git.GitRepositoryCoordinates;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

class GitLabPullRequestDiffFetcherTest
{
    private MockWebServer server;
    private GitLabPullRequestDiffFetcher fetcher;
    private GitRepositoryCoordinates repository;
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
        fetcher = new GitLabPullRequestDiffFetcher(1000, 1000);
        repository = new GitRepositoryCoordinates("openai", "codex", "openai/codex",
            serverUrl + "/openai/codex");
    }

    @AfterEach
    void tearDown() throws IOException
    {
        server.shutdown();
    }

    @Test
    void fetchesDiffFromCompareApi() throws InterruptedException
    {
        server.enqueue(json(200, """
            {"diffs":[{"diff":"diff --git a/A.java b/A.java"},{"diff":"+line"}]}
            """));

        GitPullRequestDiffResult result = fetcher.fetchDiff(repository, access, "abc1234", "def5678");

        assertTrue(result.success());
        assertTrue(result.diffContent().contains("diff --git"));
        assertTrue(result.diffContent().contains("+line"));
        var recorded = server.takeRequest();
        assertEquals("test-token", recorded.getHeader("PRIVATE-TOKEN"));
        assertTrue(recorded.getPath().contains("/repository/compare"));
    }

    @Test
    void rejectsMalformedSha()
    {
        GitPullRequestDiffResult result = fetcher.fetchDiff(repository, access, "--evil", "def5678");
        assertFalse(result.success());
        assertEquals(ReviewPipelineConstants.FAILURE_WORKSPACE_PREPARE, result.failureType());
    }

    @Test
    void classifiesCredentialErrors()
    {
        server.enqueue(new MockResponse().setResponseCode(401));
        GitPullRequestDiffResult result = fetcher.fetchDiff(
            repository, GitAccessContext.of("bad-token", access.serverUrl()), "abc1234", "def5678");
        assertFalse(result.success());
        assertEquals(ReviewPipelineConstants.FAILURE_CREDENTIAL_ERROR, result.failureType());
    }

    private MockResponse json(int status, String body)
    {
        return new MockResponse().setResponseCode(status)
            .setHeader("Content-Type", "application/json")
            .setBody(body);
    }
}
