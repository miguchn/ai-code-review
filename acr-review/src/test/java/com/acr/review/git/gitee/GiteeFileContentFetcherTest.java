package com.acr.review.git.gitee;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.acr.review.git.GitAccessContext;
import com.acr.review.git.GitFileContentResult;
import com.acr.review.git.GitRepositoryCoordinates;
import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

class GiteeFileContentFetcherTest
{
    private static final GitAccessContext ACCESS = GitAccessContext.of("test-token", "https://gitee.com");

    private MockWebServer server;
    private GiteeFileContentFetcher fetcher;
    private GitRepositoryCoordinates repository;

    @BeforeEach
    void setUp() throws IOException
    {
        server = new MockWebServer();
        server.start();
        fetcher = new GiteeFileContentFetcher(HttpUrl.get(server.url("/").toString()), 1000, 1000);
        repository = new GitRepositoryCoordinates("acme", "demo", "https://gitee.com/acme/demo");
    }

    @AfterEach
    void tearDown() throws IOException
    {
        server.shutdown();
    }

    @Test
    void fetchesBase64EncodedContent() throws InterruptedException
    {
        String encoded = Base64.getEncoder().encodeToString("hello world".getBytes(StandardCharsets.UTF_8));
        server.enqueue(new MockResponse().setResponseCode(200).setBody(
            "{\"encoding\":\"base64\",\"content\":\"" + encoded + "\"}"));

        GitFileContentResult result = fetcher.fetchFileContent(repository, ACCESS, "src/Main.java", "abc1234");

        assertTrue(result.success());
        assertEquals("hello world", result.content());
        var request = server.takeRequest();
        assertEquals("test-token", request.getRequestUrl().queryParameter("access_token"));
        assertTrue(request.getPath().contains("/contents/src/Main.java"));
    }

    @Test
    void rejectsInvalidPathAndRef()
    {
        assertFalse(fetcher.fetchFileContent(repository, ACCESS, "../evil", "abc1234").success());
        assertFalse(fetcher.fetchFileContent(repository, ACCESS, "src/Main.java", "bad-ref").success());
    }

    @Test
    void classifiesNotFound()
    {
        server.enqueue(new MockResponse().setResponseCode(404));
        GitFileContentResult result = fetcher.fetchFileContent(repository, ACCESS, "missing.txt", "abc1234");
        assertFalse(result.success());
        assertEquals("NOT_FOUND", result.failureReason());
    }
}
