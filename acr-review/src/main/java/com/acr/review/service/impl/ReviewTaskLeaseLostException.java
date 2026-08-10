package com.acr.review.service.impl;

/** 当前执行已失去数据库租约，必须停止后续事实写入和外部副作用。 */
final class ReviewTaskLeaseLostException extends RuntimeException
{
    private static final long serialVersionUID = 1L;

    ReviewTaskLeaseLostException(Long taskId, Long epoch)
    {
        super("审查任务租约已失效, taskId=" + taskId + ", epoch=" + epoch);
    }
}
