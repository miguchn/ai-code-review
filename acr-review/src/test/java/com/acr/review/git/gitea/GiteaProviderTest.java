package com.acr.review.git.gitea;

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

class GiteaProviderTest
{
    private MockWebServer server;
    private GiteaProvider provider;
    private GitAccessContext access;

    @BeforeEach
    void setUp() throws IOException
    {
        server = new MockWebServer();
        server.start();
        String serverUrl = baseServerUrl(server);
        access = GitAccessContext.of("test-token", serverUrl);
        provider = new GiteaProvider(1000, 1000);
    }

    @AfterEach
    void tearDown() throws IOException
    {
        server.shutdown();
    }

    @Test
    void parsesHttpsAndSshRepositoryUrls() throws IOException
    {
        String serverUrl = baseServerUrl(server);
        GitAccessContext ctx = GitAccessContext.of("t", serverUrl);

        GitRepositoryCoordinates https = provider.parseRepository(serverUrl + "/openai/codex.git", ctx);
        GitRepositoryCoordinates ssh = provider.parseRepository("git@" + server.getHostName() + ":openai/codex.git", ctx);

        assertEquals("openai", https.owner());
        assertEquals("codex", https.repository());
        assertEquals("openai/codex", https.fullPath());
        assertEquals(serverUrl + "/openai/codex", https.canonicalUrl());
        assertEquals(https.owner(), ssh.owner());
        assertEquals(https.repository(), ssh.repository());
    }

    @Test
    void parsesNestedRepositoryPath() throws IOException
    {
        String serverUrl = baseServerUrl(server);
        GitAccessContext ctx = GitAccessContext.of("t", serverUrl);

        GitRepositoryCoordinates nested = provider.parseRepository(serverUrl + "/org/team/demo.git", ctx);

        assertEquals("org/team", nested.owner());
        assertEquals("demo", nested.repository());
        assertEquals("org/team/demo", nested.fullPath());
    }

    @Test
    void rejectsMismatchedHost() throws IOException
    {
        GitAccessContext ctx = GitAccessContext.of("t", "https://gitea.example.com");
        assertThrows(IllegalArgumentException.class,
            () -> provider.parseRepository("https://other.example.com/openai/codex", ctx));
    }

    @Test
    void testsCredentialWithoutReturningToken() throws InterruptedException
    {
        server.enqueue(json(200, "{\"login\":\"octocat\"}"));

        GitConnectionResult result = provider.testCredential(access);

        assertTrue(result.isSuccess());
        var request = server.takeRequest();
        assertEquals("token test-token", request.getHeader("Authorization"));
        assertFalse(result.getMessage().contains("test-token"));
        assertEquals("/api/v1/user", request.getPath());
    }

    @Test
    void returnsRepositoryMetadata() throws InterruptedException
    {
        server.enqueue(json(200, "{\"login\":\"octocat\"}"));
        server.enqueue(json(200, "{\"default_branch\":\"main\",\"html_url\":\"http://localhost/openai/codex\"}"));

        GitRepositoryCoordinates repo = provider.parseRepository(baseServerUrl(server) + "/openai/codex", access);
        GitConnectionResult result = provider.testRepository(repo, access);

        assertTrue(result.isSuccess());
        assertEquals("main", result.getDefaultBranch());
        server.takeRequest();
        assertEquals("/api/v1/repos/openai/codex", server.takeRequest().getPath());
    }

    @Test
    void classifiesInvalidCredentialAndMissingRepository()
    {
        server.enqueue(json(401, "{}"));
        GitConnectionResult invalidCredential = provider.testCredential(access);
        assertEquals(GitConnectionFailure.INVALID_CREDENTIAL, invalidCredential.getFailure());

        server.enqueue(json(200, "{\"login\":\"octocat\"}"));
        server.enqueue(json(404, "{}"));
        GitRepositoryCoordinates repo = provider.parseRepository(baseServerUrl(server) + "/openai/missing", access);
        GitConnectionResult missingRepository = provider.testRepository(repo, access);
        assertEquals(GitConnectionFailure.REPOSITORY_NOT_FOUND, missingRepository.getFailure());
    }

    @Test
    void readsAllRepositoryBranchesWithPagination() throws InterruptedException
    {
        server.enqueue(json(200, "{\"login\":\"octocat\"}"));
        server.enqueue(json(200, "{\"default_branch\":\"main\",\"html_url\":\"http://localhost/openai/codex\"}"));
        server.enqueue(json(200, branches(0, 100)));
        server.enqueue(json(200, branches(100, 2)));

        GitRepositoryCoordinates repo = provider.parseRepository(baseServerUrl(server) + "/openai/codex", access);
        GitRepositoryInfoResult result = provider.readRepository(repo, access);

        assertTrue(result.success());
        assertEquals(102, result.branches().size());
        assertEquals("branch-101", result.branches().get(101));
        server.takeRequest();
        server.takeRequest();
        assertEquals("/api/v1/repos/openai/codex/branches?page=1&limit=100", server.takeRequest().getPath());
        assertEquals("/api/v1/repos/openai/codex/branches?page=2&limit=100", server.takeRequest().getPath());
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
