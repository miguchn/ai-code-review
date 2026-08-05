package com.acr.review.delivery;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Test;
import com.acr.review.domain.ReviewPipelineConstants;
import com.acr.review.domain.result.ReviewScopeStats;
import com.acr.review.domain.result.ReviewTopIssue;

class ReviewNotifyMessageRendererTest
{
    @Test
    void rendersSuccessMiniReportSections()
    {
        ReviewTopIssue issue = new ReviewTopIssue();
        issue.setSeverity("CRITICAL");
        issue.setOrigin("NEW");
        issue.setTitle("未处理异常可能中断发送");
        issue.setDescription("发送过程中可能抛出异常，但完全没有 catch。");
        issue.setSuggestion("在发送逻辑中增加 try-catch，失败时重试。");
        issue.setFilePath("src/wecom.py");
        issue.setStartLine(12);
        issue.setEndLine(18);

        ReviewScopeStats stats = new ReviewScopeStats();
        stats.setIncludedFiles(3);
        stats.setExcludedFiles(2);
        stats.setExpandedFiles(1);
        stats.setNewCount(2);
        stats.setExistingCount(1);

        ReviewSummaryContent content = ReviewSummaryContent.builder()
            .taskId(42L)
            .conclusion(ReviewPipelineConstants.CONCLUSION_WARN)
            .conclusionLabel("建议修改")
            .totalScore(78)
            .prNumber(4)
            .prTitle("Fix login exception handling")
            .prAuthor("miguchn")
            .repositoryOwner("acme")
            .repositoryName("demo")
            .sourceBranch("feature/fix")
            .targetBranch("main")
            .commitMessage("Fix login exception handling")
            .reviewTime(new Date(1754361000000L))
            .summaryText("整体风险可控……")
            .topIssues(List.of(issue))
            .scopeStats(stats)
            .prUrl("https://github.com/acme/demo/pull/4")
            .detailUrl("https://acr.example.com/review/record-detail/index/42")
            .build();

        String body = ReviewNotifyMessageRenderer.renderSuccess(content);

        assertTrue(body.startsWith("### ⚠️ AI Code Review · 建议修改 · 78/100"));

        assertTrue(body.contains("**提交信息**"));
        assertTrue(body.contains("- 提交人: miguchn"));
        assertTrue(body.contains("- 源分支: feature/fix → 目标分支: main"));
        assertTrue(body.contains("- Commit 信息: Fix login exception handling"));
        assertTrue(body.contains("- 审查时间: "));
        assertTrue(body.contains("- 合并请求: [#4 Fix login exception handling](https://github.com/acme/demo/pull/4)"));

        assertTrue(body.contains("**审查结果 (1)**"));
        assertTrue(body.contains("1. 🚨 严重 · 新增 —— 未处理异常可能中断发送"));
        assertTrue(body.contains("   位置: src/wecom.py L12-18"));
        assertTrue(body.contains("   发送过程中可能抛出异常，但完全没有 catch。"));
        assertTrue(body.contains("   建议: 在发送逻辑中增加 try-catch，失败时重试。"));

        assertTrue(body.contains("**范围统计**"));
        assertTrue(body.contains("- 纳入 3 · 排除 2 · 扩展 1"));
        assertTrue(body.contains("- 新增 2 · 存量 1"));

        assertTrue(body.contains("**总结**"));
        assertTrue(body.contains("整体风险可控……"));

        assertTrue(body.contains("[查看合并请求](https://github.com/acme/demo/pull/4)"));
        assertTrue(body.contains("[查看审查详情](https://acr.example.com/review/record-detail/index/42)"));
        assertFalse(body.contains("```"));
        assertFalse(body.contains("|---|"));
    }

    @Test
    void omitsDescriptionLineWhenEmpty()
    {
        ReviewTopIssue issue = new ReviewTopIssue();
        issue.setSeverity("HIGH");
        issue.setOrigin("NEW");
        issue.setTitle("仅有标题");
        issue.setDescription(null);
        issue.setSuggestion("补上校验");
        issue.setFilePath("UserController.java");
        issue.setStartLine(42);

        ReviewSummaryContent content = ReviewSummaryContent.builder()
            .conclusionLabel("建议修改")
            .totalScore(70)
            .topIssues(List.of(issue))
            .build();

        String body = ReviewNotifyMessageRenderer.renderSuccess(content);

        assertTrue(body.contains("1. ⚠️ 高 · 新增 —— 仅有标题"));
        assertTrue(body.contains("   位置: UserController.java L42\n   建议: 补上校验"));
        assertFalse(body.contains("仅有标题\n   \n"));
    }

    @Test
    void emptyIssuesShowsNoCriticalMessage()
    {
        ReviewSummaryContent content = ReviewSummaryContent.builder()
            .conclusionLabel("通过")
            .totalScore(95)
            .topIssues(List.of())
            .build();

        String body = ReviewNotifyMessageRenderer.renderSuccess(content);

        assertTrue(body.contains("**审查结果 (0)**"));
        assertTrue(body.contains("本次审查未发现重点问题"));
    }

    @Test
    void rendersFailedWithSubmissionAndFailureType()
    {
        ReviewSummaryContent content = ReviewSummaryContent.builder()
            .taskId(99L)
            .taskStatus(ReviewPipelineConstants.TASK_FAILED)
            .prNumber(8)
            .prTitle("重构用户登录校验")
            .prAuthor("zhangsan")
            .sourceBranch("dev")
            .targetBranch("main")
            .repositoryOwner("acme")
            .repositoryName("demo")
            .failureType(ReviewPipelineConstants.FAILURE_TIMEOUT)
            .failureTypeLabel("引擎超时")
            .detailUrl("https://acr.example.com/review/record-detail/index/99")
            .build();

        String body = ReviewNotifyMessageRenderer.renderFailed(content);

        assertTrue(body.startsWith("### ❌ AI Code Review · 执行失败"));
        assertTrue(body.contains("**提交信息**"));
        assertTrue(body.contains("- 提交人: zhangsan"));
        assertTrue(body.contains("- 源分支: dev → 目标分支: main"));
        assertTrue(body.contains("失败类型: 引擎超时"));
        assertTrue(body.contains("[查看审查详情](https://acr.example.com/review/record-detail/index/99)"));
        assertFalse(body.contains("**审查结果"));
        assertFalse(body.contains("**总结**"));
        assertFalse(body.contains("**范围统计**"));
    }

    @Test
    void truncatesDescriptionAndSuggestionAt120Chars()
    {
        ReviewTopIssue issue = new ReviewTopIssue();
        issue.setSeverity("HIGH");
        issue.setTitle("超长说明");
        issue.setDescription("d".repeat(ReviewDeliveryConstants.IM_MAX_DESCRIPTION_CHARS + 20));
        issue.setSuggestion("s".repeat(ReviewDeliveryConstants.IM_MAX_SUGGESTION_CHARS + 20));

        ReviewSummaryContent content = ReviewSummaryContent.builder()
            .conclusionLabel("建议修改")
            .totalScore(60)
            .topIssues(List.of(issue))
            .build();

        String body = ReviewNotifyMessageRenderer.renderSuccess(content);

        assertTrue(body.contains("…"));
        String descLine = body.lines()
            .filter(line -> line.trim().startsWith("d"))
            .findFirst()
            .orElse("");
        assertTrue(descLine.trim().endsWith("…"));
        assertTrue(descLine.trim().length() <= ReviewDeliveryConstants.IM_MAX_DESCRIPTION_CHARS + 1);
        String suggestionLine = body.lines()
            .filter(line -> line.contains("建议:"))
            .findFirst()
            .orElse("");
        assertTrue(suggestionLine.contains("…"));
    }

    @Test
    void rendersRecheckingSectionAfterReviewResults()
    {
        ReviewTopIssue issue = new ReviewTopIssue();
        issue.setSeverity("MEDIUM");
        issue.setOrigin("NEW");
        issue.setTitle("质量问题");

        ReviewSummaryContent content = ReviewSummaryContent.builder()
            .conclusionLabel("通过")
            .totalScore(88)
            .topIssues(List.of(issue))
            .recheckingTitles(List.of("sql-injection", "cmd-injection"))
            .build();

        String body = ReviewNotifyMessageRenderer.renderSuccess(content);
        int resultIdx = body.indexOf("**审查结果");
        int recheckIdx = body.indexOf("疑似已修复（2）：sql-injection / cmd-injection");
        int scopeIdx = body.indexOf("**范围统计**");
        assertTrue(resultIdx >= 0);
        assertTrue(recheckIdx > resultIdx);
        assertTrue(scopeIdx > recheckIdx);
        assertTrue(body.contains("请前往问题台账复核"));
    }
}
