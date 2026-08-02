package com.acr.review.scope;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * 归属判定：新增行命中、邻近宽限（≤3 行）、hunk 外上下文、不可判定与扩展全文文件。
 * 夹具统一经 UnifiedDiffParser 解析真实 diff 文本，与执行链数据来源一致。
 */
class IssueOriginClassifierTest
{
    private final UnifiedDiffParser parser = new UnifiedDiffParser();

    @Test
    void addedLineIsNew()
    {
        IssueOriginClassifier classifier = classifier(basicDiff(), Set.of());
        // Main.java：新增行右侧行号 12
        assertEquals(IssueOriginClassifier.Verdict.NEW, classifier.classify("src/main/java/Main.java", 12, 12));
    }

    @Test
    void contextLineWithinProximityIsNew()
    {
        IssueOriginClassifier classifier = classifier(basicDiff(), Set.of());
        // 右侧 14 行是上下文行，距新增行 12 距离为 2（≤3），同一 hunk 内 → NEW
        assertEquals(IssueOriginClassifier.Verdict.NEW, classifier.classify("src/main/java/Main.java", 14, null));
        // 右侧 10 行距新增行距离为 2 → NEW
        assertEquals(IssueOriginClassifier.Verdict.NEW, classifier.classify("src/main/java/Main.java", 10, 10));
    }

    @Test
    void contextLineBeyondProximityIsExisting()
    {
        IssueOriginClassifier classifier = classifier(basicDiff(), Set.of());
        // 右侧 50 行不在任何 hunk 内 → EXISTING
        assertEquals(IssueOriginClassifier.Verdict.EXISTING, classifier.classify("src/main/java/Main.java", 50, 50));
    }

    @Test
    void issueRangeIntersectingAddedLineIsNew()
    {
        IssueOriginClassifier classifier = classifier(basicDiff(), Set.of());
        // 区间 11-13 与新增行 12 相交 → NEW
        assertEquals(IssueOriginClassifier.Verdict.NEW, classifier.classify("src/main/java/Main.java", 11, 13));
    }

    @Test
    void pureDeletionHunkGrantsNoProximity()
    {
        IssueOriginClassifier classifier = classifier(deletionDiff(), Set.of());
        // 纯删除 hunk 没有新增行，邻近宽限不适用；右侧行 10 属 hunk 覆盖区间 → EXISTING
        assertEquals(IssueOriginClassifier.Verdict.EXISTING, classifier.classify("src/Legacy.java", 10, 10));
    }

    @Test
    void unknownFileIsUnverifiable()
    {
        IssueOriginClassifier classifier = classifier(basicDiff(), Set.of());
        assertEquals(IssueOriginClassifier.Verdict.UNVERIFIABLE,
            classifier.classify("src/main/java/Ghost.java", 1, 1));
    }

    @Test
    void missingPathOrLineIsUnverifiable()
    {
        IssueOriginClassifier classifier = classifier(basicDiff(), Set.of());
        assertEquals(IssueOriginClassifier.Verdict.UNVERIFIABLE, classifier.classify(null, 1, 1));
        assertEquals(IssueOriginClassifier.Verdict.UNVERIFIABLE, classifier.classify("  ", 1, 1));
        assertEquals(IssueOriginClassifier.Verdict.UNVERIFIABLE,
            classifier.classify("src/main/java/Main.java", null, null));
    }

    @Test
    void fullContentExpandedFileIsAlwaysNew()
    {
        IssueOriginClassifier classifier = classifier(basicDiff(), Set.of("src/main/resources/application.yml"));
        // 扩展全文已纳入 Prompt：整文件可上报，远超 hunk 的行号也是 NEW
        assertEquals(IssueOriginClassifier.Verdict.NEW,
            classifier.classify("src/main/resources/application.yml", 500, 500));
    }

    @Test
    void diffPrefixedPathIsTolerated()
    {
        IssueOriginClassifier classifier = classifier(basicDiff(), Set.of());
        // 模型偶尔回带 diff 的 b/ 前缀
        assertEquals(IssueOriginClassifier.Verdict.NEW, classifier.classify("b/src/main/java/Main.java", 12, 12));
    }

    private IssueOriginClassifier classifier(String diff, Set<String> fullContentPaths)
    {
        return new IssueOriginClassifier(parser.parse(diff), fullContentPaths);
    }

    /** Main.java：右侧 hunk [10,14]，新增行 [12,12]。 */
    private String basicDiff()
    {
        return "diff --git a/src/main/java/Main.java b/src/main/java/Main.java\n"
            + "index 3333333..4444444 100644\n"
            + "--- a/src/main/java/Main.java\n"
            + "+++ b/src/main/java/Main.java\n"
            + "@@ -10,4 +10,5 @@ public class Main {\n"
            + "     void call() {\n"
            + "         helper();\n"
            + "+        audit();\n"
            + "     }\n"
            + " }\n"
            + "diff --git a/src/main/resources/application.yml b/src/main/resources/application.yml\n"
            + "index 7777777..8888888 100644\n"
            + "--- a/src/main/resources/application.yml\n"
            + "+++ b/src/main/resources/application.yml\n"
            + "@@ -1 +1 @@\n"
            + "-timeout: 10\n"
            + "+timeout: 30\n";
    }

    /** Legacy.java：纯删除 hunk（右侧无新增行）。 */
    private String deletionDiff()
    {
        return "diff --git a/src/Legacy.java b/src/Legacy.java\n"
            + "index 1111111..2222222 100644\n"
            + "--- a/src/Legacy.java\n"
            + "+++ b/src/Legacy.java\n"
            + "@@ -9,3 +9,2 @@ class Legacy {\n"
            + "     void keep() {\n"
            + "-        obsolete();\n"
            + "     }\n";
    }
}
