package com.acr.review.git.github;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import com.acr.review.git.GitAccessContext;
import com.acr.review.domain.ReviewPipelineConstants;
import com.acr.review.git.GitPullRequestWorkspaceRequest;
import com.acr.review.git.GitPullRequestWorkspaceResult;
import com.acr.review.git.GitRepositoryCoordinates;

class GitHubPullRequestWorkspacePreparerTest
{
    private static final GitAccessContext ACCESS = GitAccessContext.of("token", "https://github.com");

    private final GitHubPullRequestWorkspacePreparer preparer = new GitHubPullRequestWorkspacePreparer(30);

    @Test
    void sanitizesTokenFromMessage()
    {
        String message = GitHubPullRequestWorkspacePreparer.sanitize("failed token=ghp_secret_value detail", "ghp_secret_value");
        assertFalse(message.contains("ghp_secret_value"));
        assertTrue(message.contains("***"));
    }

    @Test
    void rejectsMissingSha()
    {
        GitPullRequestWorkspaceResult result = preparer.prepare(new GitPullRequestWorkspaceRequest(
            new GitRepositoryCoordinates("o", "r", "https://github.com/o/r"),
            ACCESS, "", "head", "/tmp/acr-test"));
        assertFalse(result.success());
        assertEquals(ReviewPipelineConstants.FAILURE_WORKSPACE_PREPARE, result.failureType());
    }

    @Test
    void rejectsMalformedShaBeforeSpawningGit()
    {
        // 以 - 开头的值会被 git 当作选项，必须在入口拒绝
        GitPullRequestWorkspaceResult result = preparer.prepare(new GitPullRequestWorkspaceRequest(
            new GitRepositoryCoordinates("o", "r", "https://github.com/o/r"),
            ACCESS, "--upload-pack=touch /tmp/pwn", "abc1234", "/tmp/acr-test"));
        assertFalse(result.success());
        assertEquals(ReviewPipelineConstants.FAILURE_WORKSPACE_PREPARE, result.failureType());
    }

    @Test
    void sanitizeRedactsCommonTokenPatterns()
    {
        // 即使完整 token 未被直接引用，GitHub 格式 token 也应被正则兜底脱敏
        String message = GitHubPullRequestWorkspacePreparer.sanitize(
            "fatal: Authentication failed for 'https://github.com/o/r.git/' (ghp_AbCdEfGhIjKlMnOpQrStUvWx used)", null);
        assertFalse(message.contains("ghp_AbCdEfGhIjKlMnOpQrStUvWx"));

        String pat = GitHubPullRequestWorkspacePreparer.sanitize(
            "token github_pat_11ABCDEFG0abcdefgh_ijklmnop leaked", "other");
        assertFalse(pat.contains("github_pat_11ABCDEFG0abcdefgh_ijklmnop"));
    }

    @Test
    void remoteUrlPrefersCanonicalProjectUrl()
    {
        // GHE/自定义域名场景使用项目接入时校验过的仓库地址
        String url = GitHubPullRequestWorkspacePreparer.resolveRemoteUrl(
            new GitRepositoryCoordinates("o", "r", "https://git.corp.example.com/o/r.git"));
        assertEquals("https://git.corp.example.com/o/r.git", url);

        String trailing = GitHubPullRequestWorkspacePreparer.resolveRemoteUrl(
            new GitRepositoryCoordinates("o", "r", "https://github.com/o/r/"));
        assertEquals("https://github.com/o/r.git", trailing);
    }
}
