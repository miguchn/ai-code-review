package com.acr.review.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import com.acr.review.domain.result.ReviewTopIssue;
import com.acr.review.service.ReviewScoringConstants;

class ReviewEngineResultMapperTest
{
    private final ReviewEngineResultMapper mapper = new ReviewEngineResultMapper();

    private static ReviewTopIssue findByTitle(List<ReviewTopIssue> issues, String title)
    {
        return issues.stream().filter(i -> title.equals(i.getTitle())).findFirst().orElseThrow();
    }

    @Test
    void mapsFullCommentFields()
    {
        Map<String, Object> comment = comment(
            "src/Auth.java",
            "**SQL 注入风险**: 用户输入直接拼进查询。建议使用参数化查询。",
            "String sql = \"select * from t where id=\" + id;",
            "PreparedStatement ps = conn.prepareStatement(\"select * from t where id=?\");",
            12, 14, "security", "critical");

        List<ReviewTopIssue> issues = mapper.mapTopIssues(Map.of("comments", List.of(comment)));

        assertEquals(1, issues.size());
        ReviewTopIssue issue = issues.get(0);
        assertEquals(1, issue.getRank());
        assertEquals("SQL 注入风险", issue.getTitle());
        assertEquals("用户输入直接拼进查询。建议使用参数化查询。", issue.getDescription());
        assertEquals("String sql = \"select * from t where id=\" + id;", issue.getEvidence());
        assertTrue(issue.getSuggestion().contains("建议使用参数化查询"));
        assertTrue(issue.getSuggestion().contains("PreparedStatement"));
        assertEquals(ReviewScoringConstants.SEVERITY_CRITICAL, issue.getSeverity());
        assertEquals(ReviewScoringConstants.DIM_SECURITY, issue.getCategory());
        assertEquals(ReviewScoringConstants.ORIGIN_NEW, issue.getOrigin());
        assertEquals("src/Auth.java", issue.getFilePath());
        assertEquals(12, issue.getStartLine());
        assertEquals(14, issue.getEndLine());
    }

    @Test
    void extractsBoldTitleAndTruncatesTo80()
    {
        String longTitle = "A".repeat(100);
        Map<String, Object> comment = comment(
            "a.java",
            "**" + longTitle + "**: 描述正文",
            "code", null, 1, 1, "bug", "high");

        ReviewTopIssue issue = mapper.mapTopIssues(Map.of("comments", List.of(comment))).get(0);

        assertEquals(80, issue.getTitle().length());
        assertEquals("A".repeat(80), issue.getTitle());
        assertEquals("描述正文", issue.getDescription());
    }

    @Test
    void normalizesSeverityAndCategory()
    {
        List<ReviewTopIssue> issues = mapper.mapTopIssues(Map.of("comments", List.of(
            comment("a.java", "**t1**: d", null, null, 1, 1, "security", "critical"),
            comment("b.java", "**t2**: d", null, null, 1, 1, "bug", "warning"),
            comment("c.java", "**t3**: d", null, null, 1, 1, "performance", "unknown-sev"),
            comment("d.java", "**t4**: d", null, null, 1, 1, "style", "info"),
            comment("e.java", "**t5**: d", null, null, 1, 1, null, null))));

        ReviewTopIssue t1 = findByTitle(issues, "t1");
        ReviewTopIssue t2 = findByTitle(issues, "t2");
        ReviewTopIssue t3 = findByTitle(issues, "t3");
        ReviewTopIssue t4 = findByTitle(issues, "t4");
        ReviewTopIssue t5 = findByTitle(issues, "t5");

        assertEquals(ReviewScoringConstants.DIM_SECURITY, t1.getCategory());
        assertEquals(ReviewScoringConstants.SEVERITY_CRITICAL, t1.getSeverity());
        assertEquals(ReviewScoringConstants.DIM_CORRECTNESS, t2.getCategory());
        assertEquals(ReviewScoringConstants.SEVERITY_MEDIUM, t2.getSeverity());
        assertEquals(ReviewScoringConstants.DIM_PERFORMANCE, t3.getCategory());
        assertEquals(ReviewScoringConstants.SEVERITY_MEDIUM, t3.getSeverity());
        assertEquals(ReviewScoringConstants.DIM_PRACTICE, t4.getCategory());
        assertEquals(ReviewScoringConstants.SEVERITY_INFO, t4.getSeverity());
        assertEquals(ReviewScoringConstants.DIM_PRACTICE, t5.getCategory());
        assertEquals(ReviewScoringConstants.SEVERITY_MEDIUM, t5.getSeverity());
        assertEquals("t1", issues.get(0).getTitle());
        assertEquals("t4", issues.get(4).getTitle());
    }

    @Test
    void skipsEmptyContentAndNullsMissingLocationFieldsIndependently()
    {
        Map<String, Object> missingContent = comment("a.java", "  ", "e", "s", 1, 1, "bug", "high");
        Map<String, Object> missingPath = comment(null, "**无路径**: 描述", "e", "s", 1, 1, "bug", "high");
        Map<String, Object> zeroLine = comment("a.java", "**零行号**: 描述", "e", "s", 0, 0, "bug", "high");
        Map<String, Object> ok = comment("b.java", "**保留**: 正文", "e", "s", 3, 4, "bug", "high");

        List<ReviewTopIssue> issues = mapper.mapTopIssues(Map.of(
            "comments", List.of(missingContent, missingPath, zeroLine, ok)));

        assertEquals(3, issues.size());
        ReviewTopIssue noPath = issues.stream().filter(i -> "无路径".equals(i.getTitle())).findFirst().orElseThrow();
        ReviewTopIssue zero = issues.stream().filter(i -> "零行号".equals(i.getTitle())).findFirst().orElseThrow();
        ReviewTopIssue kept = issues.stream().filter(i -> "保留".equals(i.getTitle())).findFirst().orElseThrow();

        assertNull(noPath.getFilePath());
        assertEquals(1, noPath.getStartLine());
        assertEquals("a.java", zero.getFilePath());
        assertNull(zero.getStartLine());
        assertNull(zero.getEndLine());
        assertEquals("b.java", kept.getFilePath());
        assertEquals(3, kept.getStartLine());
        assertEquals(4, kept.getEndLine());
    }

    @Test
    void sortsBySeverityThenRewritesRank()
    {
        List<ReviewTopIssue> issues = mapper.mapTopIssues(Map.of("comments", List.of(
            comment("a.java", "**低**: d", null, null, 1, 1, "style", "low"),
            comment("b.java", "**严重**: d", null, null, 1, 1, "security", "critical"),
            comment("c.java", "**中**: d", null, null, 1, 1, "bug", "medium"))));

        assertEquals("严重", issues.get(0).getTitle());
        assertEquals(1, issues.get(0).getRank());
        assertEquals(ReviewScoringConstants.SEVERITY_CRITICAL, issues.get(0).getSeverity());
        assertEquals("中", issues.get(1).getTitle());
        assertEquals(2, issues.get(1).getRank());
        assertEquals("低", issues.get(2).getTitle());
        assertEquals(3, issues.get(2).getRank());
    }

    @Test
    void fallsBackToPlainContentAsTitleWhenNoBoldPrefix()
    {
        ReviewTopIssue issue = mapper.mapTopIssues(Map.of("comments", List.of(
            comment("a.java", "普通文本问题说明", null, "fix();", 1, 1, "bug", "high")))).get(0);

        assertEquals("普通文本问题说明", issue.getTitle());
        assertEquals("普通文本问题说明", issue.getDescription());
        assertEquals("fix();", issue.getSuggestion());
    }

    @Test
    void stripsEnglishSeverityPrefixFromBoldTitle()
    {
        ReviewTopIssue issue = mapper.mapTopIssues(Map.of("comments", List.of(
            comment("a.java", "**Critical: SQL Injection Vulnerability**: 描述", null, null, 1, 1, "security", "critical")
        ))).get(0);

        assertEquals("SQL Injection Vulnerability", issue.getTitle());
    }

    @Test
    void stripsChineseSeverityPrefixFromBoldTitle()
    {
        ReviewTopIssue issue = mapper.mapTopIssues(Map.of("comments", List.of(
            comment("a.java", "**高危：硬编码凭据**: 描述", null, null, 1, 1, "security", "high")
        ))).get(0);

        assertEquals("硬编码凭据", issue.getTitle());
    }

    @Test
    void keepsTitleWithoutSeverityPrefix()
    {
        ReviewTopIssue issue = mapper.mapTopIssues(Map.of("comments", List.of(
            comment("a.java", "**SQL Injection vulnerability**: 描述", null, null, 1, 1, "security", "critical")
        ))).get(0);

        assertEquals("SQL Injection vulnerability", issue.getTitle());
    }

    @Test
    void doesNotStripSeverityWordWithoutColon()
    {
        assertEquals("Critical SQL issue", ReviewEngineResultMapper.stripSeverityPrefix("Critical SQL issue"));
        assertEquals("严重问题说明", ReviewEngineResultMapper.stripSeverityPrefix("严重问题说明"));
        assertEquals("Medium risk in auth", ReviewEngineResultMapper.stripSeverityPrefix("Medium risk in auth"));
    }

    @Test
    void stripsSeverityPrefixCaseInsensitiveWithWhitespace()
    {
        assertEquals("SQL Injection", ReviewEngineResultMapper.stripSeverityPrefix("  HIGH : SQL Injection"));
        assertEquals("SQL注入", ReviewEngineResultMapper.stripSeverityPrefix("严重：SQL注入"));
        assertEquals("info note", ReviewEngineResultMapper.stripSeverityPrefix("Info: info note"));
    }

    @Test
    void returnsEmptyWhenNoComments()
    {
        assertTrue(mapper.mapTopIssues(null).isEmpty());
        assertTrue(mapper.mapTopIssues(Map.of()).isEmpty());
        assertTrue(mapper.mapTopIssues(Map.of("comments", List.of())).isEmpty());
    }

    private static Map<String, Object> comment(String path, String content, String existingCode,
                                               String suggestionCode, Integer startLine, Integer endLine,
                                               String category, String severity)
    {
        Map<String, Object> map = new HashMap<>();
        map.put("path", path);
        map.put("content", content);
        map.put("existing_code", existingCode);
        map.put("suggestion_code", suggestionCode);
        map.put("start_line", startLine);
        map.put("end_line", endLine);
        map.put("category", category);
        map.put("severity", severity);
        return map;
    }
}
