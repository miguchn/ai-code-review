package com.acr.review.git.gitee;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import com.acr.review.git.GitRepositoryCoordinates;

class GiteePullRequestWorkspacePreparerTest
{
    @Test
    void resolveRemoteUrlUsesOauth2Token()
    {
        GitRepositoryCoordinates repo = new GitRepositoryCoordinates("acme", "demo", "https://gitee.com/acme/demo");
        String url = GiteePullRequestWorkspacePreparer.resolveRemoteUrl(repo, "my-token");

        assertTrue(url.startsWith("https://oauth2:my-token@gitee.com/"));
        assertTrue(url.endsWith("acme/demo.git"));
    }

    @Test
    void sanitizeRemovesTokenFromMessage()
    {
        String sanitized = GiteePullRequestWorkspacePreparer.sanitize("failed with my-secret-token", "my-secret-token");
        assertFalse(sanitized.contains("my-secret-token"));
        assertTrue(sanitized.contains("***"));
    }
}
