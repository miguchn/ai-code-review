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
import com.acr.review.git.GitPullRequestDiffResult;
import com.acr.review.git.GitRepositoryCoordinates;
import com.acr.review.scope.DiffChangeType;
import com.acr.review.scope.DiffParseResult;
import com.acr.review.scope.UnifiedDiffParser;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

class GitLabPullRequestDiffFetcherTest
{
    private MockWebServer server;
    private GitLabPullRequestDiffFetcher fetcher;
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
        fetcher = new GitLabPullRequestDiffFetcher(1000, 1000);
        repository = new GitRepositoryCoordinates("openai", "codex", "openai/codex",
            serverUrl + "/openai/codex");
    }

    @AfterEach
    void tearDown() throws IOException
    {
        server.shutdown();
    }

    @Test
    void fetchesDiffFromCompareApi() throws InterruptedException
    {
        server.enqueue(json(200, """
            {"diffs":[{"diff":"diff --git a/A.java b/A.java"},{"diff":"+line"}]}
            """));

        GitPullRequestDiffResult result = fetcher.fetchDiff(repository, access, "abc1234", "def5678");

        assertTrue(result.success());
        assertTrue(result.diffContent().contains("diff --git"));
        assertTrue(result.diffContent().contains("+line"));
        var recorded = server.takeRequest();
        assertEquals("test-token", recorded.getHeader("PRIVATE-TOKEN"));
        assertTrue(recorded.getPath().contains("/repository/compare"));
    }

    @Test
    void rejectsMalformedSha()
    {
        GitPullRequestDiffResult result = fetcher.fetchDiff(repository, access, "--evil", "def5678");
        assertFalse(result.success());
        assertEquals(ReviewPipelineConstants.FAILURE_WORKSPACE_PREPARE, result.failureType());
    }

    @Test
    void classifiesCredentialErrors()
    {
        server.enqueue(new MockResponse().setResponseCode(401));
        GitPullRequestDiffResult result = fetcher.fetchDiff(
            repository, GitAccessContext.of("bad-token", access.serverUrl()), "abc1234", "def5678");
        assertFalse(result.success());
        assertEquals(ReviewPipelineConstants.FAILURE_CREDENTIAL_ERROR, result.failureType());
    }

    @Test
    void rebuildsFileHeadersFromCompareMetadata()
    {
        // 真实 GitLab Compare API 形状：diff 字段只有裸 hunk，文件路径在 old_path/new_path 元数据里
        server.enqueue(json(200, """
            {"diffs":[{"old_path":"src/app.js","new_path":"src/app.js","a_mode":"100644","b_mode":"100644",
            "new_file":false,"renamed_file":false,"deleted_file":false,
            "diff":"@@ -1,3 +1,4 @@\\n var a = 1;\\n+var b = 2;\\n var c = 3;\\n var d = 4;"}]}
            """));

        GitPullRequestDiffResult result = fetcher.fetchDiff(repository, access, "abc1234", "def5678");

        assertTrue(result.success());
        String content = result.diffContent();
        assertTrue(content.contains("diff --git a/src/app.js b/src/app.js"), content);
        assertTrue(content.contains("--- a/src/app.js"), content);
        assertTrue(content.contains("+++ b/src/app.js"), content);

        DiffParseResult parsed = new UnifiedDiffParser().parse(content);
        assertEquals(1, parsed.files().size());
        assertEquals("src/app.js", parsed.files().get(0).effectivePath());
        assertEquals(DiffChangeType.MODIFIED, parsed.files().get(0).changeType());
        assertTrue(parsed.files().get(0).hasHunks());
    }

    @Test
    void rebuildsNewDeleteRenameHeadersFromCompareMetadata()
    {
        server.enqueue(json(200, """
            {"diffs":[
            {"old_path":"new.js","new_path":"new.js","new_file":true,"renamed_file":false,"deleted_file":false,"b_mode":"100644",
             "diff":"@@ -0,0 +1,2 @@\\n+hello\\n+world"},
            {"old_path":"gone.js","new_path":"gone.js","new_file":false,"renamed_file":false,"deleted_file":true,"a_mode":"100644",
             "diff":"@@ -1,2 +0,0 @@\\n-hello\\n-world"},
            {"old_path":"a.js","new_path":"b.js","new_file":false,"renamed_file":true,"deleted_file":false,
             "diff":""}]}
            """));

        GitPullRequestDiffResult result = fetcher.fetchDiff(repository, access, "abc1234", "def5678");

        assertTrue(result.success());
        DiffParseResult parsed = new UnifiedDiffParser().parse(result.diffContent());
        assertEquals(3, parsed.files().size());
        assertEquals(DiffChangeType.ADDED, parsed.files().get(0).changeType());
        assertEquals(DiffChangeType.DELETED, parsed.files().get(1).changeType());
        assertEquals(DiffChangeType.RENAMED, parsed.files().get(2).changeType());
        assertEquals("b.js", parsed.files().get(2).effectivePath());
    }

    private MockResponse json(int status, String body)
    {
        return new MockResponse().setResponseCode(status)
            .setHeader("Content-Type", "application/json")
            .setBody(body);
    }
}
