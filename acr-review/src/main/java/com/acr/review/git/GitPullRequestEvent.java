package com.acr.review.git;

/** 平台无关的合并请求事件业务事实。平台事件头、签名与载荷差异不进此对象。 */
public record GitPullRequestEvent(
    String deliveryId,
    String action,
    String repositoryOwner,
    String repositoryName,
    String repositoryFullPath,
    Integer prNumber,
    String prTitle,
    String sourceBranch,
    String targetBranch,
    String baseSha,
    String headSha,
    String prAuthor,
    Integer additions,
    Integer deletions,
    Integer changedFiles)
{
    /** 兼容旧构造（无 fullPath / 发起人 / 行数 / 文件数）。 */
    public GitPullRequestEvent(String deliveryId, String action, String repositoryOwner, String repositoryName,
                               Integer prNumber, String prTitle, String sourceBranch, String targetBranch,
                               String baseSha, String headSha)
    {
        this(deliveryId, action, repositoryOwner, repositoryName,
            repositoryOwner + "/" + repositoryName, prNumber, prTitle,
            sourceBranch, targetBranch, baseSha, headSha, null, null, null, null);
    }

    /** 兼容旧构造（无 fullPath / 文件数）。 */
    public GitPullRequestEvent(String deliveryId, String action, String repositoryOwner, String repositoryName,
                               Integer prNumber, String prTitle, String sourceBranch, String targetBranch,
                               String baseSha, String headSha, String prAuthor, Integer additions, Integer deletions)
    {
        this(deliveryId, action, repositoryOwner, repositoryName,
            repositoryOwner + "/" + repositoryName, prNumber, prTitle,
            sourceBranch, targetBranch, baseSha, headSha, prAuthor, additions, deletions, null);
    }

    /** 兼容旧构造（无 fullPath）。 */
    public GitPullRequestEvent(String deliveryId, String action, String repositoryOwner, String repositoryName,
                               Integer prNumber, String prTitle, String sourceBranch, String targetBranch,
                               String baseSha, String headSha, String prAuthor, Integer additions, Integer deletions,
                               Integer changedFiles)
    {
        this(deliveryId, action, repositoryOwner, repositoryName,
            repositoryOwner + "/" + repositoryName, prNumber, prTitle,
            sourceBranch, targetBranch, baseSha, headSha, prAuthor, additions, deletions, changedFiles);
    }
}
