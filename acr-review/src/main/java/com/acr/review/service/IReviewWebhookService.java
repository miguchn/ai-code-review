package com.acr.review.service;

import com.acr.review.domain.WebhookHandleResult;
import com.acr.review.git.WebhookRequestHeaders;

/** Webhook 事件接入用例。 */
public interface IReviewWebhookService
{
    /**
     * 处理指定平台的 Webhook 投递。全程无外部调用，快速响应。
     *
     * @param providerCode 平台编码（GITHUB / GITLAB / GITEE / GITEA）
     * @param headers      平台相关请求头
     * @param payload      请求原始字节
     * @return 处理结果（HTTP 状态与脱敏消息）
     */
    WebhookHandleResult handleWebhook(String providerCode, WebhookRequestHeaders headers, byte[] payload);

    /**
     * @deprecated 请使用 {@link #handleWebhook(String, WebhookRequestHeaders, byte[])}。
     */
    @Deprecated
    WebhookHandleResult handleGitHubWebhook(String eventType, String deliveryId, String signatureHeader, byte[] payload);
}
