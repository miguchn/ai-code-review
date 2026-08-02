package com.acr.review.scope;

import java.util.ArrayList;
import java.util.List;
import com.acr.review.domain.ReviewTask;

/**
 * 项目级审查范围配置（步 3 起由 review_project 列 + 任务快照提供；步 1-2 仅有默认值）。
 * reportExisting 由归属打标环节（步 5）消费，决策服务不读取，仅随配置传递。
 */
public record ReviewScopeConfig(
    /** 项目追加的排除 glob（在平台默认排除之后判定）。 */
    List<String> excludePatterns,
    boolean includeTests,
    boolean reportExisting,
    boolean expandEnabled)
{
    public ReviewScopeConfig
    {
        excludePatterns = excludePatterns == null ? List.of() : List.copyOf(excludePatterns);
    }

    /** 平台默认：排除测试文件、不上报存量、开启高影响扩展。 */
    public static ReviewScopeConfig defaults()
    {
        return new ReviewScopeConfig(List.of(), false, false, true);
    }

    /**
     * 从任务执行快照构造范围配置。快照列可空（M3.2 前冻结的历史任务为 NULL），
     * NULL 一律回落平台默认，保证历史任务执行行为与快照冻结上线前一致。
     */
    public static ReviewScopeConfig fromTaskSnapshot(ReviewTask task)
    {
        if (task == null)
        {
            return defaults();
        }
        return new ReviewScopeConfig(
            splitPatterns(task.getSnapshotScopeExcludePatterns()),
            "Y".equals(task.getSnapshotScopeIncludeTests()),
            "Y".equals(task.getSnapshotScopeReportExisting()),
            !"N".equals(task.getSnapshotScopeExpandEnabled()));
    }

    private static List<String> splitPatterns(String raw)
    {
        if (raw == null || raw.isBlank())
        {
            return List.of();
        }
        List<String> patterns = new ArrayList<>();
        for (String line : raw.split("\n"))
        {
            String trimmed = line.trim();
            if (!trimmed.isEmpty())
            {
                patterns.add(trimmed);
            }
        }
        return patterns;
    }
}
