package com.acr.review.git;

import java.util.Optional;

/** PR/MR 总结评论读写能力（平台适配边界）。 */
public interface GitPullRequestCommentClient
{
    String providerCode();

    /**
     * 分页查找正文含指定标记的第一条评论。
     *
     * @throws GitPullRequestCommentException 网络/鉴权/限流等失败
     */
    Optional<GitPullRequestComment> findCommentWithMarker(GitRepositoryCoordinates repository,
                                                          GitAccessContext access,
                                                          int prNumber,
                                                          String marker);

    /**
     * 新建总结评论。
     *
     * @throws GitPullRequestCommentException 失败
     */
    GitPullRequestComment createIssueComment(GitRepositoryCoordinates repository,
                                             GitAccessContext access,
                                             int prNumber,
                                             String body);

    /**
     * 更新已有总结评论。
     *
     * @throws GitPullRequestCommentException 失败
     */
    GitPullRequestComment updateIssueComment(GitRepositoryCoordinates repository,
                                             GitAccessContext access,
                                             String commentId,
                                             String body);
}
