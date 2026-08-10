package com.acr.system.service.impl;

import java.util.Date;
import org.springframework.stereotype.Service;
import com.acr.common.ai.LlmCallResult;
import com.acr.common.ai.LlmCallService;
import com.acr.common.ai.LlmErrorClassifier;
import com.acr.common.enums.LlmCallErrorType;
import com.acr.common.exception.ServiceException;
import com.acr.common.security.ApiKeyMaskUtils;
import com.acr.common.security.LlmApiKeyCryptoService;
import com.acr.common.utils.StringUtils;
import com.acr.common.utils.http.HttpPostResult;
import com.acr.common.utils.http.OkHttpUtils;
import com.acr.system.domain.SysAiModelConfig;
import com.acr.system.mapper.SysAiModelConfigMapper;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;

/**
 * 统一 LLM 调用服务实现。
 */
@Service
public class LlmCallServiceImpl implements LlmCallService
{
    private static final String CONNECTION_PROMPT = "Hi, respond with OK";
    private static final String MODEL_CALL_PROMPT = "Reply with exactly: OK";

    private final SysAiModelConfigMapper aiModelConfigMapper;
    private final LlmApiKeyCryptoService apiKeyCryptoService;
    private final LlmEndpointValidator endpointValidator;

    public LlmCallServiceImpl(SysAiModelConfigMapper aiModelConfigMapper,
        LlmApiKeyCryptoService apiKeyCryptoService, LlmEndpointValidator endpointValidator)
    {
        this.aiModelConfigMapper = aiModelConfigMapper;
        this.apiKeyCryptoService = apiKeyCryptoService;
        this.endpointValidator = endpointValidator;
    }

    @Override
    public LlmCallResult chat(Long modelConfigId, String prompt)
    {
        SysAiModelConfig config = requireRuntimeConfig(modelConfigId);
        return invokeChat(config, prompt, config.getMaxTokens(), config.getTemperature(), null);
    }

    @Override
    public LlmCallResult chat(Long modelConfigId, String prompt, int timeoutMs)
    {
        SysAiModelConfig config = requireRuntimeConfig(modelConfigId);
        return invokeChat(config, prompt, config.getMaxTokens(), config.getTemperature(), Math.max(1, timeoutMs));
    }

    @Override
    public LlmCallResult chatWithDefault(String prompt)
    {
        SysAiModelConfig query = new SysAiModelConfig();
        query.setEnabled("1");
        SysAiModelConfig config = aiModelConfigMapper.selectSysAiModelConfigList(query).stream()
            .filter(item -> "1".equals(item.getIsDefault()))
            .findFirst()
            .orElse(null);
        if (config == null)
        {
            return LlmCallResult.failure(LlmCallErrorType.UNKNOWN, "未找到可用默认模型", 0, null);
        }
        return invokeChat(config, prompt, config.getMaxTokens(), config.getTemperature(), null);
    }

    @Override
    public LlmCallResult testConnection(Long modelConfigId, String apiUrl, String apiKey, String model, Integer timeout)
    {
        SysAiModelConfig config = buildTransientConfig(modelConfigId, apiUrl, apiKey, model, timeout, 10, null);
        LlmCallResult result = invokeHttp(config, CONNECTION_PROMPT, 10, null, true, true, null);
        persistLastCheck(modelConfigId, result);
        return result;
    }

    @Override
    public LlmCallResult testModelCall(Long modelConfigId, String apiUrl, String apiKey, String model,
        Integer timeout, Integer maxTokens, Double temperature)
    {
        int tokens = maxTokens != null && maxTokens > 0 ? maxTokens : 64;
        SysAiModelConfig config = buildTransientConfig(modelConfigId, apiUrl, apiKey, model, timeout, tokens, temperature);
        LlmCallResult result = invokeHttp(config, MODEL_CALL_PROMPT, tokens, temperature, false, true, null);
        persistLastCheck(modelConfigId, result);
        return result;
    }

    private LlmCallResult invokeChat(SysAiModelConfig config, String prompt, Integer maxTokens, Double temperature,
        Integer callTimeoutMs)
    {
        int tokens = maxTokens != null && maxTokens > 0 ? maxTokens : 4096;
        return invokeHttp(config, prompt, tokens, temperature, false, false, callTimeoutMs);
    }

    private LlmCallResult invokeHttp(SysAiModelConfig config, String prompt, int maxTokens, Double temperature,
        boolean connectionOnly, boolean allowPlainApiKey, Integer callTimeoutMs)
    {
        if (StringUtils.isEmpty(config.getApiUrl()))
        {
            return LlmCallResult.failure(LlmCallErrorType.ADDRESS_ERROR, "模型地址不能为空", 0, null);
        }
        try
        {
            endpointValidator.validate(config.getApiUrl());
        }
        catch (ServiceException e)
        {
            return LlmCallResult.failure(LlmCallErrorType.ADDRESS_ERROR, e.getMessage(), 0, null);
        }
        String apiKey = resolvePlainApiKey(config, allowPlainApiKey);
        if (StringUtils.isEmpty(apiKey))
        {
            return LlmCallResult.failure(LlmCallErrorType.AUTH, "API Key 未配置", 0, null);
        }
        if (StringUtils.isEmpty(config.getModel()))
        {
            return LlmCallResult.failure(LlmCallErrorType.MODEL_NOT_FOUND, "模型标识不能为空", 0, null);
        }
        String model = config.getModel();
        int configuredTimeout = config.getTimeout() != null && config.getTimeout() > 0 ? config.getTimeout() : 60000;
        int timeout = callTimeoutMs != null && callTimeoutMs > 0
            ? Math.min(configuredTimeout, callTimeoutMs) : configuredTimeout;
        String body = buildRequestBody(model, prompt, maxTokens, temperature);
        HttpPostResult http = OkHttpUtils.postJsonDetailed(config.getApiUrl(), apiKey, body, timeout);
        if (http.getException() != null)
        {
            LlmCallErrorType errorType = LlmErrorClassifier.classify(0, null, http.getException());
            return LlmCallResult.failure(errorType, errorType.getLabel(), http.getLatencyMs(), null);
        }
        if (!http.isSuccessful())
        {
            LlmCallErrorType errorType = LlmErrorClassifier.classify(http.getStatusCode(), http.getBody(), null);
            String message = LlmErrorClassifier.message(errorType, http.getStatusCode(), null);
            return LlmCallResult.failure(errorType, message, http.getLatencyMs(), null);
        }
        String content = parseContent(http.getBody());
        if (connectionOnly)
        {
            if (StringUtils.isNotEmpty(content))
            {
                return LlmCallResult.success(http.getLatencyMs(), content, LlmErrorClassifier.snippet(http.getBody(), 120));
            }
            return LlmCallResult.failure(LlmCallErrorType.UNKNOWN, "连接失败: 响应格式不正确", http.getLatencyMs(),
                LlmErrorClassifier.snippet(http.getBody(), 120));
        }
        if (StringUtils.isEmpty(content))
        {
            return LlmCallResult.failure(LlmCallErrorType.UNKNOWN, "模型返回内容为空", http.getLatencyMs(),
                LlmErrorClassifier.snippet(http.getBody(), 120));
        }
        return LlmCallResult.success(http.getLatencyMs(), content, LlmErrorClassifier.snippet(http.getBody(), 120));
    }

    private SysAiModelConfig buildTransientConfig(Long modelConfigId, String apiUrl, String apiKey, String model,
        Integer timeout, int maxTokens, Double temperature)
    {
        SysAiModelConfig config = new SysAiModelConfig();
        config.setModelId(modelConfigId);
        config.setApiUrl(apiUrl);
        config.setApiKey(apiKey);
        config.setModel(model);
        config.setTimeout(timeout);
        config.setMaxTokens(maxTokens);
        config.setTemperature(temperature);
        if (modelConfigId != null && (StringUtils.isEmpty(apiUrl) || ApiKeyMaskUtils.isMaskedOrBlank(apiKey)
            || StringUtils.isEmpty(model) || timeout == null || temperature == null))
        {
            SysAiModelConfig stored = aiModelConfigMapper.selectSysAiModelConfigById(modelConfigId);
            if (stored == null)
            {
                throw new IllegalArgumentException("模型配置不存在: " + modelConfigId);
            }
            if (ApiKeyMaskUtils.isMaskedOrBlank(apiKey))
            {
                if (!apiKeyCryptoService.isEncrypted(stored.getApiKey()))
                {
                    throw new IllegalStateException("模型 API Key 尚未加密，请重新保存配置");
                }
                // 使用数据库密钥时必须同时绑定数据库中的目标，防止将密钥转发到调用方指定地址。
                config.setApiKey(stored.getApiKey());
                config.setApiUrl(stored.getApiUrl());
                config.setModel(stored.getModel());
            }
            else
            {
                if (StringUtils.isEmpty(config.getApiUrl()))
                {
                    config.setApiUrl(stored.getApiUrl());
                }
                if (StringUtils.isEmpty(config.getModel()))
                {
                    config.setModel(stored.getModel());
                }
            }
            if (config.getTimeout() == null || config.getTimeout() <= 0)
            {
                config.setTimeout(stored.getTimeout());
            }
            if (config.getMaxTokens() == null)
            {
                config.setMaxTokens(stored.getMaxTokens());
            }
            if (config.getTemperature() == null)
            {
                config.setTemperature(stored.getTemperature());
            }
            config.setProvider(stored.getProvider());
        }
        if (config.getTimeout() == null)
        {
            config.setTimeout(60000);
        }
        return config;
    }

    private SysAiModelConfig requireRuntimeConfig(Long modelConfigId)
    {
        if (modelConfigId == null)
        {
            throw new IllegalArgumentException("modelConfigId 不能为空");
        }
        SysAiModelConfig config = aiModelConfigMapper.selectSysAiModelConfigById(modelConfigId);
        if (config == null)
        {
            throw new IllegalArgumentException("模型配置不存在: " + modelConfigId);
        }
        if (!"1".equals(config.getEnabled()))
        {
            throw new IllegalArgumentException("模型配置未启用: " + modelConfigId);
        }
        return config;
    }

    private String resolvePlainApiKey(SysAiModelConfig config, boolean allowPlaintext)
    {
        String apiKey = config.getApiKey();
        if (StringUtils.isEmpty(apiKey))
        {
            return null;
        }
        if (apiKeyCryptoService.isEncrypted(apiKey))
        {
            return apiKeyCryptoService.decrypt(apiKey);
        }
        if (allowPlaintext)
        {
            return apiKey;
        }
        throw new IllegalStateException("模型 API Key 尚未加密，请重新保存配置");
    }

    private void persistLastCheck(Long modelConfigId, LlmCallResult result)
    {
        if (modelConfigId == null)
        {
            return;
        }
        SysAiModelConfig patch = new SysAiModelConfig();
        patch.setModelId(modelConfigId);
        patch.setLastCheckTime(new Date());
        if (result.isSuccess())
        {
            patch.setLastCheckResult("成功 (" + result.getLatencyMs() + "ms)");
        }
        else
        {
            String label = result.getErrorType() != null ? result.getErrorType().getLabel() : "失败";
            patch.setLastCheckResult(label + ": " + (result.getErrorMessage() != null ? result.getErrorMessage() : ""));
        }
        aiModelConfigMapper.updateLastCheck(patch);
    }

    private static String buildRequestBody(String model, String prompt, int maxTokens, Double temperature)
    {
        JSONObject body = new JSONObject();
        body.put("model", model);
        body.put("max_tokens", maxTokens);
        if (temperature != null)
        {
            body.put("temperature", temperature);
        }
        JSONArray messages = new JSONArray();
        JSONObject userMsg = new JSONObject();
        userMsg.put("role", "user");
        userMsg.put("content", prompt);
        messages.add(userMsg);
        body.put("messages", messages);
        return body.toJSONString();
    }

    private static String parseContent(String response)
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
        catch (Exception ignored)
        {
            return "";
        }
        return "";
    }
}
