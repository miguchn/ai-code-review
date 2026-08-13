package com.acr.review.insight;

/** 数据洞察模块稳定常量。 */
public final class InsightConstants
{
    public static final String PERM_OVERVIEW_VIEW = "insight:overview:view";
    public static final String PERM_PROJECT_VIEW = "insight:project:view";
    public static final String PERM_TEAM_VIEW = "insight:team:view";
    public static final String PERM_IDENTITY_MANAGE = "insight:identity:manage";
    public static final String PERM_TOKEN_VIEW = "insight:token:view";

    public static final String CONFIG_METRICS_VERSION = "insight.metrics.dict.version";
    public static final String DEFAULT_METRICS_VERSION = "m12-v1";

    public static final String ZONE_ID = "Asia/Shanghai";

    /** 夜间全量重算窗口（天）。 */
    public static final int FULL_RECALC_DAYS = 35;

    public static final int DEFAULT_RANGE_DAYS = 7;
    public static final int MAX_RANGE_DAYS = 90;

    public static final String EVENT_ACCEPTED = "ACCEPTED";
    public static final String EVENT_IGNORED = "IGNORED";

    public static final String DELIVERY_SUCCESS = "SUCCESS";

    private InsightConstants()
    {
    }
}
