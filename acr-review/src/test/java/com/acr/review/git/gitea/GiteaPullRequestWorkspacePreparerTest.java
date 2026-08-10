package com.acr.review.git.gitea;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import com.acr.review.domain.ReviewPipelineConstants;
import com.acr.review.git.GitAccessContext;
import com.acr.review.git.GitCommandRunner;
import com.acr.review.git.GitPullRequestWorkspaceRequest;
import com.acr.review.git.GitPullRequestWorkspaceResult;
import com.acr.review.git.GitRepositoryCoordinates;

class GiteaPullRequestWorkspacePreparerTest
{
    private final GiteaPullRequestWorkspacePreparer preparer = new GiteaPullRequestWorkspacePreparer(
        org.mockito.Mockito.mock(GitCommandRunner.class), 30);

    @Test
    void buildsFullFetchArgsWithoutDepth()
    {
        String[] args = GiteaPullRequestWorkspacePreparer.buildFetchArgs("origin", "abc1234");
        org.junit.jupiter.api.Assertions.assertArrayEquals(
            new String[] { "fetch", "origin", "abc1234" }, args);
        org.junit.jupiter.api.Assertions.assertFalse(java.util.Arrays.asList(args).contains("--depth"));
    }

    @Test
    void sanitizesTokenFromMessage()
    {
        String message = GiteaPullRequestWorkspacePreparer.sanitize("failed token=abc123secret detail", "abc123secret");
        assertFalse(message.contains("abc123secret"));
        assertTrue(message.contains("***"));
    }

    @Test
    void rejectsMissingSha()
    {
        GitAccessContext access = GitAccessContext.of("token", "https://gitea.example.com");
        GitPullRequestWorkspaceResult result = preparer.prepare(new GitPullRequestWorkspaceRequest(
            new GitRepositoryCoordinates("o", "r", "o/r", "https://gitea.example.com/o/r"),
            access, "", "head", "/tmp/acr-test"));
        assertFalse(result.success());
        assertEquals(ReviewPipelineConstants.FAILURE_WORKSPACE_PREPARE, result.failureType());
    }

    @Test
    void rejectsMalformedShaBeforeSpawningGit()
    {
        GitAccessContext access = GitAccessContext.of("token", "https://gitea.example.com");
        GitPullRequestWorkspaceResult result = preparer.prepare(new GitPullRequestWorkspaceRequest(
            new GitRepositoryCoordinates("o", "r", "o/r", "https://gitea.example.com/o/r"),
            access, "--upload-pack=touch /tmp/pwn", "abc1234", "/tmp/acr-test"));
        assertFalse(result.success());
        assertEquals(ReviewPipelineConstants.FAILURE_WORKSPACE_PREPARE, result.failureType());
    }

    @Test
    void remoteUrlEmbedsTokenForSelfHosted()
    {
        GitAccessContext access = GitAccessContext.of("mytoken", "https://gitea.example.com:8443");
        GitRepositoryCoordinates repo = new GitRepositoryCoordinates("org", "repo", "org/repo",
            "https://gitea.example.com:8443/org/repo");

        String url = GiteaPullRequestWorkspacePreparer.resolveRemoteUrl(access, repo, "mytoken");

        assertEquals("https://mytoken@gitea.example.com:8443/org/repo.git", url);
        assertFalse(url.contains("mytoken@gitea.example.com:8443/org/repo.git".replace("mytoken", "***")));
    }

    @Test
    void remoteUrlUsesFullPathForNestedRepo()
    {
        GitAccessContext access = GitAccessContext.of("tok", "http://gitea.local:3000");
        GitRepositoryCoordinates repo = new GitRepositoryCoordinates("org/team", "demo", "org/team/demo",
            "http://gitea.local:3000/org/team/demo");

        String url = GiteaPullRequestWorkspacePreparer.resolveRemoteUrl(access, repo, "tok");

        assertEquals("http://tok@gitea.local:3000/org/team/demo.git", url);
    }
}
