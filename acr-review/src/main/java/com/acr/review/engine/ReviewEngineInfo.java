package com.acr.review.engine;

import java.time.LocalDateTime;

/** 审查引擎展示信息。 */
public class ReviewEngineInfo
{
    private String engineName;
    private String engineType;
    private String executablePath;
    private String configSource;
    private String version;
    private String availabilityStatus;
    private LocalDateTime lastDetectTime;
    private String lastDetectMessage;
    private boolean lastDetectSuccess;
    private LocalDateTime lastTestTime;
    private String lastTestMessage;
    private boolean lastTestSuccess;
    private int defaultTimeoutSeconds;
    private int maxConcurrency;
    private int maxOutputBytes;
    private String workspaceRoot;

    public String getEngineName()
    {
        return engineName;
    }

    public void setEngineName(String engineName)
    {
        this.engineName = engineName;
    }

    public String getEngineType()
    {
        return engineType;
    }

    public void setEngineType(String engineType)
    {
        this.engineType = engineType;
    }

    public String getExecutablePath()
    {
        return executablePath;
    }

    public void setExecutablePath(String executablePath)
    {
        this.executablePath = executablePath;
    }

    public String getConfigSource()
    {
        return configSource;
    }

    public void setConfigSource(String configSource)
    {
        this.configSource = configSource;
    }

    public String getVersion()
    {
        return version;
    }

    public void setVersion(String version)
    {
        this.version = version;
    }

    public String getAvailabilityStatus()
    {
        return availabilityStatus;
    }

    public void setAvailabilityStatus(String availabilityStatus)
    {
        this.availabilityStatus = availabilityStatus;
    }

    public LocalDateTime getLastDetectTime()
    {
        return lastDetectTime;
    }

    public void setLastDetectTime(LocalDateTime lastDetectTime)
    {
        this.lastDetectTime = lastDetectTime;
    }

    public String getLastDetectMessage()
    {
        return lastDetectMessage;
    }

    public void setLastDetectMessage(String lastDetectMessage)
    {
        this.lastDetectMessage = lastDetectMessage;
    }

    public boolean isLastDetectSuccess()
    {
        return lastDetectSuccess;
    }

    public void setLastDetectSuccess(boolean lastDetectSuccess)
    {
        this.lastDetectSuccess = lastDetectSuccess;
    }

    public LocalDateTime getLastTestTime()
    {
        return lastTestTime;
    }

    public void setLastTestTime(LocalDateTime lastTestTime)
    {
        this.lastTestTime = lastTestTime;
    }

    public String getLastTestMessage()
    {
        return lastTestMessage;
    }

    public void setLastTestMessage(String lastTestMessage)
    {
        this.lastTestMessage = lastTestMessage;
    }

    public boolean isLastTestSuccess()
    {
        return lastTestSuccess;
    }

    public void setLastTestSuccess(boolean lastTestSuccess)
    {
        this.lastTestSuccess = lastTestSuccess;
    }

    public int getDefaultTimeoutSeconds()
    {
        return defaultTimeoutSeconds;
    }

    public void setDefaultTimeoutSeconds(int defaultTimeoutSeconds)
    {
        this.defaultTimeoutSeconds = defaultTimeoutSeconds;
    }

    public int getMaxConcurrency()
    {
        return maxConcurrency;
    }

    public void setMaxConcurrency(int maxConcurrency)
    {
        this.maxConcurrency = maxConcurrency;
    }

    public int getMaxOutputBytes()
    {
        return maxOutputBytes;
    }

    public void setMaxOutputBytes(int maxOutputBytes)
    {
        this.maxOutputBytes = maxOutputBytes;
    }

    public String getWorkspaceRoot()
    {
        return workspaceRoot;
    }

    public void setWorkspaceRoot(String workspaceRoot)
    {
        this.workspaceRoot = workspaceRoot;
    }
}
