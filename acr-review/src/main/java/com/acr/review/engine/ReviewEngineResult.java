package com.acr.review.engine;

import java.util.Map;

/** 标准审查引擎执行结果。 */
public class ReviewEngineResult
{
    private boolean success;
    private String engineName;
    private String engineVersion;
    private long durationMs;
    private String stdout;
    private String stderr;
    private Map<String, Object> structuredResult;
    private Integer exitCode;
    private ReviewEngineFailureType failureType;
    private String failureReason;

    public static ReviewEngineResult success(String engineName, String engineVersion, long durationMs,
        String stdout, String stderr, Map<String, Object> structuredResult, Integer exitCode)
    {
        ReviewEngineResult result = new ReviewEngineResult();
        result.success = true;
        result.engineName = engineName;
        result.engineVersion = engineVersion;
        result.durationMs = durationMs;
        result.stdout = stdout;
        result.stderr = stderr;
        result.structuredResult = structuredResult;
        result.exitCode = exitCode;
        return result;
    }

    public static ReviewEngineResult failure(String engineName, String engineVersion, long durationMs,
        String stdout, String stderr, Integer exitCode, ReviewEngineFailureType failureType, String failureReason)
    {
        ReviewEngineResult result = new ReviewEngineResult();
        result.success = false;
        result.engineName = engineName;
        result.engineVersion = engineVersion;
        result.durationMs = durationMs;
        result.stdout = stdout;
        result.stderr = stderr;
        result.exitCode = exitCode;
        result.failureType = failureType;
        result.failureReason = failureReason;
        return result;
    }

    public boolean isSuccess()
    {
        return success;
    }

    public String getEngineName()
    {
        return engineName;
    }

    public String getEngineVersion()
    {
        return engineVersion;
    }

    public long getDurationMs()
    {
        return durationMs;
    }

    public String getStdout()
    {
        return stdout;
    }

    public String getStderr()
    {
        return stderr;
    }

    public Map<String, Object> getStructuredResult()
    {
        return structuredResult;
    }

    public Integer getExitCode()
    {
        return exitCode;
    }

    public ReviewEngineFailureType getFailureType()
    {
        return failureType;
    }

    public String getFailureReason()
    {
        return failureReason;
    }
}
