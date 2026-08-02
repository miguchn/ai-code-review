package com.acr.review.scope;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.List;
import org.junit.jupiter.api.Test;

/** OCR --exclude 规则集合并：平台默认 + 测试文件（按开关）+ 项目追加，去重保序。 */
class ReviewScopeRulesTest
{
    @Test
    void defaultsMergePlatformAndTestGlobs()
    {
        List<String> merged = ReviewScopeRules.mergedExcludeGlobs(ReviewScopeConfig.defaults());
        assertTrue(merged.contains("**/package-lock.json"), "平台默认排除");
        assertTrue(merged.contains("**/src/test/**"), "默认不审测试文件");
        assertEquals(ReviewScopeRules.DEFAULT_EXCLUDE_GLOBS.size() + ReviewScopeRules.TEST_FILE_GLOBS.size(),
            merged.size());
    }

    @Test
    void includeTestsDropsTestGlobs()
    {
        List<String> merged = ReviewScopeRules.mergedExcludeGlobs(
            new ReviewScopeConfig(List.of(), true, false, true));
        assertTrue(merged.contains("**/package-lock.json"));
        assertFalse(merged.contains("**/src/test/**"));
        assertFalse(merged.stream().anyMatch(glob -> glob.contains("Test.java")));
    }

    @Test
    void projectPatternsAppendedAndDeduplicated()
    {
        List<String> merged = ReviewScopeRules.mergedExcludeGlobs(
            new ReviewScopeConfig(List.of("docs/**", "**/package-lock.json", "*.generated.java"), false, false, true));
        assertTrue(merged.contains("docs/**"));
        assertTrue(merged.contains("*.generated.java"));
        // 与平台默认重复的项目规则不重复出现
        assertEquals(1, merged.stream().filter("**/package-lock.json"::equals).count());
        // 平台默认在前，项目追加在后（保序）
        assertTrue(merged.indexOf("docs/**") > merged.indexOf("**/package-lock.json"));
    }

    @Test
    void nullConfigFallsBackToDefaults()
    {
        List<String> merged = ReviewScopeRules.mergedExcludeGlobs(null);
        assertTrue(merged.contains("**/package-lock.json"));
        assertTrue(merged.contains("**/src/test/**"));
    }
}
