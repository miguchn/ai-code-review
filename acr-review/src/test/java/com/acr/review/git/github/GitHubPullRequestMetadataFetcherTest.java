package com.acr.review.git.github;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.IOException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.acr.review.git.GitPullRequestMetadata;
import com.acr.review.git.GitRepositoryCoordinates;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

class GitHubPullRequestMetadataFetcherTest
{
    private MockWebServer server;
    private GitHubPullRequestMetadataFetcher fetcher;
    private GitRepositoryCoordinates repository;

    @BeforeEach
    void setUp() throws IOException
    {
        server = new MockWebServer();
        server.start();
        fetcher = new GitHubPullRequestMetadataFetcher(server.url("/").toString(), 1000, 1000);
        repository = new GitRepositoryCoordinates("openai", "codex", "https://github.com/openai/codex");
    }

    @AfterEach
    void tearDown() throws IOException
    {
        server.shutdown();
    }

    @Test
    void fetchesDescriptionAndCommitMessages() throws InterruptedException
    {
        server.enqueue(json(200, "{\"body\":\"Fix review pipeline\"}"));
        server.enqueue(json(200, """
            [
              {"commit":{"message":"Fix review pipeline\\n\\nDetails"}},
              {"commit":{"message":"Add metadata fetcher"}}
            ]
            """));

        GitPullRequestMetadata metadata = fetcher.fetch(repository, "test-token", 42);

        assertTrue(metadata.fetched());
        assertEquals("Fix review pipeline", metadata.prDescription());
        assertEquals("Fix review pipeline\nAdd metadata fetcher", metadata.commitMessages());
        assertEquals("PR 元数据获取成功", metadata.message());

        var pullRequest = server.takeRequest();
        assertEquals("/repos/openai/codex/pulls/42", pullRequest.getPath());
        assertEquals("Bearer test-token", pullRequest.getHeader("Authorization"));
        assertEquals("/repos/openai/codex/pulls/42/commits", server.takeRequest().getPath());
    }

    @Test
    void returnsUnavailableWhenPullRequestFails()
    {
        server.enqueue(json(404, "{}"));

        GitPullRequestMetadata metadata = fetcher.fetch(repository, "test-token", 42);

        assertFalse(metadata.fetched());
        assertEquals("", metadata.prDescription());
        assertEquals("", metadata.commitMessages());
        assertTrue(metadata.message().contains("未找到对应 PR"));
    }

    @Test
    void returnsUnavailableWhenCommitsFail()
    {
        server.enqueue(json(200, "{\"body\":\"Only description\"}"));
        server.enqueue(json(403, "{}"));

        GitPullRequestMetadata metadata = fetcher.fetch(repository, "test-token", 42);

        assertFalse(metadata.fetched());
        assertEquals("", metadata.prDescription());
        assertEquals("", metadata.commitMessages());
        assertTrue(metadata.message().contains("权限不足"));
    }

    @Test
    void rejectsMissingInputsWithoutCallingApi()
    {
        GitPullRequestMetadata missingRepository = fetcher.fetch(null, "token", 1);
        GitPullRequestMetadata missingToken = fetcher.fetch(repository, " ", 1);
        GitPullRequestMetadata invalidNumber = fetcher.fetch(repository, "token", 0);

        assertFalse(missingRepository.fetched());
        assertFalse(missingToken.fetched());
        assertFalse(invalidNumber.fetched());
        assertEquals(0, server.getRequestCount());
    }

    @Test
    void returnsUnavailableOnNonJsonSuccessResponse()
    {
        // 200 但返回 HTML 错误页（网关/CDN 故障）时按不可用处理，不得抛运行时异常
        server.enqueue(json(200, "<html><body>Bad Gateway</body></html>"));

        GitPullRequestMetadata metadata = fetcher.fetch(repository, "test-token", 42);

        assertFalse(metadata.fetched());
        assertTrue(metadata.message().contains("JSON"));
    }

    private MockResponse json(int status, String body)
    {
        return new MockResponse().setResponseCode(status)
            .setHeader("Content-Type", "application/json")
            .setBody(body);
    }
}
