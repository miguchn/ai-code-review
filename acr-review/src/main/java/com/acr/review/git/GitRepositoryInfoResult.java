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
    Date syncedAt,
    String mainLanguage)
{
    public static GitRepositoryInfoResult success(GitRepositoryCoordinates repository,
                                                   String repositoryUrl,
                                                   String defaultBranch,
                                                   List<String> branches)
    {
        return success(repository, repositoryUrl, defaultBranch, branches, null);
    }

    public static GitRepositoryInfoResult success(GitRepositoryCoordinates repository,
                                                   String repositoryUrl,
                                                   String defaultBranch,
                                                   List<String> branches,
                                                   String mainLanguage)
    {
        return new GitRepositoryInfoResult(true, null, "仓库信息读取成功", repositoryUrl,
            repository.owner(), repository.repository(), defaultBranch, List.copyOf(branches), new Date(), mainLanguage);
    }

    public static GitRepositoryInfoResult failure(GitConnectionFailure failure, String message)
    {
        return new GitRepositoryInfoResult(false, failure, message, null, null, null, null, List.of(), new Date(), null);
    }
}
