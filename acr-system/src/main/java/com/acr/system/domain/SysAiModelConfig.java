package com.acr.system.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.acr.common.annotation.Excel;
import com.acr.common.core.domain.BaseEntity;

/**
 * AI 大模型配置表 sys_ai_model_config
 */
public class SysAiModelConfig extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 模型配置ID */
    @Excel(name = "模型配置ID", cellType = Excel.ColumnType.NUMERIC)
    private Long modelId;

    /** 模型名称 */
    @Excel(name = "模型名称")
    private String modelName;

    /** 模型厂商 */
    @Excel(name = "模型厂商")
    private String provider;

    /** 模型地址 */
    @Excel(name = "模型地址")
    private String apiUrl;

    /** API Key */
    private String apiKey;

    /** Model 名称 */
    @Excel(name = "Model")
    private String model;

    /** Embedding 模型（OpenAI 兼容 /v1/embeddings） */
    @Excel(name = "Embedding模型")
    private String embeddingModel;

    /** Embedding 接口 URL，空则从模型地址推导 */
    @Excel(name = "Embedding地址")
    private String embeddingApiUrl;

    /** 是否启用(0否 1是) */
    @Excel(name = "是否启用", readConverterExp = "0=否,1=是")
    private String enabled;

    /** 是否默认模型(0否 1是) */
    @Excel(name = "是否默认", readConverterExp = "0=否,1=是")
    private String isDefault;

    /** 超时时间(ms) */
    @Excel(name = "超时时间(ms)")
    private Integer timeout;

    /** 最大 Token 数 */
    @Excel(name = "最大 Token")
    private Integer maxTokens;

    /** 排序 */
    @Excel(name = "排序")
    private Integer sortOrder;

    /** Temperature */
    private Double temperature;

    /** 上下文长度 */
    private Integer contextLength;

    /** 最近检测结果 */
    private String lastCheckResult;

    /** 最近检测时间 */
    private java.util.Date lastCheckTime;

    public Long getModelId()
    {
        return modelId;
    }

    public void setModelId(Long modelId)
    {
        this.modelId = modelId;
    }

    @NotBlank(message = "模型名称不能为空")
    @Size(max = 64, message = "模型名称不能超过64个字符")
    public String getModelName()
    {
        return modelName;
    }

    public void setModelName(String modelName)
    {
        this.modelName = modelName;
    }

    @NotBlank(message = "模型厂商不能为空")
    @Size(max = 32, message = "模型厂商不能超过32个字符")
    public String getProvider()
    {
        return provider;
    }

    public void setProvider(String provider)
    {
        this.provider = provider;
    }

    @NotBlank(message = "模型地址不能为空")
    @Size(max = 500, message = "模型地址不能超过500个字符")
    public String getApiUrl()
    {
        return apiUrl;
    }

    public void setApiUrl(String apiUrl)
    {
        this.apiUrl = apiUrl;
    }

    public String getApiKey()
    {
        return apiKey;
    }

    public void setApiKey(String apiKey)
    {
        this.apiKey = apiKey;
    }

    @NotBlank(message = "模型标识不能为空")
    @Size(max = 64, message = "模型标识不能超过64个字符")
    public String getModel()
    {
        return model;
    }

    public void setModel(String model)
    {
        this.model = model;
    }

    public String getEmbeddingModel()
    {
        return embeddingModel;
    }

    public void setEmbeddingModel(String embeddingModel)
    {
        this.embeddingModel = embeddingModel;
    }

    public String getEmbeddingApiUrl()
    {
        return embeddingApiUrl;
    }

    public void setEmbeddingApiUrl(String embeddingApiUrl)
    {
        this.embeddingApiUrl = embeddingApiUrl;
    }

    public String getEnabled()
    {
        return enabled;
    }

    public void setEnabled(String enabled)
    {
        this.enabled = enabled;
    }

    public String getIsDefault()
    {
        return isDefault;
    }

    public void setIsDefault(String isDefault)
    {
        this.isDefault = isDefault;
    }

    public Integer getTimeout()
    {
        return timeout;
    }

    public void setTimeout(Integer timeout)
    {
        this.timeout = timeout;
    }

    public Integer getMaxTokens()
    {
        return maxTokens;
    }

    public void setMaxTokens(Integer maxTokens)
    {
        this.maxTokens = maxTokens;
    }

    public Integer getSortOrder()
    {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder)
    {
        this.sortOrder = sortOrder;
    }

    public Double getTemperature()
    {
        return temperature;
    }

    public void setTemperature(Double temperature)
    {
        this.temperature = temperature;
    }

    public Integer getContextLength()
    {
        return contextLength;
    }

    public void setContextLength(Integer contextLength)
    {
        this.contextLength = contextLength;
    }

    public String getLastCheckResult()
    {
        return lastCheckResult;
    }

    public void setLastCheckResult(String lastCheckResult)
    {
        this.lastCheckResult = lastCheckResult;
    }

    public java.util.Date getLastCheckTime()
    {
        return lastCheckTime;
    }

    public void setLastCheckTime(java.util.Date lastCheckTime)
    {
        this.lastCheckTime = lastCheckTime;
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("modelId", getModelId())
            .append("modelName", getModelName())
            .append("provider", getProvider())
            .append("apiUrl", getApiUrl())
            .append("model", getModel())
            .append("embeddingModel", getEmbeddingModel())
            .append("embeddingApiUrl", getEmbeddingApiUrl())
            .append("enabled", getEnabled())
            .append("isDefault", getIsDefault())
            .append("timeout", getTimeout())
            .append("maxTokens", getMaxTokens())
            .append("sortOrder", getSortOrder())
            .append("temperature", getTemperature())
            .append("contextLength", getContextLength())
            .append("lastCheckResult", getLastCheckResult())
            .append("lastCheckTime", getLastCheckTime())
            .append("remark", getRemark())
            .toString();
    }
}
