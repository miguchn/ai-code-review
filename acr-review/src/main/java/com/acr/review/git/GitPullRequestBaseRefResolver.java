package com.acr.review.git;

/**
 * 按平台补全 PR/MR 审查基线（base SHA）。
 *
 * GitLab 19.x 起 MR Webhook 载荷不再携带 diff_refs / target_branch_sha，
 * 解析阶段允许 base 缺失，执行阶段经平台 API（如 GET /merge_requests/:iid 的 diff_refs）回填。
 * 不需要补全的平台（GitHub 等载荷自带 base.sha）无需提供实现。
 */
public interface GitPullRequestBaseRefResolver
{
    /** 平台编码，与 ReviewProject.provider 对齐（如 GITLAB）。 */
    String providerCode();

    /**
     * 解析 PR/MR 的审查基线 SHA。
     *
     * @param repository 仓库坐标
     * @param access 平台访问上下文（含凭据 Token）
     * @param prNumber PR/MR 编号（GitLab iid）
     * @return 基线 SHA；无法解析时返回 null，由调用方按缺省策略处理
     */
    String resolveBaseRef(GitRepositoryCoordinates repository, GitAccessContext access, int prNumber);
}
