package com.acr.review.git.gitee;

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
import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

class GiteePullRequestMetadataFetcherTest
{
    private static final GitAccessContext ACCESS = GitAccessContext.of("test-token", "https://gitee.com");

    private MockWebServer server;
    private GiteePullRequestMetadataFetcher fetcher;
    private GitRepositoryCoordinates repository;

    @BeforeEach
    void setUp() throws IOException
    {
        server = new MockWebServer();
        server.start();
        fetcher = new GiteePullRequestMetadataFetcher(HttpUrl.get(server.url("/").toString()), 1000, 1000);
        repository = new GitRepositoryCoordinates("acme", "demo", "https://gitee.com/acme/demo");
    }

    @AfterEach
    void tearDown() throws IOException
    {
        server.shutdown();
    }

    @Test
    void fetchesMetadataAndCommits() throws InterruptedException
    {
        server.enqueue(json(200, """
            {"body":"desc","user":{"login":"alice"},"additions":10,"deletions":2,"changed_files":3}
            """));
        server.enqueue(json(200, """
            [{"commit":{"message":"first commit\\nmore"}}]
            """));

        GitPullRequestMetadata metadata = fetcher.fetch(repository, ACCESS, 5);

        assertTrue(metadata.fetched());
        assertEquals("desc", metadata.prDescription());
        assertEquals("alice", metadata.prAuthor());
        assertEquals(10, metadata.additions());
        assertEquals("first commit", metadata.commitMessages());
        assertTrue(server.takeRequest().getPath().contains("/pulls/5"));
        assertTrue(server.takeRequest().getPath().contains("/pulls/5/commits"));
    }

    @Test
    void returnsUnavailableOnCredentialError()
    {
        server.enqueue(new MockResponse().setResponseCode(401));
        GitPullRequestMetadata metadata = fetcher.fetch(repository, ACCESS, 5);
        assertFalse(metadata.fetched());
    }

    private MockResponse json(int status, String body)
    {
        return new MockResponse().setResponseCode(status)
            .setHeader("Content-Type", "application/json")
            .setBody(body);
    }
}
