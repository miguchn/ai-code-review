package com.acr.review.delivery;

import java.util.List;
import com.acr.common.utils.StringUtils;
import com.acr.review.domain.ReviewIssueConstants;
import com.acr.review.domain.ReviewPipelineConstants;
import com.acr.review.domain.ReviewTask;
import com.acr.review.domain.ReviewTaskRun;
import com.acr.review.domain.result.ReviewScopeStats;
import com.acr.review.domain.result.ReviewTopIssue;
import com.acr.review.service.ReviewScoringConstants;

/**
 * 将审查成功结果渲染为 GitHub PR 总结评论 Markdown（纯函数，无 IO）。
 */
public final class ReviewCommentBodyRenderer
{
    private ReviewCommentBodyRenderer()
    {
    }

    public static String render(ReviewTask task, ReviewTaskRun run)
    {
        return render(buildMinimalContent(task, run));
    }

    public static String render(ReviewSummaryContent content)
    {
        if (content == null)
        {
            content = ReviewSummaryContent.builder().build();
        }
        String conclusion = StringUtils.defaultIfEmpty(content.getConclusionLabel(), "--");
        String scoreText = content.getTotalScore() == null ? "--" : content.getTotalScore() + " / 100";
        Long taskId = content.getTaskId();
        String headSha = StringUtils.defaultIfEmpty(content.getHeadShaShort(), "--");

        List<ReviewTopIssue> allIssues = content.getTopIssues();
        List<ReviewTopIssue> issues = content.displayTopIssues();
        ReviewScopeStats scopeStats = content.getScopeStats();

        StringBuilder sb = new StringBuilder();
        sb.append("## AI Code Review 审查结论\n\n");
        sb.append("| 项目 | 内容 |\n");
        sb.append("|---|---|\n");
        sb.append("| 结论 | ").append(conclusion).append(" |\n");
        sb.append("| 总分 | ").append(scoreText).append(" |\n");
        sb.append("| 任务 | #").append(taskId == null ? "--" : taskId)
            .append(" · `").append(headSha).append("` |\n");
        sb.append("\n### Top 3 重点问题\n\n");
        if (issues.isEmpty())
        {
            sb.append("暂无重点问题\n");
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
                sb.append("\n共 ").append(allIssues.size()).append(" 个问题，其余见问题台账\n");
            }
        }
        appendRecheckingSection(sb, content.getRecheckingTitles());
        sb.append("\n### 范围统计\n\n");
        sb.append(formatScopeStats(scopeStats));
        sb.append("\n\n---\n");
        sb.append("*由 AI Code Review 自动生成并更新；请勿手动删除本标记评论。*\n");
        sb.append(ReviewDeliveryConstants.COMMENT_MARKER).append("\n");
        return sb.toString();
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
                joined.append(escapePipe(StringUtils.defaultIfEmpty(titles.get(i), ReviewIssueConstants.DEFAULT_TITLE)));
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
            // 不阻塞评论主体
        }
    }

    static ReviewSummaryContent buildMinimalContent(ReviewTask task, ReviewTaskRun run)
    {
        String conclusion = task == null ? null : task.getReviewConclusion();
        String headSha = ReviewSummaryContentFactory.shortSha(task == null ? null : task.getHeadSha());
        if (StringUtils.isEmpty(headSha) && run != null)
        {
            headSha = ReviewSummaryContentFactory.shortSha(run.getSnapshotHeadSha());
        }
        return ReviewSummaryContent.builder()
            .taskId(task == null ? null : task.getTaskId())
            .conclusion(conclusion)
            .conclusionLabel(conclusionLabel(conclusion))
            .totalScore(firstNonNull(
                task == null ? null : task.getTotalScore(),
                run == null ? null : run.getTotalScore()))
            .headShaShort(StringUtils.isEmpty(headSha) ? null : headSha)
            .topIssues(ReviewSummaryContentFactory.resolveTopIssues(run))
            .scopeStats(ReviewSummaryContentFactory.resolveScopeStats(run))
            .build();
    }

    static String conclusionLabel(String conclusion)
    {
        if (ReviewPipelineConstants.CONCLUSION_PASS.equals(conclusion))
        {
            return "通过";
        }
        if (ReviewPipelineConstants.CONCLUSION_WARN.equals(conclusion))
        {
            return "建议修改";
        }
        if (ReviewPipelineConstants.CONCLUSION_BLOCK.equals(conclusion))
        {
            return "高风险";
        }
        return "--";
    }

    static String severityLabel(String severity)
    {
        if (severity == null)
        {
            return "信息";
        }
        return switch (severity.trim().toUpperCase())
        {
            case "CRITICAL" -> "严重";
            case "HIGH" -> "高";
            case "MEDIUM" -> "中";
            case "LOW" -> "低";
            default -> "信息";
        };
    }

    static String originLabel(String origin)
    {
        if (origin != null && "EXISTING".equalsIgnoreCase(origin.trim()))
        {
            return "存量";
        }
        return "新增";
    }

    static String formatScopeStats(ReviewScopeStats stats)
    {
        String body = formatScopeStatsBody(stats);
        if (body == null)
        {
            return "范围统计：—";
        }
        return body;
    }

    /**
     * 白话范围正文：问题结论行 + 审查文件行；无可展示内容时返回 null。
     * IM / PR 评论共用，前缀由调用方自行添加。
     */
    static String formatScopeStatsBody(ReviewScopeStats stats)
    {
        if (stats == null)
        {
            return null;
        }
        List<String> lines = new java.util.ArrayList<>();
        if (stats.getNewCount() != null)
        {
            StringBuilder issueLine = new StringBuilder();
            issueLine.append("本次新增 ").append(stats.getNewCount()).append(" 个问题");
            if (stats.getExistingCount() != null && stats.getExistingCount() > 0)
            {
                issueLine.append(" · 存量 ").append(stats.getExistingCount()).append(" 个");
            }
            lines.add(issueLine.toString());
        }
        List<String> fileParts = new java.util.ArrayList<>();
        if (stats.getIncludedFiles() != null)
        {
            fileParts.add("纳入 " + stats.getIncludedFiles() + " 个");
        }
        if (stats.getExpandedFiles() != null)
        {
            fileParts.add("扩展 " + stats.getExpandedFiles() + " 个");
        }
        if (!fileParts.isEmpty())
        {
            lines.add("审查文件：" + String.join(" · ", fileParts));
        }
        if (lines.isEmpty())
        {
            return null;
        }
        return String.join("\n", lines);
    }

    private static void appendIssue(StringBuilder sb, int index, ReviewTopIssue issue)
    {
        String title = StringUtils.defaultIfEmpty(issue.getTitle(), "未命名问题");
        String locate = formatLocate(issue);
        sb.append(index).append(". **[")
            .append(severityLabel(issue.getSeverity())).append("][")
            .append(originLabel(issue.getOrigin())).append("]** ")
            .append(escapePipe(title));
        if (StringUtils.isNotEmpty(locate))
        {
            sb.append(" — ").append(locate);
        }
        sb.append("\n");
        String description = truncate(issue.getDescription(), ReviewDeliveryConstants.MAX_ISSUE_DESCRIPTION_CHARS);
        if (StringUtils.isNotEmpty(description))
        {
            sb.append("   - ").append(escapePipe(description).replace("\n", " ")).append("\n");
        }
        String disposition = formatDisposition(issue);
        if (StringUtils.isNotEmpty(disposition))
        {
            sb.append("   - 处置：").append(escapePipe(disposition)).append("\n");
        }
    }

    static String formatDisposition(ReviewTopIssue issue)
    {
        if (issue == null || StringUtils.isEmpty(issue.getDispositionStatus()))
        {
            return null;
        }
        String label = ReviewIssueConstants.statusLabel(issue.getDispositionStatus());
        String note = truncate(issue.getDispositionNote(), ReviewIssueConstants.MAX_DISPOSITION_NOTE_IN_COMMENT);
        if (StringUtils.isEmpty(note))
        {
            return label;
        }
        return label + "（" + note.replace("\n", " ") + "）";
    }

    private static String formatLocate(ReviewTopIssue issue)
    {
        StringBuilder locate = new StringBuilder();
        if (StringUtils.isNotEmpty(issue.getFilePath()))
        {
            locate.append('`').append(issue.getFilePath()).append('`');
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

    private static String escapePipe(String value)
    {
        return value == null ? "" : value.replace("|", "\\|");
    }

    private static Integer firstNonNull(Integer a, Integer b)
    {
        return a != null ? a : b;
    }
}
