package com.acr.review.scope;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.List;
import org.junit.jupiter.api.Test;
import com.acr.review.domain.ReviewPipelineConstants;

class ReviewScopeDecisionServiceTest
{
    private final UnifiedDiffParser parser = new UnifiedDiffParser();
    private final ReviewScopeDecisionService service = new ReviewScopeDecisionService();

    @Test
    void classifiesAllTypicalScenarios()
    {
        String diff = """
            diff --git a/src/main/java/com/demo/UserService.java b/src/main/java/com/demo/UserService.java
            index 1234567..89abcde 100644
            --- a/src/main/java/com/demo/UserService.java
            +++ b/src/main/java/com/demo/UserService.java
            @@ -20,3 +20,3 @@ private String format
             ctx
            -old
            +new
             ctx
            diff --git a/src/main/java/com/demo/NewController.java b/src/main/java/com/demo/NewController.java
            new file mode 100644
            index 0000000..1234567
            --- /dev/null
            +++ b/src/main/java/com/demo/NewController.java
            @@ -0,0 +1,2 @@
            +package com.demo;
            +public class NewController {}
            diff --git a/src/main/java/com/demo/OldDao.java b/src/main/java/com/demo/OldDao.java
            deleted file mode 100644
            index 1234567..0000000
            --- a/src/main/java/com/demo/OldDao.java
            +++ /dev/null
            @@ -1,1 +0,0 @@
            -public class OldDao {}
            diff --git a/package-lock.json b/package-lock.json
            index 1234567..89abcde 100644
            --- a/package-lock.json
            +++ b/package-lock.json
            @@ -1,1 +1,1 @@
            -old
            +new
            diff --git a/package.json b/package.json
            index 1234567..89abcde 100644
            --- a/package.json
            +++ b/package.json
            @@ -1,1 +1,1 @@
            -old
            +new
            diff --git a/src/main/resources/application.yml b/src/main/resources/application.yml
            index 1234567..89abcde 100644
            --- a/src/main/resources/application.yml
            +++ b/src/main/resources/application.yml
            @@ -1,1 +1,1 @@
            -old
            +new
            diff --git a/sql/22_user_table.sql b/sql/22_user_table.sql
            index 1234567..89abcde 100644
            --- a/sql/22_user_table.sql
            +++ b/sql/22_user_table.sql
            @@ -1,1 +1,1 @@
            -old
            +new
            diff --git a/src/main/java/com/acr/review/security/TokenService.java b/src/main/java/com/acr/review/security/TokenService.java
            index 1234567..89abcde 100644
            --- a/src/main/java/com/acr/review/security/TokenService.java
            +++ b/src/main/java/com/acr/review/security/TokenService.java
            @@ -1,1 +1,1 @@
            -old
            +new
            diff --git a/src/main/java/com/demo/ApiClient.java b/src/main/java/com/demo/ApiClient.java
            index 1234567..89abcde 100644
            --- a/src/main/java/com/demo/ApiClient.java
            +++ b/src/main/java/com/demo/ApiClient.java
            @@ -5,2 +5,2 @@
             ctx
            -    private String fetch(Long id) {
            +    public String fetch(Long id) {
            diff --git a/src/test/java/com/demo/UserServiceTest.java b/src/test/java/com/demo/UserServiceTest.java
            index 1234567..89abcde 100644
            --- a/src/test/java/com/demo/UserServiceTest.java
            +++ b/src/test/java/com/demo/UserServiceTest.java
            @@ -1,1 +1,1 @@
            -old
            +new
            diff --git a/images/logo.png b/images/logo.png
            index 1234567..89abcde 100644
            Binary files a/images/logo.png and b/images/logo.png differ
            diff --git a/web/dist/app.js b/web/dist/app.js
            index 1234567..89abcde 100644
            --- a/web/dist/app.js
            +++ b/web/dist/app.js
            @@ -1,1 +1,1 @@
            -old
            +new
            """;

        ReviewScopeDecision decision = service.decide(parser.parse(diff), ReviewScopeConfig.defaults());

        // 排除：锁文件与构建产物走默认排除；测试文件默认排除
        assertEquals(3, decision.excludedFiles().size());
        assertExcluded(decision, "package-lock.json", ReviewScopeRules.EXCLUDE_DEFAULT);
        assertExcluded(decision, "web/dist/app.js", ReviewScopeRules.EXCLUDE_DEFAULT);
        assertExcluded(decision, "src/test/java/com/demo/UserServiceTest.java", ReviewScopeRules.EXCLUDE_TEST);

        // 记录类：删除、二进制
        assertEquals(2, decision.recordOnlyFiles().size());
        assertRecordOnly(decision, "src/main/java/com/demo/OldDao.java", ReviewScopeRules.RECORD_DELETED);
        assertRecordOnly(decision, "images/logo.png", ReviewScopeRules.RECORD_BINARY);

        // 高影响扩展按优先级排序：SECURITY > DEPENDENCY > DB_SCRIPT > CONFIG > SIGNATURE > NEW_FILE
        assertEquals(List.of(
            "src/main/java/com/acr/review/security/TokenService.java",
            "package.json",
            "sql/22_user_table.sql",
            "src/main/resources/application.yml",
            "src/main/java/com/demo/ApiClient.java",
            "src/main/java/com/demo/NewController.java"),
            decision.expandedFiles().stream().map(ReviewScopeDecision.ExpandedFile::path).toList());
        assertExpandedRule(decision, "src/main/java/com/acr/review/security/TokenService.java",
            ReviewScopeRules.RULE_SECURITY, true);
        assertExpandedRule(decision, "package.json", ReviewScopeRules.RULE_DEPENDENCY, true);
        assertExpandedRule(decision, "sql/22_user_table.sql", ReviewScopeRules.RULE_DB_SCRIPT, true);
        assertExpandedRule(decision, "src/main/resources/application.yml", ReviewScopeRules.RULE_CONFIG, true);
        assertExpandedRule(decision, "src/main/java/com/demo/ApiClient.java", ReviewScopeRules.RULE_SIGNATURE, true);
        // 新增文件内容已在 Diff 中，无需拉取全文
        assertExpandedRule(decision, "src/main/java/com/demo/NewController.java",
            ReviewScopeRules.RULE_NEW_FILE, false);

        // 普通修改：纳入但不扩展；scoped diff 只含纳入文件
        assertTrue(decision.includedFiles().contains("src/main/java/com/demo/UserService.java"));
        assertEquals(7, decision.effectiveFileCount());
        assertFalse(decision.truncated());
        assertTrue(decision.scopedDiff().contains("UserService.java"));
        assertFalse(decision.scopedDiff().contains("package-lock.json"));
        assertFalse(decision.scopedDiff().contains("OldDao.java"));
        assertFalse(decision.scopedDiff().contains("logo.png"));
    }

    @Test
    void signatureRulesCoverGoPythonAndJavaScript()
    {
        String diff = """
            diff --git a/server/main.go b/server/main.go
            index 1234567..89abcde 100644
            --- a/server/main.go
            +++ b/server/main.go
            @@ -1,1 +1,2 @@
             package main
            +func HandleRequest(ctx context.Context) error {
            diff --git a/app/service.py b/app/service.py
            index 1234567..89abcde 100644
            --- a/app/service.py
            +++ b/app/service.py
            @@ -1,1 +1,2 @@
             import os
            +def handle_request(req):
            diff --git a/web/api.ts b/web/api.ts
            index 1234567..89abcde 100644
            --- a/web/api.ts
            +++ b/web/api.ts
            @@ -1,1 +1,2 @@
             import axios from 'axios'
            +export function fetchUser(id: number) {
            """;

        ReviewScopeDecision decision = service.decide(parser.parse(diff), ReviewScopeConfig.defaults());

        assertEquals(3, decision.expandedFiles().size());
        assertTrue(decision.expandedFiles().stream()
            .allMatch(file -> ReviewScopeRules.RULE_SIGNATURE.equals(file.rule())));
        assertTrue(decision.expandedFiles().stream()
            .allMatch(ReviewScopeDecision.ExpandedFile::needsFullContent));
    }

    @Test
    void projectExcludeAndTestToggleAreApplied()
    {
        String diff = """
            diff --git a/docs/guide.md b/docs/guide.md
            index 1234567..89abcde 100644
            --- a/docs/guide.md
            +++ b/docs/guide.md
            @@ -1,1 +1,1 @@
            -old
            +new
            diff --git a/src/test/java/com/demo/UserServiceTest.java b/src/test/java/com/demo/UserServiceTest.java
            index 1234567..89abcde 100644
            --- a/src/test/java/com/demo/UserServiceTest.java
            +++ b/src/test/java/com/demo/UserServiceTest.java
            @@ -1,1 +1,1 @@
            -old
            +new
            """;

        ReviewScopeConfig config = new ReviewScopeConfig(List.of("**/docs/**"), true, false, true);
        ReviewScopeDecision decision = service.decide(parser.parse(diff), config);

        assertExcluded(decision, "docs/guide.md", ReviewScopeRules.EXCLUDE_PROJECT);
        // includeTests=true 时测试文件纳入审查
        assertTrue(decision.includedFiles().contains("src/test/java/com/demo/UserServiceTest.java"));
        assertEquals(1, decision.effectiveFileCount());
    }

    @Test
    void expandDisabledTreatsHighImpactFilesAsNormal()
    {
        String diff = """
            diff --git a/src/main/resources/application.yml b/src/main/resources/application.yml
            index 1234567..89abcde 100644
            --- a/src/main/resources/application.yml
            +++ b/src/main/resources/application.yml
            @@ -1,1 +1,1 @@
            -old
            +new
            diff --git a/src/main/java/com/demo/NewController.java b/src/main/java/com/demo/NewController.java
            new file mode 100644
            index 0000000..1234567
            --- /dev/null
            +++ b/src/main/java/com/demo/NewController.java
            @@ -0,0 +1,1 @@
            +public class NewController {}
            """;

        ReviewScopeConfig config = new ReviewScopeConfig(List.of(), false, false, false);
        ReviewScopeDecision decision = service.decide(parser.parse(diff), config);

        assertTrue(decision.expandedFiles().isEmpty());
        assertEquals(2, decision.effectiveFileCount());
        assertTrue(decision.includedFiles().contains("src/main/resources/application.yml"));
        assertTrue(decision.includedFiles().contains("src/main/java/com/demo/NewController.java"));
    }

    @Test
    void budgetDropsNormalFilesAtFileBoundaryAndKeepsExpanded()
    {
        String padding = "x".repeat(ReviewPipelineConstants.MAX_DIFF_CHARS);
        String diff = """
            diff --git a/src/main/resources/application.yml b/src/main/resources/application.yml
            index 1234567..89abcde 100644
            --- a/src/main/resources/application.yml
            +++ b/src/main/resources/application.yml
            @@ -1,1 +1,1 @@
            -old
            +new
            diff --git a/src/main/java/com/demo/Giant.java b/src/main/java/com/demo/Giant.java
            index 1234567..89abcde 100644
            --- a/src/main/java/com/demo/Giant.java
            +++ b/src/main/java/com/demo/Giant.java
            @@ -1,1 +1,1 @@
            -old
            +PADDING_LINE
            """.replace("PADDING_LINE", padding);

        ReviewScopeDecision decision = service.decide(parser.parse(diff), ReviewScopeConfig.defaults());

        // 高影响文件优先占用预算；普通文件整文件丢弃且不切断其内容
        assertTrue(decision.truncated());
        assertEquals(List.of("src/main/java/com/demo/Giant.java"), decision.droppedFiles());
        assertEquals(List.of("src/main/resources/application.yml"), decision.includedFiles());
        assertEquals(1, decision.expandedFiles().size());
        assertFalse(decision.scopedDiff().contains("Giant.java"));
    }

    @Test
    void allExcludedYieldsEmptyEffectiveScope()
    {
        String diff = """
            diff --git a/package-lock.json b/package-lock.json
            index 1234567..89abcde 100644
            --- a/package-lock.json
            +++ b/package-lock.json
            @@ -1,1 +1,1 @@
            -old
            +new
            diff --git a/src/main/java/com/demo/OldDao.java b/src/main/java/com/demo/OldDao.java
            deleted file mode 100644
            index 1234567..0000000
            --- a/src/main/java/com/demo/OldDao.java
            +++ /dev/null
            @@ -1,1 +0,0 @@
            -public class OldDao {}
            """;

        ReviewScopeDecision decision = service.decide(parser.parse(diff), ReviewScopeConfig.defaults());

        assertEquals(0, decision.effectiveFileCount());
        assertTrue(decision.scopedDiff().isEmpty());
    }

    @Test
    void snapshotMapReportsAllBuckets()
    {
        String diff = """
            diff --git a/src/main/java/com/demo/UserService.java b/src/main/java/com/demo/UserService.java
            index 1234567..89abcde 100644
            --- a/src/main/java/com/demo/UserService.java
            +++ b/src/main/java/com/demo/UserService.java
            @@ -1,1 +1,1 @@
            -old
            +new
            diff --git a/package-lock.json b/package-lock.json
            index 1234567..89abcde 100644
            --- a/package-lock.json
            +++ b/package-lock.json
            @@ -1,1 +1,1 @@
            -old
            +new
            """;

        ReviewScopeDecision decision = service.decide(parser.parse(diff), ReviewScopeConfig.defaults());
        java.util.Map<String, Object> snapshot = decision.toSnapshotMap();

        assertTrue(snapshot.containsKey("includedFiles"));
        assertTrue(snapshot.containsKey("excludedFiles"));
        assertTrue(snapshot.containsKey("expandedFiles"));
        assertTrue(snapshot.containsKey("recordOnlyFiles"));
        assertTrue(snapshot.containsKey("droppedFiles"));
        assertTrue(snapshot.containsKey("truncated"));
        assertTrue(snapshot.containsKey("scopedDiffChars"));
        assertEquals(decision.scopedDiff().length(), snapshot.get("scopedDiffChars"));
    }

    private void assertExcluded(ReviewScopeDecision decision, String path, String reason)
    {
        assertTrue(decision.excludedFiles().stream()
            .anyMatch(file -> file.path().equals(path) && file.reason().equals(reason)),
            () -> "应排除 " + path + "（" + reason + "），实际：" + decision.excludedFiles());
    }

    private void assertRecordOnly(ReviewScopeDecision decision, String path, String reason)
    {
        assertTrue(decision.recordOnlyFiles().stream()
            .anyMatch(file -> file.path().equals(path) && file.reason().equals(reason)),
            () -> "应为记录类 " + path + "（" + reason + "），实际：" + decision.recordOnlyFiles());
    }

    private void assertExpandedRule(ReviewScopeDecision decision, String path, String rule, boolean needsFullContent)
    {
        assertTrue(decision.expandedFiles().stream()
            .anyMatch(file -> file.path().equals(path) && file.rule().equals(rule)
                && file.needsFullContent() == needsFullContent),
            () -> "应命中扩展 " + path + "（" + rule + "），实际：" + decision.expandedFiles());
    }
}
