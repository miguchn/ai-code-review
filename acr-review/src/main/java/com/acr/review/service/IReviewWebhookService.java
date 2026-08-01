package com.acr.review.service;

import com.acr.review.domain.WebhookHandleResult;

/** Webhook 事件接入用例。 */
public interface IReviewWebhookService
{
    /**
     * 处理 GitHub Webhook 投递。全程无外部调用，快速响应。
     *
     * @param eventType       X-GitHub-Event
     * @param deliveryId      X-GitHub-Delivery
     * @param signatureHeader X-Hub-Signature-256
     * @param payload         请求原始字节
     * @return 处理结果（HTTP 状态与脱敏消息）
     */
    WebhookHandleResult handleGitHubWebhook(String eventType, String deliveryId, String signatureHeader, byte[] payload);
}
