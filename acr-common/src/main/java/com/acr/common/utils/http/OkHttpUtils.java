package com.acr.common.utils.http;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * OkHttp 工具类，用于带自定义 Header 的 HTTP 请求
 */
public class OkHttpUtils
{
    private static final Logger log = LoggerFactory.getLogger(OkHttpUtils.class);

    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    /**
     * 发送 POST JSON 请求（带自定义 Headers）
     */
    public static String postJson(String url, String apiKey, String jsonBody, int timeoutMs) throws IOException
    {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                .readTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                .build();

        Request.Builder builder = new Request.Builder()
                .url(url)
                .post(RequestBody.create(jsonBody, JSON));

        if (apiKey != null && !apiKey.isEmpty())
        {
            builder.addHeader("Authorization", "Bearer " + apiKey);
        }
        builder.addHeader("Content-Type", "application/json");

        try (Response response = client.newCall(builder.build()).execute())
        {
            if (response.body() != null)
            {
                return response.body().string();
            }
            return null;
        }
    }

    /**
     * 发送 POST JSON 请求（带自定义 Headers 和额外参数）
     */
    public static String postJson(String url, Map<String, String> headers, String jsonBody, int timeoutMs) throws IOException
    {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                .readTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                .build();

        Request.Builder builder = new Request.Builder()
                .url(url)
                .post(RequestBody.create(jsonBody, JSON));

        if (headers != null)
        {
            for (Map.Entry<String, String> entry : headers.entrySet())
            {
                builder.addHeader(entry.getKey(), entry.getValue());
            }
        }

        try (Response response = client.newCall(builder.build()).execute())
        {
            if (response.body() != null)
            {
                return response.body().string();
            }
            return null;
        }
    }
}
