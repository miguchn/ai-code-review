package com.acr.review.git.gitea;

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
import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

class GiteaPullRequestDiffFetcherTest
{
    private MockWebServer server;
    private GiteaPullRequestDiffFetcher fetcher;
    private GitRepositoryCoordinates repository;
    private GitAccessContext access;

    @BeforeEach
    void setUp() throws IOException
    {
        server = new MockWebServer();
        server.start();
        String serverUrl = baseServerUrl(server);
        access = GitAccessContext.of("test-token", serverUrl);
        fetcher = new GiteaPullRequestDiffFetcher(1000, 1000);
        repository = new GitRepositoryCoordinates("openai", "codex", "openai/codex", serverUrl + "/openai/codex");
    }

    @AfterEach
    void tearDown() throws IOException
    {
        server.shutdown();
    }

    @Test
    void fetchesDiffSuccessfully() throws InterruptedException
    {
        server.enqueue(new MockResponse().setResponseCode(200).setBody("diff --git a/A.java b/A.java"));

        GitPullRequestDiffResult result = fetcher.fetchDiff(repository, access, "abc1234", "def5678");

        assertTrue(result.success());
        assertTrue(result.diffContent().contains("diff --git"));
        var recorded = server.takeRequest();
        assertEquals("token test-token", recorded.getHeader("Authorization"));
        assertTrue(recorded.getPath().contains("/api/v1/repos/openai/codex/compare/abc1234...def5678"));
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
        GitPullRequestDiffResult result = fetcher.fetchDiff(repository,
            GitAccessContext.of("bad-token", baseServerUrl(server)), "abc1234", "def5678");
        assertFalse(result.success());
        assertEquals(ReviewPipelineConstants.FAILURE_CREDENTIAL_ERROR, result.failureType());
    }

    @Test
    void classifiesRateLimit()
    {
        server.enqueue(new MockResponse().setResponseCode(429));
        GitPullRequestDiffResult result = fetcher.fetchDiff(repository, access, "abc1234", "def5678");
        assertFalse(result.success());
        assertEquals(ReviewPipelineConstants.FAILURE_RATE_LIMIT, result.failureType());
    }

    @Test
    void capsOversizedDiffBody()
    {
        String huge = "x".repeat(ReviewPipelineConstants.MAX_DIFF_CHARS * 3);
        server.enqueue(new MockResponse().setResponseCode(200).setBody(huge));

        GitPullRequestDiffResult result = fetcher.fetchDiff(repository, access, "abc1234", "def5678");

        assertTrue(result.success());
        assertTrue(result.diffContent().length() <= ReviewPipelineConstants.MAX_DIFF_CHARS * 2);
    }

    private static String baseServerUrl(MockWebServer server)
    {
        HttpUrl url = server.url("/");
        return url.scheme() + "://" + url.host() + ":" + url.port();
    }
}
