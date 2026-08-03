package com.acr.review.domain;

/** 问题台账状态、动作与来源常量。 */
public final class ReviewIssueConstants
{
    public static final String STATUS_AWAITING_CONFIRM = "AWAITING_CONFIRM";
    public static final String STATUS_AWAITING_FIX = "AWAITING_FIX";
    public static final String STATUS_CLOSED = "CLOSED";
    public static final String STATUS_IGNORED = "IGNORED";
    public static final String STATUS_FALSE_POSITIVE = "FALSE_POSITIVE";
    /** 字典预留，本期 API 拒绝。 */
    public static final String STATUS_RECHECKING = "RECHECKING";

    public static final String ACTION_CONFIRM = "CONFIRM";
    public static final String ACTION_CLOSE = "CLOSE";
    public static final String ACTION_DISMISS = "DISMISS";

    public static final String CLOSE_SOURCE_MANUAL = "manual";
    public static final String CLOSE_SOURCE_AUTO_RECHECK = "auto_recheck";

    public static final String ORIGIN_NEW = "NEW";
    public static final String ORIGIN_EXISTING = "EXISTING";

    public static final String PROVIDER_GITHUB = "GITHUB";

    public static final String DEFAULT_TITLE = "未命名问题";

    public static final int MAX_RESOLVE_NOTE_CHARS = 500;
    public static final int MAX_DISPOSITION_NOTE_IN_COMMENT = 80;

    private ReviewIssueConstants()
    {
    }

    public static boolean isTerminal(String status)
    {
        return STATUS_CLOSED.equals(status)
            || STATUS_IGNORED.equals(status)
            || STATUS_FALSE_POSITIVE.equals(status);
    }

    public static boolean isOpen(String status)
    {
        return STATUS_AWAITING_CONFIRM.equals(status) || STATUS_AWAITING_FIX.equals(status);
    }

    public static String statusLabel(String status)
    {
        if (status == null)
        {
            return "--";
        }
        return switch (status)
        {
            case STATUS_AWAITING_CONFIRM -> "待确认";
            case STATUS_AWAITING_FIX -> "待修复";
            case STATUS_CLOSED -> "已关闭";
            case STATUS_IGNORED -> "已忽略";
            case STATUS_FALSE_POSITIVE -> "误报";
            case STATUS_RECHECKING -> "复核中";
            default -> status;
        };
    }
}
