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
import com.acr.review.git.GitFileContentResult;
import com.acr.review.git.GitRepositoryCoordinates;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

class GitLabFileContentFetcherTest
{
    private MockWebServer server;
    private GitLabFileContentFetcher fetcher;
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
        fetcher = new GitLabFileContentFetcher(1000, 1000);
        repository = new GitRepositoryCoordinates("openai", "codex", "openai/codex",
            serverUrl + "/openai/codex");
    }

    @AfterEach
    void tearDown() throws IOException
    {
        server.shutdown();
    }

    @Test
    void fetchesRawContentWithRef() throws InterruptedException
    {
        server.enqueue(new MockResponse().setResponseCode(200).setBody("server:\n  port: 8080\n"));

        GitFileContentResult result = fetcher.fetchFileContent(repository, access, "src/main/app.yml", "def5678");

        assertTrue(result.success());
        assertEquals("server:\n  port: 8080\n", result.content());
        var recorded = server.takeRequest();
        assertEquals("test-token", recorded.getHeader("PRIVATE-TOKEN"));
        assertTrue(recorded.getPath().contains("/repository/files/"));
        assertTrue(recorded.getPath().contains("ref=def5678"));
    }

    @Test
    void rejectsInvalidPathAndRef()
    {
        assertEquals("INVALID_PATH", fetcher.fetchFileContent(repository, access, "../etc/passwd", "def5678").failureReason());
        assertEquals("INVALID_REF", fetcher.fetchFileContent(repository, access, "a.yml", "--evil").failureReason());
    }

    @Test
    void failsAsTooLargeWhenBodyExceedsCap()
    {
        String big = "x".repeat(ReviewPipelineConstants.MAX_EXPANDED_FILE_BYTES + 100);
        server.enqueue(new MockResponse().setResponseCode(200).setBody(big));
        assertFalse(fetcher.fetchFileContent(repository, access, "big.sql", "def5678").success());
    }
}
