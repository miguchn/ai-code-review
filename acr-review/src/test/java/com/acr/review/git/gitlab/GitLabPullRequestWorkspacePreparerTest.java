package com.acr.review.git.gitlab;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import com.acr.review.domain.ReviewPipelineConstants;
import com.acr.review.git.GitAccessContext;
import com.acr.review.git.GitCommandRunner;
import com.acr.review.git.GitPullRequestWorkspaceRequest;
import com.acr.review.git.GitPullRequestWorkspaceResult;
import com.acr.review.git.GitRepositoryCoordinates;

class GitLabPullRequestWorkspacePreparerTest
{
    private final GitLabPullRequestWorkspacePreparer preparer = new GitLabPullRequestWorkspacePreparer(
        org.mockito.Mockito.mock(GitCommandRunner.class), 30);

    @Test
    void buildsFullFetchArgsWithoutDepth()
    {
        String[] args = GitLabPullRequestWorkspacePreparer.buildFetchArgs("origin", "abc1234");
        org.junit.jupiter.api.Assertions.assertArrayEquals(
            new String[] { "fetch", "origin", "abc1234" }, args);
        org.junit.jupiter.api.Assertions.assertFalse(java.util.Arrays.asList(args).contains("--depth"));
    }

    @Test
    void sanitizesTokenFromMessage()
    {
        String message = GitLabPullRequestWorkspacePreparer.sanitize("failed token=glpat_secret detail", "glpat_secret");
        assertFalse(message.contains("glpat_secret"));
        assertTrue(message.contains("***"));
    }

    @Test
    void buildsBasicOauth2AuthorizationHeader()
    {
        String header = GitLabPullRequestWorkspacePreparer.buildAuthorizationExtraHeader("glpat_test_token");
        assertTrue(header.startsWith("Authorization: Basic "));
        assertFalse(header.contains("PRIVATE-TOKEN"));
        assertFalse(header.contains("Bearer"));
        String encoded = header.substring("Authorization: Basic ".length());
        String decoded = new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8);
        assertEquals("oauth2:glpat_test_token", decoded);
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
