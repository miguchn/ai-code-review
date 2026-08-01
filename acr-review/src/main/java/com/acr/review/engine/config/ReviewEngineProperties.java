package com.acr.review.engine.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** 本地审查引擎运行配置。 */
@Component
@ConfigurationProperties(prefix = "review.engine")
public class ReviewEngineProperties
{
    /** CLI 可执行文件路径或命令名（需在 PATH 中） */
    private String executablePath = "ocr";

    /** 引擎工作目录根路径，每次调用在其下创建独立子目录 */
    private String workspaceRoot = System.getProperty("java.io.tmpdir") + "/acr-review-engine";

    /** 默认超时（秒） */
    private int defaultTimeoutSeconds = 600;

    /** 最大并发调用数 */
    private int maxConcurrency = 2;

    /** stdout/stderr 单次读取上限（字节） */
    private int maxOutputBytes = 1_048_576;

    /** 引擎显示名称 */
    private String engineName = "open-code-review";

    /** 引擎类型标签 */
    private String engineType = "LOCAL_CLI";

    public String getExecutablePath()
    {
        return executablePath;
    }

    public void setExecutablePath(String executablePath)
    {
        this.executablePath = executablePath;
    }

    public String getWorkspaceRoot()
    {
        return workspaceRoot;
    }

    public void setWorkspaceRoot(String workspaceRoot)
    {
        this.workspaceRoot = workspaceRoot;
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
}
