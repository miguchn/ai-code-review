package com.acr.review.scheduling;

/** 审查运行态查询入口：队列深度与各预算占用/拒绝数。 */
public interface IReviewRuntimeStatusService
{
    ReviewRuntimeStatus snapshot();
}
