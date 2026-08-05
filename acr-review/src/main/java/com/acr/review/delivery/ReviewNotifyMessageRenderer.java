package com.acr.review.delivery;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import com.acr.common.utils.DateUtils;
import com.acr.common.utils.StringUtils;
import com.acr.review.domain.ReviewIssueConstants;
import com.acr.review.domain.result.ReviewScopeStats;
import com.acr.review.domain.result.ReviewTopIssue;
import com.acr.review.service.ReviewScoringConstants;

/**
 * 将审查摘要渲染为 IM「迷你报告」Markdown（纯函数，无 IO）。
 * 仅使用标题 / 加粗 / 列表 / 链接语法，保证钉钉 / 企微 / 飞书交集可渲染。
 */
public final class ReviewNotifyMessageRenderer
{
    private static final String EMPTY = "—";

    private ReviewNotifyMessageRenderer()
    {
    }

    public static String renderSuccess(ReviewSummaryContent content)
    {
        if (content == null)
        {
            return "";
        }
        String conclusionLabel = StringUtils.defaultIfEmpty(content.getConclusionLabel(), EMPTY);
        String icon = conclusionIcon(conclusionLabel);
        String score = content.getTotalScore() == null ? EMPTY : content.getTotalScore() + "/100";

        StringBuilder sb = new StringBuilder();
        sb.append("### ").append(icon).append(" AI Code Review · ")
            .append(conclusionLabel).append(" · ").append(score).append("\n\n");

        appendSubmissionInfo(sb, content);

        List<ReviewTopIssue> allIssues = content.getTopIssues();
        List<ReviewTopIssue> issues = content.displayTopIssues();
        sb.append("\n**审查结果 (").append(allIssues.size()).append(")**\n\n");
        if (issues.isEmpty())
        {
            sb.append("本次审查未发现重点问题\n");
        }
        else
        {
            int index = 1;
            for (ReviewTopIssue issue : issues)
            {
                appendIssue(sb, index++, issue);
            }
            if (allIssues.size() > ReviewScoringConstants.MAX_TOP_ISSUES)
            {
                sb.append("共 ").append(allIssues.size()).append(" 个问题，其余见问题台账\n");
            }
        }
        appendRecheckingSection(sb, content.getRecheckingTitles());

        sb.append("\n**范围统计**\n");
        appendScopeStats(sb, content.getScopeStats());

        sb.append("\n**总结**\n");
        sb.append(displayOrEmpty(content.getSummaryText())).append('\n');

        appendActionLinks(sb, content, true);
        return sb.toString().trim();
    }

    public static String renderFailed(ReviewSummaryContent content)
    {
        if (content == null)
        {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("### ❌ AI Code Review · 执行失败\n\n");
        appendSubmissionInfo(sb, content);
        sb.append("\n失败类型: ")
            .append(StringUtils.defaultIfEmpty(content.getFailureTypeLabel(), "未知"))
            .append('\n');
        appendActionLinks(sb, content, false);
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

    static String severityIcon(String severity)
    {
        if (severity == null)
        {
            return "ℹ️";
        }
        return switch (severity.trim().toUpperCase())
        {
            case "CRITICAL" -> "🚨";
            case "HIGH" -> "⚠️";
            case "MEDIUM" -> "⚡";
            case "LOW" -> "💡";
            default -> "ℹ️";
        };
    }

    /** 疑似已修复段；装配异常时静默跳过。 */
    static void appendRecheckingSection(StringBuilder sb, List<String> titles)
    {
        try
        {
            if (sb == null || titles == null || titles.isEmpty())
            {
                return;
            }
            int total = titles.size();
            int limit = Math.min(total, ReviewIssueConstants.MAX_RECHECKING_TITLES_IN_DELIVERY);
            StringBuilder joined = new StringBuilder();
            for (int i = 0; i < limit; i++)
            {
                if (i > 0)
                {
                    joined.append(" / ");
                }
                joined.append(StringUtils.defaultIfEmpty(titles.get(i), ReviewIssueConstants.DEFAULT_TITLE));
            }
            if (total > limit)
            {
                joined.append("…");
            }
            sb.append("\n**疑似已修复（").append(total).append("）：")
                .append(joined)
                .append(" — 请前往问题台账复核**\n");
        }
        catch (Exception ignored)
        {
            // 不阻塞通知主体
        }
    }

    private static void appendSubmissionInfo(StringBuilder sb, ReviewSummaryContent content)
    {
        sb.append("**提交信息**\n");
        sb.append("- 提交人: ").append(displayOrEmpty(content.getPrAuthor())).append('\n');
        sb.append("- 源分支: ").append(displayOrEmpty(content.getSourceBranch()))
            .append(" → 目标分支: ").append(displayOrEmpty(content.getTargetBranch())).append('\n');
        sb.append("- Commit 信息: ").append(displayOrEmpty(content.getCommitMessage())).append('\n');
        sb.append("- 审查时间: ").append(formatReviewTime(content.getReviewTime())).append('\n');
        sb.append("- 合并请求: ").append(formatMergeRequest(content)).append('\n');
    }

    private static String formatMergeRequest(ReviewSummaryContent content)
    {
        Integer prNumber = content.getPrNumber();
        String title = content.getPrTitle();
        String url = content.getPrUrl();
        if (prNumber == null && StringUtils.isEmpty(title) && StringUtils.isEmpty(url))
        {
            return EMPTY;
        }
        String label = "#";
        label += prNumber == null ? EMPTY : prNumber;
        if (StringUtils.isNotEmpty(title))
        {
            label += " " + title;
        }
        if (StringUtils.isNotEmpty(url))
        {
            return "[" + label + "](" + url + ")";
        }
        return label;
    }

    private static void appendIssue(StringBuilder sb, int index, ReviewTopIssue issue)
    {
        String title = StringUtils.defaultIfEmpty(issue.getTitle(), "未命名问题");
        sb.append(index).append(". ")
            .append(severityIcon(issue.getSeverity())).append(' ')
            .append(ReviewCommentBodyRenderer.severityLabel(issue.getSeverity()))
            .append(" · ")
            .append(ReviewCommentBodyRenderer.originLabel(issue.getOrigin()))
            .append(" —— ")
            .append(title)
            .append('\n');

        String locate = formatLocate(issue);
        if (StringUtils.isNotEmpty(locate))
        {
            sb.append("   位置: ").append(locate).append('\n');
        }

        String description = truncateFlat(issue.getDescription(), ReviewDeliveryConstants.IM_MAX_DESCRIPTION_CHARS);
        if (StringUtils.isNotEmpty(description))
        {
            sb.append("   ").append(description).append('\n');
        }

        String suggestion = truncateFlat(issue.getSuggestion(), ReviewDeliveryConstants.IM_MAX_SUGGESTION_CHARS);
        if (StringUtils.isNotEmpty(suggestion))
        {
            sb.append("   建议: ").append(suggestion).append('\n');
        }
    }

    private static String formatLocate(ReviewTopIssue issue)
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

    private static void appendScopeStats(StringBuilder sb, ReviewScopeStats stats)
    {
        if (stats == null)
        {
            sb.append("- ").append(EMPTY).append('\n');
            return;
        }
        sb.append("- 纳入 ").append(n(stats.getIncludedFiles()))
            .append(" · 排除 ").append(n(stats.getExcludedFiles()))
            .append(" · 扩展 ").append(n(stats.getExpandedFiles()))
            .append('\n');
        sb.append("- 新增 ").append(n(stats.getNewCount()))
            .append(" · 存量 ").append(n(stats.getExistingCount()))
            .append('\n');
    }

    private static void appendActionLinks(StringBuilder sb, ReviewSummaryContent content, boolean includePr)
    {
        List<String> links = new ArrayList<>();
        if (includePr && StringUtils.isNotEmpty(content.getPrUrl()))
        {
            links.add("[查看合并请求](" + content.getPrUrl() + ")");
        }
        if (StringUtils.isNotEmpty(content.getDetailUrl()))
        {
            links.add("[查看审查详情](" + content.getDetailUrl() + ")");
        }
        if (links.isEmpty())
        {
            return;
        }
        sb.append('\n').append(String.join(" · ", links)).append('\n');
    }

    private static String formatReviewTime(Date reviewTime)
    {
        if (reviewTime == null)
        {
            return EMPTY;
        }
        return DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM, reviewTime);
    }

    private static String displayOrEmpty(String value)
    {
        return StringUtils.isEmpty(value) ? EMPTY : value.trim();
    }

    private static String n(Integer value)
    {
        return value == null ? EMPTY : String.valueOf(value);
    }

    /** 压平换行后截断，超长追加省略号 … */
    static String truncateFlat(String value, int max)
    {
        if (value == null)
        {
            return null;
        }
        String flat = value.replaceAll("\\R+", " ").trim();
        if (flat.isEmpty())
        {
            return null;
        }
        if (flat.length() <= max)
        {
            return flat;
        }
        return flat.substring(0, max) + "…";
    }
}
