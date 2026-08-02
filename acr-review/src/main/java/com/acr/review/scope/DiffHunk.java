package com.acr.review.scope;

import java.util.List;

/**
 * 单个 hunk 的结构化数据。
 * addedRanges / rightRange 均为右侧（head 版本）行号，支撑范围归属判定。
 */
public record DiffHunk(
    int oldStart,
    int oldCount,
    int newStart,
    int newCount,
    /** hunk header 中 @@ 之后的节标题（通常是所在函数/方法签名），可能为空。 */
    String sectionHeading,
    /** 新增行文本（不含 + 前缀），按出现顺序。 */
    List<String> addedLines,
    /** 删除行文本（不含 - 前缀），按出现顺序。 */
    List<String> deletedLines,
    /** 新增行在右侧的行号区间（已合并相邻行），按升序。 */
    List<LineRange> addedRanges,
    /** hunk 在右侧覆盖的总区间（含上下文与新增行）；纯删除 hunk 为 null。 */
    LineRange rightRange)
{
}
