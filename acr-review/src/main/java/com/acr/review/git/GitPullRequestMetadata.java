package com.acr.review.git;

/** PR 元数据（描述与提交说明），供大模型审查上下文使用。 */
public record GitPullRequestMetadata(
    String prDescription,
    String commitMessages,
    boolean fetched,
    String message
)
{
    public static GitPullRequestMetadata ok(String prDescription, String commitMessages)
    {
        return new GitPullRequestMetadata(
            prDescription == null ? "" : prDescription,
            commitMessages == null ? "" : commitMessages,
            true,
            "PR 元数据获取成功");
    }

    public static GitPullRequestMetadata unavailable(String message)
    {
        return new GitPullRequestMetadata("", "", false, message);
    }
}
