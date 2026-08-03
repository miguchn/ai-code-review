package com.acr.review.git.gitlab;

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
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

class GitLabProviderTest
{
    private MockWebServer server;
    private GitLabProvider provider;
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
        provider = new GitLabProvider(1000, 1000);
    }

    @AfterEach
    void tearDown() throws IOException
    {
        server.shutdown();
    }

    @Test
    void parsesHttpsAndSshNestedRepositoryPaths()
    {
        String base = access.serverUrl();
        GitRepositoryCoordinates https = provider.parseRepository(
            base + "/acme/platform/demo-service.git", access);
        GitRepositoryCoordinates ssh = provider.parseRepository(
            "git@" + server.getHostName() + ":acme/platform/demo-service.git", access);

        assertEquals("acme/platform", https.owner());
        assertEquals("demo-service", https.repository());
        assertEquals("acme/platform/demo-service", https.fullPath());
        assertEquals(base + "/acme/platform/demo-service", https.canonicalUrl());
        assertEquals(https.fullPath(), ssh.fullPath());
        assertThrows(IllegalArgumentException.class,
            () -> provider.parseRepository("https://gitlab.com/other/project", access));
    }

    @Test
    void testsCredentialWithoutReturningToken() throws InterruptedException
    {
        server.enqueue(json(200, "{\"username\":\"devops\"}"));

        GitConnectionResult result = provider.testCredential(access);

        assertTrue(result.isSuccess());
        assertEquals("test-token", server.takeRequest().getHeader("PRIVATE-TOKEN"));
        assertFalse(result.getMessage().contains("test-token"));
    }

    @Test
    void classifiesInvalidCredential()
    {
        server.enqueue(json(401, "{}"));
        GitConnectionResult invalidCredential = provider.testCredential(access);
        assertEquals(GitConnectionFailure.INVALID_CREDENTIAL, invalidCredential.getFailure());
    }

    @Test
    void returnsRepositoryMetadata() throws InterruptedException
    {
        server.enqueue(json(200, "{\"username\":\"devops\"}"));
        server.enqueue(json(200, "{\"default_branch\":\"main\",\"web_url\":\""
            + access.serverUrl() + "/acme/demo\"}"));

        GitRepositoryCoordinates repo = provider.parseRepository(access.serverUrl() + "/acme/demo", access);
        GitConnectionResult result = provider.testRepository(repo, access);

        assertTrue(result.isSuccess());
        assertEquals("main", result.getDefaultBranch());
        server.takeRequest();
        assertTrue(server.takeRequest().getPath().contains("/api/v4/projects/acme%2Fdemo"));
    }

    @Test
    void readsAllRepositoryBranchesWithPagination() throws InterruptedException
    {
        server.enqueue(json(200, "{\"username\":\"devops\"}"));
        server.enqueue(json(200, "{\"default_branch\":\"main\",\"web_url\":\""
            + access.serverUrl() + "/acme/demo\"}"));
        server.enqueue(json(200, branches(0, 100)));
        server.enqueue(json(200, branches(100, 2)));

        GitRepositoryCoordinates repo = provider.parseRepository(access.serverUrl() + "/acme/demo", access);
        GitRepositoryInfoResult result = provider.readRepository(repo, access);

        assertTrue(result.success());
        assertEquals(102, result.branches().size());
        assertEquals("branch-101", result.branches().get(101));
        server.takeRequest();
        server.takeRequest();
        assertTrue(server.takeRequest().getPath().contains("repository/branches"));
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
