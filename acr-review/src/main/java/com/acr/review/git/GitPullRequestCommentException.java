package com.acr.review.git;

/** GitHub 评论读写失败（消息已脱敏，可落库/展示）。 */
public class GitPullRequestCommentException extends RuntimeException
{
    public GitPullRequestCommentException(String message)
    {
        super(message);
    }

    public GitPullRequestCommentException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
