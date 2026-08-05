package com.acr.review.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.Test;
import com.acr.review.domain.ReviewPipelineConstants;
import com.acr.review.domain.result.ReviewScoreResult;
import com.acr.system.service.ISysConfigService;

class ReviewScoreResultParserTest
{
    private final ReviewScoreResultParser parser = new ReviewScoreResultParser();

    @Test
    void recalculatesTotalScoreAndIgnoresModelTotal()
    {
        String json = """
            {
              "protocolVersion":"1.0",
              "scores":[
                {"dimension":"CORRECTNESS","score":30,"maxScore":40,"reason":"ok"},
                {"dimension":"SECURITY","score":20,"maxScore":30,"reason":"ok"},
                {"dimension":"PRACTICE","score":15,"maxScore":20,"reason":"ok"},
                {"dimension":"PERFORMANCE","score":4,"maxScore":5,"reason":"ok"},
                {"dimension":"COMMIT_QUALITY","score":3,"maxScore":5,"reason":"ok"}
              ],
              "totalScore":1,
              "summary":"整体尚可",
              "topIssues":[],
              "focusIssueCount":9,
              "hasCriticalSecurityIssue":false
            }
            """;
        ReviewScoreParseResult parsed = parser.parse(json);
        assertTrue(parsed.isSuccess());
        assertEquals(72, parsed.getResult().getTotalScore());
        assertEquals(0, parsed.getResult().getFocusIssueCount());
        assertEquals("1.2", parsed.getResult().getProtocolVersion());
    }

    @Test
    void rejectsOutOfRangeScore()
    {
        String json = validBase().replace("\"score\":30", "\"score\":41");
        ReviewScoreParseResult parsed = parser.parse(json);
        assertFalse(parsed.isSuccess());
        assertTrue(parsed.getErrorMessage().contains("超出范围"));
    }

    @Test
    void keepsFullIssueListAndRecalculatesFocusCount()
    {
        String json = """
            {
              "protocolVersion":"1.2",
              "scores":[
                {"dimension":"CORRECTNESS","score":30,"maxScore":40,"reason":"ok"},
                {"dimension":"SECURITY","score":20,"maxScore":30,"reason":"ok"},
                {"dimension":"PRACTICE","score":15,"maxScore":20,"reason":"ok"},
                {"dimension":"PERFORMANCE","score":4,"maxScore":5,"reason":"ok"},
                {"dimension":"COMMIT_QUALITY","score":3,"maxScore":5,"reason":"ok"}
              ],
              "totalScore":72,
              "summary":"有多项问题",
              "topIssues":[
                {"rank":9,"severity":"LOW","category":"style","title":"a","description":"d","filePath":null,"startLine":null,"endLine":null,"evidence":"e","suggestion":"s"},
                {"rank":8,"severity":"CRITICAL","category":"security","title":"b","description":"d","filePath":"a.java","startLine":1,"endLine":2,"evidence":"e","suggestion":"s"},
                {"rank":7,"severity":"HIGH","category":"bug","title":"c","description":"d","filePath":null,"startLine":null,"endLine":null,"evidence":"e","suggestion":"s"},
                {"rank":6,"severity":"MEDIUM","category":"practice","title":"d","description":"d","filePath":null,"startLine":null,"endLine":null,"evidence":"e","suggestion":"s"}
              ],
              "focusIssueCount":4,
              "hasCriticalSecurityIssue":true
            }
            """;
        ReviewScoreParseResult parsed = parser.parse(json);
        assertTrue(parsed.isSuccess());
        ReviewScoreResult result = parsed.getResult();
        assertEquals(4, result.getTopIssues().size());
        assertEquals(2, result.getFocusIssueCount(), "focusIssueCount = NEW 中 CRITICAL/HIGH");
        assertEquals(1, result.getTopIssues().get(0).getRank());
        assertEquals("CRITICAL", result.getTopIssues().get(0).getSeverity());
        assertEquals(ReviewPipelineConstants.CONCLUSION_BLOCK, parser.resolveConclusion(result));
        assertFalse(parsed.isIssuesTruncated());
    }

    @Test
    void truncatesAtMaxIssuesAndMarksTruncated()
    {
        ISysConfigService config = mock(ISysConfigService.class);
        when(config.selectConfigByKey(ReviewScoringConstants.CONFIG_MAX_ISSUES)).thenReturn("20");
        ReviewScoreResultParser limited = new ReviewScoreResultParser(config);

        StringBuilder issues = new StringBuilder();
        for (int i = 1; i <= 21; i++)
        {
            if (i > 1)
            {
                issues.append(',');
            }
            issues.append("{\"rank\":").append(i)
                .append(",\"severity\":\"LOW\",\"category\":\"style\",\"title\":\"t").append(i)
                .append("\",\"description\":\"d\",\"filePath\":null,\"startLine\":null,\"endLine\":null,")
                .append("\"evidence\":\"e\",\"suggestion\":\"s\"}");
        }
        String json = """
            {
              "protocolVersion":"1.2",
              "scores":[
                {"dimension":"CORRECTNESS","score":30,"maxScore":40,"reason":"ok"},
                {"dimension":"SECURITY","score":20,"maxScore":30,"reason":"ok"},
                {"dimension":"PRACTICE","score":15,"maxScore":20,"reason":"ok"},
                {"dimension":"PERFORMANCE","score":4,"maxScore":5,"reason":"ok"},
                {"dimension":"COMMIT_QUALITY","score":3,"maxScore":5,"reason":"ok"}
              ],
              "summary":"很多问题",
              "topIssues":[%s],
              "focusIssueCount":21,
              "hasCriticalSecurityIssue":false
            }
            """.formatted(issues);
        ReviewScoreParseResult parsed = limited.parse(json);
        assertTrue(parsed.isSuccess());
        assertEquals(20, parsed.getResult().getTopIssues().size());
        assertTrue(parsed.isIssuesTruncated());
        assertEquals(0, parsed.getResult().getFocusIssueCount());
    }

    @Test
    void acceptsProtocolVersions101112()
    {
        for (String version : new String[] {"1.0", "1.1", "1.2"})
        {
            ReviewScoreParseResult parsed = parser.parse(validBase().replace("\"1.0\"", "\"" + version + "\""));
            assertTrue(parsed.isSuccess(), version);
            assertEquals("1.2", parsed.getResult().getProtocolVersion());
        }
    }

    @Test
    void failsOnNonJson()
    {
        ReviewScoreParseResult parsed = parser.parse("审查通过，没有问题");
        assertFalse(parsed.isSuccess());
        assertTrue(parsed.getErrorMessage().contains("JSON"));
    }

    @Test
    void parsesMarkdownFencedJsonWithPreamble()
    {
        String raw = "以下是审查结果：\n```json\n" + validBase() + "\n```";
        ReviewScoreParseResult parsed = parser.parse(raw);
        assertTrue(parsed.isSuccess());
        assertEquals(72, parsed.getResult().getTotalScore());
    }

    @Test
    void parsesJsonFollowedByProseContainingBrace()
    {
        String raw = validBase() + "\n注意：以上分数 } 仅供参考";
        ReviewScoreParseResult parsed = parser.parse(raw);
        assertTrue(parsed.isSuccess());
        assertEquals(72, parsed.getResult().getTotalScore());
    }

    @Test
    void parsesJsonWithBraceInsideStringValues()
    {
        String raw = validBase().replace("整体尚可", "使用 {code} 块包裹 } 示例");
        ReviewScoreParseResult parsed = parser.parse(raw);
        assertTrue(parsed.isSuccess());
        assertTrue(parsed.getResult().getSummary().contains("{code}"));
    }

    @Test
    void failsWhenScoresMissing()
    {
        String raw = """
            {"protocolVersion":"1.2","summary":"s","topIssues":[],"focusIssueCount":0,"hasCriticalSecurityIssue":false}
            """;
        ReviewScoreParseResult parsed = parser.parse(raw);
        assertFalse(parsed.isSuccess());
        assertTrue(parsed.getErrorMessage().contains("scores"));
    }

    @Test
    void failsOnInvertedLineRange()
    {
        String raw = """
            {
              "protocolVersion":"1.2",
              "scores":[
                {"dimension":"CORRECTNESS","score":30,"maxScore":40,"reason":"ok"},
                {"dimension":"SECURITY","score":20,"maxScore":30,"reason":"ok"},
                {"dimension":"PRACTICE","score":15,"maxScore":20,"reason":"ok"},
                {"dimension":"PERFORMANCE","score":4,"maxScore":5,"reason":"ok"},
                {"dimension":"COMMIT_QUALITY","score":3,"maxScore":5,"reason":"ok"}
              ],
              "summary":"s",
              "topIssues":[
                {"rank":1,"severity":"LOW","category":"style","title":"a","description":"d","filePath":"a.java","startLine":10,"endLine":5,"evidence":"e","suggestion":"s"}
              ],
              "focusIssueCount":1,
              "hasCriticalSecurityIssue":false
            }
            """;
        ReviewScoreParseResult parsed = parser.parse(raw);
        assertFalse(parsed.isSuccess());
        assertTrue(parsed.getErrorMessage().contains("endLine"));
    }

    private String validBase()
    {
        return """
            {
              "protocolVersion":"1.0",
              "scores":[
                {"dimension":"CORRECTNESS","score":30,"maxScore":40,"reason":"ok"},
                {"dimension":"SECURITY","score":20,"maxScore":30,"reason":"ok"},
                {"dimension":"PRACTICE","score":15,"maxScore":20,"reason":"ok"},
                {"dimension":"PERFORMANCE","score":4,"maxScore":5,"reason":"ok"},
                {"dimension":"COMMIT_QUALITY","score":3,"maxScore":5,"reason":"ok"}
              ],
              "totalScore":72,
              "summary":"整体尚可",
              "topIssues":[],
              "focusIssueCount":0,
              "hasCriticalSecurityIssue":false
            }
            """;
    }

    // ---------- 归属打标 ----------

    @Test
    void existingIssuesDoNotBlockFullNewList()
    {
        ReviewScoreParseResult parsed = parser.parse(originFixtureJson("1.2", true), originClassifier(), false);
        assertTrue(parsed.isSuccess());
        ReviewScoreResult result = parsed.getResult();

        assertEquals("1.2", result.getProtocolVersion());
        assertEquals(3, result.getTopIssues().size());
        assertEquals(1, result.getFocusIssueCount(), "仅 HIGH 计入 focus");
        assertEquals("HIGH", result.getTopIssues().get(0).getSeverity());
        assertEquals("NEW", result.getTopIssues().get(0).getOrigin());
        assertTrue(result.getTopIssues().stream().allMatch(i -> "NEW".equals(i.getOrigin())));
        assertEquals(3, parsed.getNewCount());
        assertEquals(1, parsed.getExistingCount());
        assertEquals(1, parsed.getOriginUnverifiableCount());
        assertEquals(ReviewPipelineConstants.CONCLUSION_WARN, parser.resolveConclusion(result));
    }

    @Test
    void reportExistingKeepsAnnotatedExistingIssues()
    {
        ReviewScoreParseResult parsed = parser.parse(originFixtureJson("1.1", true), originClassifier(), true);
        assertTrue(parsed.isSuccess());
        ReviewScoreResult result = parsed.getResult();

        assertEquals(4, result.getTopIssues().size());
        assertEquals(1, result.getFocusIssueCount());
        com.acr.review.domain.result.ReviewTopIssue existing = result.getTopIssues().get(3);
        assertEquals("EXISTING", existing.getOrigin());
        assertEquals("CRITICAL", existing.getSeverity());
        assertEquals(ReviewPipelineConstants.CONCLUSION_WARN, parser.resolveConclusion(result));
    }

    @Test
    void existingCriticalAloneDoesNotBlockConclusion()
    {
        String json = originFixtureJson("1.2", true)
            .replace("\"severity\":\"HIGH\"", "\"severity\":\"MEDIUM\"");
        ReviewScoreParseResult parsed = parser.parse(json, originClassifier(), false);
        assertTrue(parsed.isSuccess());
        assertEquals(ReviewPipelineConstants.CONCLUSION_PASS, parser.resolveConclusion(parsed.getResult()));
    }

    @Test
    void v10ProtocolAcceptedAndTaggedAsV12()
    {
        ReviewScoreParseResult parsed = parser.parse(originFixtureJson("1.0", false), originClassifier(), false);
        assertTrue(parsed.isSuccess());
        assertEquals("1.2", parsed.getResult().getProtocolVersion());
        assertTrue(parsed.getResult().getTopIssues().stream().allMatch(i -> i.getOrigin() != null));
    }

    @Test
    void noClassifierKeepsFullListWithoutOrigin()
    {
        ReviewScoreParseResult parsed = parser.parse(originFixtureJson("1.2", true));
        assertTrue(parsed.isSuccess());
        ReviewScoreResult result = parsed.getResult();
        assertEquals(4, result.getTopIssues().size());
        assertEquals("CRITICAL", result.getTopIssues().get(0).getSeverity());
        assertTrue(result.getTopIssues().stream().allMatch(i -> i.getOrigin() == null));
        assertEquals(2, result.getFocusIssueCount(), "未打标时 CRITICAL+HIGH");
        assertEquals(0, parsed.getExistingCount());
        assertEquals(ReviewPipelineConstants.CONCLUSION_BLOCK, parser.resolveConclusion(result));
    }

    private String originFixtureJson(String protocolVersion, boolean criticalFlag)
    {
        return """
            {
              "protocolVersion":"%s",
              "scores":[
                {"dimension":"CORRECTNESS","score":30,"maxScore":40,"reason":"ok"},
                {"dimension":"SECURITY","score":20,"maxScore":30,"reason":"ok"},
                {"dimension":"PRACTICE","score":15,"maxScore":20,"reason":"ok"},
                {"dimension":"PERFORMANCE","score":4,"maxScore":5,"reason":"ok"},
                {"dimension":"COMMIT_QUALITY","score":3,"maxScore":5,"reason":"ok"}
              ],
              "summary":"发现新增与存量问题",
              "topIssues":[
                {"rank":1,"severity":"CRITICAL","category":"security","title":"存量问题","description":"d","filePath":"src/main/java/Main.java","startLine":50,"endLine":50,"evidence":"e","suggestion":"s"},
                {"rank":2,"severity":"HIGH","category":"bug","title":"新增行问题","description":"d","filePath":"src/main/java/Main.java","startLine":12,"endLine":12,"evidence":"e","suggestion":"s"},
                {"rank":3,"severity":"MEDIUM","category":"practice","title":"不可判定问题","description":"d","filePath":"src/Ghost.java","startLine":3,"endLine":3,"evidence":"e","suggestion":"s"},
                {"rank":4,"severity":"LOW","category":"style","title":"邻近上下文问题","description":"d","filePath":"src/main/java/Main.java","startLine":14,"endLine":14,"evidence":"e","suggestion":"s"}
              ],
              "focusIssueCount":4,
              "hasCriticalSecurityIssue":%s
            }
            """.formatted(protocolVersion, criticalFlag);
    }

    private com.acr.review.scope.IssueOriginClassifier originClassifier()
    {
        String diff = "diff --git a/src/main/java/Main.java b/src/main/java/Main.java\n"
            + "index 3333333..4444444 100644\n"
            + "--- a/src/main/java/Main.java\n"
            + "+++ b/src/main/java/Main.java\n"
            + "@@ -10,4 +10,5 @@ public class Main {\n"
            + "     void call() {\n"
            + "         helper();\n"
            + "+        audit();\n"
            + "     }\n"
            + " }\n";
        return new com.acr.review.scope.IssueOriginClassifier(
            new com.acr.review.scope.UnifiedDiffParser().parse(diff), java.util.Set.of());
    }
}
