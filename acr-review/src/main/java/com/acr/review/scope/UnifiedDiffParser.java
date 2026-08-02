package com.acr.review.scope;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * 统一 Diff（git / GitHub Compare API 格式）解析器。
 * 纯函数式：只做文本解析，不抛业务异常；遇到残缺尾部（服务端截断等）graceful 停止并记 warnings。
 */
@Component
public class UnifiedDiffParser
{
    private static final Pattern DIFF_GIT = Pattern.compile("^diff --git (.+?) (.+)$");
    private static final Pattern HUNK_HEADER = Pattern.compile("^@@ -(\\d+)(?:,(\\d+))? \\+(\\d+)(?:,(\\d+))? @@ ?(.*)$");
    private static final Pattern INDEX_LINE = Pattern.compile("^index [0-9a-fA-F.]+(?: (\\d+))?$");

    public DiffParseResult parse(String diff)
    {
        List<DiffFileChange> files = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        if (diff == null || diff.isBlank())
        {
            return new DiffParseResult(files, warnings);
        }

        FileBuilder current = null;
        HunkBuilder hunk = null;
        String[] lines = diff.split("\n", -1);
        int limit = lines.length;
        if (limit > 0 && lines[limit - 1].isEmpty())
        {
            // 结尾换行产生的空元素不是真实行，丢弃避免被当作上下文行消费
            limit--;
        }
        for (int index = 0; index < limit; index++)
        {
            String raw = lines[index];
            String line = raw.endsWith("\r") ? raw.substring(0, raw.length() - 1) : raw;

            if (line.startsWith("diff --git "))
            {
                if (current != null)
                {
                    files.add(current.build(hunk, warnings));
                    hunk = null;
                }
                current = new FileBuilder();
                Matcher matcher = DIFF_GIT.matcher(line);
                if (matcher.matches())
                {
                    current.oldPath = unquote(matcher.group(1), "a/");
                    current.newPath = unquote(matcher.group(2), "b/");
                }
                current.raw.append(line).append('\n');
                continue;
            }

            if (current == null)
            {
                if (!line.isBlank())
                {
                    warnings.add("忽略 Diff 起始处无法识别的内容：" + abbreviate(line));
                }
                continue;
            }
            current.raw.append(line).append('\n');

            if (hunk != null && !line.isEmpty()
                && line.charAt(0) != ' ' && line.charAt(0) != '+' && line.charAt(0) != '-' && line.charAt(0) != '\\')
            {
                // hunk 提前结束（截断或新 hunk/头部），回到头部分派
                hunk.finish(warnings, current.label());
                current.hunks.add(hunk);
                hunk = null;
            }

            if (hunk != null)
            {
                hunk.consume(line);
                continue;
            }

            if (line.startsWith("@@ "))
            {
                Matcher matcher = HUNK_HEADER.matcher(line);
                if (!matcher.matches())
                {
                    warnings.add(current.label() + "：无法解析的 hunk 头，已跳过：" + abbreviate(line));
                    continue;
                }
                hunk = new HunkBuilder(
                    Integer.parseInt(matcher.group(1)),
                    matcher.group(2) == null ? 1 : Integer.parseInt(matcher.group(2)),
                    Integer.parseInt(matcher.group(3)),
                    matcher.group(4) == null ? 1 : Integer.parseInt(matcher.group(4)),
                    matcher.group(5) == null || matcher.group(5).isBlank() ? null : matcher.group(5));
                continue;
            }

            current.consumeHeader(line);
        }

        if (current != null)
        {
            files.add(current.build(hunk, warnings));
        }
        return new DiffParseResult(List.copyOf(files), List.copyOf(warnings));
    }

    /** 去掉 a//b/ 前缀与 C 风格引号；/dev/null 返回 null。 */
    private static String unquote(String path, String prefix)
    {
        if (path == null || "/dev/null".equals(path))
        {
            return null;
        }
        String value = path;
        if (value.startsWith("\"") && value.endsWith("\"") && value.length() >= 2)
        {
            value = value.substring(1, value.length() - 1).replace("\\\"", "\"").replace("\\\\", "\\");
        }
        if (value.startsWith(prefix))
        {
            value = value.substring(prefix.length());
        }
        return value;
    }

    private static String abbreviate(String line)
    {
        return line.length() <= 80 ? line : line.substring(0, 80) + "...";
    }

    private static final class FileBuilder
    {
        private String oldPath;
        private String newPath;
        private boolean added;
        private boolean deleted;
        private boolean renamed;
        private boolean binary;
        private boolean gitlink;
        private boolean modeChange;
        private final StringBuilder raw = new StringBuilder();
        private final List<HunkBuilder> hunks = new ArrayList<>();

        void consumeHeader(String line)
        {
            if (line.startsWith("new file mode"))
            {
                added = true;
            }
            else if (line.startsWith("deleted file mode"))
            {
                deleted = true;
            }
            else if (line.startsWith("old mode") || line.startsWith("new mode"))
            {
                modeChange = true;
            }
            else if (line.startsWith("similarity index") || line.startsWith("dissimilarity index"))
            {
                // 仅提示性信息，rename 由 rename from/to 判定
            }
            else if (line.startsWith("rename from "))
            {
                renamed = true;
                oldPath = unquote(line.substring("rename from ".length()), "");
            }
            else if (line.startsWith("rename to "))
            {
                renamed = true;
                newPath = unquote(line.substring("rename to ".length()), "");
            }
            else if (line.startsWith("copy from ") || line.startsWith("copy to "))
            {
                // 拷贝按普通修改处理，路径以 ---/+++ 为准
            }
            else if (line.startsWith("--- "))
            {
                oldPath = unquote(line.substring(4), "a/");
            }
            else if (line.startsWith("+++ "))
            {
                newPath = unquote(line.substring(4), "b/");
            }
            else if (line.startsWith("Binary files ") || line.startsWith("GIT binary patch"))
            {
                binary = true;
            }
            else if (line.startsWith("index "))
            {
                Matcher matcher = INDEX_LINE.matcher(line);
                if (matcher.matches() && "160000".equals(matcher.group(1)))
                {
                    gitlink = true;
                }
            }
            else if (line.startsWith("Subproject commit"))
            {
                gitlink = true;
            }
            else if (!line.isBlank())
            {
                // 未识别的头部行（如截断产生）：容忍，不计警告，避免噪声
            }
        }

        String label()
        {
            String path = newPath != null ? newPath : oldPath;
            return path == null ? "未知文件" : path;
        }

        DiffFileChange build(HunkBuilder pending, List<String> warnings)
        {
            if (pending != null)
            {
                pending.finish(warnings, label());
                hunks.add(pending);
            }
            DiffChangeType type;
            if (deleted)
            {
                type = DiffChangeType.DELETED;
            }
            else if (added)
            {
                type = DiffChangeType.ADDED;
            }
            else if (renamed)
            {
                type = DiffChangeType.RENAMED;
            }
            else
            {
                type = DiffChangeType.MODIFIED;
            }
            List<DiffHunk> built = new ArrayList<>(hunks.size());
            for (HunkBuilder builder : hunks)
            {
                built.add(builder.build());
            }
            boolean modeOnly = modeChange && built.isEmpty() && !binary && !gitlink;
            String section = raw.toString();
            // 去掉末尾多余换行，拼装 scoped diff 时统一补
            if (section.endsWith("\n"))
            {
                section = section.substring(0, section.length() - 1);
            }
            return new DiffFileChange(oldPath, newPath, type, binary, gitlink, modeOnly,
                List.copyOf(built), section);
        }
    }

    private static final class HunkBuilder
    {
        private final int oldStart;
        private final int oldCount;
        private final int newStart;
        private final int newCount;
        private final String sectionHeading;
        private final List<String> addedLines = new ArrayList<>();
        private final List<String> deletedLines = new ArrayList<>();
        private final List<LineRange> addedRanges = new ArrayList<>();
        private int rightLine;
        private int rightMin = -1;
        private int rightMax = -1;
        private int pendingAddStart = -1;
        private int pendingAddEnd = -1;
        private int consumedOld;
        private int consumedNew;

        HunkBuilder(int oldStart, int oldCount, int newStart, int newCount, String sectionHeading)
        {
            this.oldStart = oldStart;
            this.oldCount = oldCount;
            this.newStart = newStart;
            this.newCount = newCount;
            this.sectionHeading = sectionHeading;
            this.rightLine = newStart;
        }

        void consume(String line)
        {
            if (line.isEmpty())
            {
                // 截断或松散格式产生的空行，按上下文行容忍
                touchRight();
                consumedOld++;
                consumedNew++;
                rightLine++;
                flushPendingAdd();
                return;
            }
            char marker = line.charAt(0);
            String text = line.substring(1);
            switch (marker)
            {
                case ' ' ->
                {
                    touchRight();
                    consumedOld++;
                    consumedNew++;
                    rightLine++;
                    flushPendingAdd();
                }
                case '+' ->
                {
                    addedLines.add(text);
                    touchRight();
                    consumedNew++;
                    extendPendingAdd(rightLine);
                    rightLine++;
                }
                case '-' ->
                {
                    deletedLines.add(text);
                    consumedOld++;
                    flushPendingAdd();
                }
                case '\\' ->
                {
                    // "\ No newline at end of file"，不占行号
                }
                default ->
                {
                    // 不可达：外层已按首字符分派
                }
            }
        }

        private void touchRight()
        {
            if (rightMin < 0)
            {
                rightMin = rightLine;
            }
            rightMax = rightLine;
        }

        private void extendPendingAdd(int line)
        {
            if (pendingAddStart < 0)
            {
                pendingAddStart = line;
            }
            pendingAddEnd = line;
        }

        private void flushPendingAdd()
        {
            if (pendingAddStart >= 0)
            {
                addedRanges.add(new LineRange(pendingAddStart, pendingAddEnd));
                pendingAddStart = -1;
                pendingAddEnd = -1;
            }
        }

        void finish(List<String> warnings, String fileLabel)
        {
            flushPendingAdd();
            if (consumedOld != oldCount || consumedNew != newCount)
            {
                warnings.add(fileLabel + "：hunk 声明 -" + oldStart + "," + oldCount + " +" + newStart + "," + newCount
                    + "，实际消费 -" + consumedOld + " +" + consumedNew + "（Diff 可能被截断）");
            }
        }

        DiffHunk build()
        {
            return new DiffHunk(oldStart, oldCount, newStart, newCount, sectionHeading,
                List.copyOf(addedLines), List.copyOf(deletedLines), List.copyOf(addedRanges),
                rightMin < 0 ? null : new LineRange(rightMin, rightMax));
        }
    }
}
