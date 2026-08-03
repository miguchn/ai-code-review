package com.acr.review.notify;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/** 群机器人 OkHttp 公共发送逻辑。 */
public abstract class AbstractNotifyRobotClient implements NotifyRobotClient
{
    protected static final MediaType JSON_MEDIA = MediaType.parse("application/json; charset=utf-8");
    private static final Pattern SENSITIVE = Pattern.compile(
        "(?i)(access_token|sign|timestamp|secret)=([^&\\s]+)");

    private final OkHttpClient client;

    protected AbstractNotifyRobotClient(int connectTimeoutMs, int readTimeoutMs)
    {
        this.client = new OkHttpClient.Builder()
            .connectTimeout(connectTimeoutMs, TimeUnit.MILLISECONDS)
            .readTimeout(readTimeoutMs, TimeUnit.MILLISECONDS)
            .callTimeout(readTimeoutMs, TimeUnit.MILLISECONDS)
            .build();
    }

    protected String postJson(String url, String jsonBody, String action)
    {
        Request request = new Request.Builder()
            .url(url)
            .post(RequestBody.create(jsonBody, JSON_MEDIA))
            .header("Content-Type", "application/json; charset=utf-8")
            .build();
        try (Response response = client.newCall(request).execute())
        {
            String body = response.body() == null ? "" : response.body().string();
            if (!response.isSuccessful())
            {
                throw new NotifyRobotException(action + "失败：HTTP " + response.code()
                    + " " + sanitize(truncate(body, 200)));
            }
            return body;
        }
        catch (InterruptedIOException ex)
        {
            Thread.currentThread().interrupt();
            throw new NotifyRobotException(action + "超时，请稍后重试", ex);
        }
        catch (IOException ex)
        {
            throw new NotifyRobotException(action + "网络异常：" + sanitize(ex.getMessage()), ex);
        }
    }

    protected static String sanitize(String text)
    {
        if (text == null)
        {
            return "";
        }
        return SENSITIVE.matcher(text).replaceAll("$1=***");
    }

    protected static String truncate(String text, int max)
    {
        if (text == null)
        {
            return "";
        }
        return text.length() <= max ? text : text.substring(0, max) + "...";
    }

    protected static void requireWebhook(String webhookUrl)
    {
        if (webhookUrl == null || webhookUrl.isBlank())
        {
            throw new NotifyRobotException("Webhook URL 无效");
        }
        if (!webhookUrl.startsWith("https://") && !webhookUrl.startsWith("http://"))
        {
            throw new NotifyRobotException("Webhook URL 必须以 http(s):// 开头");
        }
    }
}
