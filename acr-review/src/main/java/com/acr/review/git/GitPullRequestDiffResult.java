package com.acr.review.git;

/** PR Diff 拉取结果。 */
public record GitPullRequestDiffResult(
    boolean success,
    String diffContent,
    String failureType,
    String message
)
{
    public static GitPullRequestDiffResult ok(String diffContent)
    {
        return new GitPullRequestDiffResult(true, diffContent == null ? "" : diffContent, null, "Diff 获取成功");
    }

    public static GitPullRequestDiffResult fail(String failureType, String message)
    {
        return new GitPullRequestDiffResult(false, null, failureType, message);
    }
}
