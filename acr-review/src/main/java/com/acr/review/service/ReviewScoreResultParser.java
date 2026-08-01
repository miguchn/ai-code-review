package com.acr.review.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;
import com.acr.review.domain.ReviewPipelineConstants;
import com.acr.review.domain.result.ReviewScoreDimension;
import com.acr.review.domain.result.ReviewScoreResult;
import com.acr.review.domain.result.ReviewTopIssue;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 解析并校验大模型统一评分 JSON；总分由后端重算，Top3 由后端截断校正。
 */
@Component
public class ReviewScoreResultParser
{
    /** 匹配任意位置的 markdown 围栏 JSON 块（模型常见输出形态）。 */
    private static final java.util.regex.Pattern FENCE_PATTERN = java.util.regex.Pattern.compile(
        "```(?:json)?\\s*\\n(.*?)```", java.util.regex.Pattern.DOTALL | java.util.regex.Pattern.CASE_INSENSITIVE);

    private final ObjectMapper objectMapper = new ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public ReviewScoreParseResult parse(String rawContent)
    {
        String excerpt = excerpt(rawContent);
        if (rawContent == null || rawContent.isBlank())
        {
            return ReviewScoreParseResult.fail("模型返回为空，无法解析审查结果", excerpt);
        }

        String json = extractJson(rawContent.trim());
        if (json == null)
        {
            return ReviewScoreParseResult.fail("模型未返回合法 JSON，结果格式异常", excerpt);
        }

        ReviewScoreResult parsed;
        try
        {
            parsed = objectMapper.readValue(json, ReviewScoreResult.class);
        }
        catch (Exception ex)
        {
            return ReviewScoreParseResult.fail("审查结果 JSON 反序列化失败：" + safeMessage(ex), excerpt);
        }

        try
        {
            ReviewScoreResult normalized = normalizeAndValidate(parsed);
            return ReviewScoreParseResult.ok(normalized, excerpt);
        }
        catch (IllegalArgumentException ex)
        {
            return ReviewScoreParseResult.fail(ex.getMessage(), excerpt);
        }
    }

    public String resolveConclusion(ReviewScoreResult result)
    {
        if (result == null)
        {
            return ReviewPipelineConstants.CONCLUSION_WARN;
        }
        if (Boolean.TRUE.equals(result.getHasCriticalSecurityIssue()))
        {
            return ReviewPipelineConstants.CONCLUSION_BLOCK;
        }
        for (ReviewTopIssue issue : result.getTopIssues())
        {
            String severity = issue.getSeverity() == null ? "" : issue.getSeverity().toUpperCase(Locale.ROOT);
            if (ReviewScoringConstants.SEVERITY_CRITICAL.equals(severity)
                || ReviewScoringConstants.SEVERITY_HIGH.equals(severity))
            {
                return ReviewPipelineConstants.CONCLUSION_WARN;
            }
        }
        return ReviewPipelineConstants.CONCLUSION_PASS;
    }

    public String summarize(ReviewScoreResult result, String conclusion)
    {
        String label = switch (conclusion)
        {
            case ReviewPipelineConstants.CONCLUSION_BLOCK -> "阻断";
            case ReviewPipelineConstants.CONCLUSION_WARN -> "警告";
            default -> "通过";
        };
        int score = result == null || result.getTotalScore() == null ? 0 : result.getTotalScore();
        int focus = result == null || result.getFocusIssueCount() == null ? 0 : result.getFocusIssueCount();
        String summary = result == null || result.getSummary() == null ? "" : result.getSummary().trim();
        String prefix = "审查结论：" + label + "；总分 " + score + "；重点问题 " + focus + " 项";
        if (summary.isEmpty())
        {
            return prefix;
        }
        return prefix + "。摘要：" + truncate(summary, 800);
    }

    private ReviewScoreResult normalizeAndValidate(ReviewScoreResult parsed)
    {
        if (parsed == null)
        {
            throw new IllegalArgumentException("审查结果为空");
        }
        if (!ReviewScoringConstants.PROTOCOL_VERSION.equals(parsed.getProtocolVersion()))
        {
            throw new IllegalArgumentException("不支持的协议版本：" + parsed.getProtocolVersion()
                + "，期望 " + ReviewScoringConstants.PROTOCOL_VERSION);
        }
        if (parsed.getSummary() == null || parsed.getSummary().isBlank())
        {
            throw new IllegalArgumentException("审查摘要 summary 不能为空");
        }
        if (parsed.getHasCriticalSecurityIssue() == null)
        {
            throw new IllegalArgumentException("缺少 hasCriticalSecurityIssue");
        }

        Map<String, Integer> expected = ReviewScoringConstants.scoreWeights();
        if (parsed.getScores() == null || parsed.getScores().isEmpty())
        {
            throw new IllegalArgumentException("缺少评分维度 scores");
        }
        Map<String, ReviewScoreDimension> byDim = new HashMap<>();
        for (ReviewScoreDimension dim : parsed.getScores())
        {
            if (dim == null || dim.getDimension() == null)
            {
                throw new IllegalArgumentException("评分维度缺少 dimension");
            }
            String key = dim.getDimension().trim().toUpperCase(Locale.ROOT);
            if (!expected.containsKey(key))
            {
                throw new IllegalArgumentException("未知评分维度：" + key);
            }
            if (byDim.containsKey(key))
            {
                throw new IllegalArgumentException("评分维度重复：" + key);
            }
            Integer max = expected.get(key);
            if (dim.getMaxScore() == null || !max.equals(dim.getMaxScore()))
            {
                throw new IllegalArgumentException("维度 " + key + " 的满分必须为 " + max);
            }
            if (dim.getScore() == null)
            {
                throw new IllegalArgumentException("维度 " + key + " 缺少得分");
            }
            if (dim.getScore() < 0 || dim.getScore() > max)
            {
                throw new IllegalArgumentException("维度 " + key + " 得分超出范围 [0, " + max + "]");
            }
            if (dim.getReason() == null || dim.getReason().isBlank())
            {
                throw new IllegalArgumentException("维度 " + key + " 缺少评分理由");
            }
            dim.setDimension(key);
            byDim.put(key, dim);
        }
        Set<String> missing = new HashSet<>(expected.keySet());
        missing.removeAll(byDim.keySet());
        if (!missing.isEmpty())
        {
            throw new IllegalArgumentException("缺少评分维度：" + missing);
        }

        List<ReviewScoreDimension> ordered = new ArrayList<>();
        int total = 0;
        for (String key : ReviewScoringConstants.requiredDimensions())
        {
            ReviewScoreDimension dim = byDim.get(key);
            ordered.add(dim);
            total += dim.getScore();
        }

        List<ReviewTopIssue> issues = new ArrayList<>();
        if (parsed.getTopIssues() != null)
        {
            for (ReviewTopIssue issue : parsed.getTopIssues())
            {
                if (issue == null)
                {
                    continue;
                }
                validateIssue(issue);
                issues.add(issue);
            }
        }
        issues.sort(Comparator
            .comparingInt((ReviewTopIssue i) -> severityRank(i.getSeverity()))
            .thenComparing(i -> i.getRank() == null ? Integer.MAX_VALUE : i.getRank()));
        if (issues.size() > ReviewScoringConstants.MAX_TOP_ISSUES)
        {
            issues = new ArrayList<>(issues.subList(0, ReviewScoringConstants.MAX_TOP_ISSUES));
        }
        for (int i = 0; i < issues.size(); i++)
        {
            issues.get(i).setRank(i + 1);
            issues.get(i).setSeverity(normalizeSeverity(issues.get(i).getSeverity()));
        }

        ReviewScoreResult normalized = new ReviewScoreResult();
        normalized.setProtocolVersion(ReviewScoringConstants.PROTOCOL_VERSION);
        normalized.setScores(ordered);
        normalized.setTotalScore(total);
        normalized.setSummary(parsed.getSummary().trim());
        normalized.setTopIssues(issues);
        normalized.setFocusIssueCount(issues.size());
        normalized.setHasCriticalSecurityIssue(parsed.getHasCriticalSecurityIssue());
        return normalized;
    }

    private void validateIssue(ReviewTopIssue issue)
    {
        if (isBlank(issue.getTitle()))
        {
            throw new IllegalArgumentException("重点问题缺少 title");
        }
        if (isBlank(issue.getDescription()))
        {
            throw new IllegalArgumentException("重点问题缺少 description");
        }
        if (isBlank(issue.getCategory()))
        {
            throw new IllegalArgumentException("重点问题缺少 category");
        }
        if (isBlank(issue.getSeverity()))
        {
            throw new IllegalArgumentException("重点问题缺少 severity");
        }
        if (normalizeSeverity(issue.getSeverity()) == null)
        {
            throw new IllegalArgumentException("重点问题 severity 非法：" + issue.getSeverity());
        }
        if (isBlank(issue.getEvidence()))
        {
            throw new IllegalArgumentException("重点问题缺少 evidence");
        }
        if (isBlank(issue.getSuggestion()))
        {
            throw new IllegalArgumentException("重点问题缺少 suggestion");
        }
        if (issue.getStartLine() != null && issue.getStartLine() < 1)
        {
            throw new IllegalArgumentException("重点问题 startLine 非法");
        }
        if (issue.getEndLine() != null && issue.getEndLine() < 1)
        {
            throw new IllegalArgumentException("重点问题 endLine 非法");
        }
        if (issue.getStartLine() != null && issue.getEndLine() != null
            && issue.getEndLine() < issue.getStartLine())
        {
            throw new IllegalArgumentException("重点问题 endLine 不能小于 startLine");
        }
    }

    private String normalizeSeverity(String severity)
    {
        if (severity == null)
        {
            return null;
        }
        String value = severity.trim().toUpperCase(Locale.ROOT);
        return switch (value)
        {
            case "CRITICAL", "HIGH", "MEDIUM", "LOW", "INFO" -> value;
            case "ERROR", "BLOCKER", "严重", "阻断" -> ReviewScoringConstants.SEVERITY_CRITICAL;
            case "WARNING", "WARN", "警告" -> ReviewScoringConstants.SEVERITY_MEDIUM;
            default -> null;
        };
    }

    private int severityRank(String severity)
    {
        String normalized = normalizeSeverity(severity);
        if (normalized == null)
        {
            return 99;
        }
        return switch (normalized)
        {
            case ReviewScoringConstants.SEVERITY_CRITICAL -> 1;
            case ReviewScoringConstants.SEVERITY_HIGH -> 2;
            case ReviewScoringConstants.SEVERITY_MEDIUM -> 3;
            case ReviewScoringConstants.SEVERITY_LOW -> 4;
            default -> 5;
        };
    }

    /**
     * 从模型输出中提取 JSON：
     * 1) 优先取任意位置的 markdown 围栏块；2) 从首个 '{' 起做括号配平扫描（忽略字符串内括号），
     * 容忍前后说明文字，避免被尾随文字中的 '}' 截错边界。
     */
    private String extractJson(String content)
    {
        java.util.regex.Matcher fence = FENCE_PATTERN.matcher(content);
        if (fence.find())
        {
            String fenced = fence.group(1).trim();
            if (!fenced.isEmpty())
            {
                return fenced;
            }
        }
        int start = content.indexOf('{');
        if (start < 0)
        {
            return null;
        }
        int depth = 0;
        boolean inString = false;
        boolean escaped = false;
        for (int i = start; i < content.length(); i++)
        {
            char c = content.charAt(i);
            if (inString)
            {
                if (escaped)
                {
                    escaped = false;
                }
                else if (c == '\\')
                {
                    escaped = true;
                }
                else if (c == '"')
                {
                    inString = false;
                }
                continue;
            }
            if (c == '"')
            {
                inString = true;
            }
            else if (c == '{')
            {
                depth++;
            }
            else if (c == '}')
            {
                depth--;
                if (depth == 0)
                {
                    return content.substring(start, i + 1);
                }
            }
        }
        return null;
    }

    private String excerpt(String raw)
    {
        if (raw == null)
        {
            return "";
        }
        return truncate(raw, ReviewScoringConstants.MAX_RAW_RESPONSE_CHARS);
    }

    private String truncate(String value, int max)
    {
        if (value == null)
        {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }

    private boolean isBlank(String value)
    {
        return value == null || value.isBlank();
    }

    private String safeMessage(Exception ex)
    {
        return ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
    }

}
