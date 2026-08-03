package com.acr.review.git.gitee;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.IOException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.acr.review.git.GitAccessContext;
import com.acr.review.git.GitConnectionFailure;
import com.acr.review.git.GitConnectionResult;
import com.acr.review.git.GitRepositoryCoordinates;
import com.acr.review.git.GitRepositoryInfoResult;
import okhttp3.HttpUrl;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

class GiteeProviderTest
{
    private static final GitAccessContext ACCESS = GitAccessContext.of("test-token", "https://gitee.com");

    private MockWebServer server;
    private GiteeProvider provider;

    @BeforeEach
    void setUp() throws IOException
    {
        server = new MockWebServer();
        server.start();
        provider = new GiteeProvider(HttpUrl.get(server.url("/").toString()), 1000, 1000);
    }

    @AfterEach
    void tearDown() throws IOException
    {
        server.shutdown();
    }

    @Test
    void parsesHttpsAndSshRepositoryUrls()
    {
        GitRepositoryCoordinates https = provider.parseRepository("https://gitee.com/openai/codex.git", ACCESS);
        GitRepositoryCoordinates ssh = provider.parseRepository("git@gitee.com:openai/codex.git", ACCESS);

        assertEquals("openai", https.owner());
        assertEquals("codex", https.repository());
        assertEquals("openai/codex", https.fullPath());
        assertEquals("https://gitee.com/openai/codex", https.canonicalUrl());
        assertEquals(https.owner(), ssh.owner());
        assertEquals(https.repository(), ssh.repository());
        assertThrows(IllegalArgumentException.class,
            () -> provider.parseRepository("https://github.com/openai/codex", ACCESS));
    }

    @Test
    void testsCredentialWithAccessTokenQueryParam() throws InterruptedException
    {
        server.enqueue(json(200, "{\"login\":\"octocat\"}"));

        GitConnectionResult result = provider.testCredential(ACCESS);

        assertTrue(result.isSuccess());
        assertEquals("test-token", server.takeRequest().getRequestUrl().queryParameter("access_token"));
        assertFalse(result.getMessage().contains("test-token"));
    }

    @Test
    void returnsRepositoryMetadata() throws InterruptedException
    {
        server.enqueue(json(200, "{\"login\":\"octocat\"}"));
        server.enqueue(json(200, "{\"default_branch\":\"main\",\"html_url\":\"https://gitee.com/openai/codex\"}"));

        GitConnectionResult result = provider.testRepository(
            provider.parseRepository("https://gitee.com/openai/codex", ACCESS), ACCESS);

        assertTrue(result.isSuccess());
        assertEquals("main", result.getDefaultBranch());
        server.takeRequest();
        assertEquals("/repos/openai/codex", server.takeRequest().getRequestUrl().encodedPath());
    }

    @Test
    void classifiesInvalidCredentialAndMissingRepository()
    {
        server.enqueue(json(401, "{}"));
        GitConnectionResult invalidCredential = provider.testCredential(ACCESS);
        assertEquals(GitConnectionFailure.INVALID_CREDENTIAL, invalidCredential.getFailure());

        server.enqueue(json(200, "{\"login\":\"octocat\"}"));
        server.enqueue(json(404, "{}"));
        GitConnectionResult missingRepository = provider.testRepository(
            provider.parseRepository("https://gitee.com/openai/missing", ACCESS), ACCESS);
        assertEquals(GitConnectionFailure.REPOSITORY_NOT_FOUND, missingRepository.getFailure());
    }

    @Test
    void readsAllRepositoryBranchesWithPagination() throws InterruptedException
    {
        server.enqueue(json(200, "{\"login\":\"octocat\"}"));
        server.enqueue(json(200, "{\"default_branch\":\"main\",\"html_url\":\"https://gitee.com/openai/codex\"}"));
        server.enqueue(json(200, branches(0, 100)));
        server.enqueue(json(200, branches(100, 2)));

        GitRepositoryInfoResult result = provider.readRepository(
            provider.parseRepository("https://gitee.com/openai/codex", ACCESS), ACCESS);

        assertTrue(result.success());
        assertEquals(102, result.branches().size());
        assertEquals("branch-101", result.branches().get(101));
        server.takeRequest();
        server.takeRequest();
        assertTrue(server.takeRequest().getPath().startsWith("/repos/openai/codex/branches"));
    }

    private MockResponse json(int status, String body)
    {
        return new MockResponse().setResponseCode(status)
            .setHeader("Content-Type", "application/json")
            .setBody(body);
    }

    private String branches(int start, int count)
    {
        StringBuilder body = new StringBuilder("[");
        for (int index = 0; index < count; index++)
        {
            if (index > 0)
            {
                body.append(',');
            }
            body.append("{\"name\":\"branch-").append(start + index).append("\"}");
        }
        return body.append(']').toString();
    }
}
