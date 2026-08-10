package com.acr.review.delivery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import com.acr.review.domain.ReviewPipelineConstants;
import com.acr.review.domain.result.ReviewScopeStats;
import com.acr.review.domain.result.ReviewTopIssue;
import com.acr.review.service.ReviewScoringConstants;

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
            .projectName("Demo 项目")
            .businessSystemName("长寿官网系统")
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
        assertTrue(!body.contains("· —"));
        assertTrue(body.contains(
            "**长寿官网系统 · Demo 项目 · [PR #4](https://github.com/acme/demo/pull/4)**"));
        assertTrue(body.contains("Fix login exception handling"));

        assertTrue(body.contains("**提交信息**"));
        assertTrue(body.contains("   - 提交人: miguchn · "));
        assertTrue(body.contains("   - 分支: feature/fix → main"));
        assertTrue(body.contains("   - Commit: Fix login exception handling"));
        assertFalse(body.contains("- 合并请求:"));
        assertFalse(body.contains("源分支:"));

        assertTrue(body.contains("**审查问题 (1)**"));
        assertTrue(body.contains("**🚨 严重 (1)**"));
        assertTrue(body.contains("1. 未处理异常可能中断发送"));
        assertTrue(body.contains("   📍 src/wecom.py L12-18"));
        assertTrue(body.contains("   💡 在发送逻辑中增加 try-catch，失败时重试。"));
        assertFalse(body.contains("发送过程中可能抛出异常"));

        assertTrue(body.contains("**范围**：本次新增 2 个问题 · 存量 1 个"));
        assertTrue(body.contains("审查文件：纳入 3 个 · 扩展 1 个"));
        assertFalse(body.contains("**范围统计**"));

        assertTrue(body.contains("**总结**"));
        assertTrue(body.contains("\u3000整体风险可控……"));

        assertTrue(body.contains("[查看合并请求](https://github.com/acme/demo/pull/4)"));
        assertTrue(body.contains("[查看审查详情](https://acr.example.com/review/record-detail/index/42)"));
        assertFalse(body.contains("```"));
        assertFalse(body.contains("|---|"));
    }

    @Test
    void omitsScoreSegmentWhenTotalScoreNull()
    {
        ReviewSummaryContent content = ReviewSummaryContent.builder()
            .taskId(17L)
            .conclusionLabel("高风险")
            .totalScore(null)
            .prNumber(3)
            .projectName("Demo")
            .summaryText("审查结论：阻断；高风险 1 项，警告 0 项")
            .topIssues(List.of())
            .build();

        String body = ReviewNotifyMessageRenderer.renderSuccess(content);
        String header = body.lines().findFirst().orElse("");

        assertEquals("### 🚨 AI Code Review · 高风险", header);
        assertFalse(header.contains("/100"));
        assertFalse(header.contains("—"));
        assertTrue(body.contains("**审查问题 (0)**"));
    }

    @Test
    void rendersPushTaskAttributionAndSubmissionBlock()
    {
        ReviewSummaryContent content = ReviewSummaryContent.builder()
            .taskId(99L)
            .conclusion(ReviewPipelineConstants.CONCLUSION_WARN)
            .conclusionLabel("建议修改")
            .totalScore(70)
            .prNumber(0)
            .prTitle("fix: cache race")
            .prAuthor("alice")
            .projectName("Demo 项目")
            .businessSystemName("长寿官网系统")
            .sourceBranch("main")
            .targetBranch("main")
            .headShaShort("abcdef1")
            .commitMessage("fix: cache race")
            .reviewTime(new Date(1754361000000L))
            .summaryText("推送审查小结")
            .detailUrl("https://acr.example.com/review/record-detail/index/99")
            .eventSource(ReviewPipelineConstants.EVENT_SOURCE_PUSH)
            .build();

        String body = ReviewNotifyMessageRenderer.renderSuccess(content);

        assertTrue(body.contains("**长寿官网系统 · Demo 项目 · main @abcdef1**"));
        assertTrue(body.contains("   - 推送人: alice · "));
        assertTrue(body.contains("   - 分支: main"));
        assertTrue(body.contains("   - Commit: fix: cache race"));
        assertFalse(body.contains("PR #0"));
        assertFalse(body.contains("查看合并请求"));
        assertTrue(body.contains("[查看审查详情](https://acr.example.com/review/record-detail/index/99)"));
        assertTrue(body.contains(ReviewDeliveryConstants.PUSH_SCOPE_NOTE));
    }

    @Test
    void prSuccessSummaryOmitsPushScopeNote()
    {
        ReviewSummaryContent content = ReviewSummaryContent.builder()
            .conclusionLabel("通过")
            .totalScore(90)
            .prNumber(5)
            .eventSource(ReviewPipelineConstants.EVENT_SOURCE_PR)
            .summaryText("PR 审查小结")
            .build();

        String body = ReviewNotifyMessageRenderer.renderSuccess(content);
        assertFalse(body.contains(ReviewDeliveryConstants.PUSH_SCOPE_NOTE));
        assertFalse(body.contains("本结论仅覆盖本次推送的变更"));
    }

    @Test
    void attributionOmitsBusinessSystemWhenEmpty()
    {
        ReviewSummaryContent content = ReviewSummaryContent.builder()
            .conclusionLabel("通过")
            .totalScore(90)
            .prNumber(5)
            .repositoryOwner("miguchn")
            .repositoryName("webhook-test")
            .projectName("webhook-test 项目")
            .topIssues(List.of())
            .build();

        String body = ReviewNotifyMessageRenderer.renderSuccess(content);
        assertTrue(body.contains("**webhook-test 项目 · PR #5**"));
        assertFalse(body.contains("null"));
    }

    @Test
    void attributionFallsBackToOwnerRepoWhenProjectNameEmpty()
    {
        ReviewSummaryContent content = ReviewSummaryContent.builder()
            .conclusionLabel("高风险")
            .totalScore(19)
            .prNumber(5)
            .businessSystemName("长寿官网系统")
            .repositoryOwner("miguchn")
            .repositoryName("webhook-test")
            .topIssues(List.of())
            .build();

        String line = ReviewNotifyMessageRenderer.formatAttributionLine(content);
        assertEquals("**长寿官网系统 · miguchn/webhook-test · PR #5**", line);

        String body = ReviewNotifyMessageRenderer.renderSuccess(content);
        assertTrue(body.contains("**长寿官网系统 · miguchn/webhook-test · PR #5**"));
    }

    @Test
    void groupsSeverityAndCompressesMediumLow()
    {
        List<ReviewTopIssue> issues = new ArrayList<>();
        issues.add(issue("CRITICAL", "SQL 注入"));
        issues.add(issue("CRITICAL", "硬编码凭据"));
        issues.add(issue("HIGH", "合入主分支风险"));
        issues.add(issue("MEDIUM", "中等问题 A"));
        issues.add(issue("LOW", "低问题 B"));

        // displayTopIssues 仍截 Top N；此处直接验证分组在 top 列表内的行为
        List<ReviewTopIssue> top = issues.subList(0, Math.min(issues.size(), ReviewScoringConstants.MAX_TOP_ISSUES));
        ReviewSummaryContent content = ReviewSummaryContent.builder()
            .conclusionLabel("高风险")
            .totalScore(19)
            .topIssues(top)
            .build();

        String body = ReviewNotifyMessageRenderer.renderSuccess(content);
        assertTrue(body.contains("**🚨 严重 (2)**"));
        assertTrue(body.contains("1. SQL 注入"));
        assertTrue(body.contains("2. 硬编码凭据"));
        assertTrue(body.contains("**⚠️ 高 (1)**"));
        assertTrue(body.contains("3. 合入主分支风险"));
        assertFalse(body.contains("**其他问题"));
        assertFalse(body.contains("中等问题 A"));
    }

    @Test
    void compressesMediumAndLowIntoOtherIssuesLine()
    {
        ReviewTopIssue critical = issue("CRITICAL", "严重问题");
        ReviewTopIssue medium = issue("MEDIUM", "中等问题");
        ReviewTopIssue low = issue("LOW", "低问题");

        ReviewSummaryContent content = ReviewSummaryContent.builder()
            .conclusionLabel("建议修改")
            .totalScore(60)
            .topIssues(List.of(critical, medium, low))
            .build();

        String body = ReviewNotifyMessageRenderer.renderSuccess(content);
        assertTrue(body.contains("**🚨 严重 (1)**"));
        assertTrue(body.contains("1. 严重问题"));
        assertTrue(body.contains("**其他问题 (2)**：中 1 · 低 1，详见问题台账"));
        assertFalse(body.contains("中等问题"));
        assertFalse(body.contains("低问题"));
        assertFalse(body.contains("**⚡ 中"));
        assertFalse(body.contains("**💡 低"));
    }

    @Test
    void omitsSuggestionLineWhenEmpty()
    {
        ReviewTopIssue issue = new ReviewTopIssue();
        issue.setSeverity("HIGH");
        issue.setTitle("仅有标题");
        issue.setSuggestion(null);
        issue.setFilePath("UserController.java");
        issue.setStartLine(42);

        ReviewSummaryContent content = ReviewSummaryContent.builder()
            .conclusionLabel("建议修改")
            .totalScore(70)
            .topIssues(List.of(issue))
            .build();

        String body = ReviewNotifyMessageRenderer.renderSuccess(content);

        assertTrue(body.contains("**⚠️ 高 (1)**"));
        assertTrue(body.contains("1. 仅有标题"));
        assertTrue(body.contains("   📍 UserController.java L42"));
        assertFalse(body.contains("💡"));
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

        assertTrue(body.contains("**审查问题 (0)**"));
        assertTrue(body.contains("本次审查未发现重点问题"));
        assertFalse(body.contains("**审查结果"));
    }

    @Test
    void rendersFailedWithAttributionAndSubmission()
    {
        ReviewSummaryContent content = ReviewSummaryContent.builder()
            .taskId(99L)
            .taskStatus(ReviewPipelineConstants.TASK_FAILED)
            .prNumber(8)
            .prTitle("重构用户登录校验")
            .prAuthor("zhangsan")
            .businessSystemName("支付中台")
            .projectName("结算服务")
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
        assertTrue(body.contains("**支付中台 · 结算服务 · PR #8**"));
        assertTrue(body.contains("重构用户登录校验"));
        assertTrue(body.contains("**提交信息**"));
        assertTrue(body.contains("   - 提交人: zhangsan · "));
        assertTrue(body.contains("   - 分支: dev → main"));
        assertTrue(body.contains("失败类型: 引擎超时"));
        assertTrue(body.contains("[查看审查详情](https://acr.example.com/review/record-detail/index/99)"));
        assertFalse(body.contains("**总结**"));
        assertFalse(body.contains("**范围"));
    }

    @Test
    void truncatesSuggestionAt120Chars()
    {
        ReviewTopIssue issue = new ReviewTopIssue();
        issue.setSeverity("HIGH");
        issue.setTitle("超长建议");
        issue.setSuggestion("s".repeat(ReviewDeliveryConstants.IM_MAX_SUGGESTION_CHARS + 20));

        ReviewSummaryContent content = ReviewSummaryContent.builder()
            .conclusionLabel("建议修改")
            .totalScore(60)
            .topIssues(List.of(issue))
            .build();

        String body = ReviewNotifyMessageRenderer.renderSuccess(content);

        String suggestionLine = body.lines()
            .filter(line -> line.contains("💡"))
            .findFirst()
            .orElse("");
        assertTrue(suggestionLine.contains("…"));
        assertTrue(suggestionLine.trim().length() <= ReviewDeliveryConstants.IM_MAX_SUGGESTION_CHARS + 4);
    }

    @Test
    void truncatesSummaryAt200Chars()
    {
        ReviewSummaryContent content = ReviewSummaryContent.builder()
            .conclusionLabel("通过")
            .totalScore(95)
            .summaryText("x".repeat(ReviewDeliveryConstants.IM_MAX_SUMMARY_CHARS + 30))
            .topIssues(List.of())
            .build();

        String body = ReviewNotifyMessageRenderer.renderSuccess(content);
        assertTrue(body.contains("…"));
        String summarySection = body.substring(body.indexOf("**总结**"));
        assertTrue(summarySection.contains("x".repeat(ReviewDeliveryConstants.IM_MAX_SUMMARY_CHARS) + "…"));
    }

    @Test
    void rendersRecheckingSectionAfterIssuesBeforeScope()
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
        int otherIdx = body.indexOf("**其他问题");
        int recheckIdx = body.indexOf("疑似已修复（2）：sql-injection / cmd-injection");
        int scopeIdx = body.indexOf("**范围**：");
        assertTrue(otherIdx >= 0);
        assertTrue(recheckIdx > otherIdx);
        assertTrue(scopeIdx > recheckIdx);
        assertTrue(body.contains("请前往问题台账复核"));
    }

    @Test
    void resolveProjectSegmentPrefersProjectName()
    {
        ReviewSummaryContent withName = ReviewSummaryContent.builder()
            .projectName("企业内部名")
            .repositoryOwner("acme")
            .repositoryName("demo")
            .build();
        assertEquals("企业内部名", ReviewNotifyMessageRenderer.resolveProjectSegment(withName));

        ReviewSummaryContent fallback = ReviewSummaryContent.builder()
            .repositoryOwner("acme")
            .repositoryName("demo")
            .build();
        assertEquals("acme/demo", ReviewNotifyMessageRenderer.resolveProjectSegment(fallback));

        assertNull(ReviewNotifyMessageRenderer.resolveProjectSegment(
            ReviewSummaryContent.builder().build()));
    }

    @Test
    void issuesSectionTitleUsesAllIssuesCountIncludingEmpty()
    {
        ReviewSummaryContent empty = ReviewSummaryContent.builder()
            .conclusionLabel("通过")
            .totalScore(95)
            .topIssues(List.of())
            .build();
        String emptyBody = ReviewNotifyMessageRenderer.renderSuccess(empty);
        assertTrue(emptyBody.contains("**审查问题 (0)**"));
        assertTrue(emptyBody.contains("本次审查未发现重点问题"));

        List<ReviewTopIssue> issues = List.of(
            issue("CRITICAL", "严重 A"),
            issue("HIGH", "高 B"),
            issue("MEDIUM", "中 C"),
            issue("LOW", "低 D"));
        ReviewSummaryContent overflow = ReviewSummaryContent.builder()
            .conclusionLabel("建议修改")
            .totalScore(50)
            .topIssues(issues)
            .build();
        String overflowBody = ReviewNotifyMessageRenderer.renderSuccess(overflow);
        assertTrue(overflowBody.contains("**审查问题 (4)**"));
    }

    @Test
    void sectionContentIndentationPreservesIssueFlushLeft()
    {
        ReviewTopIssue issue = new ReviewTopIssue();
        issue.setSeverity("HIGH");
        issue.setTitle("顶格编号问题");
        issue.setSuggestion("保持缩进");
        issue.setFilePath("A.java");
        issue.setStartLine(1);

        ReviewSummaryContent content = ReviewSummaryContent.builder()
            .conclusionLabel("建议修改")
            .totalScore(70)
            .prAuthor("alice")
            .sourceBranch("dev")
            .targetBranch("main")
            .commitMessage("fix indent")
            .summaryText("总结正文缩进")
            .topIssues(List.of(issue))
            .build();

        String body = ReviewNotifyMessageRenderer.renderSuccess(content);
        assertTrue(body.lines().anyMatch(line -> line.equals("   - 提交人: alice · —")));
        assertTrue(body.lines().anyMatch(line -> line.equals("   - 分支: dev → main")));
        assertTrue(body.lines().anyMatch(line -> line.equals("   - Commit: fix indent")));
        assertTrue(body.lines().anyMatch(line -> line.equals("\u3000总结正文缩进")));
        assertTrue(body.lines().anyMatch(line -> line.equals("**⚠️ 高 (1)**")));
        assertTrue(body.lines().anyMatch(line -> line.equals("1. 顶格编号问题")));
        assertFalse(body.lines().anyMatch(line -> line.startsWith(" ") && line.contains("1. 顶格编号问题")));
        assertFalse(body.lines().anyMatch(line -> line.startsWith(" ") && line.contains("**⚠️ 高")));
        assertTrue(body.lines().anyMatch(line -> line.equals("   📍 A.java L1")));
        assertTrue(body.lines().anyMatch(line -> line.equals("   💡 保持缩进")));
    }

    @Test
    void formatPrSegmentUsesNumberOnlyAndTitleOnSeparateLine()
    {
        assertEquals(
            "[PR #5](https://github.com/miguchn/webhook-test/pull/5)",
            ReviewNotifyMessageRenderer.formatPrSegment(
                5, "feat: 新增用户数据访问层", "https://github.com/miguchn/webhook-test/pull/5"));
        assertEquals("PR #5",
            ReviewNotifyMessageRenderer.formatPrSegment(5, "feat: 新增用户数据访问层", null));
        assertEquals("[PR #5](https://example.com/pull/5)",
            ReviewNotifyMessageRenderer.formatPrSegment(5, null, "https://example.com/pull/5"));
        assertEquals("PR #5", ReviewNotifyMessageRenderer.formatPrSegment(5, null, null));
        assertNull(ReviewNotifyMessageRenderer.formatPrSegment(null, "title", "https://x"));

        ReviewSummaryContent content = ReviewSummaryContent.builder()
            .conclusionLabel("通过")
            .totalScore(90)
            .prNumber(5)
            .prTitle("feat: 新增用户数据访问层")
            .prUrl("https://github.com/miguchn/webhook-test/pull/5")
            .businessSystemName("官微后端")
            .repositoryOwner("miguchn")
            .repositoryName("webhook-test")
            .topIssues(List.of())
            .build();
        assertEquals(
            "**官微后端 · miguchn/webhook-test · [PR #5](https://github.com/miguchn/webhook-test/pull/5)**",
            ReviewNotifyMessageRenderer.formatAttributionLine(content));

        String body = ReviewNotifyMessageRenderer.renderSuccess(content);
        assertTrue(body.contains(
            "**官微后端 · miguchn/webhook-test · [PR #5](https://github.com/miguchn/webhook-test/pull/5)**\n"
                + "feat: 新增用户数据访问层\n"));
    }

    @Test
    void overflowWithCompressedLevelsShowsCombinedLedgerHint()
    {
        List<ReviewTopIssue> issues = List.of(
            issue("CRITICAL", "严重问题"),
            issue("MEDIUM", "中等问题"),
            issue("LOW", "低问题"),
            issue("LOW", "被截断的低问题"));

        ReviewSummaryContent content = ReviewSummaryContent.builder()
            .conclusionLabel("建议修改")
            .totalScore(55)
            .topIssues(issues)
            .build();

        String body = ReviewNotifyMessageRenderer.renderSuccess(content);
        // 展开 1 严重；其余 3 = 中 1 + 低 2（含被截断的低危）
        assertTrue(body.contains("共 4 个问题，其余 3 个（中 1 · 低 2）详见问题台账"));
        assertFalse(body.contains("**其他问题"));
        assertEquals(1, body.lines().filter(line -> line.contains("详见问题台账")).count());
        assertOverflowInvariant(1, 3, 4, Map.of("中", 1, "低", 2));
    }

    @Test
    void overflowWithoutCompressedLevelsShowsRemainderLedgerHint()
    {
        List<ReviewTopIssue> issues = List.of(
            issue("CRITICAL", "严重 A"),
            issue("CRITICAL", "严重 B"),
            issue("HIGH", "高 C"),
            issue("HIGH", "高 D"));

        ReviewSummaryContent content = ReviewSummaryContent.builder()
            .conclusionLabel("高风险")
            .totalScore(20)
            .topIssues(issues)
            .build();

        String body = ReviewNotifyMessageRenderer.renderSuccess(content);
        // 展示窗展开 2 严重 + 1 高；其余 1 = 被截断的高危
        assertTrue(body.contains("共 4 个问题，其余 1 个（高 1）详见问题台账"));
        assertFalse(body.contains("**其他问题"));
        assertEquals(1, body.lines().filter(line -> line.contains("详见问题台账")).count());
        assertOverflowInvariant(3, 1, 4, Map.of("高", 1));
    }

    @Test
    void overflowRemainderUsesFullIssueListNotDisplayWindow()
    {
        // 验收：6 条 = 1 高 + 2 中 + 3 低；展开 1 高 → 其余 5（中 2 · 低 3）
        List<ReviewTopIssue> issues = List.of(
            issue("HIGH", "高危问题"),
            issue("MEDIUM", "中等问题 A"),
            issue("MEDIUM", "中等问题 B"),
            issue("LOW", "低问题 A"),
            issue("LOW", "低问题 B"),
            issue("LOW", "低问题 C"));

        ReviewSummaryContent content = ReviewSummaryContent.builder()
            .conclusionLabel("建议修改")
            .totalScore(50)
            .topIssues(issues)
            .build();

        String body = ReviewNotifyMessageRenderer.renderSuccess(content);
        assertTrue(body.contains("**⚠️ 高 (1)**"));
        assertTrue(body.contains("1. 高危问题"));
        assertTrue(body.contains("共 6 个问题，其余 5 个（中 2 · 低 3）详见问题台账"));
        assertFalse(body.contains("**其他问题"));
        assertOverflowInvariant(1, 5, 6, Map.of("中", 2, "低", 3));
    }

    @Test
    void allIssuesVisibleWithCompressedLevelsShowsOtherIssuesLineOnly()
    {
        ReviewSummaryContent content = ReviewSummaryContent.builder()
            .conclusionLabel("建议修改")
            .totalScore(60)
            .topIssues(List.of(
                issue("CRITICAL", "严重问题"),
                issue("MEDIUM", "中等问题"),
                issue("LOW", "低问题")))
            .build();

        String body = ReviewNotifyMessageRenderer.renderSuccess(content);
        assertTrue(body.contains("**其他问题 (2)**：中 1 · 低 1，详见问题台账"));
        assertFalse(body.contains("共 "));
        assertFalse(body.contains("其余详见问题台账"));
        assertEquals(1, body.lines().filter(line -> line.contains("详见问题台账")).count());
    }

    /** 溢出不变量：expanded + remainder == total，且明细各项之和 == remainder。 */
    private static void assertOverflowInvariant(int expandedCount, int remainderCount, int total,
                                                Map<String, Integer> detailParts)
    {
        assertEquals(total, expandedCount + remainderCount);
        int detailSum = detailParts.values().stream().mapToInt(Integer::intValue).sum();
        assertEquals(remainderCount, detailSum);
    }

    private static ReviewTopIssue issue(String severity, String title)
    {
        ReviewTopIssue issue = new ReviewTopIssue();
        issue.setSeverity(severity);
        issue.setTitle(title);
        issue.setSuggestion("建议修复");
        return issue;
    }
}
