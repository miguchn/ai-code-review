package com.acr.review.git;

import java.util.Date;
import java.util.List;

/** Provider 读取仓库元数据和分支的统一结果。 */
public record GitRepositoryInfoResult(
    boolean success,
    GitConnectionFailure failure,
    String message,
    String repositoryUrl,
    String repositoryOwner,
    String repositoryName,
    String defaultBranch,
    List<String> branches,
    Date syncedAt)
{
    public static GitRepositoryInfoResult success(GitRepositoryCoordinates repository,
                                                   String repositoryUrl,
                                                   String defaultBranch,
                                                   List<String> branches)
    {
        return new GitRepositoryInfoResult(true, null, "仓库信息读取成功", repositoryUrl,
            repository.owner(), repository.repository(), defaultBranch, List.copyOf(branches), new Date());
    }

    public static GitRepositoryInfoResult failure(GitConnectionFailure failure, String message)
    {
        return new GitRepositoryInfoResult(false, failure, message, null, null, null, null, List.of(), new Date());
    }
}
