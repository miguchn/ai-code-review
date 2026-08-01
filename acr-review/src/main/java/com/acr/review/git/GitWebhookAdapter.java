package com.acr.review.git;

/** Git Provider 的 Webhook 适配契约：只做验签与载荷解析，不发起 HTTP 调用，不含业务规则。 */
public interface GitWebhookAdapter
{
    /** 与 GitProvider.providerCode() 同一选择键。 */
    String providerCode();

    /**
     * 校验 Webhook 签名。
     *
     * @param secret          项目配置的 Webhook Secret 明文
     * @param payload         请求原始字节
     * @param signatureHeader 平台签名头（GitHub: X-Hub-Signature-256）
     * @return 签名是否有效
     */
    boolean verifySignature(String secret, byte[] payload, String signatureHeader);

    /**
     * 解析载荷中的仓库坐标（验签前项目匹配使用）。
     *
     * @param payload 请求原始字节
     * @return 仓库坐标；无法解析时返回 null
     */
    GitRepositoryCoordinates parseRepository(byte[] payload);

    /**
     * 解析 PR 事件载荷。
     *
     * @param eventType  平台事件头（GitHub: X-GitHub-Event）
     * @param deliveryId 平台投递 ID（GitHub: X-GitHub-Delivery）
     * @param payload    请求原始字节
     * @return 平台无关 PR 事件；非 PR 事件或载荷非法时返回 null
     */
    GitPullRequestEvent parsePullRequestEvent(String eventType, String deliveryId, byte[] payload);
}
