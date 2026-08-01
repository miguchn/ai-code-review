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
        HttpPostResult result = postJsonDetailed(url, apiKey, jsonBody, timeoutMs);
        if (result.getException() != null)
        {
            if (result.getException() instanceof IOException io)
            {
                throw io;
            }
            throw new IOException(result.getException());
        }
        return result.getBody();
    }

    /**
     * 发送 POST JSON 请求并返回状态码与耗时（非 2xx 仍返回 body，便于错误分类）。
     */
    public static HttpPostResult postJsonDetailed(String url, String apiKey, String jsonBody, int timeoutMs)
    {
        long start = System.currentTimeMillis();
        try
        {
            OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                .readTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                .followRedirects(false)
                .followSslRedirects(false)
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
                String body = response.body() != null ? response.body().string() : null;
                return new HttpPostResult(response.code(), body, System.currentTimeMillis() - start, null);
            }
        }
        catch (Exception e)
        {
            return new HttpPostResult(0, null, System.currentTimeMillis() - start, e);
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
