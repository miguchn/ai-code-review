package com.acr.review.delivery;

/** 审查结果投递稳定常量。 */
public final class ReviewDeliveryConstants
{
    public static final String CHANNEL_GITHUB_PR_SUMMARY = "GITHUB_PR_SUMMARY_COMMENT";
    public static final String CHANNEL_GITLAB_MR_SUMMARY = "GITLAB_MR_SUMMARY_COMMENT";
    public static final String CHANNEL_GITEE_PR_SUMMARY = "GITEE_PR_SUMMARY_COMMENT";
    public static final String CHANNEL_GITEA_PR_SUMMARY = "GITEA_PR_SUMMARY_COMMENT";
    public static final String CHANNEL_GITHUB_PR_INLINE = "GITHUB_PR_INLINE_COMMENT";
    public static final String CHANNEL_GITLAB_MR_INLINE = "GITLAB_MR_INLINE_COMMENT";
    public static final String CHANNEL_GITEE_PR_INLINE = "GITEE_PR_INLINE_COMMENT";
    public static final String CHANNEL_GITEA_PR_INLINE = "GITEA_PR_INLINE_COMMENT";
    public static final String CHANNEL_DINGTALK_ROBOT = "DINGTALK_ROBOT";
    public static final String CHANNEL_WECOM_ROBOT = "WECOM_ROBOT";
    public static final String CHANNEL_FEISHU_BOT = "FEISHU_BOT";
    /** 项目启用通知但渠道配置不可解析时的可运维占位渠道。 */
    public static final String CHANNEL_IM_NOTIFICATION = "IM_NOTIFICATION";

    /** 评论正文固定标记：用于查找并更新同一 PR 上的 ACR 总结评论。 */
    public static final String COMMENT_MARKER = "<!-- acr-review-summary -->";

    /** 投递错误码：旧 head 任务被更新结论围栏抑制，不得覆盖当前评论。 */
    public static final String ERROR_SKIPPED_STALE = "SKIPPED_STALE";

    /** 总结评论中嵌入的 run 代次标记前缀（完整形态见 {@link #commentRunMarker(Long)}）。 */
    public static final String COMMENT_RUN_MARKER_PREFIX = "<!-- acr-run:";

    /** 构造嵌入 run_id 的隐藏标记；检索仍只用 {@link #COMMENT_MARKER}，保证存量评论可命中。 */
    public static String commentRunMarker(Long runId)
    {
        if (runId == null)
        {
            return "";
        }
        return COMMENT_RUN_MARKER_PREFIX + runId + " -->";
    }

    /** 行内评论默认严重度白名单。 */
    public static final String DEFAULT_INLINE_SEVERITIES = "CRITICAL,HIGH";

    /** 审查成功后全部结论都通知。 */
    public static final String NOTIFY_POLICY_ALL = "ALL";
    /** 仅 WARN / BLOCK 结论通知；新项目默认值。 */
    public static final String NOTIFY_POLICY_RISK_ONLY = "RISK_ONLY";
    /** 仅 BLOCK 结论通知。 */
    public static final String NOTIFY_POLICY_BLOCK_ONLY = "BLOCK_ONLY";
    public static final int MAX_NOTIFY_COOLDOWN_MINUTES = 1440;

    /** 行内评论描述截断上限。 */
    public static final int INLINE_MAX_DESCRIPTION_CHARS = 200;

    /** 行内评论建议截断上限。 */
    public static final int INLINE_MAX_SUGGESTION_CHARS = 120;

    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_MANUAL = "MANUAL";
    public static final String STATUS_SKIPPED = "SKIPPED";

    /** 任务 SUCCESS 后的外部投递（GitHub 总结评论或 IM）。 */
    public static final String TRIGGER_TASK_SUCCESS = "TASK_SUCCESS";
    /** 任务 FAILED 后的 IM 失败简讯投递。 */
    public static final String TRIGGER_TASK_FAILED = "TASK_FAILED";
    /** 问题处置后重渲染 PR 总结评论。 */
    public static final String TRIGGER_ISSUE_DISPOSITION = "ISSUE_DISPOSITION";
    /** 投递记录页或任务详情手动重试/补发。 */
    public static final String TRIGGER_MANUAL_RETRY = "MANUAL_RETRY";

    public static final String ERROR_CONFIGURATION = "DELIVERY_CONFIGURATION";
    public static final String ERROR_EXTERNAL_CALL = "DELIVERY_EXTERNAL_CALL";
    public static final String ERROR_LEASE_EXPIRED = "DELIVERY_LEASE_EXPIRED";
    /** SUCCESS 结论未达到项目通知策略门槛。 */
    public static final String ERROR_NOTIFY_POLICY_SUPPRESSED = "NOTIFY_POLICY_SUPPRESSED";
    /** 低优先级通知命中项目冷却窗口。 */
    public static final String ERROR_NOTIFY_RATE_LIMITED = "NOTIFY_RATE_LIMITED";
    /** 运营人员标记人工已处理，不再自动投递。 */
    public static final String ERROR_MANUAL_HANDLED = "MANUAL_HANDLED";

    public static final String PROVIDER_GITHUB = "GITHUB";

    /** GitHub Top3 单条描述最大字符数。 */
    public static final int MAX_ISSUE_DESCRIPTION_CHARS = 500;

    /** IM 单条问题描述最大字符数（压平换行后截断）。 */
    public static final int IM_MAX_DESCRIPTION_CHARS = 120;

    /** IM 单条问题建议最大字符数（压平换行后截断）。 */
    public static final int IM_MAX_SUGGESTION_CHARS = 120;

    /** IM 总结文本最大字符数（压平换行后截断）。 */
    public static final int IM_MAX_SUMMARY_CHARS = 200;

    /** Push 审查结论范围标注（记录详情 / IM 总结段）。 */
    public static final String PUSH_SCOPE_NOTE = "本结论仅覆盖本次推送的变更（base..head 增量）";

    /** 企微 markdown 正文最大字节数。 */
    public static final int WECOM_MAX_MARKDOWN_BYTES = 4096;

    /** 投递正文快照 kind：IM 通知。 */
    public static final String SNAPSHOT_KIND_IM = "IM";

    /** 投递正文快照 kind：PR/MR 总结评论。 */
    public static final String SNAPSHOT_KIND_SUMMARY_COMMENT = "SUMMARY_COMMENT";

    /** 投递正文快照 kind：PR/MR 行内评论。 */
    public static final String SNAPSHOT_KIND_INLINE_COMMENT = "INLINE_COMMENT";

    /** 稳定错误码：平台不支持行内评论。 */
    public static final String ERROR_INLINE_UNSUPPORTED = "DELIVERY_INLINE_UNSUPPORTED";

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

    /** 按平台返回行内评论投递渠道编码。 */
    public static String inlineChannelForProvider(String provider)
    {
        if (provider == null)
        {
            return CHANNEL_GITHUB_PR_INLINE;
        }
        return switch (normalizeProvider(provider))
        {
            case "GITLAB" -> CHANNEL_GITLAB_MR_INLINE;
            case "GITEE" -> CHANNEL_GITEE_PR_INLINE;
            case "GITEA" -> CHANNEL_GITEA_PR_INLINE;
            default -> CHANNEL_GITHUB_PR_INLINE;
        };
    }

    public static boolean isInlineCommentChannel(String channel)
    {
        return CHANNEL_GITHUB_PR_INLINE.equals(channel)
            || CHANNEL_GITLAB_MR_INLINE.equals(channel)
            || CHANNEL_GITEE_PR_INLINE.equals(channel)
            || CHANNEL_GITEA_PR_INLINE.equals(channel);
    }

    /** 行内幂等键：{provider}:{projectId}:{issueId}:INLINE_COMMENT */
    public static String inlineIdempotencyKey(String provider, Long projectId, Long issueId)
    {
        return normalizeProvider(provider) + ":" + projectId + ":" + issueId + ":INLINE_COMMENT";
    }

    /** 行内评论正文隐藏标记。 */
    public static String inlineCommentMarker(Long issueId)
    {
        return "<!-- acr:inline:issue-" + issueId + " -->";
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
