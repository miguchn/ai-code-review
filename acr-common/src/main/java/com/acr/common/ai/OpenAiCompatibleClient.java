package com.acr.common.ai;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import java.util.concurrent.TimeUnit;
import com.acr.common.utils.http.OkHttpUtils;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * OpenAI 兼容协议客户端（OpenAI / Qwen / DeepSeek 等兼容 OpenAI API 的 Provider）
 */
public class OpenAiCompatibleClient implements AiClient
{
    private static final Logger log = LoggerFactory.getLogger(OpenAiCompatibleClient.class);

    @Override
    public String chat(AiProviderConfig config, String prompt)
    {
        try
        {
            int maxTokens = config.getMaxTokens() > 0 ? config.getMaxTokens() : 4096;
            String body = buildRequestBody(config, prompt, maxTokens);
            String maskedUrl = maskApiKeyInUrl(config.getApiUrl());
            log.debug("[HTTP调用] POST {} | model={} | max_tokens={} | timeout={}ms", maskedUrl, config.getModel(), maxTokens, config.getTimeout());

            long start = System.currentTimeMillis();
            String response = OkHttpUtils.postJson(config.getApiUrl(), config.getApiKey(), body, config.getTimeout());
            long elapsed = System.currentTimeMillis() - start;
            log.debug("[HTTP调用] 返回耗时={}ms | 原始响应长度={}字符", elapsed, response != null ? response.length() : 0);

            String content = parseContent(response);
            if (content == null || content.isEmpty())
            {
                log.warn("[HTTP调用] AI 返回内容解析为空 | 原始响应前300字符: {}", truncate(response, 300));
            }
            else
            {
                log.debug("[HTTP调用] 解析后content长度={}字符", content.length());
            }
            return content;
        }
        catch (Exception e)
        {
            log.error("[HTTP调用] 异常: {} | model={} | 地址={}", e.getMessage(), config.getModel(), maskApiKeyInUrl(config.getApiUrl()));
            throw new RuntimeException("AI 调用失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String chatJson(AiProviderConfig config, String prompt)
    {
        String systemPrompt = prompt + "\n\n请以 JSON 格式返回结果，不要包含多余说明文字。";
        return chat(config, systemPrompt);
    }

    @Override
    public String testConnection(AiProviderConfig config)
    {
        try
        {
            String body = buildRequestBody(config, "Hi, respond with OK", 10);
            String response = OkHttpUtils.postJson(config.getApiUrl(), config.getApiKey(), body, config.getTimeout());
            if (response != null && response.contains("content"))
            {
                return "连接成功";
            }
            return "连接失败: 响应格式不正确";
        }
        catch (Exception e)
        {
            return "连接失败: " + e.getMessage();
        }
    }

    @Override
    public String embedAsJsonArray(AiProviderConfig config, String input)
    {
        if (input == null || input.isBlank())
        {
            return null;
        }
        String model = config.getEmbeddingModel();
        if (model == null || model.isBlank())
        {
            log.debug("[Embedding] 未配置 embeddingModel，跳过");
            return null;
        }
        String url = resolveEmbeddingsEndpoint(config.getApiUrl(), config.getEmbeddingApiUrl());
        if (url == null || url.isBlank())
        {
            log.warn("[Embedding] 无法解析 embeddings URL");
            return null;
        }
        JSONObject body = new JSONObject();
        body.put("model", model.trim());
        body.put("input", input);
        int timeout = config.getTimeout() > 0 ? config.getTimeout() : 60000;
        try
        {
            log.debug("[Embedding] POST {} | model={}", maskApiKeyInUrl(url), model);
            long start = System.currentTimeMillis();
            OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(timeout, TimeUnit.MILLISECONDS)
                .readTimeout(timeout, TimeUnit.MILLISECONDS)
                .build();
            Request request = new Request.Builder()
                .url(url)
                .post(RequestBody.create(body.toJSONString(), OkHttpUtils.JSON))
                .addHeader("Authorization", "Bearer " + (config.getApiKey() != null ? config.getApiKey() : ""))
                .addHeader("Content-Type", "application/json")
                .build();
            try (Response response = client.newCall(request).execute())
            {
                String raw = response.body() != null ? response.body().string() : null;
                long elapsed = System.currentTimeMillis() - start;
                if (!response.isSuccessful())
                {
                    log.warn("[Embedding] HTTP {} 耗时={}ms 响应摘要: {}", response.code(), elapsed, truncate(raw, 400));
                    return null;
                }
                JSONArray emb = parseEmbeddingVector(raw);
                if (emb == null || emb.isEmpty())
                {
                    log.warn("[Embedding] 解析为空 耗时={}ms 响应摘要: {}", elapsed, truncate(raw, 400));
                    return null;
                }
                log.debug("[Embedding] 成功 维={} 耗时={}ms", emb.size(), elapsed);
                return emb.toJSONString();
            }
        }
        catch (Exception e)
        {
            log.error("[Embedding] 调用失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 由 Chat Completions 地址推导 OpenAI 兼容的 Embeddings 地址。
     */
    static String resolveEmbeddingsEndpoint(String chatApiUrl, String explicitEmbeddingUrl)
    {
        if (explicitEmbeddingUrl != null && !explicitEmbeddingUrl.isBlank())
        {
            return explicitEmbeddingUrl.trim();
        }
        if (chatApiUrl == null || chatApiUrl.isBlank())
        {
            return null;
        }
        String u = chatApiUrl.trim();
        int idx = u.indexOf("/v1/chat/completions");
        if (idx >= 0)
        {
            return u.substring(0, idx) + "/v1/embeddings";
        }
        idx = u.indexOf("/chat/completions");
        if (idx >= 0)
        {
            return u.substring(0, idx) + "/embeddings";
        }
        int proto = u.indexOf("//");
        int last = u.lastIndexOf('/');
        if (proto >= 0 && last > proto + 2)
        {
            return u.substring(0, last) + "/v1/embeddings";
        }
        return u.endsWith("/") ? u + "v1/embeddings" : u + "/v1/embeddings";
    }

    private static JSONArray parseEmbeddingVector(String response)
    {
        if (response == null || response.isBlank())
        {
            return null;
        }
        try
        {
            JSONObject json = JSON.parseObject(response);
            if (json.containsKey("error"))
            {
                return null;
            }
            JSONArray data = json.getJSONArray("data");
            if (data == null || data.isEmpty())
            {
                return null;
            }
            JSONObject first = data.getJSONObject(0);
            if (first == null)
            {
                return null;
            }
            return first.getJSONArray("embedding");
        }
        catch (Exception e)
        {
            return null;
        }
    }

    private String buildRequestBody(AiProviderConfig config, String prompt, int maxTokens)
    {
        JSONObject body = new JSONObject();
        body.put("model", config.getModel());
        body.put("max_tokens", maxTokens);
        if (config.getTemperature() != null)
        {
            body.put("temperature", config.getTemperature());
        }
        JSONArray messages = new JSONArray();
        JSONObject userMsg = new JSONObject();
        userMsg.put("role", "user");
        userMsg.put("content", prompt);
        messages.add(userMsg);
        body.put("messages", messages);
        return body.toJSONString();
    }

    private String parseContent(String response)
    {
        if (response == null)
        {
            return "";
        }
        try
        {
            JSONObject json = JSON.parseObject(response);
            JSONArray choices = json.getJSONArray("choices");
            if (choices != null && !choices.isEmpty())
            {
                JSONObject message = choices.getJSONObject(0).getJSONObject("message");
                if (message != null)
                {
                    return message.getString("content");
                }
            }
        }
        catch (Exception e)
        {
            // 非 JSON 响应，直接返回
            return response;
        }
        return "";
    }

    private String truncate(String s, int maxLen)
    {
        if (s == null) return "(null)";
        return s.length() <= maxLen ? s : s.substring(0, maxLen) + "...(+" + (s.length() - maxLen) + "字符)";
    }

    /**
     * URL 脱敏：隐藏 API Key 等敏感参数
     */
    private String maskApiKeyInUrl(String url)
    {
        if (url == null) return "(null)";
        if (url.contains("api_key=") || url.contains("api-key="))
        {
            int q = url.indexOf('?');
            if (q >= 0)
            {
                return url.substring(0, q) + "?***(已脱敏)";
            }
        }
        try
        {
            int slashIdx = url.indexOf("//");
            int pathStart = url.indexOf('/', slashIdx + 2);
            if (pathStart > 0)
            {
                return url.substring(0, pathStart) + "/***";
            }
            return url;
        }
        catch (Exception e)
        {
            return "(解析失败)";
        }
    }
}
