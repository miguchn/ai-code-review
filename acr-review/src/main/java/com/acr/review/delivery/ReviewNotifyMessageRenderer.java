package com.acr.review.delivery;

import java.util.ArrayList;
import java.util.List;
import com.acr.common.utils.StringUtils;
import com.acr.review.domain.result.ReviewScopeStats;
import com.acr.review.domain.result.ReviewTopIssue;

/** 将审查摘要渲染为 IM 行式文本（纯函数，无 IO）。 */
public final class ReviewNotifyMessageRenderer
{
    private ReviewNotifyMessageRenderer()
    {
    }

    public static String renderSuccess(ReviewSummaryContent content)
    {
        if (content == null)
        {
            return "";
        }
        String conclusionLabel = StringUtils.defaultIfEmpty(content.getConclusionLabel(), "--");
        String icon = conclusionIcon(conclusionLabel);

        StringBuilder sb = new StringBuilder();
        sb.append("### ").append(icon).append(" AI Code Review · ").append(conclusionLabel).append('\n');

        sb.append("总分 ");
        sb.append(content.getTotalScore() == null ? "--" : content.getTotalScore() + "/100");
        sb.append(" · PR #");
        sb.append(content.getPrNumber() == null ? "--" : content.getPrNumber());
        if (StringUtils.isNotEmpty(content.getPrTitle()))
        {
            sb.append(' ').append(content.getPrTitle());
        }
        sb.append('\n');

        appendMetaLine(sb, content);

        sb.append("\nTop 3 重点问题\n");
        List<ReviewTopIssue> issues = content.getTopIssues();
        if (issues.isEmpty())
        {
            sb.append("暂无重点问题\n");
        }
        else
        {
            int index = 1;
            for (ReviewTopIssue issue : issues)
            {
                appendImIssue(sb, index++, issue);
            }
        }

        sb.append('\n').append(formatImScopeStats(content.getScopeStats())).append('\n');
        appendLinkLine(sb, content);
        return sb.toString().trim();
    }

    public static String renderFailed(ReviewSummaryContent content)
    {
        if (content == null)
        {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("### ❌ AI Code Review · 执行失败\n");

        sb.append("PR #");
        sb.append(content.getPrNumber() == null ? "--" : content.getPrNumber());
        if (StringUtils.isNotEmpty(content.getPrTitle()))
        {
            sb.append(' ').append(content.getPrTitle());
        }
        sb.append(" · ").append(content.repositoryFullName()).append('\n');

        sb.append("失败类型：");
        sb.append(StringUtils.defaultIfEmpty(content.getFailureTypeLabel(), "未知"));
        sb.append(" · 任务 #");
        sb.append(content.getTaskId() == null ? "--" : content.getTaskId());
        sb.append('\n');

        if (StringUtils.isNotEmpty(content.getDetailUrl()))
        {
            sb.append("详情：").append(content.getDetailUrl());
        }
        return sb.toString().trim();
    }

    static String conclusionIcon(String conclusionLabel)
    {
        if ("通过".equals(conclusionLabel))
        {
            return "✅";
        }
        if ("建议修改".equals(conclusionLabel))
        {
            return "⚠️";
        }
        if ("高风险".equals(conclusionLabel))
        {
            return "🚨";
        }
        return "ℹ️";
    }

    static String formatImScopeStats(ReviewScopeStats stats)
    {
        if (stats == null)
        {
            return "范围统计：—";
        }
        List<String> parts = new ArrayList<>();
        if (stats.getIncludedFiles() != null)
        {
            parts.add("纳入 " + stats.getIncludedFiles());
        }
        if (stats.getExcludedFiles() != null)
        {
            parts.add("排除 " + stats.getExcludedFiles());
        }
        if (stats.getExpandedFiles() != null)
        {
            parts.add("扩展 " + stats.getExpandedFiles());
        }
        if (stats.getNewCount() != null)
        {
            parts.add("新增 " + stats.getNewCount());
        }
        if (stats.getExistingCount() != null)
        {
            parts.add("存量 " + stats.getExistingCount());
        }
        if (parts.isEmpty())
        {
            return "范围统计：—";
        }
        return "范围统计：" + String.join(" · ", parts);
    }

    private static void appendMetaLine(StringBuilder sb, ReviewSummaryContent content)
    {
        List<String> parts = new ArrayList<>();
        parts.add(content.repositoryFullName());
        if (StringUtils.isNotEmpty(content.getPrAuthor()))
        {
            parts.add(content.getPrAuthor());
        }
        if (StringUtils.isNotEmpty(content.getSourceBranch()) || StringUtils.isNotEmpty(content.getTargetBranch()))
        {
            parts.add(StringUtils.defaultIfEmpty(content.getSourceBranch(), "--")
                + " → " + StringUtils.defaultIfEmpty(content.getTargetBranch(), "--"));
        }
        String changeScale = formatChangeScale(content);
        if (StringUtils.isNotEmpty(changeScale))
        {
            parts.add(changeScale);
        }
        sb.append(String.join(" · ", parts));
    }

    private static String formatChangeScale(ReviewSummaryContent content)
    {
        if (content.getChangedFiles() == null && content.getAdditions() == null && content.getDeletions() == null)
        {
            return "";
        }
        StringBuilder scale = new StringBuilder();
        if (content.getChangedFiles() != null)
        {
            scale.append(content.getChangedFiles()).append(" 文件");
        }
        if (content.getAdditions() != null || content.getDeletions() != null)
        {
            if (scale.length() > 0)
            {
                scale.append(' ');
            }
            scale.append('+').append(content.getAdditions() == null ? 0 : content.getAdditions());
            scale.append('/').append('−');
            scale.append(content.getDeletions() == null ? 0 : content.getDeletions());
        }
        return scale.toString();
    }

    private static void appendImIssue(StringBuilder sb, int index, ReviewTopIssue issue)
    {
        String title = StringUtils.defaultIfEmpty(issue.getTitle(), "未命名问题");
        sb.append(index).append(". [")
            .append(ReviewCommentBodyRenderer.severityLabel(issue.getSeverity()))
            .append('·')
            .append(ReviewCommentBodyRenderer.originLabel(issue.getOrigin()))
            .append("] ")
            .append(title);
        String locate = formatImLocate(issue);
        if (StringUtils.isNotEmpty(locate))
        {
            sb.append(" — ").append(locate);
        }
        sb.append('\n');
        String description = truncate(issue.getDescription(), ReviewDeliveryConstants.IM_MAX_ISSUE_DESCRIPTION_CHARS);
        if (StringUtils.isNotEmpty(description))
        {
            sb.append("   ").append(description.replace("\n", " ")).append('\n');
        }
    }

    private static String formatImLocate(ReviewTopIssue issue)
    {
        StringBuilder locate = new StringBuilder();
        if (StringUtils.isNotEmpty(issue.getFilePath()))
        {
            locate.append(issue.getFilePath());
        }
        String lines = formatLines(issue.getStartLine(), issue.getEndLine());
        if (StringUtils.isNotEmpty(lines))
        {
            if (locate.length() > 0)
            {
                locate.append(' ');
            }
            locate.append(lines);
        }
        return locate.toString();
    }

    private static String formatLines(Integer start, Integer end)
    {
        if (start == null && end == null)
        {
            return "";
        }
        if (start != null && end != null && !start.equals(end))
        {
            return "L" + start + "-" + end;
        }
        Integer line = start != null ? start : end;
        return "L" + line;
    }

    private static void appendLinkLine(StringBuilder sb, ReviewSummaryContent content)
    {
        boolean hasPr = StringUtils.isNotEmpty(content.getPrUrl());
        boolean hasDetail = StringUtils.isNotEmpty(content.getDetailUrl());
        if (!hasPr && !hasDetail)
        {
            return;
        }
        if (hasPr)
        {
            sb.append("PR：").append(content.getPrUrl());
        }
        if (hasDetail)
        {
            if (hasPr)
            {
                sb.append("  ");
            }
            sb.append("详情：").append(content.getDetailUrl());
        }
    }

    private static String truncate(String value, int max)
    {
        if (value == null)
        {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() <= max)
        {
            return trimmed;
        }
        return trimmed.substring(0, max) + "...";
    }
}
