package com.acr.review.domain;

/** 工作台模型健康条目（白名单字段，不含 apiUrl/apiKey 等配置细节）。 */
public class WorkbenchModelItem
{
    private String modelName;
    private String provider;
    private String providerLabel;
    private String model;
    private boolean isDefault;
    /** SUCCESS / FAILED / NEVER，见 WorkbenchConstants.CHECK_STATUS_*。 */
    private String checkStatus;
    private String lastCheckResult;
    private String lastCheckTime;

    public String getModelName()
    {
        return modelName;
    }

    public void setModelName(String modelName)
    {
        this.modelName = modelName;
    }

    public String getProvider()
    {
        return provider;
    }

    public void setProvider(String provider)
    {
        this.provider = provider;
    }

    public String getProviderLabel()
    {
        return providerLabel;
    }

    public void setProviderLabel(String providerLabel)
    {
        this.providerLabel = providerLabel;
    }

    public String getModel()
    {
        return model;
    }

    public void setModel(String model)
    {
        this.model = model;
    }

    public boolean getIsDefault()
    {
        return isDefault;
    }

    public void setIsDefault(boolean isDefault)
    {
        this.isDefault = isDefault;
    }

    public String getCheckStatus()
    {
        return checkStatus;
    }

    public void setCheckStatus(String checkStatus)
    {
        this.checkStatus = checkStatus;
    }

    public String getLastCheckResult()
    {
        return lastCheckResult;
    }

    public void setLastCheckResult(String lastCheckResult)
    {
        this.lastCheckResult = lastCheckResult;
    }

    public String getLastCheckTime()
    {
        return lastCheckTime;
    }

    public void setLastCheckTime(String lastCheckTime)
    {
        this.lastCheckTime = lastCheckTime;
    }
}
