package com.acr.review.service.impl;

/** 审查终态事务未能完整提交；调用方应将任务置为可恢复失败。 */
class ReviewTaskCompletionException extends RuntimeException
{
    private static final long serialVersionUID = 1L;

    ReviewTaskCompletionException(String message, Throwable cause)
    {
        super(message, cause);
    }
}
