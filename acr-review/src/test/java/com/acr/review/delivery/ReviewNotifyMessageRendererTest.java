package com.acr.review.delivery;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.List;
import org.junit.jupiter.api.Test;
import com.acr.review.domain.ReviewPipelineConstants;
import com.acr.review.domain.result.ReviewScopeStats;
import com.acr.review.domain.result.ReviewTopIssue;

class ReviewNotifyMessageRendererTest
{
    @Test
    void rendersSuccessSampleStructure()
    {
        ReviewTopIssue issue = new ReviewTopIssue();
        issue.setSeverity("HIGH");
        issue.setOrigin("NEW");
        issue.setTitle("密码明文传输风险");
        issue.setDescription("请求体未加密");
        issue.setFilePath("UserController.java");
        issue.setStartLine(42);
        issue.setEndLine(48);

        ReviewScopeStats stats = new ReviewScopeStats();
        stats.setIncludedFiles(10);
        stats.setExcludedFiles(2);
        stats.setExpandedFiles(1);
        stats.setNewCount(2);
        stats.setExistingCount(1);

        ReviewSummaryContent content = ReviewSummaryContent.builder()
            .taskId(42L)
            .conclusion(ReviewPipelineConstants.CONCLUSION_WARN)
            .conclusionLabel("建议修改")
            .totalScore(72)
            .prNumber(8)
            .prTitle("重构用户登录校验")
            .prAuthor("zhangsan")
            .repositoryOwner("acme")
            .repositoryName("demo")
            .sourceBranch("dev")
            .targetBranch("main")
            .changedFiles(12)
            .additions(120)
            .deletions(30)
            .topIssues(List.of(issue))
            .scopeStats(stats)
            .prUrl("https://github.com/acme/demo/pull/8")
            .detailUrl("https://acr.example.com/review/record-detail/index/42")
            .build();

        String body = ReviewNotifyMessageRenderer.renderSuccess(content);

        assertTrue(body.startsWith("### ⚠️ AI Code Review · 建议修改"));
        assertTrue(body.contains("总分 72/100 · PR #8 重构用户登录校验"));
        assertTrue(body.contains("acme/demo · zhangsan · dev → main · 12 文件 +120/−30"));
        assertTrue(body.contains("Top 3 重点问题"));
        assertTrue(body.contains("[高·新增] 密码明文传输风险 — UserController.java L42-48"));
        assertTrue(body.contains("范围统计：纳入 10 · 排除 2 · 扩展 1 · 新增 2 · 存量 1"));
        assertTrue(body.contains("PR：https://github.com/acme/demo/pull/8"));
        assertTrue(body.contains("详情：https://acr.example.com/review/record-detail/index/42"));
        assertFalse(body.contains("```"));
        assertFalse(body.contains("**["));
    }

    @Test
    void rendersFailedBrief()
    {
        ReviewSummaryContent content = ReviewSummaryContent.builder()
            .taskId(99L)
            .taskStatus(ReviewPipelineConstants.TASK_FAILED)
            .prNumber(8)
            .prTitle("重构用户登录校验")
            .repositoryOwner("acme")
            .repositoryName("demo")
            .failureType(ReviewPipelineConstants.FAILURE_TIMEOUT)
            .failureTypeLabel("引擎超时")
            .detailUrl("https://acr.example.com/review/record-detail/index/99")
            .build();

        String body = ReviewNotifyMessageRenderer.renderFailed(content);

        assertTrue(body.startsWith("### ❌ AI Code Review · 执行失败"));
        assertTrue(body.contains("PR #8 重构用户登录校验 · acme/demo"));
        assertTrue(body.contains("失败类型：引擎超时 · 任务 #99"));
        assertTrue(body.contains("详情：https://acr.example.com/review/record-detail/index/99"));
        assertFalse(body.contains("Top 3"));
        assertFalse(body.contains("范围统计"));
        assertFalse(body.contains("总分"));
    }

    @Test
    void truncatesLongIssueDescriptionAt150Chars()
    {
        ReviewTopIssue issue = new ReviewTopIssue();
        issue.setSeverity("HIGH");
        issue.setTitle("超长说明");
        issue.setDescription("x".repeat(ReviewDeliveryConstants.IM_MAX_ISSUE_DESCRIPTION_CHARS + 20));

        ReviewSummaryContent content = ReviewSummaryContent.builder()
            .conclusionLabel("建议修改")
            .topIssues(List.of(issue))
            .build();

        String body = ReviewNotifyMessageRenderer.renderSuccess(content);

        assertTrue(body.contains("..."));
        assertTrue(body.length() < ReviewDeliveryConstants.IM_MAX_ISSUE_DESCRIPTION_CHARS + 500);
    }
}
