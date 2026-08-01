package com.acr.system.service;

import java.util.List;
import com.acr.common.ai.LlmCallResult;
import com.acr.system.domain.SysAiModelConfig;

/**
 * 大模型配置 服务层
 */
public interface ISysAiModelConfigService
{
    SysAiModelConfig selectSysAiModelConfigById(Long modelId);

    List<SysAiModelConfig> selectSysAiModelConfigList(SysAiModelConfig sysAiModelConfig);

    int insertSysAiModelConfig(SysAiModelConfig sysAiModelConfig);

    int updateSysAiModelConfig(SysAiModelConfig sysAiModelConfig);

    void deleteSysAiModelConfigByIds(Long[] modelIds);

    int enableModel(Long modelId, String enabled);

    int setDefaultModel(Long modelId);

    LlmCallResult testConnection(SysAiModelConfig sysAiModelConfig);

    LlmCallResult testModelCall(SysAiModelConfig sysAiModelConfig);

    SysAiModelConfig selectRuntimeConfigById(Long modelId);

    SysAiModelConfig selectDefaultRuntimeConfig();
}
