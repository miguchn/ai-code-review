package com.acr.review.service;

import com.acr.review.domain.WorkbenchModels;
import com.acr.review.domain.WorkbenchSummary;
import com.acr.review.domain.WorkbenchTrend;

/** 首页工作台汇总。 */
public interface IWorkbenchService
{
    WorkbenchSummary getSummary();

    /** 审查结论按天趋势（近 days 天，补零）；无 review:record:list 权限返回 null。 */
    WorkbenchTrend getTrend(int days);

    /** 启用模型健康摘要（登录可调，字段白名单脱敏）。 */
    WorkbenchModels getModelHealth();
}
