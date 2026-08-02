package com.acr.review.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class ReviewPromptComposerTest
{
    private final ReviewPromptComposer composer = new ReviewPromptComposer();

    @Test
    void stripsLegacyJsonOutputBlock()
    {
        String body = """
            你是资深 Java 代码审查助手。重点关注空指针。

            【PR 信息】
            标题：{{pr_title}}

            请用中文输出可执行建议。若可能，按 JSON 返回：
            {"summary":"一句话总结","conclusion":"PASS|WARN|BLOCK","comments":[{"severity":"critical|warning|info","file":"路径","line":1,"message":"问题说明","suggestion":"修复建议"}]}
            不要编造未在 Diff 中出现的文件或行号。
            """;
        String stripped = composer.stripConflictingOutputInstructions(body);
        assertFalse(stripped.contains("comments"));
        assertFalse(stripped.contains("PASS|WARN|BLOCK"));
        assertTrue(stripped.contains("空指针"));
    }

    @Test
    void appendsPlatformProtocol()
    {
        String rendered = "重点关注 Hooks。\n标题：demo";
        String finalPrompt = composer.composeFromSnapshotTemplate("重点关注 Hooks。", rendered);
        assertTrue(finalPrompt.contains("重点关注 Hooks"));
        assertTrue(finalPrompt.contains("平台公共评分标准与输出协议"));
        assertTrue(finalPrompt.contains("COMMIT_QUALITY"));
        assertTrue(finalPrompt.contains("功能正确性与健壮性"));
        assertTrue(finalPrompt.contains("主要评估注入、越权、敏感信息泄露及其他安全风险"));
        assertTrue(finalPrompt.contains("protocolVersion"));
    }

    @Test
    void composeWithScopeInsertsInstructionBeforeProtocol()
    {
        String finalPrompt = composer.composeWithScope("审查正文", true, true);

        int scopeIndex = finalPrompt.indexOf("【审查范围说明");
        int protocolIndex = finalPrompt.indexOf("平台公共评分标准与输出协议");
        assertTrue(scopeIndex > 0, "应含范围指令块");
        assertTrue(protocolIndex > scopeIndex, "范围指令块应位于输出协议之前");
        assertTrue(finalPrompt.contains("已经过平台范围筛选"));
        assertTrue(finalPrompt.contains("高影响扩展文件完整内容"));
    }

    @Test
    void composeWithScopeOmitsFilteredNoteWhenDegraded()
    {
        // 决策失败降级全量 Diff 时：不得出现"已筛选"表述，但"只报变更引入问题"约束保留
        String finalPrompt = composer.composeWithScope("审查正文", false, false);

        assertFalse(finalPrompt.contains("已经过平台范围筛选"));
        assertTrue(finalPrompt.contains("只报告本次变更引入的问题"));
        assertFalse(finalPrompt.contains("高影响扩展文件完整内容"));
    }
}
