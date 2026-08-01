package com.acr.review.git;

/** 拉取 PR 描述与提交说明，供大模型审查上下文使用。 */
public interface GitPullRequestMetadataFetcher
{
    String providerCode();

    GitPullRequestMetadata fetch(GitRepositoryCoordinates repository, String token, int prNumber);
}
