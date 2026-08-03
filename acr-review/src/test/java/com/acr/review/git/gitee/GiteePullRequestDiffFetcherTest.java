package com.acr.review.git.gitee;

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

class GiteePullRequestDiffFetcherTest
{
    private static final GitAccessContext ACCESS = GitAccessContext.of("test-token", "https://gitee.com");

    private MockWebServer server;
    private GiteePullRequestDiffFetcher fetcher;
    private GitRepositoryCoordinates repository;

    @BeforeEach
    void setUp() throws IOException
    {
        server = new MockWebServer();
        server.start();
        fetcher = new GiteePullRequestDiffFetcher(HttpUrl.get(server.url("/").toString()), 1000, 1000);
        repository = new GitRepositoryCoordinates("openai", "codex", "https://gitee.com/openai/codex");
    }

    @AfterEach
    void tearDown() throws IOException
    {
        server.shutdown();
    }

    @Test
    void fetchesDiffFromCompareApi() throws InterruptedException
    {
        server.enqueue(new MockResponse().setResponseCode(200).setBody(
            "{\"files\":[{\"patch\":\"diff --git a/A.java b/A.java\\n+line\"}]}"));

        GitPullRequestDiffResult result = fetcher.fetchDiff(repository, ACCESS, "abc1234", "def5678");

        assertTrue(result.success());
        assertTrue(result.diffContent().contains("diff --git"));
        var recorded = server.takeRequest();
        assertEquals("test-token", recorded.getRequestUrl().queryParameter("access_token"));
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
        GitPullRequestDiffResult result = fetcher.fetchDiff(
            repository, GitAccessContext.of("bad-token", "https://gitee.com"), "abc1234", "def5678");
        assertFalse(result.success());
        assertEquals(ReviewPipelineConstants.FAILURE_CREDENTIAL_ERROR, result.failureType());
    }

    @Test
    void extractDiffConcatenatesPatches()
    {
        String body = "{\"files\":[{\"patch\":\"patch-a\"},{\"patch\":\"patch-b\"}]}";
        String diff = GiteePullRequestDiffFetcher.extractDiff(body);
        assertTrue(diff.contains("patch-a"));
        assertTrue(diff.contains("patch-b"));
    }
}
