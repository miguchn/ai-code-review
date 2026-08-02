package com.acr.review.scope;

import java.util.ArrayList;
import java.util.List;

/**
 * 单个文件的 Diff 解析结果。
 * rawSection 保留该文件在原始 Diff 中的完整文本段（diff --git 行至下一文件前），
 * 用于按原格式拼装 scoped diff，避免重建导致的格式漂移。
 */
public record DiffFileChange(
    /** 旧路径（a/ 侧）；新增文件为 null。 */
    String oldPath,
    /** 新路径（b/ 侧）；删除文件为 null。 */
    String newPath,
    DiffChangeType changeType,
    boolean binary,
    boolean gitlink,
    boolean modeOnly,
    List<DiffHunk> hunks,
    String rawSection)
{
    /** 用于展示与规则匹配的有效路径：优先新路径。 */
    public String effectivePath()
    {
        return newPath != null ? newPath : oldPath;
    }

    public boolean hasHunks()
    {
        return hunks != null && !hunks.isEmpty();
    }

    /** 全部 hunk 的新增行区间（右侧行号）并集，升序。 */
    public List<LineRange> addedLineRanges()
    {
        List<LineRange> ranges = new ArrayList<>();
        if (hunks != null)
        {
            for (DiffHunk hunk : hunks)
            {
                ranges.addAll(hunk.addedRanges());
            }
        }
        return ranges;
    }

    /** 全部 hunk 的右侧覆盖区间（含上下文），升序。 */
    public List<LineRange> hunkRightRanges()
    {
        List<LineRange> ranges = new ArrayList<>();
        if (hunks != null)
        {
            for (DiffHunk hunk : hunks)
            {
                if (hunk.rightRange() != null)
                {
                    ranges.add(hunk.rightRange());
                }
            }
        }
        return ranges;
    }

    /** 全部新增 + 删除行文本（供签名变更等规则扫描）。 */
    public List<String> changedLines()
    {
        List<String> lines = new ArrayList<>();
        if (hunks != null)
        {
            for (DiffHunk hunk : hunks)
            {
                lines.addAll(hunk.addedLines());
                lines.addAll(hunk.deletedLines());
            }
        }
        return lines;
    }
}
