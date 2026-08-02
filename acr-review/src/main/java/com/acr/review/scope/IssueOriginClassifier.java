package com.acr.review.scope;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 问题归属判定（M3.2 步 5）：按 Diff 行号映射把模型报告的问题打标为 NEW / EXISTING。
 * 不信任模型自报归属，后端行号映射为唯一判定依据。
 *
 * 判定规则（设计文档 §5）：
 * 1. 问题行区间命中任一新增行区间 → NEW；
 * 2. 未命中但落在同一含新增行的 hunk 内，且与最近新增行距离 ≤ {@link #PROXIMITY_TOLERANCE_LINES} → NEW（邻近宽限）；
 * 3. 其余落在 hunk 上下文或文件其他位置 → EXISTING；
 * 4. 文件不在 Diff 中或行号缺失 → UNVERIFIABLE（按 NEW 计，单列计数供排障）；
 * 5. 已纳入完整内容的高影响扩展文件（FULL）→ 整个文件视为可上报范围，直接 NEW。
 */
public final class IssueOriginClassifier
{
    /** 邻近宽限行数：变更可能使相邻上下文行出错（如改了调用方）。 */
    public static final int PROXIMITY_TOLERANCE_LINES = 3;

    /** 归属判定结果。UNVERIFIABLE 落库时按 NEW 打标，但在 scopeStats 单列计数。 */
    public enum Verdict
    {
        NEW, EXISTING, UNVERIFIABLE
    }

    private final Map<String, DiffFileChange> filesByPath;
    private final Set<String> fullContentPaths;

    /**
     * @param parsed           统一 Diff 解析结果（行号区间为右侧/head 版本）
     * @param fullContentPaths 已纳入完整内容的扩展文件路径（ReviewScopePromptAssembler.STATUS_FULL）
     */
    public IssueOriginClassifier(DiffParseResult parsed, Set<String> fullContentPaths)
    {
        this.filesByPath = new HashMap<>();
        if (parsed != null && parsed.files() != null)
        {
            for (DiffFileChange file : parsed.files())
            {
                if (file.effectivePath() != null)
                {
                    filesByPath.put(file.effectivePath(), file);
                }
            }
        }
        this.fullContentPaths = fullContentPaths == null ? Set.of() : Set.copyOf(fullContentPaths);
    }

    /**
     * 判定问题归属。
     *
     * @param filePath  模型报告的文件路径（容忍 "./"、"b/" 前缀）
     * @param startLine 起始行号（右侧版本），null 按不可判定
     * @param endLine   结束行号，null 视为与 startLine 相同
     */
    public Verdict classify(String filePath, Integer startLine, Integer endLine)
    {
        if (filePath == null || filePath.isBlank())
        {
            return Verdict.UNVERIFIABLE;
        }
        String path = normalizePath(filePath);
        if (fullContentPaths.contains(path))
        {
            return Verdict.NEW;
        }
        DiffFileChange file = filesByPath.get(path);
        if (file == null)
        {
            return Verdict.UNVERIFIABLE;
        }
        if (startLine == null || startLine < 1)
        {
            return Verdict.UNVERIFIABLE;
        }
        int end = endLine == null || endLine < startLine ? startLine : endLine;
        LineRange issueRange = new LineRange(startLine, end);

        for (LineRange added : file.addedLineRanges())
        {
            if (added.intersects(issueRange))
            {
                return Verdict.NEW;
            }
        }
        if (file.hunks() != null)
        {
            for (DiffHunk hunk : file.hunks())
            {
                if (hunk.rightRange() == null || hunk.addedRanges().isEmpty())
                {
                    continue;
                }
                if (!hunk.rightRange().intersects(issueRange))
                {
                    continue;
                }
                for (LineRange added : hunk.addedRanges())
                {
                    if (added.distanceTo(issueRange) <= PROXIMITY_TOLERANCE_LINES)
                    {
                        return Verdict.NEW;
                    }
                }
            }
        }
        return Verdict.EXISTING;
    }

    /** 路径归一：去空白与 "./" 前缀；模型偶尔回带 diff 的 "b/" 前缀，一并剥除。 */
    private String normalizePath(String filePath)
    {
        String path = filePath.trim();
        if (path.startsWith("./"))
        {
            path = path.substring(2);
        }
        if (path.startsWith("b/") && !filesByPath.containsKey(path) && filesByPath.containsKey(path.substring(2)))
        {
            path = path.substring(2);
        }
        return path;
    }
}
