package com.acr.review.notify.wecom;

import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.acr.common.utils.StringUtils;
import com.acr.review.delivery.ReviewDeliveryConstants;
import com.acr.review.notify.AbstractNotifyRobotClient;
import com.acr.review.notify.NotifyRobotException;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;

/** 企业微信群机器人（markdown，正文 ≤4096 字节）。 */
@Component
public class WeComRobotClient extends AbstractNotifyRobotClient
{
    public WeComRobotClient(
        @Value("${review.notify.connect-timeout-ms:5000}") int connectTimeoutMs,
        @Value("${review.notify.read-timeout-ms:15000}") int readTimeoutMs)
    {
        super(connectTimeoutMs, readTimeoutMs);
    }

    @Override
    public String channelType()
    {
        return ReviewDeliveryConstants.CHANNEL_WECOM_ROBOT;
    }

    @Override
    public void send(String webhookUrl, String secret, String title, String body)
    {
        requireWebhook(webhookUrl);
        String content = body == null ? "" : body;
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        if (bytes.length > ReviewDeliveryConstants.WECOM_MAX_MARKDOWN_BYTES)
        {
            content = truncateUtf8(content, ReviewDeliveryConstants.WECOM_MAX_MARKDOWN_BYTES - 3) + "...";
        }
        JSONObject payload = new JSONObject();
        payload.put("msgtype", "markdown");
        JSONObject markdown = new JSONObject();
        markdown.put("content", content);
        payload.put("markdown", markdown);

        String response = postJson(webhookUrl, payload.toJSONString(), "企微机器人发送");
        JSONObject json = JSON.parseObject(response);
        if (json != null && json.getIntValue("errcode") != 0)
        {
            throw new NotifyRobotException("企微机器人发送失败："
                + sanitize(StringUtils.defaultIfEmpty(json.getString("errmsg"), "未知错误")));
        }
    }

    static String truncateUtf8(String text, int maxBytes)
    {
        if (text == null)
        {
            return "";
        }
        byte[] all = text.getBytes(StandardCharsets.UTF_8);
        if (all.length <= maxBytes)
        {
            return text;
        }
        int end = maxBytes;
        while (end > 0 && (all[end] & 0xC0) == 0x80)
        {
            end--;
        }
        return new String(all, 0, end, StandardCharsets.UTF_8);
    }
}
