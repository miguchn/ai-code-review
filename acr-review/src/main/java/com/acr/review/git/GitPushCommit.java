package com.acr.review.git;

import java.util.Date;

/** 推送载荷中的单条提交事实（平台无关）。 */
public record GitPushCommit(
    String sha,
    String authorName,
    String authorEmail,
    Date timestamp,
    String messageFirstLine)
{
}
