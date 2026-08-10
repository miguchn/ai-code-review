package com.acr.review.git;

import java.util.List;

/** 平台无关的推送事件业务事实。平台事件头、签名与载荷差异不进此对象。 */
public record GitPushEvent(
    String deliveryId,
    String repositoryOwner,
    String repositoryName,
    String repositoryFullPath,
    String branch,
    String beforeSha,
    String afterSha,
    String pusher,
    Integer commitCount,
    String headCommitMessage,
    boolean created,
    boolean deleted,
    List<GitPushCommit> commits)
{
    public GitPushEvent
    {
        commits = commits == null ? List.of() : List.copyOf(commits);
    }
}
