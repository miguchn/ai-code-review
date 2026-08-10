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

    /** 当前适配器是否支持行内评论；默认否。 */
    default boolean supportsInlineComments()
    {
        return false;
    }

    /**
     * 在 PR/MR 指定文件行创建行内评论。
     *
     * @throws GitInlineCommentUnsupportedException 平台不支持行内评论
     * @throws GitPullRequestCommentException 网络/鉴权等失败
     */
    default GitPullRequestComment createInlineComment(GitRepositoryCoordinates repository,
                                                      GitAccessContext access,
                                                      int prNumber,
                                                      GitInlineCommentRequest request)
    {
        throw new GitInlineCommentUnsupportedException(
            providerCode() + " 不支持行内评论");
    }

    /**
     * 分页查找正文含指定标记的行内评论；默认空（不支持或未实现查找）。
     *
     * @throws GitPullRequestCommentException 网络/鉴权等失败
     */
    default Optional<GitPullRequestComment> findInlineCommentWithMarker(
        GitRepositoryCoordinates repository,
        GitAccessContext access,
        int prNumber,
        String marker)
    {
        return Optional.empty();
    }
}
