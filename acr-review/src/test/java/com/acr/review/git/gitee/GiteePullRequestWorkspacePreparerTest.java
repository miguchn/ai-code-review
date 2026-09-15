package com.acr.review.git.gitee;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import com.acr.review.git.GitRepositoryCoordinates;

class GiteePullRequestWorkspacePreparerTest
{
    @Test
    void buildsFullFetchArgsWithoutDepth()
    {
        String[] args = GiteePullRequestWorkspacePreparer.buildFetchArgs("origin", "abc1234");
        org.junit.jupiter.api.Assertions.assertArrayEquals(
            new String[] { "fetch", "origin", "abc1234" }, args);
        org.junit.jupiter.api.Assertions.assertFalse(java.util.Arrays.asList(args).contains("--depth"));
    }

    @Test
    void resolveRemoteUrlDoesNotEmbedToken()
    {
        GitRepositoryCoordinates repo = new GitRepositoryCoordinates("acme", "demo", "https://gitee.com/acme/demo");
        String url = GiteePullRequestWorkspacePreparer.resolveRemoteUrl(repo);

        assertTrue(url.startsWith("https://gitee.com/"));
        assertTrue(url.endsWith("acme/demo.git"));
        assertFalse(url.contains("@"));
    }

    @Test
    void sanitizeRemovesTokenFromMessage()
    {
        String sanitized = GiteePullRequestWorkspacePreparer.sanitize("failed with my-secret-token", "my-secret-token");
        assertFalse(sanitized.contains("my-secret-token"));
        assertTrue(sanitized.contains("***"));
    }
}
