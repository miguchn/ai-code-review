package com.acr.review.git;

/** PR 审查工作区准备结果。 */
public record GitPullRequestWorkspaceResult(
    boolean success,
    String workingDirectory,
    String baseSha,
    String headSha,
    String failureType,
    String message
)
{
    public static GitPullRequestWorkspaceResult ok(String workingDirectory, String baseSha, String headSha)
    {
        return new GitPullRequestWorkspaceResult(true, workingDirectory, baseSha, headSha, null, "工作区准备完成");
    }

    public static GitPullRequestWorkspaceResult fail(String failureType, String message)
    {
        return new GitPullRequestWorkspaceResult(false, null, null, null, failureType, message);
    }
}
