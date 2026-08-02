package com.acr.review.scope;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 范围决策输出。
 * scopedDiff 仅含纳入文件的原始 L0 段（按文件边界截断）；
 * 扩展文件的完整内容（L3）由执行层按 expandedFiles 顺序拉取并竞争剩余预算，本对象不做 I/O。
 */
public record ReviewScopeDecision(
    /** 纳入审查的 Diff（仅纳入文件 hunk，保持原始格式）。 */
    String scopedDiff,
    /** 纳入文件路径（与 scopedDiff 顺序一致：高影响在前，普通在后）。 */
    List<String> includedFiles,
    /** 被排除文件及原因（DEFAULT_EXCLUDE / PROJECT_EXCLUDE / TEST_FILE）。 */
    List<ExcludedFile> excludedFiles,
    /** 高影响扩展文件，按规则优先级排序；needsFullContent=false 表示新增文件，内容已在 Diff 中。 */
    List<ExpandedFile> expandedFiles,
    /** 记录类文件（删除/二进制/纯改名/gitlink/仅 mode/空文件），不审但落快照。 */
    List<RecordOnlyFile> recordOnlyFiles,
    /** L0 预算超限被整文件丢弃的路径。 */
    List<String> droppedFiles,
    /** scopedDiff 是否发生截断（droppedFiles 非空即为 true）。 */
    boolean truncated,
    /** 解析阶段的容错警告（如服务端截断）。 */
    List<String> parseWarnings)
{
    public ReviewScopeDecision
    {
        includedFiles = List.copyOf(includedFiles);
        excludedFiles = List.copyOf(excludedFiles);
        expandedFiles = List.copyOf(expandedFiles);
        recordOnlyFiles = List.copyOf(recordOnlyFiles);
        droppedFiles = List.copyOf(droppedFiles);
        parseWarnings = List.copyOf(parseWarnings);
    }

    /** 有效审查文件数；为 0 时执行层应跳过模型调用。 */
    public int effectiveFileCount()
    {
        return includedFiles.size();
    }

    /** 决策快照（落 review_task_run 的 JSON 结构，键保持稳定）。 */
    public Map<String, Object> toSnapshotMap()
    {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("includedFiles", includedFiles);
        snapshot.put("excludedFiles", excludedFiles.stream()
            .map(file -> Map.of("path", file.path(), "reason", file.reason())).toList());
        snapshot.put("expandedFiles", expandedFiles.stream()
            .map(file -> Map.of("path", file.path(), "rule", file.rule(),
                "needsFullContent", file.needsFullContent())).toList());
        snapshot.put("recordOnlyFiles", recordOnlyFiles.stream()
            .map(file -> Map.of("path", file.path(), "reason", file.reason())).toList());
        snapshot.put("droppedFiles", droppedFiles);
        snapshot.put("truncated", truncated);
        snapshot.put("scopedDiffChars", scopedDiff.length());
        snapshot.put("parseWarnings", parseWarnings);
        return snapshot;
    }

    public record ExcludedFile(String path, String reason)
    {
    }

    public record ExpandedFile(String path, String rule, boolean needsFullContent)
    {
    }

    public record RecordOnlyFile(String path, String reason)
    {
    }
}
