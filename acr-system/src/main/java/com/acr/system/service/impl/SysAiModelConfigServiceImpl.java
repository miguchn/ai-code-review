package com.acr.system.service.impl;

import java.util.List;
import java.util.Objects;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.acr.common.ai.LlmCallResult;
import com.acr.common.ai.LlmCallService;
import com.acr.common.enums.LlmProviderCode;
import com.acr.common.exception.ServiceException;
import com.acr.common.security.ApiKeyMaskUtils;
import com.acr.common.security.LlmApiKeyCryptoService;
import com.acr.common.utils.StringUtils;
import com.acr.system.domain.SysAiModelConfig;
import com.acr.system.mapper.SysAiModelConfigMapper;
import com.acr.system.service.ISysAiModelConfigService;

/**
 * AI 大模型配置 服务层实现
 */
@Service
public class SysAiModelConfigServiceImpl implements ISysAiModelConfigService
{
    private final SysAiModelConfigMapper aiModelConfigMapper;
    private final LlmApiKeyCryptoService apiKeyCryptoService;
    private final LlmCallService llmCallService;

    public SysAiModelConfigServiceImpl(SysAiModelConfigMapper aiModelConfigMapper,
        LlmApiKeyCryptoService apiKeyCryptoService, LlmCallService llmCallService)
    {
        this.aiModelConfigMapper = aiModelConfigMapper;
        this.apiKeyCryptoService = apiKeyCryptoService;
        this.llmCallService = llmCallService;
    }

    /**
     * 升级旧版本启动时将历史明文 API Key 原地加密；失败时阻止应用进入就绪状态。
     */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void encryptLegacyApiKeys()
    {
        List<SysAiModelConfig> configs = aiModelConfigMapper.selectSysAiModelConfigList(new SysAiModelConfig());
        for (SysAiModelConfig config : configs)
        {
            if (StringUtils.isEmpty(config.getApiKey()))
            {
                continue;
            }
            if (apiKeyCryptoService.isEncrypted(config.getApiKey()))
            {
                apiKeyCryptoService.decrypt(config.getApiKey());
                continue;
            }
            SysAiModelConfig patch = new SysAiModelConfig();
            patch.setModelId(config.getModelId());
            patch.setApiKey(apiKeyCryptoService.encrypt(config.getApiKey()));
            aiModelConfigMapper.updateSysAiModelConfig(patch);
        }
    }

    @Override
    public SysAiModelConfig selectSysAiModelConfigById(Long modelId)
    {
        SysAiModelConfig config = aiModelConfigMapper.selectSysAiModelConfigById(modelId);
        return maskForResponse(config);
    }

    @Override
    public List<SysAiModelConfig> selectSysAiModelConfigList(SysAiModelConfig sysAiModelConfig)
    {
        List<SysAiModelConfig> list = aiModelConfigMapper.selectSysAiModelConfigList(sysAiModelConfig);
        for (SysAiModelConfig config : list)
        {
            maskForResponse(config);
        }
        return list;
    }

    @Override
    @Transactional
    public int insertSysAiModelConfig(SysAiModelConfig sysAiModelConfig)
    {
        validateProvider(sysAiModelConfig.getProvider());
        prepareForPersist(sysAiModelConfig, true);
        if (StringUtils.isEmpty(sysAiModelConfig.getEnabled()))
        {
            sysAiModelConfig.setEnabled("0");
        }
        validateDefaultEnabled(sysAiModelConfig);
        if ("1".equals(sysAiModelConfig.getIsDefault()))
        {
            aiModelConfigMapper.clearDefaultModel();
        }
        return aiModelConfigMapper.insertSysAiModelConfig(sysAiModelConfig);
    }

    @Override
    @Transactional
    public int updateSysAiModelConfig(SysAiModelConfig sysAiModelConfig)
    {
        SysAiModelConfig existing = aiModelConfigMapper.selectSysAiModelConfigById(sysAiModelConfig.getModelId());
        if (existing == null)
        {
            throw new ServiceException("模型配置不存在");
        }
        validateProvider(sysAiModelConfig.getProvider());
        boolean keepsStoredKey = ApiKeyMaskUtils.isMaskedOrBlank(sysAiModelConfig.getApiKey());
        if (keepsStoredKey && (!sameText(existing.getApiUrl(), sysAiModelConfig.getApiUrl())
            || !sameText(existing.getProvider(), sysAiModelConfig.getProvider())))
        {
            throw new ServiceException("修改服务厂商或模型地址时必须重新填写 API Key");
        }
        String requestedEnabled = sysAiModelConfig.getEnabled();
        String requestedDefault = sysAiModelConfig.getIsDefault();
        if ("1".equals(existing.getIsDefault())
            && (("0".equals(requestedEnabled)) || "0".equals(requestedDefault)))
        {
            throw new ServiceException("不能直接禁用或取消当前默认模型，请先切换默认模型");
        }
        String effectiveEnabled = requestedEnabled != null ? requestedEnabled : existing.getEnabled();
        if ("1".equals(requestedDefault) && !"1".equals(effectiveEnabled))
        {
            throw new ServiceException("默认模型必须处于启用状态");
        }
        prepareForPersist(sysAiModelConfig, false);
        sysAiModelConfig.setEnabled(null);
        sysAiModelConfig.setIsDefault(null);
        int updated = aiModelConfigMapper.updateSysAiModelConfig(sysAiModelConfig);
        if (requestedEnabled != null && !requestedEnabled.equals(existing.getEnabled())
            && aiModelConfigMapper.updateEnabledStatus(sysAiModelConfig.getModelId(), requestedEnabled) == 0)
        {
            throw new ServiceException("模型状态已变化，请刷新后重试");
        }
        if ("1".equals(requestedDefault) && !"1".equals(existing.getIsDefault()))
        {
            aiModelConfigMapper.clearDefaultModel();
            if (aiModelConfigMapper.markDefaultIfEnabled(sysAiModelConfig.getModelId()) == 0)
            {
                throw new ServiceException("模型状态已变化，请刷新后重试");
            }
        }
        return updated;
    }

    @Override
    @Transactional
    public void deleteSysAiModelConfigByIds(Long[] modelIds)
    {
        int deleted = aiModelConfigMapper.deleteSysAiModelConfigByIds(modelIds);
        if (deleted != modelIds.length)
        {
            throw new ServiceException("默认模型不能删除，请先切换默认模型");
        }
    }

    @Override
    public int enableModel(Long modelId, String enabled)
    {
        if (!"0".equals(enabled) && !"1".equals(enabled))
        {
            throw new ServiceException("模型启用状态无效");
        }
        SysAiModelConfig existing = aiModelConfigMapper.selectSysAiModelConfigById(modelId);
        if (existing == null)
        {
            throw new ServiceException("模型配置不存在");
        }
        if ("0".equals(enabled) && "1".equals(existing.getIsDefault()))
        {
            throw new ServiceException("默认模型不能直接禁用，请先切换默认模型");
        }
        int updated = aiModelConfigMapper.updateEnabledStatus(modelId, enabled);
        if (updated == 0)
        {
            throw new ServiceException("模型状态已变化，请刷新后重试");
        }
        return updated;
    }

    @Override
    @Transactional
    public int setDefaultModel(Long modelId)
    {
        SysAiModelConfig existing = aiModelConfigMapper.selectSysAiModelConfigById(modelId);
        if (existing == null)
        {
            throw new ServiceException("模型配置不存在");
        }
        if (!"1".equals(existing.getEnabled()))
        {
            throw new ServiceException("禁用的模型不能设为默认模型");
        }
        aiModelConfigMapper.clearDefaultModel();
        int updated = aiModelConfigMapper.markDefaultIfEnabled(modelId);
        if (updated == 0)
        {
            throw new ServiceException("模型状态已变化，请刷新后重试");
        }
        return updated;
    }

    @Override
    public LlmCallResult testConnection(SysAiModelConfig sysAiModelConfig)
    {
        return llmCallService.testConnection(
            sysAiModelConfig.getModelId(),
            sysAiModelConfig.getApiUrl(),
            sysAiModelConfig.getApiKey(),
            sysAiModelConfig.getModel(),
            sysAiModelConfig.getTimeout());
    }

    @Override
    public LlmCallResult testModelCall(SysAiModelConfig sysAiModelConfig)
    {
        return llmCallService.testModelCall(
            sysAiModelConfig.getModelId(),
            sysAiModelConfig.getApiUrl(),
            sysAiModelConfig.getApiKey(),
            sysAiModelConfig.getModel(),
            sysAiModelConfig.getTimeout(),
            sysAiModelConfig.getMaxTokens(),
            sysAiModelConfig.getTemperature());
    }

    @Override
    public SysAiModelConfig selectRuntimeConfigById(Long modelId)
    {
        return decryptRuntimeConfig(aiModelConfigMapper.selectSysAiModelConfigById(modelId));
    }

    @Override
    public SysAiModelConfig selectDefaultRuntimeConfig()
    {
        SysAiModelConfig query = new SysAiModelConfig();
        query.setEnabled("1");
        List<SysAiModelConfig> enabled = aiModelConfigMapper.selectSysAiModelConfigList(query);
        for (SysAiModelConfig config : enabled)
        {
            if ("1".equals(config.getIsDefault()))
            {
                return decryptRuntimeConfig(config);
            }
        }
        return null;
    }

    private SysAiModelConfig decryptRuntimeConfig(SysAiModelConfig config)
    {
        if (config != null && StringUtils.isNotEmpty(config.getApiKey()))
        {
            if (!apiKeyCryptoService.isEncrypted(config.getApiKey()))
            {
                throw new ServiceException("模型 API Key 尚未加密，请重新保存配置");
            }
            config.setApiKey(apiKeyCryptoService.decrypt(config.getApiKey()));
        }
        return config;
    }

    private SysAiModelConfig maskForResponse(SysAiModelConfig config)
    {
        if (StringUtils.isNotNull(config))
        {
            String apiKey = config.getApiKey();
            if (apiKeyCryptoService.isEncrypted(apiKey))
            {
                try
                {
                    apiKey = apiKeyCryptoService.decrypt(apiKey);
                }
                catch (ServiceException ignored)
                {
                    // 未配置或已轮换主密钥时，列表仍可展示不可逆的脱敏占位。
                }
            }
            config.setApiKey(ApiKeyMaskUtils.mask(apiKey));
        }
        return config;
    }

    private void validateDefaultEnabled(SysAiModelConfig config)
    {
        if ("1".equals(config.getIsDefault()) && !"1".equals(config.getEnabled()))
        {
            throw new ServiceException("默认模型必须处于启用状态");
        }
    }

    private void validateProvider(String provider)
    {
        if (!LlmProviderCode.isValid(provider))
        {
            throw new ServiceException("不支持的模型厂商: " + provider);
        }
    }

    private void prepareForPersist(SysAiModelConfig config, boolean creating)
    {
        if (creating)
        {
            if (ApiKeyMaskUtils.isMaskedOrBlank(config.getApiKey()))
            {
                throw new ServiceException("新增模型配置时必须填写 API Key");
            }
            config.setApiKey(apiKeyCryptoService.encrypt(config.getApiKey()));
            return;
        }
        if (ApiKeyMaskUtils.isMaskedOrBlank(config.getApiKey()))
        {
            config.setApiKey(null);
            return;
        }
        config.setApiKey(apiKeyCryptoService.encrypt(config.getApiKey()));
    }

    private boolean sameText(String left, String right)
    {
        String normalizedLeft = left == null ? "" : left.trim();
        String normalizedRight = right == null ? "" : right.trim();
        return Objects.equals(normalizedLeft, normalizedRight);
    }
}
