package com.acr.review.delivery;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
    private static final String[] SEVERITY_ORDER = { "CRITICAL", "HIGH", "MEDIUM", "LOW" };
    private static final String SEVERITY_OTHER = "OTHER";

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

        appendAttributionLine(sb, content);
        appendSubmissionInfo(sb, content);
        appendGroupedIssues(sb, content);
        appendRecheckingSection(sb, content.getRecheckingTitles());

        sb.append("\n**范围**：");
        appendScopeStatsInline(sb, content.getScopeStats());

        sb.append("\n**总结**\n");
        String summary = truncateFlat(content.getSummaryText(), ReviewDeliveryConstants.IM_MAX_SUMMARY_CHARS);
        // 总结正文：1 个全角空格（U+3000）缩进，层次化于段标题下
        sb.append('\u3000').append(displayOrEmpty(summary)).append('\n');

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
        appendAttributionLine(sb, content);
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
        return switch (severity.trim().toUpperCase(Locale.ROOT))
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

    /**
     * 归属块：第一行加粗「系统 · 项目 · PR #n」；第二行 PR 标题（空则省略）。
     */
    static void appendAttributionLine(StringBuilder sb, ReviewSummaryContent content)
    {
        String line = formatAttributionLine(content);
        if (StringUtils.isEmpty(line))
        {
            return;
        }
        sb.append(line).append('\n');
        if (content != null && StringUtils.isNotEmpty(content.getPrTitle()))
        {
            sb.append(content.getPrTitle().trim()).append('\n');
        }
        sb.append('\n');
    }

    /** 归属首行（加粗）：业务系统 · 项目(或 owner/repo) · PR #n；不含 PR 标题。 */
    static String formatAttributionLine(ReviewSummaryContent content)
    {
        if (content == null)
        {
            return null;
        }
        List<String> parts = new ArrayList<>(3);
        if (StringUtils.isNotEmpty(content.getBusinessSystemName()))
        {
            parts.add(content.getBusinessSystemName().trim());
        }
        String projectSeg = resolveProjectSegment(content);
        if (StringUtils.isNotEmpty(projectSeg))
        {
            parts.add(projectSeg);
        }
        if (content.getPrNumber() != null && content.getPrNumber() > 0)
        {
            String prSeg = formatPrSegment(content.getPrNumber(), content.getPrTitle(), content.getPrUrl());
            if (StringUtils.isNotEmpty(prSeg))
            {
                parts.add(prSeg);
            }
        }
        else if (content.getPrNumber() != null && content.getPrNumber() <= 0
            && StringUtils.isNotEmpty(content.getTargetBranch()))
        {
            // push 语义（pr_number 哨兵 0）：分支名 @短 SHA
            String pushSeg = content.getTargetBranch().trim();
            if (StringUtils.isNotEmpty(content.getHeadShaShort()))
            {
                pushSeg = pushSeg + " @" + content.getHeadShaShort().trim();
            }
            parts.add(pushSeg);
        }
        if (parts.isEmpty())
        {
            return null;
        }
        return "**" + String.join(" · ", parts) + "**";
    }

    /**
     * PR 段：链接文案仅为 "PR #" + n（不含标题）；有 prUrl 时包装为 Markdown 链接。
     * {@code prTitle} 保留参数以兼容调用方，但不参与链接文案。
     */
    static String formatPrSegment(Integer prNumber, String prTitle, String prUrl)
    {
        if (prNumber == null)
        {
            return null;
        }
        String label = "PR #" + prNumber;
        if (StringUtils.isNotEmpty(prUrl))
        {
            return "[" + label + "](" + prUrl.trim() + ")";
        }
        return label;
    }

    /** 项目名优先；空则回退 owner/repo。 */
    static String resolveProjectSegment(ReviewSummaryContent content)
    {
        if (content == null)
        {
            return null;
        }
        if (StringUtils.isNotEmpty(content.getProjectName()))
        {
            return content.getProjectName().trim();
        }
        String owner = content.getRepositoryOwner();
        String name = content.getRepositoryName();
        if (StringUtils.isEmpty(owner) || StringUtils.isEmpty(name))
        {
            return null;
        }
        return owner.trim() + "/" + name.trim();
    }

    private static void appendSubmissionInfo(StringBuilder sb, ReviewSummaryContent content)
    {
        sb.append("**提交信息**\n");
        // 列表行缩进 3 个半角空格（markdown 列表延续；勿超 3，否则脱离列表）
        boolean push = content.getPrNumber() != null && content.getPrNumber() <= 0;
        if (push)
        {
            sb.append("   - 推送人: ").append(displayOrEmpty(content.getPrAuthor()))
                .append(" · ").append(formatReviewTime(content.getReviewTime())).append('\n');
            sb.append("   - 分支: ").append(displayOrEmpty(content.getTargetBranch())).append('\n');
            String commit = StringUtils.isNotEmpty(content.getCommitMessage())
                ? content.getCommitMessage() : content.getPrTitle();
            sb.append("   - Commit: ").append(displayOrEmpty(commit)).append('\n');
            return;
        }
        sb.append("   - 提交人: ").append(displayOrEmpty(content.getPrAuthor()))
            .append(" · ").append(formatReviewTime(content.getReviewTime())).append('\n');
        sb.append("   - 分支: ").append(displayOrEmpty(content.getSourceBranch()))
            .append(" → ").append(displayOrEmpty(content.getTargetBranch())).append('\n');
        sb.append("   - Commit: ").append(displayOrEmpty(content.getCommitMessage())).append('\n');
    }

    private static void appendGroupedIssues(StringBuilder sb, ReviewSummaryContent content)
    {
        List<ReviewTopIssue> allIssues = content.getTopIssues();
        List<ReviewTopIssue> issues = content.displayTopIssues();
        sb.append("\n**审查问题 (").append(allIssues.size()).append(")**\n");
        if (issues.isEmpty())
        {
            sb.append("本次审查未发现重点问题\n");
            return;
        }

        Map<String, List<ReviewTopIssue>> grouped = groupBySeverity(issues);
        int index = 1;
        int expandedCount = 0;
        Map<String, Integer> expandedBySeverity = new LinkedHashMap<>();
        for (String severity : SEVERITY_ORDER)
        {
            List<ReviewTopIssue> bucket = grouped.get(severity);
            if (bucket == null || bucket.isEmpty())
            {
                continue;
            }
            if ("CRITICAL".equals(severity) || "HIGH".equals(severity))
            {
                sb.append("**").append(severityIcon(severity)).append(' ')
                    .append(ReviewCommentBodyRenderer.severityLabel(severity))
                    .append(" (").append(bucket.size()).append(")**\n");
                for (ReviewTopIssue issue : bucket)
                {
                    appendExpandedIssue(sb, index++, issue);
                    expandedCount++;
                    expandedBySeverity.merge(severity, 1, Integer::sum);
                }
            }
        }

        int otherCount = countOtherIssues(grouped);
        boolean hasOverflow = allIssues.size() > ReviewScoringConstants.MAX_TOP_ISSUES;
        boolean hasCompressed = otherCount > 0;
        // 行动指引句只出现一次：溢出（明细取全量剩余）/ 仅压缩，两分支互斥
        if (hasOverflow)
        {
            int remainderCount = allIssues.size() - expandedCount;
            sb.append("共 ").append(allIssues.size()).append(" 个问题，其余 ")
                .append(remainderCount).append(" 个（")
                .append(formatRemainderSummary(allIssues, expandedBySeverity))
                .append("）详见问题台账\n");
        }
        else if (hasCompressed)
        {
            sb.append("**其他问题 (").append(otherCount).append(")**：")
                .append(formatOtherIssuesSummary(grouped))
                .append("，详见问题台账\n");
        }
    }

    private static Map<String, List<ReviewTopIssue>> groupBySeverity(List<ReviewTopIssue> issues)
    {
        Map<String, List<ReviewTopIssue>> grouped = new LinkedHashMap<>();
        for (String severity : SEVERITY_ORDER)
        {
            grouped.put(severity, new ArrayList<>());
        }
        grouped.put(SEVERITY_OTHER, new ArrayList<>());
        for (ReviewTopIssue issue : issues)
        {
            String key = normalizeSeverity(issue == null ? null : issue.getSeverity());
            if (grouped.containsKey(key) && !SEVERITY_OTHER.equals(key))
            {
                grouped.get(key).add(issue);
            }
            else
            {
                grouped.get(SEVERITY_OTHER).add(issue);
            }
        }
        return grouped;
    }

    private static String normalizeSeverity(String severity)
    {
        if (severity == null)
        {
            return "";
        }
        return severity.trim().toUpperCase(Locale.ROOT);
    }

    private static int countOtherIssues(Map<String, List<ReviewTopIssue>> grouped)
    {
        int count = 0;
        for (Map.Entry<String, List<ReviewTopIssue>> entry : grouped.entrySet())
        {
            if ("CRITICAL".equals(entry.getKey()) || "HIGH".equals(entry.getKey()))
            {
                continue;
            }
            count += entry.getValue().size();
        }
        return count;
    }

    private static String formatOtherIssuesSummary(Map<String, List<ReviewTopIssue>> grouped)
    {
        List<String> parts = new ArrayList<>();
        appendOtherPart(parts, grouped, "MEDIUM", "中");
        appendOtherPart(parts, grouped, "LOW", "低");
        appendOtherPart(parts, grouped, SEVERITY_OTHER, "其他");
        return parts.isEmpty() ? EMPTY : String.join(" · ", parts);
    }

    private static void appendOtherPart(List<String> parts, Map<String, List<ReviewTopIssue>> grouped,
                                        String severity, String label)
    {
        List<ReviewTopIssue> bucket = grouped.get(severity);
        if (bucket == null || bucket.isEmpty())
        {
            return;
        }
        parts.add(label + " " + bucket.size());
    }

    /**
     * 溢出剩余明细：全量问题除去已展开项后的严重度分布（严重/高/中/低/其他，只列非零）。
     */
    private static String formatRemainderSummary(List<ReviewTopIssue> allIssues,
                                                 Map<String, Integer> expandedBySeverity)
    {
        Map<String, List<ReviewTopIssue>> allGrouped = groupBySeverity(allIssues);
        List<String> parts = new ArrayList<>();
        for (String severity : SEVERITY_ORDER)
        {
            List<ReviewTopIssue> bucket = allGrouped.get(severity);
            int total = bucket == null ? 0 : bucket.size();
            int expanded = expandedBySeverity.getOrDefault(severity, 0);
            int rem = total - expanded;
            if (rem > 0)
            {
                parts.add(ReviewCommentBodyRenderer.severityLabel(severity) + " " + rem);
            }
        }
        List<ReviewTopIssue> otherBucket = allGrouped.get(SEVERITY_OTHER);
        int otherRem = otherBucket == null ? 0 : otherBucket.size();
        if (otherRem > 0)
        {
            parts.add("其他 " + otherRem);
        }
        return parts.isEmpty() ? EMPTY : String.join(" · ", parts);
    }

    private static void appendExpandedIssue(StringBuilder sb, int index, ReviewTopIssue issue)
    {
        String title = StringUtils.defaultIfEmpty(issue.getTitle(), "未命名问题");
        sb.append(index).append(". ").append(title).append('\n');

        String locate = formatLocate(issue);
        if (StringUtils.isNotEmpty(locate))
        {
            sb.append("   📍 ").append(locate).append('\n');
        }

        String suggestion = truncateFlat(issue.getSuggestion(), ReviewDeliveryConstants.IM_MAX_SUGGESTION_CHARS);
        if (StringUtils.isNotEmpty(suggestion))
        {
            sb.append("   💡 ").append(suggestion).append('\n');
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

    private static void appendScopeStatsInline(StringBuilder sb, ReviewScopeStats stats)
    {
        String body = ReviewCommentBodyRenderer.formatScopeStatsBody(stats);
        if (body == null)
        {
            sb.append(EMPTY).append('\n');
            return;
        }
        sb.append(body).append('\n');
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
