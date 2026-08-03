package com.acr.review.delivery;

/** 审查结果投递稳定常量。 */
public final class ReviewDeliveryConstants
{
    public static final String CHANNEL_GITHUB_PR_SUMMARY = "GITHUB_PR_SUMMARY_COMMENT";
    public static final String CHANNEL_GITLAB_MR_SUMMARY = "GITLAB_MR_SUMMARY_COMMENT";
    public static final String CHANNEL_GITEE_PR_SUMMARY = "GITEE_PR_SUMMARY_COMMENT";
    public static final String CHANNEL_GITEA_PR_SUMMARY = "GITEA_PR_SUMMARY_COMMENT";
    public static final String CHANNEL_DINGTALK_ROBOT = "DINGTALK_ROBOT";
    public static final String CHANNEL_WECOM_ROBOT = "WECOM_ROBOT";
    public static final String CHANNEL_FEISHU_BOT = "FEISHU_BOT";

    /** 评论正文固定标记：用于查找并更新同一 PR 上的 ACR 总结评论。 */
    public static final String COMMENT_MARKER = "<!-- acr-review-summary -->";

    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_SKIPPED = "SKIPPED";

    /** 任务 SUCCESS/FAILED 后的外部投递（GitHub 总结评论或 IM）。 */
    public static final String TRIGGER_TASK_SUCCESS = "TASK_SUCCESS";
    /** 问题处置后重渲染 PR 总结评论。 */
    public static final String TRIGGER_ISSUE_DISPOSITION = "ISSUE_DISPOSITION";
    /** 投递记录页或任务详情手动重试/补发。 */
    public static final String TRIGGER_MANUAL_RETRY = "MANUAL_RETRY";

    public static final String PROVIDER_GITHUB = "GITHUB";

    /** GitHub Top3 单条描述最大字符数。 */
    public static final int MAX_ISSUE_DESCRIPTION_CHARS = 500;

    /** IM Top3 单条描述最大字符数（企微字节上限更严）。 */
    public static final int IM_MAX_ISSUE_DESCRIPTION_CHARS = 150;

    /** 企微 markdown 正文最大字节数。 */
    public static final int WECOM_MAX_MARKDOWN_BYTES = 4096;

    /** 失败原因落库上限。 */
    public static final int MAX_FAILURE_MESSAGE_CHARS = 500;

    /** 查找标记时每页评论数。 */
    public static final int COMMENT_PAGE_SIZE = 100;

    /** 最多翻页数（合计最多 300 条）。 */
    public static final int COMMENT_MAX_PAGES = 3;

    public static final String UI_BASE_URL_CONFIG_KEY = "review.ui.base-url";

    public static final String TEST_MESSAGE_TITLE = "AI Code Review 测试";
    public static final String TEST_MESSAGE_BODY = "### AI Code Review 渠道测试\n这是一条测试消息，用于验证通知渠道配置是否可用。";

    private ReviewDeliveryConstants()
    {
    }

    /** 幂等键：{provider}:{projectId}:{prNumber}:SUMMARY_COMMENT（GitHub 旧键不变）。 */
    public static String idempotencyKey(String provider, Long projectId, Integer prNumber)
    {
        return normalizeProvider(provider) + ":" + projectId + ":" + prNumber + ":SUMMARY_COMMENT";
    }

    /** 兼容 GitHub 存量调用。 */
    public static String idempotencyKey(Long projectId, Integer prNumber)
    {
        return idempotencyKey(PROVIDER_GITHUB, projectId, prNumber);
    }

    /** 按平台返回总结评论投递渠道编码。 */
    public static String channelForProvider(String provider)
    {
        if (provider == null)
        {
            return CHANNEL_GITHUB_PR_SUMMARY;
        }
        return switch (normalizeProvider(provider))
        {
            case "GITLAB" -> CHANNEL_GITLAB_MR_SUMMARY;
            case "GITEE" -> CHANNEL_GITEE_PR_SUMMARY;
            case "GITEA" -> CHANNEL_GITEA_PR_SUMMARY;
            default -> CHANNEL_GITHUB_PR_SUMMARY;
        };
    }

    public static boolean isSummaryCommentChannel(String channel)
    {
        return CHANNEL_GITHUB_PR_SUMMARY.equals(channel)
            || CHANNEL_GITLAB_MR_SUMMARY.equals(channel)
            || CHANNEL_GITEE_PR_SUMMARY.equals(channel)
            || CHANNEL_GITEA_PR_SUMMARY.equals(channel);
    }

    /** IM 幂等键：{channelType}:{taskId}:REVIEW_DONE */
    public static String imIdempotencyKey(String channelType, Long taskId)
    {
        return channelType + ":" + taskId + ":REVIEW_DONE";
    }

    public static boolean isImChannel(String channel)
    {
        return CHANNEL_DINGTALK_ROBOT.equals(channel)
            || CHANNEL_WECOM_ROBOT.equals(channel)
            || CHANNEL_FEISHU_BOT.equals(channel);
    }

    public static boolean isSupportedNotifyChannelType(String channelType)
    {
        return isImChannel(channelType);
    }

    private static String normalizeProvider(String provider)
    {
        return provider.trim().toUpperCase(java.util.Locale.ROOT);
    }
}
