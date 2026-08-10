package com.acr.review.insight;

import org.springframework.stereotype.Component;

/**
 * RuoYi Quartz 调用入口（不写死 cron）。
 * 建议配置：
 * <ul>
 *   <li>insightStatsJobTask.fullRecalc() — 夜间全量近 35 天</li>
 *   <li>insightStatsJobTask.refreshRecent() — 分钟级昨日+今日</li>
 * </ul>
 */
@Component("insightStatsJobTask")
public class InsightStatsJobTask
{
    private final ReviewStatsAggregationService aggregationService;

    public InsightStatsJobTask(ReviewStatsAggregationService aggregationService)
    {
        this.aggregationService = aggregationService;
    }

    public void fullRecalc()
    {
        aggregationService.fullRecalc();
    }

    public void refreshRecent()
    {
        aggregationService.refreshRecent();
    }
}
