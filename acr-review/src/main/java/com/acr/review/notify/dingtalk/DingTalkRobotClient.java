package com.acr.review.notify.dingtalk;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.acr.common.utils.StringUtils;
import com.acr.review.delivery.ReviewDeliveryConstants;
import com.acr.review.notify.AbstractNotifyRobotClient;
import com.acr.review.notify.NotifyRobotException;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;

/** 钉钉自定义机器人群消息（markdown + 可选加签）。 */
@Component
public class DingTalkRobotClient extends AbstractNotifyRobotClient
{
    public DingTalkRobotClient(
        @Value("${review.notify.connect-timeout-ms:5000}") int connectTimeoutMs,
        @Value("${review.notify.read-timeout-ms:15000}") int readTimeoutMs)
    {
        super(connectTimeoutMs, readTimeoutMs);
    }

    @Override
    public String channelType()
    {
        return ReviewDeliveryConstants.CHANNEL_DINGTALK_ROBOT;
    }

    @Override
    public void send(String webhookUrl, String secret, String title, String body, List<String> atIds)
    {
        requireWebhook(webhookUrl);
        String url = appendSign(webhookUrl, secret);
        JSONObject payload = new JSONObject();
        payload.put("msgtype", "markdown");
        JSONObject markdown = new JSONObject();
        markdown.put("title", StringUtils.defaultIfEmpty(title, "AI Code Review"));
        markdown.put("text", body == null ? "" : body);
        payload.put("markdown", markdown);
        // TODO 待真实群验证：at.atMobiles 需与正文 @手机号 同时出现才会渲染 @
        JSONObject at = new JSONObject();
        at.put("atMobiles", atIds == null ? new ArrayList<>() : new ArrayList<>(atIds));
        at.put("isAtAll", false);
        payload.put("at", at);

        String response = postJson(url, payload.toJSONString(), "钉钉机器人发送");
        JSONObject json = JSON.parseObject(response);
        if (json != null && json.getIntValue("errcode") != 0)
        {
            throw new NotifyRobotException("钉钉机器人发送失败："
                + sanitize(StringUtils.defaultIfEmpty(json.getString("errmsg"), "未知错误")));
        }
    }

    static String appendSign(String webhookUrl, String secret)
    {
        if (StringUtils.isEmpty(secret))
        {
            return webhookUrl;
        }
        try
        {
            long timestamp = System.currentTimeMillis();
            String stringToSign = timestamp + "\n" + secret;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] signData = mac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8));
            String sign = URLEncoder.encode(Base64.getEncoder().encodeToString(signData), StandardCharsets.UTF_8);
            String joiner = webhookUrl.contains("?") ? "&" : "?";
            return webhookUrl + joiner + "timestamp=" + timestamp + "&sign=" + sign;
        }
        catch (Exception ex)
        {
            throw new NotifyRobotException("钉钉加签失败", ex);
        }
    }
}
