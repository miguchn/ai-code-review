package com.acr.review.notify.feishu;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
 * 飞书自定义机器人：将简化 Markdown 转为 post 富文本；不使用互动卡片。
 */
@Component
public class FeishuBotClient extends AbstractNotifyRobotClient
{
    private static final Pattern MD_LINK = Pattern.compile("\\[([^\\]]+)]\\((https?://[^)\\s]+)\\)");

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
     * 简化 Markdown → 飞书 post 行：
     * 标题行 → 加粗段落；**标签** → 加粗段落；列表项 → 以点号开头的段落；
     * [text](link) → 链接元素；其余降级为普通段落。
     */
    static JSONArray toPostLines(String body)
    {
        JSONArray lines = new JSONArray();
        if (body == null || body.isBlank())
        {
            JSONArray row = new JSONArray();
            row.add(textElement("（空消息）", false));
            lines.add(row);
            return lines;
        }
        for (String raw : body.split("\\R", -1))
        {
            JSONArray row = new JSONArray();
            appendMarkdownLine(row, raw);
            lines.add(row);
        }
        return lines;
    }

    private static void appendMarkdownLine(JSONArray row, String line)
    {
        if (line.isEmpty())
        {
            row.add(textElement(" ", false));
            return;
        }
        String trimmed = line.trim();
        if (trimmed.startsWith("### "))
        {
            appendInline(row, trimmed.substring(4), true);
            return;
        }
        if (trimmed.startsWith("**") && trimmed.endsWith("**") && trimmed.length() > 4
            && trimmed.indexOf("**", 2) == trimmed.length() - 2)
        {
            appendInline(row, trimmed.substring(2, trimmed.length() - 2), true);
            return;
        }
        if (trimmed.startsWith("- "))
        {
            appendInline(row, "· " + trimmed.substring(2), false);
            return;
        }
        appendInline(row, line, false);
    }

    private static void appendInline(JSONArray row, String text, boolean bold)
    {
        Matcher matcher = MD_LINK.matcher(text);
        int cursor = 0;
        while (matcher.find())
        {
            if (matcher.start() > cursor)
            {
                row.add(textElement(text.substring(cursor, matcher.start()), bold));
            }
            JSONObject link = new JSONObject();
            link.put("tag", "a");
            link.put("text", matcher.group(1));
            link.put("href", matcher.group(2));
            row.add(link);
            cursor = matcher.end();
        }
        if (cursor < text.length())
        {
            row.add(textElement(text.substring(cursor), bold));
        }
        if (row.isEmpty())
        {
            row.add(textElement(" ", bold));
        }
    }

    private static JSONObject textElement(String text, boolean bold)
    {
        JSONObject el = new JSONObject();
        el.put("tag", "text");
        el.put("text", text);
        if (bold)
        {
            el.put("style", new String[] { "bold" });
        }
        return el;
    }

    static String sign(long timestamp, String secret)
    {
        try
        {
            String stringToSign = timestamp + "\n" + secret;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(stringToSign.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] signData = mac.doFinal(new byte[] {});
            return Base64.getEncoder().encodeToString(signData);
        }
        catch (Exception ex)
        {
            throw new NotifyRobotException("飞书加签失败", ex);
        }
    }
}
