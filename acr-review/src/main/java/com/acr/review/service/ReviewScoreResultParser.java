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
import com.acr.review.scope.IssueOriginClassifier;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 解析并校验大模型统一评分 JSON；总分由后端重算，Top3 由后端截断校正。
 * v1.1：传入归属分类器时按 Diff 行号映射打标 origin，EXISTING 问题不进 Top 3、
 * 不计 focusIssueCount、不影响结论；分类在排序后、截断前执行，避免存量问题占用 Top 3 名额。
 */
@Component
public class ReviewScoreResultParser
{
    /** 匹配任意位置的 markdown 围栏 JSON 块（模型常见输出形态）。 */
    private static final java.util.regex.Pattern FENCE_PATTERN = java.util.regex.Pattern.compile(
        "```(?:json)?\\s*\\n(.*?)```", java.util.regex.Pattern.DOTALL | java.util.regex.Pattern.CASE_INSENSITIVE);

    private final ObjectMapper objectMapper = new ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    /** v1.0 兼容入口：不做归属打标，行为与协议 v1.0 一致。 */
    public ReviewScoreParseResult parse(String rawContent)
    {
        return parse(rawContent, null, false);
    }

    /**
     * v1.1 入口。
     *
     * @param originClassifier 归属分类器；null 时退化为 v1.0 行为（不打标、直接截断 Top 3）
     * @param reportExisting   项目快照 scope_report_existing：true 时 EXISTING 问题标注后保留（仅信息展示），
     *                         false 时剔除并计数
     */
    public ReviewScoreParseResult parse(String rawContent, IssueOriginClassifier originClassifier, boolean reportExisting)
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
            NormalizeOutcome outcome = normalizeAndValidate(parsed, originClassifier, reportExisting);
            return ReviewScoreParseResult.ok(outcome.result(), excerpt,
                outcome.newCount(), outcome.existingCount(), outcome.unverifiableCount());
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
        boolean taggingActive = result.getTopIssues().stream().anyMatch(issue -> issue.getOrigin() != null);
        if (Boolean.TRUE.equals(result.getHasCriticalSecurityIssue()))
        {
            // 打标生效时，旗标对应的 CRITICAL 若已判为存量则不得阻断（EXISTING 不影响结论）；
            // 未打标（降级/v1.0）保持旗标即阻断的既有行为。
            if (!taggingActive || hasNewIssueWithSeverity(result, ReviewScoringConstants.SEVERITY_CRITICAL))
            {
                return ReviewPipelineConstants.CONCLUSION_BLOCK;
            }
        }
        for (ReviewTopIssue issue : result.getTopIssues())
        {
            if (ReviewScoringConstants.ORIGIN_EXISTING.equals(issue.getOrigin()))
            {
                continue;
            }
            String severity = issue.getSeverity() == null ? "" : issue.getSeverity().toUpperCase(Locale.ROOT);
            if (ReviewScoringConstants.SEVERITY_CRITICAL.equals(severity)
                || ReviewScoringConstants.SEVERITY_HIGH.equals(severity))
            {
                return ReviewPipelineConstants.CONCLUSION_WARN;
            }
        }
        return ReviewPipelineConstants.CONCLUSION_PASS;
    }

    private boolean hasNewIssueWithSeverity(ReviewScoreResult result, String expectedSeverity)
    {
        for (ReviewTopIssue issue : result.getTopIssues())
        {
            if (ReviewScoringConstants.ORIGIN_EXISTING.equals(issue.getOrigin()))
            {
                continue;
            }
            if (expectedSeverity.equalsIgnoreCase(issue.getSeverity()))
            {
                return true;
            }
        }
        return false;
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

    private NormalizeOutcome normalizeAndValidate(ReviewScoreResult parsed,
                                                  IssueOriginClassifier originClassifier, boolean reportExisting)
    {
        if (parsed == null)
        {
            throw new IllegalArgumentException("审查结果为空");
        }
        if (!ReviewScoringConstants.COMPATIBLE_PROTOCOL_VERSIONS.contains(parsed.getProtocolVersion()))
        {
            throw new IllegalArgumentException("不支持的协议版本：" + parsed.getProtocolVersion()
                + "，期望 " + ReviewScoringConstants.PROTOCOL_VERSION
                + "（兼容 " + String.join("/", ReviewScoringConstants.COMPATIBLE_PROTOCOL_VERSIONS) + "）");
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

        // 归属打标（v1.1）：分类在排序后、Top 3 截断前执行，存量问题不占用 Top 3 名额。
        // 未传分类器（降级/v1.0 调用方）时保持既有行为：不打标、直接截断。
        List<ReviewTopIssue> newIssues = new ArrayList<>();
        List<ReviewTopIssue> existingIssues = new ArrayList<>();
        int unverifiableCount = 0;
        for (ReviewTopIssue issue : issues)
        {
            if (originClassifier == null)
            {
                newIssues.add(issue);
                continue;
            }
            IssueOriginClassifier.Verdict verdict = originClassifier.classify(
                issue.getFilePath(), issue.getStartLine(), issue.getEndLine());
            if (verdict == IssueOriginClassifier.Verdict.EXISTING)
            {
                issue.setOrigin(ReviewScoringConstants.ORIGIN_EXISTING);
                existingIssues.add(issue);
            }
            else
            {
                issue.setOrigin(ReviewScoringConstants.ORIGIN_NEW);
                newIssues.add(issue);
                if (verdict == IssueOriginClassifier.Verdict.UNVERIFIABLE)
                {
                    unverifiableCount++;
                }
            }
        }

        int totalNewCount = newIssues.size();
        if (newIssues.size() > ReviewScoringConstants.MAX_TOP_ISSUES)
        {
            newIssues = new ArrayList<>(newIssues.subList(0, ReviewScoringConstants.MAX_TOP_ISSUES));
        }
        List<ReviewTopIssue> finalIssues = new ArrayList<>(newIssues);
        for (int i = 0; i < newIssues.size(); i++)
        {
            newIssues.get(i).setRank(i + 1);
            newIssues.get(i).setSeverity(normalizeSeverity(newIssues.get(i).getSeverity()));
        }
        // reportExisting=Y：存量问题标注保留（排在新增问题之后，同样封顶，仅信息展示，不计 focusIssueCount）。
        int existingKept = 0;
        if (originClassifier != null && reportExisting)
        {
            for (ReviewTopIssue issue : existingIssues)
            {
                if (existingKept >= ReviewScoringConstants.MAX_TOP_ISSUES)
                {
                    break;
                }
                issue.setRank(finalIssues.size() + 1);
                issue.setSeverity(normalizeSeverity(issue.getSeverity()));
                finalIssues.add(issue);
                existingKept++;
            }
        }

        ReviewScoreResult normalized = new ReviewScoreResult();
        normalized.setProtocolVersion(ReviewScoringConstants.PROTOCOL_VERSION);
        normalized.setScores(ordered);
        normalized.setTotalScore(total);
        normalized.setSummary(parsed.getSummary().trim());
        normalized.setTopIssues(finalIssues);
        normalized.setFocusIssueCount(newIssues.size());
        normalized.setHasCriticalSecurityIssue(parsed.getHasCriticalSecurityIssue());
        return new NormalizeOutcome(normalized, totalNewCount, existingIssues.size(), unverifiableCount);
    }

    /** 归一化输出：结果 + 归属统计（均为截断前发现总数；未打标时全为 0）。 */
    private record NormalizeOutcome(ReviewScoreResult result, int newCount, int existingCount, int unverifiableCount)
    {
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
