package com.acr.system.mapper;

import java.util.List;
import com.acr.system.domain.SysAiModelConfig;

/**
 * AI 大模型配置 数据层
 */
public interface SysAiModelConfigMapper
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
     * 删除 AI 大模型配置
     *
     * @param modelId 模型配置ID
     * @return 结果
     */
    public int deleteSysAiModelConfigById(Long modelId);

    /**
     * 批量删除 AI 大模型配置
     *
     * @param modelIds 需要删除的模型配置ID
     * @return 结果
     */
    public int deleteSysAiModelConfigByIds(Long[] modelIds);

    /**
     * 取消所有默认模型
     *
     * @return 结果
     */
    public int clearDefaultModel();
}
