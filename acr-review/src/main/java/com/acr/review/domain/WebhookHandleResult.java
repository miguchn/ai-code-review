package com.acr.review.domain;

/** Webhook 处理结果：HTTP 状态与响应消息（不携带 Secret、签名或内部异常细节）。 */
public record WebhookHandleResult(int httpStatus, String message)
{
    public static WebhookHandleResult ok(String message)
    {
        return new WebhookHandleResult(200, message);
    }

    public static WebhookHandleResult badRequest(String message)
    {
        return new WebhookHandleResult(400, message);
    }

    public static WebhookHandleResult unauthorized(String message)
    {
        return new WebhookHandleResult(401, message);
    }

    public static WebhookHandleResult payloadTooLarge(String message)
    {
        return new WebhookHandleResult(413, message);
    }

    /** 验签去重前的内部失败：返回 5xx 以便 Git 平台重试，避免事件被静默丢弃。 */
    public static WebhookHandleResult serverError(String message)
    {
        return new WebhookHandleResult(500, message);
    }
}
