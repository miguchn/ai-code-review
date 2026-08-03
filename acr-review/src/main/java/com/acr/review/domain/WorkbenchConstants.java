package com.acr.review.domain;

/** 工作台卡片类型与文案常量（稳定值，不建字典）。 */
public final class WorkbenchConstants
{
    public static final String CARD_ISSUE_AWAITING_CONFIRM = "ISSUE_AWAITING_CONFIRM";
    public static final String CARD_ISSUE_AWAITING_FIX = "ISSUE_AWAITING_FIX";
    public static final String CARD_HIGH_RISK_CONCLUSION = "HIGH_RISK_CONCLUSION";
    public static final String CARD_TASK_FAILED = "TASK_FAILED";
    public static final String CARD_DELIVERY_FAILED = "DELIVERY_FAILED";

    public static final String TITLE_ISSUE_AWAITING_CONFIRM = "待确认问题";
    public static final String TITLE_ISSUE_AWAITING_FIX = "待修复问题";
    public static final String TITLE_HIGH_RISK_CONCLUSION = "高风险结论（7天）";
    public static final String TITLE_TASK_FAILED = "失败任务";
    public static final String TITLE_DELIVERY_FAILED = "投递失败";

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

    public static final int RECENT_LIMIT = 5;

    private WorkbenchConstants()
    {
    }
}
