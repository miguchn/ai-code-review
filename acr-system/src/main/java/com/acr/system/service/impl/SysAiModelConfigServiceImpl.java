package com.acr.system.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.acr.common.ai.AiClient;
import com.acr.common.ai.AiClientFactory;
import com.acr.common.ai.AiProviderConfig;
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
    @Autowired
    private SysAiModelConfigMapper aiModelConfigMapper;

    /**
     * 查询 AI 大模型配置
     */
    @Override
    public SysAiModelConfig selectSysAiModelConfigById(Long modelId)
    {
        SysAiModelConfig config = aiModelConfigMapper.selectSysAiModelConfigById(modelId);
        if (StringUtils.isNotNull(config))
        {
            config.setApiKey(maskApiKey(config.getApiKey()));
        }
        return config;
    }

    /**
     * 查询 AI 大模型配置列表
     */
    @Override
    public List<SysAiModelConfig> selectSysAiModelConfigList(SysAiModelConfig sysAiModelConfig)
    {
        List<SysAiModelConfig> list = aiModelConfigMapper.selectSysAiModelConfigList(sysAiModelConfig);
        for (SysAiModelConfig config : list)
        {
            config.setApiKey(maskApiKey(config.getApiKey()));
        }
        return list;
    }

    /**
     * 新增 AI 大模型配置
     */
    @Override
    public int insertSysAiModelConfig(SysAiModelConfig sysAiModelConfig)
    {
        if ("1".equals(sysAiModelConfig.getIsDefault()))
        {
            aiModelConfigMapper.clearDefaultModel();
        }
        if (StringUtils.isEmpty(sysAiModelConfig.getEnabled()))
        {
            sysAiModelConfig.setEnabled("0");
        }
        return aiModelConfigMapper.insertSysAiModelConfig(sysAiModelConfig);
    }

    /**
     * 修改 AI 大模型配置
     */
    @Override
    public int updateSysAiModelConfig(SysAiModelConfig sysAiModelConfig)
    {
        if ("1".equals(sysAiModelConfig.getIsDefault()))
        {
            SysAiModelConfig existing = aiModelConfigMapper.selectSysAiModelConfigById(sysAiModelConfig.getModelId());
            if (existing != null && !"1".equals(existing.getIsDefault()))
            {
                aiModelConfigMapper.clearDefaultModel();
            }
        }
        return aiModelConfigMapper.updateSysAiModelConfig(sysAiModelConfig);
    }

    /**
     * 批量删除 AI 大模型配置
     */
    @Override
    public void deleteSysAiModelConfigByIds(Long[] modelIds)
    {
        aiModelConfigMapper.deleteSysAiModelConfigByIds(modelIds);
    }

    /**
     * 启用/禁用模型
     */
    @Override
    public int enableModel(Long modelId, String enabled)
    {
        SysAiModelConfig config = new SysAiModelConfig();
        config.setModelId(modelId);
        config.setEnabled(enabled);
        return aiModelConfigMapper.updateSysAiModelConfig(config);
    }

    /**
     * 设为默认模型
     */
    @Override
    public int setDefaultModel(Long modelId)
    {
        aiModelConfigMapper.clearDefaultModel();
        SysAiModelConfig config = new SysAiModelConfig();
        config.setModelId(modelId);
        config.setIsDefault("1");
        return aiModelConfigMapper.updateSysAiModelConfig(config);
    }

    /**
     * 脱敏 API Key，仅展示前后各4位
     */
    private String maskApiKey(String apiKey)
    {
        if (StringUtils.isEmpty(apiKey) || apiKey.length() <= 8)
        {
            return apiKey;
        }
        return apiKey.substring(0, 4) + "****" + apiKey.substring(apiKey.length() - 4);
    }

    /**
     * 测试大模型连接
     */
    @Override
    public String testConnection(SysAiModelConfig sysAiModelConfig)
    {
        AiProviderConfig config = toConfig(sysAiModelConfig);
        AiClient client = AiClientFactory.getClient(sysAiModelConfig.getProvider());
        return client.testConnection(config);
    }

    private AiProviderConfig toConfig(SysAiModelConfig config)
    {
        AiProviderConfig aiConfig = new AiProviderConfig();
        aiConfig.setProviderName(config.getProvider());
        aiConfig.setProviderCode(config.getProvider());
        aiConfig.setApiUrl(config.getApiUrl());
        aiConfig.setApiKey(config.getApiKey());
        aiConfig.setModel(config.getModel());
        aiConfig.setEmbeddingModel(config.getEmbeddingModel());
        aiConfig.setEmbeddingApiUrl(config.getEmbeddingApiUrl());
        aiConfig.setTimeout(config.getTimeout() != null ? config.getTimeout() : 60000);
        aiConfig.setMaxTokens(config.getMaxTokens() != null ? config.getMaxTokens() : 8000);
        return aiConfig;
    }
}
