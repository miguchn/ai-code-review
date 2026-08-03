package com.acr.review.git;

import java.util.Optional;

/** PR Issue 级评论读写能力（平台适配边界）。 */
public interface GitPullRequestCommentClient
{
    String providerCode();

    /**
     * 分页查找正文含指定标记的第一条评论。
     *
     * @throws GitPullRequestCommentException 网络/鉴权/限流等失败
     */
    Optional<GitPullRequestComment> findCommentWithMarker(GitRepositoryCoordinates repository,
                                                          String token,
                                                          int prNumber,
                                                          String marker);

    /**
     * 新建 Issue 评论。
     *
     * @throws GitPullRequestCommentException 失败
     */
    GitPullRequestComment createIssueComment(GitRepositoryCoordinates repository,
                                             String token,
                                             int prNumber,
                                             String body);

    /**
     * 更新已有 Issue 评论。
     *
     * @throws GitPullRequestCommentException 失败
     */
    GitPullRequestComment updateIssueComment(GitRepositoryCoordinates repository,
                                             String token,
                                             String commentId,
                                             String body);
}
