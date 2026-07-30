package com.acr.system.service;

import java.util.List;
import com.acr.system.domain.SysAiModelConfig;

/**
 * AI 大模型配置 服务层
 */
public interface ISysAiModelConfigService
{
    /**
     * 查询 AI 大模型配置
     *
     * @param modelId 模型配置ID
     * @return AI 大模型配置信息
     */
    public SysAiModelConfig selectSysAiModelConfigById(Long modelId);

    /**
     * 查询 AI 大模型配置列表
     *
     * @param sysAiModelConfig AI 大模型配置信息
     * @return AI 大模型配置集合
     */
    public List<SysAiModelConfig> selectSysAiModelConfigList(SysAiModelConfig sysAiModelConfig);

    /**
     * 新增 AI 大模型配置
     *
     * @param sysAiModelConfig AI 大模型配置信息
     * @return 结果
     */
    public int insertSysAiModelConfig(SysAiModelConfig sysAiModelConfig);

    /**
     * 修改 AI 大模型配置
     *
     * @param sysAiModelConfig AI 大模型配置信息
     * @return 结果
     */
    public int updateSysAiModelConfig(SysAiModelConfig sysAiModelConfig);

    /**
     * 批量删除 AI 大模型配置信息
     *
     * @param modelIds 需要删除的模型配置ID
     */
    public void deleteSysAiModelConfigByIds(Long[] modelIds);

    /**
     * 启用/禁用模型
     *
     * @param modelId 模型配置ID
     * @param enabled 是否启用(0否 1是)
     * @return 结果
     */
    public int enableModel(Long modelId, String enabled);

    /**
     * 设为默认模型
     *
     * @param modelId 模型配置ID
     * @return 结果
     */
    public int setDefaultModel(Long modelId);

    /**
     * 测试大模型连接
     *
     * @param sysAiModelConfig 模型配置信息
     * @return 测试结果
     */
    public String testConnection(SysAiModelConfig sysAiModelConfig);
}
