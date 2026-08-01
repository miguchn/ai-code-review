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
}
