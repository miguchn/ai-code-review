package com.acr.review.git.gitea;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.IOException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.acr.review.git.GitAccessContext;
import com.acr.review.git.GitPullRequestMetadata;
import com.acr.review.git.GitRepositoryCoordinates;
import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

class GiteaPullRequestMetadataFetcherTest
{
    private MockWebServer server;
    private GiteaPullRequestMetadataFetcher fetcher;
    private GitRepositoryCoordinates repository;
    private GitAccessContext access;

    @BeforeEach
    void setUp() throws IOException
    {
        server = new MockWebServer();
        server.start();
        String serverUrl = baseServerUrl(server);
        access = GitAccessContext.of("test-token", serverUrl);
        fetcher = new GiteaPullRequestMetadataFetcher(1000, 1000);
        repository = new GitRepositoryCoordinates("openai", "codex", "openai/codex", serverUrl + "/openai/codex");
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
              "body":"Fix review pipeline",
              "user":{"login":"miguchn"},
              "additions":12,
              "deletions":3,
              "changed_files":4
            }
            """));
        server.enqueue(json(200, """
            [
              {"commit":{"message":"Fix review pipeline\\n\\nDetails"}},
              {"commit":{"message":"Add metadata fetcher"}}
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

        var pullRequest = server.takeRequest();
        assertEquals("/api/v1/repos/openai/codex/pulls/42", pullRequest.getPath());
        assertEquals("token test-token", pullRequest.getHeader("Authorization"));
        assertEquals("/api/v1/repos/openai/codex/pulls/42/commits", server.takeRequest().getPath());
    }

    @Test
    void returnsUnavailableWhenPullRequestFails()
    {
        server.enqueue(json(404, "{}"));

        GitPullRequestMetadata metadata = fetcher.fetch(repository, access, 42);

        assertFalse(metadata.fetched());
        assertTrue(metadata.message().contains("未找到对应 PR"));
    }

    @Test
    void keepsAuthorAndLinesWhenCommitsFail()
    {
        server.enqueue(json(200, """
            {
              "body":"Only description",
              "user":{"login":"alice"},
              "additions":5,
              "deletions":1,
              "changed_files":2
            }
            """));
        server.enqueue(json(403, "{}"));

        GitPullRequestMetadata metadata = fetcher.fetch(repository, access, 42);

        assertTrue(metadata.fetched());
        assertEquals("Only description", metadata.prDescription());
        assertEquals("", metadata.commitMessages());
        assertEquals("alice", metadata.prAuthor());
    }

    @Test
    void rejectsMissingInputsWithoutCallingApi()
    {
        GitPullRequestMetadata missingRepository = fetcher.fetch(null, access, 1);
        GitPullRequestMetadata missingToken = fetcher.fetch(repository, GitAccessContext.of(" ", baseServerUrl(server)), 1);
        GitPullRequestMetadata invalidNumber = fetcher.fetch(repository, access, 0);

        assertFalse(missingRepository.fetched());
        assertFalse(missingToken.fetched());
        assertFalse(invalidNumber.fetched());
        assertEquals(0, server.getRequestCount());
    }

    private static String baseServerUrl(MockWebServer server)
    {
        HttpUrl url = server.url("/");
        return url.scheme() + "://" + url.host() + ":" + url.port();
    }

    private MockResponse json(int status, String body)
    {
        return new MockResponse().setResponseCode(status)
            .setHeader("Content-Type", "application/json")
            .setBody(body);
    }
}
