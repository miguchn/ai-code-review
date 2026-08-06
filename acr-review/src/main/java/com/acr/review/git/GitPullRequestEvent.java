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
    Integer changedFiles,
    boolean merged)
{
    /** 兼容旧构造（无 fullPath / 发起人 / 行数 / 文件数 / merged）。 */
    public GitPullRequestEvent(String deliveryId, String action, String repositoryOwner, String repositoryName,
                               Integer prNumber, String prTitle, String sourceBranch, String targetBranch,
                               String baseSha, String headSha)
    {
        this(deliveryId, action, repositoryOwner, repositoryName,
            repositoryOwner + "/" + repositoryName, prNumber, prTitle,
            sourceBranch, targetBranch, baseSha, headSha, null, null, null, null, false);
    }

    /** 兼容旧构造（无 fullPath / 文件数 / merged）。 */
    public GitPullRequestEvent(String deliveryId, String action, String repositoryOwner, String repositoryName,
                               Integer prNumber, String prTitle, String sourceBranch, String targetBranch,
                               String baseSha, String headSha, String prAuthor, Integer additions, Integer deletions)
    {
        this(deliveryId, action, repositoryOwner, repositoryName,
            repositoryOwner + "/" + repositoryName, prNumber, prTitle,
            sourceBranch, targetBranch, baseSha, headSha, prAuthor, additions, deletions, null, false);
    }

    /** 兼容旧构造（无 fullPath / merged）。 */
    public GitPullRequestEvent(String deliveryId, String action, String repositoryOwner, String repositoryName,
                               Integer prNumber, String prTitle, String sourceBranch, String targetBranch,
                               String baseSha, String headSha, String prAuthor, Integer additions, Integer deletions,
                               Integer changedFiles)
    {
        this(deliveryId, action, repositoryOwner, repositoryName,
            repositoryOwner + "/" + repositoryName, prNumber, prTitle,
            sourceBranch, targetBranch, baseSha, headSha, prAuthor, additions, deletions, changedFiles, false);
    }

    /** 兼容旧构造（无 merged）。 */
    public GitPullRequestEvent(String deliveryId, String action, String repositoryOwner, String repositoryName,
                               String repositoryFullPath, Integer prNumber, String prTitle, String sourceBranch,
                               String targetBranch, String baseSha, String headSha, String prAuthor,
                               Integer additions, Integer deletions, Integer changedFiles)
    {
        this(deliveryId, action, repositoryOwner, repositoryName, repositoryFullPath, prNumber, prTitle,
            sourceBranch, targetBranch, baseSha, headSha, prAuthor, additions, deletions, changedFiles, false);
    }

    /**
     * 是否为 PR 关闭/合并类生命周期事件（不创建评审任务，只走问题联动）。
     * 覆盖 GitHub/Gitea 的 closed、GitLab/Gitee 的 close/merge。
     */
    public boolean isCloseLifecycle()
    {
        if (action == null || action.isBlank())
        {
            return false;
        }
        return switch (action)
        {
            case "closed", "close", "merged", "merge" -> true;
            default -> false;
        };
    }
}
