package com.acr.review.scope;

import java.util.List;

/**
 * Diff 解析输出。
 * warnings 非空表示存在容错（如服务端截断的残缺尾部），解析结果仍可用于范围决策。
 */
public record DiffParseResult(List<DiffFileChange> files, List<String> warnings)
{
    public boolean isEmpty()
    {
        return files == null || files.isEmpty();
    }
}
