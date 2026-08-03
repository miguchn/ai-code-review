package com.acr.review.delivery;

import java.util.ArrayList;
import java.util.List;
import com.acr.common.utils.StringUtils;
import com.acr.review.domain.ReviewPipelineConstants;
import com.acr.review.domain.ReviewTask;
import com.acr.review.domain.ReviewTaskRun;
import com.acr.review.domain.result.ReviewScopeStats;
import com.acr.review.domain.result.ReviewTopIssue;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;

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
        String conclusion = conclusionLabel(task == null ? null : task.getReviewConclusion());
        Integer totalScore = firstNonNull(
            task == null ? null : task.getTotalScore(),
            run == null ? null : run.getTotalScore());
        String scoreText = totalScore == null ? "--" : totalScore + " / 100";
        Long taskId = task == null ? null : task.getTaskId();
        String headSha = shortSha(task == null ? null : task.getHeadSha());
        if (StringUtils.isEmpty(headSha) && run != null)
        {
            headSha = shortSha(run.getSnapshotHeadSha());
        }

        List<ReviewTopIssue> issues = resolveTopIssues(run);
        ReviewScopeStats scopeStats = resolveScopeStats(run);

        StringBuilder sb = new StringBuilder();
        sb.append("## AI Code Review 审查结论\n\n");
        sb.append("| 项目 | 内容 |\n");
        sb.append("|---|---|\n");
        sb.append("| 结论 | ").append(conclusion).append(" |\n");
        sb.append("| 总分 | ").append(scoreText).append(" |\n");
        sb.append("| 任务 | #").append(taskId == null ? "--" : taskId)
            .append(" · `").append(StringUtils.isEmpty(headSha) ? "--" : headSha).append("` |\n");
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
        }
        sb.append("\n### 范围统计\n\n");
        sb.append(formatScopeStats(scopeStats));
        sb.append("\n\n---\n");
        sb.append("*由 AI Code Review 自动生成并更新；请勿手动删除本标记评论。*\n");
        sb.append(ReviewDeliveryConstants.COMMENT_MARKER).append("\n");
        return sb.toString();
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
            parts.add("新增问题 " + stats.getNewCount());
        }
        if (stats.getExistingCount() != null)
        {
            parts.add("存量 " + stats.getExistingCount());
        }
        if (parts.isEmpty())
        {
            return "范围统计：—";
        }
        return String.join(" · ", parts);
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

    private static List<ReviewTopIssue> resolveTopIssues(ReviewTaskRun run)
    {
        if (run == null)
        {
            return List.of();
        }
        List<ReviewTopIssue> fromColumn = parseTopIssues(run.getTopIssuesJson());
        if (!fromColumn.isEmpty())
        {
            return fromColumn;
        }
        JSONObject result = parseJsonObject(run.getResultJson());
        if (result == null)
        {
            return List.of();
        }
        JSONArray array = result.getJSONArray("topIssues");
        return array == null ? List.of() : parseTopIssues(array.toJSONString());
    }

    private static List<ReviewTopIssue> parseTopIssues(String json)
    {
        if (StringUtils.isEmpty(json))
        {
            return List.of();
        }
        try
        {
            List<ReviewTopIssue> list = JSON.parseArray(json, ReviewTopIssue.class);
            return list == null ? List.of() : list;
        }
        catch (Exception ex)
        {
            return List.of();
        }
    }

    private static ReviewScopeStats resolveScopeStats(ReviewTaskRun run)
    {
        if (run == null || StringUtils.isEmpty(run.getResultJson()))
        {
            return null;
        }
        JSONObject result = parseJsonObject(run.getResultJson());
        if (result == null || !result.containsKey("scopeStats"))
        {
            return null;
        }
        try
        {
            return result.getObject("scopeStats", ReviewScopeStats.class);
        }
        catch (Exception ex)
        {
            return null;
        }
    }

    private static JSONObject parseJsonObject(String json)
    {
        if (StringUtils.isEmpty(json))
        {
            return null;
        }
        try
        {
            Object parsed = JSON.parse(json);
            if (parsed instanceof JSONObject object)
            {
                return object;
            }
            return null;
        }
        catch (Exception ex)
        {
            return null;
        }
    }

    private static String shortSha(String sha)
    {
        if (StringUtils.isEmpty(sha))
        {
            return "";
        }
        return sha.length() <= 7 ? sha : sha.substring(0, 7);
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
