package com.acr.review.scheduling;

/** 四类资源预算的可查询快照（S6 指标面复用，本切片只暴露状态）。 */
public record ReviewResourceBudgetStatus(
    int workspaceHeld,
    int workspaceLimit,
    long workspaceRejected,
    long workspaceUsedMb,
    int workspaceDiskLimitMb,
    int ocrHeld,
    int ocrLimit,
    long ocrRejected,
    int llmHeld,
    int llmLimit,
    long llmRejected,
    int projectHeldTotal,
    int projectLimitPerProject,
    long projectRejected)
{
}
