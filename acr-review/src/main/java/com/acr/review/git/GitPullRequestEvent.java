package com.acr.review.git;

/** 平台无关的 PR 事件业务事实。平台事件头、签名与载荷差异不进此对象。 */
public record GitPullRequestEvent(
    String deliveryId,
    String action,
    String repositoryOwner,
    String repositoryName,
    Integer prNumber,
    String prTitle,
    String sourceBranch,
    String targetBranch,
    String baseSha,
    String headSha)
{
}
