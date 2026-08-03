package com.acr.review.domain;

import java.util.Date;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.acr.review.git.GitConnectionFailure;

/** 项目表单使用的仓库信息读取结果。 */
public record ReviewRepositoryInfo(
    boolean success,
    GitConnectionFailure failure,
    String message,
    String repositoryUrl,
    String repositoryOwner,
    String repositoryName,
    String repositoryFullPath,
    String defaultBranch,
    List<String> branches,
    List<String> recommendedTargetBranches,
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss") Date syncedAt)
{
}
