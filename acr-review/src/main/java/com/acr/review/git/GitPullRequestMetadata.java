package com.acr.review.git;

/** PR 元数据：描述、提交说明、发起人 login 与变更规模。 */
public record GitPullRequestMetadata(
    String prDescription,
    String commitMessages,
    String prAuthor,
    Integer additions,
    Integer deletions,
    Integer changedFiles,
    boolean fetched,
    String message
)
{
    public static GitPullRequestMetadata ok(String prDescription, String commitMessages,
                                            String prAuthor, Integer additions, Integer deletions,
                                            Integer changedFiles)
    {
        return new GitPullRequestMetadata(
            prDescription == null ? "" : prDescription,
            commitMessages == null ? "" : commitMessages,
            blankToNull(prAuthor),
            additions,
            deletions,
            changedFiles,
            true,
            "PR 元数据获取成功");
    }

    public static GitPullRequestMetadata unavailable(String message)
    {
        return new GitPullRequestMetadata("", "", null, null, null, null, false, message);
    }

    private static String blankToNull(String value)
    {
        if (value == null || value.isBlank())
        {
            return null;
        }
        return value.trim();
    }
}
