package com.acr.review.scope;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class UnifiedDiffParserTest
{
    private final UnifiedDiffParser parser = new UnifiedDiffParser();

    @Test
    void parsesStandardModification()
    {
        String diff = """
            diff --git a/src/main/java/com/demo/Foo.java b/src/main/java/com/demo/Foo.java
            index 1234567..89abcde 100644
            --- a/src/main/java/com/demo/Foo.java
            +++ b/src/main/java/com/demo/Foo.java
            @@ -10,6 +10,7 @@ public class Foo
             context1
             context2
            -old line
            +new line
            +added line
             context3
             context4
             context5
            """;

        DiffParseResult result = parser.parse(diff);

        assertEquals(1, result.files().size());
        assertTrue(result.warnings().isEmpty());
        DiffFileChange file = result.files().get(0);
        assertEquals("src/main/java/com/demo/Foo.java", file.effectivePath());
        assertEquals(DiffChangeType.MODIFIED, file.changeType());
        assertFalse(file.binary());
        assertEquals(1, file.hunks().size());

        DiffHunk hunk = file.hunks().get(0);
        assertEquals("public class Foo", hunk.sectionHeading());
        // 右侧行号：context1=10, context2=11, new=12, added=13, context3..5=14..16
        assertEquals(1, hunk.addedRanges().size());
        assertEquals(new LineRange(12, 13), hunk.addedRanges().get(0));
        assertEquals(new LineRange(10, 16), hunk.rightRange());
        assertEquals(java.util.List.of("new line", "added line"), hunk.addedLines());
        assertEquals(java.util.List.of("old line"), hunk.deletedLines());
    }

    @Test
    void parsesNewFile()
    {
        String diff = """
            diff --git a/src/main/java/com/demo/NewService.java b/src/main/java/com/demo/NewService.java
            new file mode 100644
            index 0000000..1234567
            --- /dev/null
            +++ b/src/main/java/com/demo/NewService.java
            @@ -0,0 +1,4 @@
            +package com.demo;
            +
            +public class NewService {
            +}
            """;

        DiffParseResult result = parser.parse(diff);

        DiffFileChange file = result.files().get(0);
        assertEquals(DiffChangeType.ADDED, file.changeType());
        assertNull(file.oldPath());
        assertEquals("src/main/java/com/demo/NewService.java", file.newPath());
        // 新增文件全部行都在 hunk 中，内容天然完整
        assertEquals(java.util.List.of(new LineRange(1, 4)), file.addedLineRanges());
        assertEquals(new LineRange(1, 4), file.hunks().get(0).rightRange());
        assertTrue(result.warnings().isEmpty());
    }

    @Test
    void parsesDeletedFile()
    {
        String diff = """
            diff --git a/src/main/java/com/demo/Old.java b/src/main/java/com/demo/Old.java
            deleted file mode 100644
            index 1234567..0000000
            --- a/src/main/java/com/demo/Old.java
            +++ /dev/null
            @@ -1,3 +0,0 @@
            -package com.demo;
            -
            -public class Old {}
            """;

        DiffParseResult result = parser.parse(diff);

        DiffFileChange file = result.files().get(0);
        assertEquals(DiffChangeType.DELETED, file.changeType());
        assertEquals("src/main/java/com/demo/Old.java", file.effectivePath());
        assertNull(file.newPath());
        assertTrue(file.addedLineRanges().isEmpty());
        assertNull(file.hunks().get(0).rightRange());
        assertTrue(result.warnings().isEmpty());
    }

    @Test
    void parsesRenameWithoutHunks()
    {
        String diff = """
            diff --git a/src/OldName.java b/src/NewName.java
            similarity index 100%
            rename from src/OldName.java
            rename to src/NewName.java
            """;

        DiffParseResult result = parser.parse(diff);

        DiffFileChange file = result.files().get(0);
        assertEquals(DiffChangeType.RENAMED, file.changeType());
        assertEquals("src/OldName.java", file.oldPath());
        assertEquals("src/NewName.java", file.newPath());
        assertFalse(file.hasHunks());
    }

    @Test
    void parsesRenameWithHunks()
    {
        String diff = """
            diff --git a/src/OldName.java b/src/NewName.java
            similarity index 90%
            rename from src/OldName.java
            rename to src/NewName.java
            index 1234567..89abcde 100644
            --- a/src/OldName.java
            +++ b/src/NewName.java
            @@ -1,3 +1,3 @@
             context
            -old
            +new
             context
            """;

        DiffParseResult result = parser.parse(diff);

        DiffFileChange file = result.files().get(0);
        assertEquals(DiffChangeType.RENAMED, file.changeType());
        assertTrue(file.hasHunks());
        assertEquals(java.util.List.of(new LineRange(2, 2)), file.addedLineRanges());
        assertTrue(result.warnings().isEmpty());
    }

    @Test
    void flagsBinaryGitlinkAndModeOnly()
    {
        String diff = """
            diff --git a/images/logo.png b/images/logo.png
            index 1234567..89abcde 100644
            Binary files a/images/logo.png and b/images/logo.png differ
            diff --git a/libs/sub b/libs/sub
            index 1234567..89abcde 160000
            --- a/libs/sub
            +++ b/libs/sub
            @@ -1 +1 @@
            -Subproject commit aaaa1111
            +Subproject commit bbbb2222
            diff --git a/scripts/run.sh b/scripts/run.sh
            old mode 100644
            new mode 100755
            """;

        DiffParseResult result = parser.parse(diff);

        assertEquals(3, result.files().size());
        assertTrue(result.files().get(0).binary());
        assertTrue(result.files().get(1).gitlink());
        assertTrue(result.files().get(2).modeOnly());
    }

    @Test
    void toleratesNoNewlineMarker()
    {
        String diff = """
            diff --git a/a.txt b/a.txt
            index 1234567..89abcde 100644
            --- a/a.txt
            +++ b/a.txt
            @@ -1,2 +1,2 @@
            -old
            +new
            \\ No newline at end of file
             tail
            """;

        DiffParseResult result = parser.parse(diff);

        assertTrue(result.warnings().isEmpty());
        assertEquals(java.util.List.of(new LineRange(1, 1)), result.files().get(0).addedLineRanges());
    }

    @Test
    void warnsOnTruncatedTailButKeepsParsedContent()
    {
        // 服务端截断：hunk 声明 5/5 行，实际只给到 3 行
        String diff = """
            diff --git a/src/Foo.java b/src/Foo.java
            index 1234567..89abcde 100644
            --- a/src/Foo.java
            +++ b/src/Foo.java
            @@ -10,5 +10,5 @@
             context
            -old
            +new
            """;

        DiffParseResult result = parser.parse(diff);

        assertEquals(1, result.files().size());
        assertFalse(result.warnings().isEmpty());
        assertTrue(result.warnings().get(0).contains("截断"));
        assertEquals(java.util.List.of(new LineRange(11, 11)), result.files().get(0).addedLineRanges());
    }

    @Test
    void parsesMultipleFilesAndKeepsRawSectionsSeparate()
    {
        String diff = """
            diff --git a/src/A.java b/src/A.java
            index 1234567..89abcde 100644
            --- a/src/A.java
            +++ b/src/A.java
            @@ -1,2 +1,2 @@
            -oldA
            +newA
             ctx
            diff --git a/src/B.java b/src/B.java
            index 1234567..89abcde 100644
            --- a/src/B.java
            +++ b/src/B.java
            @@ -1,2 +1,2 @@
            -oldB
            +newB
             ctx
            """;

        DiffParseResult result = parser.parse(diff);

        assertEquals(2, result.files().size());
        assertEquals("src/A.java", result.files().get(0).effectivePath());
        assertEquals("src/B.java", result.files().get(1).effectivePath());
        assertTrue(result.files().get(0).rawSection().contains("newA"));
        assertFalse(result.files().get(0).rawSection().contains("newB"));
        assertFalse(result.files().get(0).rawSection().endsWith("\n"));
    }

    @Test
    void unquotesPathsWithSpaces()
    {
        String diff = """
            diff --git "a/src/my file.txt" "b/src/my file.txt"
            index 1234567..89abcde 100644
            --- "a/src/my file.txt"
            +++ "b/src/my file.txt"
            @@ -1,1 +1,1 @@
            -old
            +new
            """;

        DiffParseResult result = parser.parse(diff);

        assertEquals("src/my file.txt", result.files().get(0).effectivePath());
    }

    @Test
    void emptyAndBlankDiffYieldEmptyResult()
    {
        assertTrue(parser.parse(null).isEmpty());
        assertTrue(parser.parse("").isEmpty());
        assertTrue(parser.parse("   \n").isEmpty());
    }
}
