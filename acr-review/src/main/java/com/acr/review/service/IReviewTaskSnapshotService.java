package com.acr.review.service;

import com.acr.review.domain.ReviewProject;
import com.acr.review.domain.ReviewTask;

/**
 * 审查任务执行快照：把审查方式、模型、模板、引擎按项目当前配置冻结进任务。
 * 建单时冻结一次；执行链路只读快照，模板/模型后续修改不影响已建任务。
 */
public interface IReviewTaskSnapshotService
{
    /**
     * 按项目当前审查配置冻结任务执行快照。
     * 配置不完整时抛出带修复指引的 ServiceException，调用方不得继续执行。
     */
    void freezeExecutionSnapshot(ReviewProject project, ReviewTask task);
}
