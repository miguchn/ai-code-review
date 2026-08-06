package com.acr.review.delivery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.List;
import org.junit.jupiter.api.Test;
import com.acr.review.domain.ReviewPipelineConstants;
import com.acr.review.domain.ReviewTask;
import com.acr.review.domain.ReviewTaskRun;
import com.acr.review.domain.result.ReviewScopeStats;
import com.acr.review.domain.result.ReviewTopIssue;
import com.alibaba.fastjson2.JSON;

class ReviewCommentBodyRendererTest
{
    @Test
    void rendersConclusionScoreTopIssuesOriginAndMarker()
    {
        ReviewTask task = new ReviewTask();
        task.setTaskId(42L);
        task.setReviewConclusion(ReviewPipelineConstants.CONCLUSION_WARN);
        task.setTotalScore(87);
        task.setHeadSha("abcdef1234567890");

        ReviewTopIssue issue = new ReviewTopIssue();
        issue.setRank(1);
        issue.setSeverity("HIGH");
        issue.setOrigin("NEW");
        issue.setTitle("空指针风险");
        issue.setDescription("可能 NPE");
        issue.setFilePath("src/A.java");
        issue.setStartLine(12);
        issue.setEndLine(14);

        ReviewTopIssue existing = new ReviewTopIssue();
        existing.setRank(2);
        existing.setSeverity("MEDIUM");
        existing.setOrigin("EXISTING");
        existing.setTitle("旧风格问题");
        existing.setFilePath("src/B.java");
        existing.setStartLine(3);

        ReviewScopeStats stats = new ReviewScopeStats();
        stats.setIncludedFiles(3);
        stats.setExcludedFiles(2);
        stats.setExpandedFiles(1);
        stats.setNewCount(2);
        stats.setExistingCount(1);

        ReviewTaskRun run = new ReviewTaskRun();
        run.setTopIssuesJson(JSON.toJSONString(List.of(issue, existing)));
        run.setResultJson(JSON.toJSONString(java.util.Map.of("scopeStats", stats)));

        String body = ReviewCommentBodyRenderer.render(task, run);

        assertTrue(body.contains("| 结论 | 建议修改 |"));
        assertTrue(body.contains("| 总分 | 87 / 100 |"));
        assertTrue(body.contains("| 任务 | #42 · `abcdef1` |"));
        assertTrue(body.contains("**[高][新增]** 空指针风险 — `src/A.java` L12-14"));
        assertTrue(body.contains("**[中][存量]** 旧风格问题 — `src/B.java` L3"));
        assertTrue(body.contains("本次新增 2 个问题 · 存量 1 个"));
        assertTrue(body.contains("审查文件：纳入 3 个 · 扩展 1 个"));
        assertTrue(body.contains(ReviewDeliveryConstants.COMMENT_MARKER));
        assertEquals(ReviewDeliveryConstants.COMMENT_MARKER,
            body.substring(body.lastIndexOf("<!--")).trim());
    }

    @Test
    void rendersDispositionLineWhenPresent()
    {
        ReviewTopIssue issue = new ReviewTopIssue();
        issue.setSeverity("HIGH");
        issue.setOrigin("NEW");
        issue.setTitle("误报样例");
        issue.setDispositionStatus("FALSE_POSITIVE");
        issue.setDispositionNote("与本次变更无关");

        ReviewSummaryContent content = ReviewSummaryContent.builder()
            .taskId(1L)
            .conclusionLabel("建议修改")
            .totalScore(70)
            .topIssues(List.of(issue))
            .build();

        String body = ReviewCommentBodyRenderer.render(content);
        assertTrue(body.contains("处置：误报（与本次变更无关）"));
        assertTrue(body.contains(ReviewDeliveryConstants.COMMENT_MARKER));
    }

    @Test
    void rendersPlaceholdersWhenFieldsMissing()
    {
        ReviewTask task = new ReviewTask();
        task.setTaskId(7L);
        task.setReviewConclusion(ReviewPipelineConstants.CONCLUSION_PASS);

        ReviewTaskRun run = new ReviewTaskRun();

        String body = ReviewCommentBodyRenderer.render(task, run);

        assertTrue(body.contains("| 结论 | 通过 |"));
        assertTrue(body.contains("| 总分 | -- |"));
        assertTrue(body.contains("暂无重点问题"));
        assertTrue(body.contains("范围统计：—"));
        assertTrue(body.contains(ReviewDeliveryConstants.COMMENT_MARKER));
    }

    @Test
    void truncatesLongIssueDescription()
    {
        ReviewTask task = new ReviewTask();
        task.setTaskId(1L);
        task.setReviewConclusion(ReviewPipelineConstants.CONCLUSION_BLOCK);

        ReviewTopIssue issue = new ReviewTopIssue();
        issue.setSeverity("CRITICAL");
        issue.setTitle("超长说明");
        issue.setDescription("x".repeat(ReviewDeliveryConstants.MAX_ISSUE_DESCRIPTION_CHARS + 20));

        ReviewTaskRun run = new ReviewTaskRun();
        run.setTopIssuesJson(JSON.toJSONString(List.of(issue)));

        String body = ReviewCommentBodyRenderer.render(task, run);

        assertTrue(body.contains("高风险"));
        assertTrue(body.contains("**[严重][新增]** 超长说明"));
        assertTrue(body.contains("..."));
        assertTrue(body.length() < ReviewDeliveryConstants.MAX_ISSUE_DESCRIPTION_CHARS + 400);
    }

    @Test
    void buildsIdempotencyKey()
    {
        assertEquals("GITHUB:9:15:SUMMARY_COMMENT",
            ReviewDeliveryConstants.idempotencyKey(9L, 15));
    }

    @Test
    void rendersRecheckingSectionAndDispositionPendingLabel()
    {
        ReviewTopIssue issue = new ReviewTopIssue();
        issue.setSeverity("HIGH");
        issue.setOrigin("NEW");
        issue.setTitle("注入风险");
        issue.setDispositionStatus("RECHECKING");

        ReviewSummaryContent content = ReviewSummaryContent.builder()
            .taskId(1L)
            .conclusionLabel("建议修改")
            .totalScore(70)
            .topIssues(List.of(issue))
            .recheckingTitles(List.of("sql-injection", "cmd-injection", "hardcoded-password", "extra"))
            .build();

        String body = ReviewCommentBodyRenderer.render(content);
        assertTrue(body.contains("处置：待复核"));
        assertTrue(body.contains("疑似已修复（4）：sql-injection / cmd-injection / hardcoded-password…"));
        assertTrue(body.contains("请前往问题台账复核"));
        assertTrue(body.contains(ReviewDeliveryConstants.COMMENT_MARKER));
    }

    @Test
    void displayTopIssuesLimitsToThreeWithTotalHint()
    {
        List<ReviewTopIssue> issues = new java.util.ArrayList<>();
        for (int i = 1; i <= 5; i++)
        {
            ReviewTopIssue issue = new ReviewTopIssue();
            issue.setSeverity("LOW");
            issue.setOrigin("NEW");
            issue.setTitle("issue-" + i);
            issues.add(issue);
        }
        ReviewSummaryContent content = ReviewSummaryContent.builder()
            .taskId(1L)
            .conclusionLabel("通过")
            .totalScore(90)
            .topIssues(issues)
            .build();

        String body = ReviewCommentBodyRenderer.render(content);
        assertTrue(body.contains("issue-1"));
        assertTrue(body.contains("issue-3"));
        assertTrue(body.contains("共 5 个问题，其余见问题台账"));
        assertTrue(!body.contains("issue-4") || body.contains("共 5 个问题"));
    }
}
