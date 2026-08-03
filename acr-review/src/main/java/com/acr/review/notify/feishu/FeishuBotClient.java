package com.acr.review.notify.feishu;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.acr.common.utils.StringUtils;
import com.acr.review.delivery.ReviewDeliveryConstants;
import com.acr.review.notify.AbstractNotifyRobotClient;
import com.acr.review.notify.NotifyRobotException;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;

/**
 * 飞书自定义机器人：使用 post 富文本保证链接可点击；不使用互动卡片。
 */
@Component
public class FeishuBotClient extends AbstractNotifyRobotClient
{
    public FeishuBotClient(
        @Value("${review.notify.connect-timeout-ms:5000}") int connectTimeoutMs,
        @Value("${review.notify.read-timeout-ms:15000}") int readTimeoutMs)
    {
        super(connectTimeoutMs, readTimeoutMs);
    }

    @Override
    public String channelType()
    {
        return ReviewDeliveryConstants.CHANNEL_FEISHU_BOT;
    }

    @Override
    public void send(String webhookUrl, String secret, String title, String body)
    {
        requireWebhook(webhookUrl);
        JSONObject payload = new JSONObject();
        payload.put("msg_type", "post");
        if (StringUtils.isNotEmpty(secret))
        {
            long timestamp = System.currentTimeMillis() / 1000;
            payload.put("timestamp", String.valueOf(timestamp));
            payload.put("sign", sign(timestamp, secret));
        }
        JSONObject content = new JSONObject();
        JSONObject post = new JSONObject();
        JSONObject zhCn = new JSONObject();
        zhCn.put("title", StringUtils.defaultIfEmpty(title, "AI Code Review"));
        zhCn.put("content", toPostLines(body));
        post.put("zh_cn", zhCn);
        content.put("post", post);
        payload.put("content", content);

        String response = postJson(webhookUrl, payload.toJSONString(), "飞书机器人发送");
        JSONObject json = JSON.parseObject(response);
        if (json != null)
        {
            int code = json.containsKey("code") ? json.getIntValue("code") : json.getIntValue("StatusCode");
            if (code != 0)
            {
                String msg = StringUtils.defaultIfEmpty(json.getString("msg"),
                    StringUtils.defaultIfEmpty(json.getString("StatusMessage"), "未知错误"));
                throw new NotifyRobotException("飞书机器人发送失败：" + sanitize(msg));
            }
        }
    }

    /**
     * 将行式正文转为飞书 post 段落；识别「标签：URL」行中的 http(s) 链接。
     */
    static JSONArray toPostLines(String body)
    {
        JSONArray lines = new JSONArray();
        if (body == null || body.isBlank())
        {
            JSONArray row = new JSONArray();
            row.add(textElement("（空消息）"));
            lines.add(row);
            return lines;
        }
        for (String raw : body.split("\\R"))
        {
            JSONArray row = new JSONArray();
            appendLine(row, raw);
            lines.add(row);
        }
        return lines;
    }

    private static void appendLine(JSONArray row, String line)
    {
        if (line.isEmpty())
        {
            row.add(textElement(" "));
            return;
        }
        String rest = line;
        while (true)
        {
            int http = indexOfUrl(rest);
            if (http < 0)
            {
                row.add(textElement(rest));
                return;
            }
            if (http > 0)
            {
                row.add(textElement(rest.substring(0, http)));
            }
            String fromUrl = rest.substring(http);
            int end = fromUrl.indexOf(' ');
            String url = end < 0 ? fromUrl : fromUrl.substring(0, end);
            JSONObject link = new JSONObject();
            link.put("tag", "a");
            link.put("text", url);
            link.put("href", url);
            row.add(link);
            if (end < 0)
            {
                return;
            }
            rest = fromUrl.substring(end);
        }
    }

    private static int indexOfUrl(String line)
    {
        int https = line.indexOf("https://");
        int http = line.indexOf("http://");
        if (https < 0)
        {
            return http;
        }
        if (http < 0)
        {
            return https;
        }
        return Math.min(https, http);
    }

    private static JSONObject textElement(String text)
    {
        JSONObject el = new JSONObject();
        el.put("tag", "text");
        el.put("text", text);
        return el;
    }

    static String sign(long timestamp, String secret)
    {
        try
        {
            String stringToSign = timestamp + "\n" + secret;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(stringToSign.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            // 飞书：对空消息做 HMAC，密钥为 stringToSign
            byte[] signData = mac.doFinal(new byte[] {});
            return Base64.getEncoder().encodeToString(signData);
        }
        catch (Exception ex)
        {
            throw new NotifyRobotException("飞书加签失败", ex);
        }
    }
}
