package com.acr.review.engine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import com.acr.common.utils.StringUtils;
import com.acr.review.domain.result.ReviewTopIssue;
import com.acr.review.service.ReviewScoringConstants;

/**
 * 将 OCR 引擎 structuredResult（comments 数组）映射为平台 {@link ReviewTopIssue} 清单。
 * 位置缺失时置 null（禁止伪造）；无 content 的条目跳过。
 */
@Component
public class ReviewEngineResultMapper
{
    private static final int MAX_TITLE_CHARS = 80;
    private static final Pattern BOLD_TITLE = Pattern.compile(
        "^\\*\\*(.+?)\\*\\*\\s*:?\\s*(.*)$", Pattern.DOTALL);
    private static final Pattern SUGGESTION_CLAUSE = Pattern.compile(
        "建议[:：]?\\s*.+");

    public List<ReviewTopIssue> mapTopIssues(Map<String, Object> structured)
    {
        if (structured == null || structured.isEmpty())
        {
            return List.of();
        }
        Object commentsNode = structured.get("comments");
        if (!(commentsNode instanceof Collection<?> comments) || comments.isEmpty())
        {
            return List.of();
        }

        List<ReviewTopIssue> issues = new ArrayList<>();
        for (Object item : comments)
        {
            if (!(item instanceof Map<?, ?> raw))
            {
                continue;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> comment = (Map<String, Object>) raw;
            ReviewTopIssue issue = mapComment(comment);
            if (issue == null)
            {
                continue;
            }
            issues.add(issue);
            if (issues.size() >= ReviewScoringConstants.MAX_ISSUES)
            {
                break;
            }
        }
        issues.sort(Comparator.comparingInt((ReviewTopIssue i) -> severityRank(i.getSeverity())));
        for (int i = 0; i < issues.size(); i++)
        {
            issues.get(i).setRank(i + 1);
        }
        return issues;
    }

    private ReviewTopIssue mapComment(Map<String, Object> comment)
    {
        String content = stringValue(comment.get("content"));
        if (StringUtils.isEmpty(content))
        {
            return null;
        }

        ParsedContent parsed = parseContent(content.trim());
        // 禁止伪造位置：有则保留，缺/非法则该字段置 null；path 与行号各自独立。
        String path = stringValue(comment.get("path"));
        Integer startLine = toPositiveLine(comment.get("start_line"));
        Integer endLine = toPositiveLine(comment.get("end_line"));
        if (startLine != null && endLine != null && endLine < startLine)
        {
            startLine = null;
            endLine = null;
        }

        ReviewTopIssue issue = new ReviewTopIssue();
        issue.setTitle(parsed.title());
        issue.setDescription(parsed.description());
        issue.setEvidence(stringValue(comment.get("existing_code")));
        issue.setSuggestion(buildSuggestion(parsed.description(), content, stringValue(comment.get("suggestion_code"))));
        issue.setSeverity(normalizeSeverity(stringValue(comment.get("severity"))));
        issue.setCategory(normalizeCategory(stringValue(comment.get("category"))));
        issue.setOrigin(ReviewScoringConstants.ORIGIN_NEW);
        issue.setFilePath(path == null ? null : path.trim());
        issue.setStartLine(startLine);
        issue.setEndLine(endLine);
        return issue;
    }

    private ParsedContent parseContent(String content)
    {
        Matcher matcher = BOLD_TITLE.matcher(content);
        if (matcher.matches())
        {
            String title = truncate(matcher.group(1).trim(), MAX_TITLE_CHARS);
            String description = matcher.group(2) == null ? "" : matcher.group(2).trim();
            if (StringUtils.isEmpty(title))
            {
                title = truncate(content, MAX_TITLE_CHARS);
            }
            return new ParsedContent(title, description);
        }
        return new ParsedContent(truncate(content, MAX_TITLE_CHARS), content);
    }

    private String buildSuggestion(String description, String content, String suggestionCode)
    {
        String semantic = extractSuggestionSemantic(description);
        if (StringUtils.isEmpty(semantic))
        {
            semantic = extractSuggestionSemantic(content);
        }
        boolean hasSemantic = StringUtils.isNotEmpty(semantic);
        boolean hasCode = StringUtils.isNotEmpty(suggestionCode);
        if (!hasSemantic && !hasCode)
        {
            return null;
        }
        if (!hasCode)
        {
            return semantic;
        }
        if (!hasSemantic)
        {
            return suggestionCode.trim();
        }
        return semantic.trim() + "\n" + suggestionCode.trim();
    }

    private String extractSuggestionSemantic(String text)
    {
        if (StringUtils.isEmpty(text))
        {
            return null;
        }
        Matcher matcher = SUGGESTION_CLAUSE.matcher(text);
        if (matcher.find())
        {
            return matcher.group().trim();
        }
        return null;
    }

    private String normalizeSeverity(String severity)
    {
        if (StringUtils.isEmpty(severity))
        {
            return ReviewScoringConstants.SEVERITY_MEDIUM;
        }
        String value = severity.trim().toUpperCase(Locale.ROOT);
        return switch (value)
        {
            case ReviewScoringConstants.SEVERITY_CRITICAL,
                ReviewScoringConstants.SEVERITY_HIGH,
                ReviewScoringConstants.SEVERITY_MEDIUM,
                ReviewScoringConstants.SEVERITY_LOW,
                ReviewScoringConstants.SEVERITY_INFO -> value;
            case "ERROR", "BLOCKER", "严重", "阻断" -> ReviewScoringConstants.SEVERITY_CRITICAL;
            case "WARNING", "WARN", "警告" -> ReviewScoringConstants.SEVERITY_MEDIUM;
            default -> ReviewScoringConstants.SEVERITY_MEDIUM;
        };
    }

    private String normalizeCategory(String category)
    {
        if (StringUtils.isEmpty(category))
        {
            return ReviewScoringConstants.DIM_PRACTICE;
        }
        String value = category.trim().toLowerCase(Locale.ROOT);
        return switch (value)
        {
            case "security" -> ReviewScoringConstants.DIM_SECURITY;
            case "bug" -> ReviewScoringConstants.DIM_CORRECTNESS;
            case "performance" -> ReviewScoringConstants.DIM_PERFORMANCE;
            default -> ReviewScoringConstants.DIM_PRACTICE;
        };
    }

    private static Integer toPositiveLine(Object value)
    {
        if (value instanceof Number number)
        {
            int line = number.intValue();
            return line > 0 ? line : null;
        }
        if (value instanceof String text && StringUtils.isNotEmpty(text))
        {
            try
            {
                int line = Integer.parseInt(text.trim());
                return line > 0 ? line : null;
            }
            catch (NumberFormatException ignored)
            {
                return null;
            }
        }
        return null;
    }

    private static String stringValue(Object value)
    {
        if (value == null)
        {
            return null;
        }
        String text = String.valueOf(value);
        return text.isBlank() ? null : text;
    }

    private static String truncate(String value, int maxChars)
    {
        if (value == null)
        {
            return "";
        }
        if (value.length() <= maxChars)
        {
            return value;
        }
        return value.substring(0, maxChars);
    }

    private static int severityRank(String severity)
    {
        if (severity == null)
        {
            return 99;
        }
        return switch (severity)
        {
            case ReviewScoringConstants.SEVERITY_CRITICAL -> 1;
            case ReviewScoringConstants.SEVERITY_HIGH -> 2;
            case ReviewScoringConstants.SEVERITY_MEDIUM -> 3;
            case ReviewScoringConstants.SEVERITY_LOW -> 4;
            case ReviewScoringConstants.SEVERITY_INFO -> 5;
            default -> 99;
        };
    }

    private record ParsedContent(String title, String description)
    {
    }
}
