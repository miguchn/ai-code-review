package com.acr.review.git.gitlab;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.IOException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.acr.review.git.GitAccessContext;
import com.acr.review.git.GitPullRequestMetadata;
import com.acr.review.git.GitRepositoryCoordinates;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

class GitLabPullRequestMetadataFetcherTest
{
    private MockWebServer server;
    private GitLabPullRequestMetadataFetcher fetcher;
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
        fetcher = new GitLabPullRequestMetadataFetcher(1000, 1000);
        repository = new GitRepositoryCoordinates("openai", "codex", "openai/codex",
            serverUrl + "/openai/codex");
    }

    @AfterEach
    void tearDown() throws IOException
    {
        server.shutdown();
    }

    @Test
    void fetchesDescriptionAuthorLinesAndCommitMessages() throws InterruptedException
    {
        server.enqueue(json(200, """
            {
              "description":"Fix review pipeline",
              "author":{"username":"miguchn"},
              "changes_count":4,
              "stats":{"additions":12,"deletions":3}
            }
            """));
        server.enqueue(json(200, """
            [
              {"title":"Fix review pipeline","message":"Fix review pipeline\\n\\nDetails"},
              {"title":"Add metadata fetcher"}
            ]
            """));

        GitPullRequestMetadata metadata = fetcher.fetch(repository, access, 42);

        assertTrue(metadata.fetched());
        assertEquals("Fix review pipeline", metadata.prDescription());
        assertEquals("Fix review pipeline\nAdd metadata fetcher", metadata.commitMessages());
        assertEquals("miguchn", metadata.prAuthor());
        assertEquals(12, metadata.additions());
        assertEquals(3, metadata.deletions());
        assertEquals(4, metadata.changedFiles());
        assertTrue(server.takeRequest().getPath().contains("/merge_requests/42"));
        assertTrue(server.takeRequest().getPath().contains("/merge_requests/42/commits"));
    }

    @Test
    void returnsUnavailableWhenMergeRequestFails()
    {
        server.enqueue(json(404, "{}"));
        GitPullRequestMetadata metadata = fetcher.fetch(repository, access, 42);
        assertFalse(metadata.fetched());
        assertTrue(metadata.message().contains("未找到对应 MR"));
    }

    private MockResponse json(int status, String body)
    {
        return new MockResponse().setResponseCode(status)
            .setHeader("Content-Type", "application/json")
            .setBody(body);
    }
}
