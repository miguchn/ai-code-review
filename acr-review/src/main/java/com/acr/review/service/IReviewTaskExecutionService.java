package com.acr.review.service;

/** 审查任务执行与重试用例。 */
public interface IReviewTaskExecutionService
{
    /** 事务提交后调度执行；可重复调用，靠 CAS 领取防重。 */
    void scheduleExecution(Long taskId);

    /** 同步执行入口（由异步调度或测试调用）。 */
    void executeTask(Long taskId);

    /** 人工重试失败任务：校验后重新调度。 */
    void retryTask(Long taskId);
}
