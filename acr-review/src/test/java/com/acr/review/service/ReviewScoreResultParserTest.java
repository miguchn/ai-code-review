package com.acr.review.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import com.acr.review.domain.ReviewPipelineConstants;
import com.acr.review.domain.result.ReviewScoreResult;

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
    void truncatesTopIssuesToThreeAndRewritesRank()
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
        assertEquals(3, result.getTopIssues().size());
        assertEquals(3, result.getFocusIssueCount());
        assertEquals(1, result.getTopIssues().get(0).getRank());
        assertEquals("CRITICAL", result.getTopIssues().get(0).getSeverity());
        assertEquals(ReviewPipelineConstants.CONCLUSION_BLOCK, parser.resolveConclusion(result));
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
        // 模型常见输出：前导说明 + ```json 围栏
        String raw = "以下是审查结果：\n```json\n" + validBase() + "\n```";
        ReviewScoreParseResult parsed = parser.parse(raw);
        assertTrue(parsed.isSuccess());
        assertEquals(72, parsed.getResult().getTotalScore());
    }

    @Test
    void parsesJsonFollowedByProseContainingBrace()
    {
        // 尾随说明文字中含 }，不得截错边界
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
            {"protocolVersion":"1.0","summary":"s","topIssues":[],"focusIssueCount":0,"hasCriticalSecurityIssue":false}
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
              "protocolVersion":"1.0",
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

    // ---------- v1.1 归属打标 ----------

    @Test
    void existingIssuesDoNotOccupyTop3Slots()
    {
        // 存量 CRITICAL 被剔除后，第 4 名的新增 LOW 必须递补进 Top 3，而不是被存量占位丢弃
        ReviewScoreParseResult parsed = parser.parse(originFixtureJson("1.1", true), originClassifier(), false);
        assertTrue(parsed.isSuccess());
        ReviewScoreResult result = parsed.getResult();

        assertEquals("1.1", result.getProtocolVersion());
        assertEquals(3, result.getTopIssues().size());
        assertEquals(3, result.getFocusIssueCount());
        assertEquals("HIGH", result.getTopIssues().get(0).getSeverity());
        assertEquals("NEW", result.getTopIssues().get(0).getOrigin());
        assertEquals("MEDIUM", result.getTopIssues().get(1).getSeverity());
        assertEquals("LOW", result.getTopIssues().get(2).getSeverity());
        assertTrue(result.getTopIssues().stream().allMatch(i -> "NEW".equals(i.getOrigin())));
        // 归属统计：新增 3（含 1 不可判定）、存量 1
        assertEquals(3, parsed.getNewCount());
        assertEquals(1, parsed.getExistingCount());
        assertEquals(1, parsed.getOriginUnverifiableCount());
        // 旗标为真但唯一的 CRITICAL 是存量：不阻断；新增 HIGH → 警告
        assertEquals(ReviewPipelineConstants.CONCLUSION_WARN, parser.resolveConclusion(result));
    }

    @Test
    void reportExistingKeepsAnnotatedExistingIssues()
    {
        ReviewScoreParseResult parsed = parser.parse(originFixtureJson("1.1", true), originClassifier(), true);
        assertTrue(parsed.isSuccess());
        ReviewScoreResult result = parsed.getResult();

        // 3 条新增（rank 1-3）+ 1 条存量标注保留（rank 4，仅信息展示）
        assertEquals(4, result.getTopIssues().size());
        assertEquals(3, result.getFocusIssueCount());
        com.acr.review.domain.result.ReviewTopIssue existing = result.getTopIssues().get(3);
        assertEquals("EXISTING", existing.getOrigin());
        assertEquals("CRITICAL", existing.getSeverity());
        assertEquals(4, existing.getRank());
        // 存量 CRITICAL 不影响结论：新增 HIGH → 警告
        assertEquals(ReviewPipelineConstants.CONCLUSION_WARN, parser.resolveConclusion(result));
    }

    @Test
    void existingCriticalAloneDoesNotBlockConclusion()
    {
        // 旗标为真，但 CRITICAL 全部判为存量且剔除：结论按新增问题评估 → 通过
        String json = originFixtureJson("1.1", true)
            .replace("\"severity\":\"HIGH\"", "\"severity\":\"MEDIUM\"");
        ReviewScoreParseResult parsed = parser.parse(json, originClassifier(), false);
        assertTrue(parsed.isSuccess());
        assertEquals(ReviewPipelineConstants.CONCLUSION_PASS, parser.resolveConclusion(parsed.getResult()));
    }

    @Test
    void v10ProtocolAcceptedAndTaggedAsV11()
    {
        // 模型偶发回写旧版本号：按兼容解析并正常打标，落库统一 1.1
        ReviewScoreParseResult parsed = parser.parse(originFixtureJson("1.0", false), originClassifier(), false);
        assertTrue(parsed.isSuccess());
        assertEquals("1.1", parsed.getResult().getProtocolVersion());
        assertTrue(parsed.getResult().getTopIssues().stream().allMatch(i -> i.getOrigin() != null));
    }

    @Test
    void noClassifierKeepsV10TruncationBehavior()
    {
        // 无分类器（决策降级）：维持 v1.0 行为——直接截断 Top 3、不打标、旗标即阻断
        ReviewScoreParseResult parsed = parser.parse(originFixtureJson("1.1", true));
        assertTrue(parsed.isSuccess());
        ReviewScoreResult result = parsed.getResult();
        assertEquals(3, result.getTopIssues().size());
        assertEquals("CRITICAL", result.getTopIssues().get(0).getSeverity());
        assertTrue(result.getTopIssues().stream().allMatch(i -> i.getOrigin() == null));
        assertEquals(0, parsed.getExistingCount());
        assertEquals(ReviewPipelineConstants.CONCLUSION_BLOCK, parser.resolveConclusion(result));
    }

    /**
     * 归属夹具：Main.java 右侧 hunk [10,14]、新增行 [12,12]。
     * 问题依次为：存量 CRITICAL（行 50，hunk 外）、新增 HIGH（行 12）、不可判定 MEDIUM（未知文件）、邻近 LOW（行 14）。
     */
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
