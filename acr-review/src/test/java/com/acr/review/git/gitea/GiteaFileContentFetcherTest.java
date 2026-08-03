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
import com.acr.review.git.GitFileContentResult;
import com.acr.review.git.GitRepositoryCoordinates;
import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

class GiteaFileContentFetcherTest
{
    private MockWebServer server;
    private GiteaFileContentFetcher fetcher;
    private GitRepositoryCoordinates repository;
    private GitAccessContext access;

    @BeforeEach
    void setUp() throws IOException
    {
        server = new MockWebServer();
        server.start();
        String serverUrl = baseServerUrl(server);
        access = GitAccessContext.of("test-token", serverUrl);
        fetcher = new GiteaFileContentFetcher(1000, 1000);
        repository = new GitRepositoryCoordinates("openai", "codex", "openai/codex", serverUrl + "/openai/codex");
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
            repository, access, "src/main/resources/application config.yml", "def5678");

        assertTrue(result.success());
        assertEquals("server:\n  port: 8080\n", result.content());
        var recorded = server.takeRequest();
        assertEquals("token test-token", recorded.getHeader("Authorization"));
        assertTrue(recorded.getPath().contains("/api/v1/repos/openai/codex/raw/src/main/resources/application%20config.yml"));
        assertTrue(recorded.getPath().contains("ref=def5678"));
    }

    @Test
    void rejectsTraversalAndAbsolutePath()
    {
        assertEquals("INVALID_PATH", fetcher.fetchFileContent(repository, access, "../etc/passwd", "def5678").failureReason());
        assertEquals("INVALID_PATH", fetcher.fetchFileContent(repository, access, "/etc/passwd", "def5678").failureReason());
        assertEquals("INVALID_REF", fetcher.fetchFileContent(repository, access, "a.yml", "--evil").failureReason());
    }

    @Test
    void failsAsTooLargeWhenBodyExceedsCap()
    {
        String big = "x".repeat(ReviewPipelineConstants.MAX_EXPANDED_FILE_BYTES + 100);
        server.enqueue(new MockResponse().setResponseCode(200).setBody(big));

        GitFileContentResult result = fetcher.fetchFileContent(repository, access, "big.sql", "def5678");

        assertFalse(result.success());
        assertEquals("FILE_TOO_LARGE", result.failureReason());
    }

    @Test
    void mapsHttpFailures()
    {
        server.enqueue(new MockResponse().setResponseCode(404));
        assertEquals("NOT_FOUND", fetcher.fetchFileContent(repository, access, "missing.yml", "def5678").failureReason());

        server.enqueue(new MockResponse().setResponseCode(401));
        assertEquals("CREDENTIAL", fetcher.fetchFileContent(repository, access, "a.yml", "def5678").failureReason());

        server.enqueue(new MockResponse().setResponseCode(429));
        assertEquals("RATE_LIMIT", fetcher.fetchFileContent(repository, access, "a.yml", "def5678").failureReason());
    }

    private static String baseServerUrl(MockWebServer server)
    {
        HttpUrl url = server.url("/");
        return url.scheme() + "://" + url.host() + ":" + url.port();
    }
}
