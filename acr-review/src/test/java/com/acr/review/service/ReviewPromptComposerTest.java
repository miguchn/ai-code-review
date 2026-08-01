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
}
