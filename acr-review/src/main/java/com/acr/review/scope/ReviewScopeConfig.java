package com.acr.review.scope;

import java.util.List;

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
}
