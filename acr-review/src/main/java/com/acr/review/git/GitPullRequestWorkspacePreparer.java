package com.acr.review.git;

/** 为审查引擎准备包含 base/head 提交的本地 Git 工作区。 */
public interface GitPullRequestWorkspacePreparer
{
    String providerCode();

    GitPullRequestWorkspaceResult prepare(GitPullRequestWorkspaceRequest request);
}
