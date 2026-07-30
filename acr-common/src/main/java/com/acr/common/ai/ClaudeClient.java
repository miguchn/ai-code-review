package com.acr.common.ai;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.acr.common.utils.http.OkHttpUtils;
import java.util.Map;

/**
 * Claude 客户端（Anthropic API）
 */
public class ClaudeClient implements AiClient
{
    @Override
    public String chat(AiProviderConfig config, String prompt)
    {
        try
        {
            int maxTokens = config.getMaxTokens() > 0 ? config.getMaxTokens() : 4096;
            String body = buildRequestBody(config, prompt, maxTokens);
            Map<String, String> headers = buildHeaders(config);
            String response = OkHttpUtils.postJson(config.getApiUrl(), headers, body, config.getTimeout());
            return parseContent(response);
        }
        catch (Exception e)
        {
            throw new RuntimeException("Claude AI 调用失败: " + e.getMessage(), e);
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
            Map<String, String> headers = buildHeaders(config);
            String response = OkHttpUtils.postJson(config.getApiUrl(), headers, body, config.getTimeout());
            if (response != null && response.contains("text"))
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

    private String buildRequestBody(AiProviderConfig config, String prompt, int maxTokens)
    {
        JSONObject body = new JSONObject();
        body.put("model", config.getModel());
        body.put("max_tokens", maxTokens);
        JSONArray messages = new JSONArray();
        JSONObject userMsg = new JSONObject();
        userMsg.put("role", "user");
        userMsg.put("content", prompt);
        messages.add(userMsg);
        body.put("messages", messages);
        body.put("anthropic_version", "2023-06-01");
        return body.toJSONString();
    }

    private Map<String, String> buildHeaders(AiProviderConfig config)
    {
        Map<String, String> headers = new java.util.HashMap<>();
        headers.put("x-api-key", config.getApiKey());
        headers.put("anthropic-version", "2023-06-01");
        headers.put("content-type", "application/json");
        return headers;
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
            JSONArray content = json.getJSONArray("content");
            if (content != null && !content.isEmpty())
            {
                JSONObject first = content.getJSONObject(0);
                return first.getString("text");
            }
        }
        catch (Exception e)
        {
            return response;
        }
        return "";
    }
}
