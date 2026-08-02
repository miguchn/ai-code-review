package com.acr.review.scope;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import com.acr.review.domain.ReviewPipelineConstants;

class ReviewScopePromptAssemblerTest
{
    private final ReviewScopePromptAssembler assembler = new ReviewScopePromptAssembler();

    @Test
    void planFetchesOnlyFullContentFilesWithinLimit()
    {
        ReviewScopeDecision decision = decision(List.of(
            new ReviewScopeDecision.ExpandedFile("a/New.java", "NEW_FILE", false),
            new ReviewScopeDecision.ExpandedFile("b/App.yml", "CONFIG", true),
            new ReviewScopeDecision.ExpandedFile("c/pom.xml", "DEPENDENCY", true)));

        List<ReviewScopeDecision.ExpandedFile> plan = assembler.planFetches(decision);

        assertEquals(2, plan.size());
        assertEquals("b/App.yml", plan.get(0).path());
        assertEquals("c/pom.xml", plan.get(1).path());
    }

    @Test
    void assembleMarksInDiffFullDegradedAndLimitSkipped()
    {
        List<ReviewScopeDecision.ExpandedFile> expanded = List.of(
            new ReviewScopeDecision.ExpandedFile("a/New.java", "NEW_FILE", false),
            new ReviewScopeDecision.ExpandedFile("b/App.yml", "CONFIG", true),
            new ReviewScopeDecision.ExpandedFile("c/pom.xml", "DEPENDENCY", true),
            new ReviewScopeDecision.ExpandedFile("d/Other.yml", "CONFIG", true));
        ReviewScopeDecision decision = decision(expanded);

        ReviewScopePromptAssembler.ReviewScopeAssembly assembly = assembler.assemble(
            decision,
            Map.of("b/App.yml", "server:\n  port: 8080\n", "c/pom.xml", "<project/>"),
            Map.of("c/pom.xml", "IO"));
        // d/Other.yml 未拉取（超出计划）→ FETCH_LIMIT_SKIPPED；c 的失败优先于内容（执行层不会同时给）

        List<ReviewScopePromptAssembler.ExpandedFileDisposition> dispositions = assembly.dispositions();
        assertEquals(ReviewScopePromptAssembler.STATUS_IN_DIFF, dispositions.get(0).status());
        assertEquals(ReviewScopePromptAssembler.STATUS_FULL, dispositions.get(1).status());
        assertEquals(ReviewScopePromptAssembler.STATUS_DEGRADED, dispositions.get(2).status());
        assertEquals("IO", dispositions.get(2).reason());
        assertEquals(ReviewScopePromptAssembler.STATUS_FETCH_LIMIT_SKIPPED, dispositions.get(3).status());

        assertTrue(assembly.diffForPrompt().contains("scoped-diff-body"));
        assertTrue(assembly.diffForPrompt().contains("port: 8080"));
        assertTrue(assembly.diffForPrompt().contains("规则：CONFIG"));
        assertFalse(assembly.diffForPrompt().contains("<project/>"), "失败文件不得追加全文");
        assertTrue(assembly.hasFullContent());
    }

    @Test
    void assembleSkipsWholeFileWhenBudgetExceeded()
    {
        String bigContent = "x".repeat(ReviewPipelineConstants.MAX_DIFF_CHARS);
        ReviewScopeDecision decision = decision(List.of(
            new ReviewScopeDecision.ExpandedFile("b/Big.sql", "DB_SCRIPT", true)));

        ReviewScopePromptAssembler.ReviewScopeAssembly assembly = assembler.assemble(
            decision, Map.of("b/Big.sql", bigContent), Map.of());

        assertEquals(ReviewScopePromptAssembler.STATUS_BUDGET_SKIPPED, assembly.dispositions().get(0).status());
        assertFalse(assembly.diffForPrompt().contains(bigContent), "超预算文件必须整文件跳过，不半切");
        assertTrue(assembly.diffForPrompt().length() <= ReviewPipelineConstants.MAX_DIFF_CHARS + 4096);
        assertFalse(assembly.hasFullContent());
    }

    @Test
    void assembleAppendsFullContentWithinRemainingBudget()
    {
        ReviewScopeDecision decision = decision(List.of(
            new ReviewScopeDecision.ExpandedFile("sql/01_init.sql", "DB_SCRIPT", true)));
        String content = "CREATE TABLE demo (id bigint);";

        ReviewScopePromptAssembler.ReviewScopeAssembly assembly = assembler.assemble(
            decision, Map.of("sql/01_init.sql", content), Map.of());

        String expected = "scoped-diff-body"
            + "\n\n===== 高影响扩展文件完整内容（规则：DB_SCRIPT，变更行见上方 Diff）：sql/01_init.sql =====\n"
            + content;
        assertEquals(expected, assembly.diffForPrompt());
        assertEquals(content.length(), assembly.dispositions().get(0).chars());
    }

    private ReviewScopeDecision decision(List<ReviewScopeDecision.ExpandedFile> expanded)
    {
        return new ReviewScopeDecision(
            "scoped-diff-body",
            List.of("src/Main.java"),
            List.of(),
            expanded,
            List.of(),
            List.of(),
            false,
            List.of());
    }
}
