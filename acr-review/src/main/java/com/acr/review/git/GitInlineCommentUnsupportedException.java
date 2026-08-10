package com.acr.review.git;

/**
 * 平台明确拒绝行内评论能力（如 Gitee 不支持 path/line）时抛出。
 * 调度层捕获后将投递意图置为 SKIPPED，不再自动重试。
 */
public class GitInlineCommentUnsupportedException extends GitPullRequestCommentException
{
    public GitInlineCommentUnsupportedException(String message)
    {
        super(message);
    }

    public GitInlineCommentUnsupportedException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
