package com.acr.review.git.github;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.IOException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.acr.review.git.GitAccessContext;
import com.acr.review.domain.ReviewPipelineConstants;
import com.acr.review.git.GitPullRequestDiffResult;
import com.acr.review.git.GitRepositoryCoordinates;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

class GitHubPullRequestDiffFetcherTest
{
    private static final GitAccessContext ACCESS = GitAccessContext.of("test-token", "https://github.com");

    private MockWebServer server;
    private GitHubPullRequestDiffFetcher fetcher;
    private GitRepositoryCoordinates repository;

    @BeforeEach
    void setUp() throws IOException
    {
        server = new MockWebServer();
        server.start();
        fetcher = new GitHubPullRequestDiffFetcher(server.url("/").toString(), 1000, 1000);
        repository = new GitRepositoryCoordinates("openai", "codex", "https://github.com/openai/codex");
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

        GitPullRequestDiffResult result = fetcher.fetchDiff(repository, ACCESS, "abc1234", "def5678");

        assertTrue(result.success());
        assertTrue(result.diffContent().contains("diff --git"));
        okhttp3.mockwebserver.RecordedRequest recorded = server.takeRequest();
        assertEquals("Bearer test-token", recorded.getHeader("Authorization"));
        assertEquals("application/vnd.github.v3.diff", recorded.getHeader("Accept"));
        assertTrue(recorded.getPath().contains("/repos/openai/codex/compare/abc1234...def5678"));
    }

    @Test
    void rejectsMalformedSha()
    {
        GitPullRequestDiffResult result = fetcher.fetchDiff(repository, ACCESS, "--evil", "def5678");
        assertFalse(result.success());
        assertEquals(ReviewPipelineConstants.FAILURE_WORKSPACE_PREPARE, result.failureType());
    }

    @Test
    void classifiesCredentialErrors()
    {
        server.enqueue(new MockResponse().setResponseCode(401));
        GitPullRequestDiffResult result = fetcher.fetchDiff(repository, GitAccessContext.of("bad-token", "https://github.com"), "abc1234", "def5678");
        assertFalse(result.success());
        assertEquals(ReviewPipelineConstants.FAILURE_CREDENTIAL_ERROR, result.failureType());
    }

    @Test
    void classifiesExplicitRateLimit()
    {
        server.enqueue(new MockResponse().setResponseCode(429));
        GitPullRequestDiffResult result = fetcher.fetchDiff(repository, ACCESS, "abc1234", "def5678");
        assertFalse(result.success());
        assertEquals(ReviewPipelineConstants.FAILURE_RATE_LIMIT, result.failureType());
    }

    @Test
    void classifiesForbiddenWithExhaustedRateLimit()
    {
        server.enqueue(new MockResponse().setResponseCode(403).setHeader("X-RateLimit-Remaining", "0"));
        GitPullRequestDiffResult result = fetcher.fetchDiff(repository, ACCESS, "abc1234", "def5678");
        assertFalse(result.success());
        assertEquals(ReviewPipelineConstants.FAILURE_RATE_LIMIT, result.failureType());
    }

    @Test
    void forbiddenWithoutRateLimitIsCredentialError()
    {
        server.enqueue(new MockResponse().setResponseCode(403).setHeader("X-RateLimit-Remaining", "120"));
        GitPullRequestDiffResult result = fetcher.fetchDiff(repository, ACCESS, "abc1234", "def5678");
        assertFalse(result.success());
        assertEquals(ReviewPipelineConstants.FAILURE_CREDENTIAL_ERROR, result.failureType());
    }

    @Test
    void capsOversizedDiffBody()
    {
        // 超过读取上限的响应体必须被截断，不能全量进内存
        String huge = "x".repeat(ReviewPipelineConstants.MAX_DIFF_CHARS * 3);
        server.enqueue(new MockResponse().setResponseCode(200).setBody(huge));

        GitPullRequestDiffResult result = fetcher.fetchDiff(repository, ACCESS, "abc1234", "def5678");

        assertTrue(result.success());
        assertTrue(result.diffContent().length() <= ReviewPipelineConstants.MAX_DIFF_CHARS * 2);
    }
}
