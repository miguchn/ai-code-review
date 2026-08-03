package com.acr.review.git.gitlab;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import com.acr.review.domain.ReviewPipelineConstants;
import com.acr.review.git.GitAccessContext;
import com.acr.review.git.GitPullRequestWorkspaceRequest;
import com.acr.review.git.GitPullRequestWorkspaceResult;
import com.acr.review.git.GitRepositoryCoordinates;

class GitLabPullRequestWorkspacePreparerTest
{
    private final GitLabPullRequestWorkspacePreparer preparer = new GitLabPullRequestWorkspacePreparer(30);

    @Test
    void sanitizesTokenFromMessage()
    {
        String message = GitLabPullRequestWorkspacePreparer.sanitize("failed token=glpat_secret detail", "glpat_secret");
        assertFalse(message.contains("glpat_secret"));
        assertTrue(message.contains("***"));
    }

    @Test
    void rejectsMissingSha()
    {
        GitPullRequestWorkspaceResult result = preparer.prepare(new GitPullRequestWorkspaceRequest(
            new GitRepositoryCoordinates("o", "r", "o/r", "https://gitlab.example.com/o/r"),
            GitAccessContext.of("token", "https://gitlab.example.com"),
            "", "head", "/tmp/acr-test"));
        assertFalse(result.success());
        assertEquals(ReviewPipelineConstants.FAILURE_WORKSPACE_PREPARE, result.failureType());
    }

    @Test
    void resolveRemoteUrlEmbedsOauth2Token()
    {
        String url = GitLabPullRequestWorkspacePreparer.resolveRemoteUrl(
            new GitRepositoryCoordinates("group", "repo", "group/repo", "https://gitlab.example.com/group/repo"),
            "my-token");
        assertEquals("https://oauth2:my-token@gitlab.example.com/group/repo.git", url);
    }
}
