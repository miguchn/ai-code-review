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
import com.acr.review.git.GitFileContentResult;
import com.acr.review.git.GitRepositoryCoordinates;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

class GitHubFileContentFetcherTest
{
    private static final GitAccessContext ACCESS = GitAccessContext.of("test-token", "https://github.com");
    private static final GitAccessContext SHORT_ACCESS = GitAccessContext.of("t", "https://github.com");

    private MockWebServer server;
    private GitHubFileContentFetcher fetcher;
    private GitRepositoryCoordinates repository;

    @BeforeEach
    void setUp() throws IOException
    {
        server = new MockWebServer();
        server.start();
        fetcher = new GitHubFileContentFetcher(server.url("/").toString(), 1000, 1000);
        repository = new GitRepositoryCoordinates("openai", "codex", "https://github.com/openai/codex");
    }

    @AfterEach
    void tearDown() throws IOException
    {
        server.shutdown();
    }

    @Test
    void fetchesRawContentWithRefAndEncodedPath() throws InterruptedException
    {
        server.enqueue(new MockResponse().setResponseCode(200).setBody("server:\n  port: 8080\n"));

        GitFileContentResult result = fetcher.fetchFileContent(
            repository, ACCESS, "src/main/resources/application config.yml", "def5678");

        assertTrue(result.success());
        assertEquals("server:\n  port: 8080\n", result.content());
        okhttp3.mockwebserver.RecordedRequest recorded = server.takeRequest();
        assertEquals("Bearer test-token", recorded.getHeader("Authorization"));
        assertEquals("application/vnd.github.v3.raw", recorded.getHeader("Accept"));
        assertTrue(recorded.getPath().contains("/repos/openai/codex/contents/src/main/resources/application%20config.yml"));
        assertTrue(recorded.getPath().contains("ref=def5678"));
    }

    @Test
    void rejectsTraversalAndAbsolutePath()
    {
        assertEquals("INVALID_PATH", fetcher.fetchFileContent(repository, SHORT_ACCESS, "../etc/passwd", "def5678").failureReason());
        assertEquals("INVALID_PATH", fetcher.fetchFileContent(repository, SHORT_ACCESS, "/etc/passwd", "def5678").failureReason());
        assertEquals("INVALID_REF", fetcher.fetchFileContent(repository, SHORT_ACCESS, "a.yml", "--evil").failureReason());
    }

    @Test
    void failsAsTooLargeWhenBodyExceedsCap()
    {
        String big = "x".repeat(ReviewPipelineConstants.MAX_EXPANDED_FILE_BYTES + 100);
        server.enqueue(new MockResponse().setResponseCode(200).setBody(big));

        GitFileContentResult result = fetcher.fetchFileContent(repository, SHORT_ACCESS, "big.sql", "def5678");

        assertFalse(result.success());
        assertEquals("FILE_TOO_LARGE", result.failureReason());
    }

    @Test
    void mapsHttpFailures()
    {
        server.enqueue(new MockResponse().setResponseCode(404));
        assertEquals("NOT_FOUND", fetcher.fetchFileContent(repository, SHORT_ACCESS, "missing.yml", "def5678").failureReason());

        server.enqueue(new MockResponse().setResponseCode(401));
        assertEquals("CREDENTIAL", fetcher.fetchFileContent(repository, SHORT_ACCESS, "a.yml", "def5678").failureReason());

        server.enqueue(new MockResponse().setResponseCode(403).setHeader("X-RateLimit-Remaining", "0"));
        assertEquals("RATE_LIMIT", fetcher.fetchFileContent(repository, SHORT_ACCESS, "a.yml", "def5678").failureReason());
    }
}
