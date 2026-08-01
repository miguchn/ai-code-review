package com.acr.common.utils.http;

/**
 * HTTP POST 调用结果（含状态码，便于 LLM 错误分类）。
 */
public class HttpPostResult
{
    private final int statusCode;
    private final String body;
    private final long latencyMs;
    private final Exception exception;

    public HttpPostResult(int statusCode, String body, long latencyMs, Exception exception)
    {
        this.statusCode = statusCode;
        this.body = body;
        this.latencyMs = latencyMs;
        this.exception = exception;
    }

    public int getStatusCode()
    {
        return statusCode;
    }

    public String getBody()
    {
        return body;
    }

    public long getLatencyMs()
    {
        return latencyMs;
    }

    public Exception getException()
    {
        return exception;
    }

    public boolean isSuccessful()
    {
        return exception == null && statusCode >= 200 && statusCode < 300;
    }
}
