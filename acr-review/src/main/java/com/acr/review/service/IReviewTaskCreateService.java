package com.acr.review.service;

import com.acr.review.domain.ReviewProject;
import com.acr.review.domain.ReviewWebhookEvent;
import com.acr.review.git.GitPullRequestEvent;
import com.acr.review.git.GitPushEvent;

/** 审查任务创建用例（事务边界）：任务插入与事件受理结果保持一致。 */
public interface IReviewTaskCreateService
{
    /**
     * 按 Webhook 合并请求事件创建待审查任务，并将事件置为 ACCEPTED 关联任务。
     *
     * @return 生成的任务 ID
     */
    Long createTaskFromEvent(ReviewProject project, ReviewWebhookEvent event, GitPullRequestEvent prEvent);

    /**
     * 按 Webhook 推送事件创建待审查任务，并将事件置为 ACCEPTED 关联任务。
     *
     * @return 生成的任务 ID
     */
    Long createTaskFromPushEvent(ReviewProject project, ReviewWebhookEvent event, GitPushEvent pushEvent);
}
