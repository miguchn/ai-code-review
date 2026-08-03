package com.acr.review.notify;

/** 群机器人发送能力：各厂商差异收敛在实现内。 */
public interface NotifyRobotClient
{
    /** 渠道类型常量，如 DINGTALK_ROBOT。 */
    String channelType();

    /**
     * 发送审查结论摘要/测试文案。
     *
     * @param webhookUrl 完整 Webhook URL（已解密）
     * @param secret     加签 Secret，可空
     * @param title      标题（钉钉 markdown 需要；其他渠道可忽略）
     * @param body       正文（行式文本 / markdown）
     */
    void send(String webhookUrl, String secret, String title, String body);
}
