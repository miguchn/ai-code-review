package com.acr.review.delivery;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import com.acr.common.utils.StringUtils;
import com.acr.review.domain.result.ReviewTopIssue;
import com.acr.review.service.ReviewScoringConstants;

/**
 * 将单条问题渲染为代码平台行内评论 Markdown（纯函数，无 IO）。
 * 格式见 docs/planning/inline-comments-m11.md §2 D4。
 */
public final class ReviewInlineCommentRenderer
{
    private ReviewInlineCommentRenderer()
    {
    }

    public static String render(ReviewTopIssue issue, Long taskId)
    {
        if (issue == null)
        {
            return "";
        }
        Long issueId = issue.getIssueId();
        StringBuilder sb = new StringBuilder();
        sb.append(severityIcon(issue.getSeverity())).append(' ')
            .append(severityLabel(issue.getSeverity())).append(" · ")
            .append(categoryLabel(issue.getCategory())).append(" · 问题 #")
            .append(issueId == null ? "--" : issueId)
            .append('\n');
        sb.append(StringUtils.defaultIfEmpty(issue.getTitle(), "未命名问题")).append('\n');
        String description = truncate(issue.getDescription(),
            ReviewDeliveryConstants.INLINE_MAX_DESCRIPTION_CHARS);
        if (StringUtils.isNotEmpty(description))
        {
            sb.append(description.replace("\n", " ")).append('\n');
        }
        String suggestion = truncate(issue.getSuggestion(),
            ReviewDeliveryConstants.INLINE_MAX_SUGGESTION_CHARS);
        if (StringUtils.isNotEmpty(suggestion))
        {
            sb.append('\n').append("💡 建议：").append(suggestion.replace("\n", " ")).append('\n');
        }
        sb.append('\n');
        if (issueId != null)
        {
            sb.append(ReviewDeliveryConstants.inlineCommentMarker(issueId)).append('\n');
        }
        sb.append("—— AI Code Review · 审查记录 #")
            .append(taskId == null ? "--" : taskId)
            .append(" · 处置与复核请前往问题台账");
        return sb.toString();
    }

    /** 解析项目严重度白名单；空则回退默认 CRITICAL,HIGH。 */
    public static Set<String> parseSeverities(String csv)
    {
        Set<String> set = new LinkedHashSet<>();
        String source = StringUtils.isEmpty(csv)
            ? ReviewDeliveryConstants.DEFAULT_INLINE_SEVERITIES : csv;
        for (String part : source.split("[,;\\s]+"))
        {
            if (part == null || part.isBlank())
            {
                continue;
            }
            set.add(part.trim().toUpperCase(Locale.ROOT));
        }
        if (set.isEmpty())
        {
            set.add(ReviewScoringConstants.SEVERITY_CRITICAL);
            set.add(ReviewScoringConstants.SEVERITY_HIGH);
        }
        return set;
    }

    public static boolean severityAllowed(String severity, Set<String> allowed)
    {
        if (allowed == null || allowed.isEmpty() || severity == null || severity.isBlank())
        {
            return false;
        }
        return allowed.contains(severity.trim().toUpperCase(Locale.ROOT));
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
            case "LOW" -> "ℹ️";
            default -> "ℹ️";
        };
    }

    static String severityLabel(String severity)
    {
        return ReviewCommentBodyRenderer.severityLabel(severity);
    }

    static String categoryLabel(String category)
    {
        if (category == null || category.isBlank())
        {
            return "其他";
        }
        return switch (category.trim().toUpperCase(Locale.ROOT))
        {
            case ReviewScoringConstants.DIM_SECURITY -> "安全";
            case ReviewScoringConstants.DIM_CORRECTNESS -> "正确性";
            case ReviewScoringConstants.DIM_PRACTICE -> "可维护性";
            case ReviewScoringConstants.DIM_PERFORMANCE -> "性能";
            case ReviewScoringConstants.DIM_COMMIT_QUALITY -> "提交质量";
            default -> category.trim();
        };
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
