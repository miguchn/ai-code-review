package com.acr.review.domain;

/** 工作台卡片类型与文案常量（稳定值，不建字典）。 */
public final class WorkbenchConstants
{
    public static final String CARD_ISSUE_AWAITING_CONFIRM = "ISSUE_AWAITING_CONFIRM";
    public static final String CARD_ISSUE_EXISTING_CONFIRM = "ISSUE_EXISTING_CONFIRM";
    public static final String CARD_ISSUE_AWAITING_FIX = "ISSUE_AWAITING_FIX";
    public static final String CARD_ISSUE_RECHECKING = "ISSUE_RECHECKING";
    public static final String CARD_HIGH_RISK_CONCLUSION = "HIGH_RISK_CONCLUSION";
    public static final String CARD_TASK_FAILED = "TASK_FAILED";
    public static final String CARD_DELIVERY_FAILED = "DELIVERY_FAILED";

    public static final String TITLE_ISSUE_AWAITING_CONFIRM = "待确认问题";
    public static final String TITLE_ISSUE_EXISTING_CONFIRM = "存量待确认";
    public static final String TITLE_ISSUE_AWAITING_FIX = "待修复问题";
    public static final String TITLE_ISSUE_RECHECKING = "待复核问题";
    public static final String TITLE_HIGH_RISK_CONCLUSION = "高风险结论";
    public static final String TITLE_TASK_FAILED = "失败任务";
    public static final String TITLE_DELIVERY_FAILED = "投递失败";

    public static final String SUBTITLE_ORIGIN_NEW = "本次变更";
    public static final String SUBTITLE_ORIGIN_EXISTING = "存量代码";
    public static final String SUBTITLE_ALL_ORIGIN = "全部归属";
    public static final String SUBTITLE_RECHECK_PENDING = "修复待验证";
    public static final String SUBTITLE_HIGH_RISK_WINDOW = "近 7 天";

    /** 模型最近检测派生状态（lastCheckResult 原始文案不下发判断逻辑）。 */
    public static final String CHECK_STATUS_SUCCESS = "SUCCESS";
    public static final String CHECK_STATUS_FAILED = "FAILED";
    public static final String CHECK_STATUS_NEVER = "NEVER";

    /** 最近动态结论伪枚举：任务执行失败（review_conclusion 无此值，仅前端色点映射用）。 */
    public static final String RECENT_CONCLUSION_FAILED = "FAILED";

    /** 审查结论趋势窗口：默认与上限（天）。 */
    public static final int TREND_DEFAULT_DAYS = 14;
    public static final int TREND_MAX_DAYS = 31;

    public static final String LINK_ISSUE = "/review/issue";
    public static final String LINK_RECORD = "/review/record";
    public static final String LINK_TASK = "/review/task";
    public static final String LINK_DELIVERY = "/notify/delivery";
    public static final String LINK_TASK_DETAIL_PREFIX = "/review/task-detail/index/";

    public static final String RECENT_TYPE_TASK = "TASK";

    public static final String PERM_PROJECT_LIST = "review:project:list";
    public static final String PERM_ISSUE_LIST = "review:issue:list";
    public static final String PERM_RECORD_LIST = "review:record:list";
    public static final String PERM_TASK_LIST = "review:task:list";
    public static final String PERM_DELIVERY_LIST = "review:delivery:list";

    /** 高风险结论窗口：含首尾共 7 天。 */
    public static final int HIGH_RISK_WINDOW_DAYS = 7;

    private WorkbenchConstants()
    {
    }
}
