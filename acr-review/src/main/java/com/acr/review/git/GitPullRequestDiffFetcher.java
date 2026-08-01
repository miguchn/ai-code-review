package com.acr.review.git;

/** 拉取 PR base...head 统一 Diff，供大模型审查使用。 */
public interface GitPullRequestDiffFetcher
{
    String providerCode();

    GitPullRequestDiffResult fetchDiff(GitRepositoryCoordinates repository, String token, String baseSha, String headSha);
}
