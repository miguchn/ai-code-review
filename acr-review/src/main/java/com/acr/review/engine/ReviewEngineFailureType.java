package com.acr.review.engine;

/** 审查引擎调用失败分类。 */
public enum ReviewEngineFailureType
{
    CLI_NOT_FOUND("CLI 不存在"),
    PERMISSION_DENIED("权限不足"),
    TIMEOUT("执行超时"),
    ABNORMAL_EXIT("退出码异常"),
    OUTPUT_FORMAT_ERROR("输出格式错误"),
    MODEL_CALL_FAILED("模型调用失败"),
    WORKSPACE_ERROR("工作目录错误"),
    CONCURRENCY_LIMIT("并发超限"),
    UNKNOWN("未知错误");

    private final String label;

    ReviewEngineFailureType(String label)
    {
        this.label = label;
    }

    public String getLabel()
    {
        return label;
    }
}
