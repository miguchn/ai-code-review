package com.acr.review.git;

/** PR 审查工作区准备请求（平台无关）。 */
public record GitPullRequestWorkspaceRequest(
    GitRepositoryCoordinates repository,
    String token,
    String baseSha,
    String headSha,
    String workingDirectory
)
{
}
