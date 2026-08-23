package com.acr.review.job;

import org.springframework.stereotype.Component;
import com.acr.review.service.ReviewIssueOverdueService;

/**
 * RuoYi Quartz 调用入口（不写死 cron）。
 * 建议配置：issueOverdueJobTask.scan — 每日 08:30
 */
@Component("issueOverdueJobTask")
public class IssueOverdueJobTask
{
    private final ReviewIssueOverdueService overdueService;

    public IssueOverdueJobTask(ReviewIssueOverdueService overdueService)
    {
        this.overdueService = overdueService;
    }

    public void scan()
    {
        overdueService.scan();
    }
}
