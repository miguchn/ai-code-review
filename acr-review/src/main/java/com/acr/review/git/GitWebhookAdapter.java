package com.acr.review.git;

/** Git Provider 的 Webhook 适配契约：只做验签与载荷解析，不发起 HTTP 调用，不含业务规则。 */
public interface GitWebhookAdapter
{
    /** 与 GitProvider.providerCode() 同一选择键。 */
    String providerCode();

    /**
     * 从平台头中提取投递 ID；缺失时可由适配器基于载荷合成（64 位 hex，不得截断）。
     *
     * @return 投递 ID；无法确定时返回 null
     */
    String resolveDeliveryId(WebhookRequestHeaders headers, byte[] payload);

    /**
     * 从平台头中提取事件类型。
     *
     * @return 事件类型；缺失时返回 null
     */
    String resolveEventType(WebhookRequestHeaders headers);

    /**
     * 校验 Webhook 签名或 Secret Token。
     *
     * @param secret  项目配置的 Webhook Secret 明文
     * @param payload 请求原始字节
     * @param headers 平台请求头
     * @return 签名是否有效
     */
    boolean verify(String secret, byte[] payload, WebhookRequestHeaders headers);

    /**
     * 将请求体解包为可 JSON 解析的载荷字节。默认原样返回；
     * 平台若以 form-urlencoded 包裹 JSON（如 GitHub push/ping），由适配器覆写解包。
     * 验签仍必须对原始请求字节，不得对本方法结果验签。
     *
     * @param payload 请求原始字节
     * @return 供 parse* 使用的载荷字节
     */
    default byte[] unwrapPayload(byte[] payload)
    {
        return payload;
    }

    /**
     * 解析载荷中的仓库坐标（用于项目匹配）。
     *
     * @param payload 请求原始字节（或已 unwrap 的 JSON 字节）
     * @return 仓库坐标；无法解析时返回 null
     */
    GitRepositoryCoordinates parseRepository(byte[] payload);

    /**
     * 解析合并请求事件载荷，并将平台原始动作映射为统一动作
     * （opened / reopened / synchronize）；无法映射的动作仍可返回事件但 action 保持原值或 null，
     * 由用例层白名单忽略。
     *
     * @param eventType  平台事件类型
     * @param deliveryId 投递 ID
     * @param payload    请求原始字节
     * @return 平台无关事件；非合并请求事件或载荷非法时返回 null
     */
    GitPullRequestEvent parsePullRequestEvent(String eventType, String deliveryId, byte[] payload);

    /** 是否为合并请求类事件（平台事件类型判断）。 */
    boolean isPullRequestEventType(String eventType);

    /**
     * 解析推送事件载荷。
     *
     * @param eventType  平台事件类型
     * @param deliveryId 投递 ID
     * @param payload    请求原始字节
     * @return 平台无关推送事件；非推送事件或载荷非法时返回 null
     */
    GitPushEvent parsePushEvent(String eventType, String deliveryId, byte[] payload);

    /** 是否为推送类事件（平台事件类型判断）。 */
    boolean isPushEventType(String eventType);
}
