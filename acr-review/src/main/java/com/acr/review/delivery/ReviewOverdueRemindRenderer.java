package com.acr.review.delivery;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import com.acr.common.utils.StringUtils;
import com.acr.review.domain.ReviewIssue;
import com.acr.review.domain.ReviewIssueConstants;

/** 逾期聚合提醒正文（纯函数）。 */
public final class ReviewOverdueRemindRenderer
{
    public static final String TITLE = "AI Code Review · 逾期问题提醒";

    private ReviewOverdueRemindRenderer()
    {
    }

    public static String render(List<ReviewIssue> issues, List<ReviewAssigneeMention> mentions, String channelType)
    {
        List<ReviewIssue> rows = issues == null ? List.of() : new ArrayList<>(issues);
        rows.sort(Comparator.comparingInt(ReviewOverdueRemindRenderer::severityRank)
            .thenComparing(issue -> issue.getIssueId() == null ? 0L : issue.getIssueId()));
        StringBuilder sb = new StringBuilder();
        sb.append("### ").append(TITLE).append("\n\n");
        int total = rows.size();
        int limit = Math.min(total, ReviewIssueConstants.MAX_ISSUES_IN_OVERDUE_REMIND);
        for (int i = 0; i < limit; i++)
        {
            ReviewIssue issue = rows.get(i);
            sb.append("- ").append(StringUtils.defaultIfEmpty(issue.getTitle(), ReviewIssueConstants.DEFAULT_TITLE));
            String assignee = StringUtils.defaultIfEmpty(issue.getAssigneeName(), "未指派");
            sb.append(" · ").append(assignee).append('\n');
        }
        sb.append("\n共 ").append(total).append(" 个逾期问题，请前往问题台账处理\n");
        ReviewNotifyMessageRenderer.appendAssignees(sb, mentions, channelType);
        return sb.toString().trim();
    }

    private static int severityRank(ReviewIssue issue)
    {
        if (issue == null || issue.getSeverity() == null)
        {
            return 99;
        }
        return switch (issue.getSeverity().trim().toUpperCase(Locale.ROOT))
        {
            case "CRITICAL" -> 0;
            case "HIGH" -> 1;
            case "MEDIUM" -> 2;
            case "LOW" -> 3;
            case "INFO" -> 4;
            default -> 50;
        };
    }
}
