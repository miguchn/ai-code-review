package com.acr.review.git;

/**
 * 行内评论创建请求（平台适配边界入参）。
 * GitHub 用 headSha 作 commit_id；GitLab position 三 SHA 由适配器内部解析。
 */
public record GitInlineCommentRequest(
    String path,
    Integer startLine,
    Integer endLine,
    String body,
    String headSha)
{
}
