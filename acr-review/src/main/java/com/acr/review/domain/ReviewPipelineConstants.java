package com.acr.review.domain;

/** 审查流水线稳定常量（代码内部使用，字典提供中文展示）。 */
public final class ReviewPipelineConstants
{
    /** 大模型审查：平台直接调用模型服务 + 提示词。 */
    public static final String REVIEW_MODE_LLM_DIRECT = "LLM_DIRECT";
    /** 审查引擎：调用本机 open-code-review。 */
    public static final String REVIEW_MODE_OCR_ENGINE = "OCR_ENGINE";
    /** 历史兼容值，运行时视为 OCR_ENGINE。 */
    public static final String REVIEW_MODE_OCR_PR_DIFF_LEGACY = "OCR_PR_DIFF";

    public static final String ENGINE_OPEN_CODE_REVIEW = "OPEN_CODE_REVIEW";

    public static final String TASK_PENDING = "PENDING";
    public static final String TASK_RUNNING = "RUNNING";
    public static final String TASK_RETRYING = "RETRYING";
    public static final String TASK_SUCCESS = "SUCCESS";
    public static final String TASK_FAILED = "FAILED";
    public static final String TASK_CANCELLED = "CANCELLED";
    public static final String TASK_SUPERSEDED = "SUPERSEDED";

    public static final String RUN_RUNNING = "RUNNING";
    public static final String RUN_SUCCESS = "SUCCESS";
    public static final String RUN_FAILED = "FAILED";

    public static final String STEP_RESOLVE_CONFIG = "RESOLVE_CONFIG";
    public static final String STEP_PREPARE_WORKSPACE = "PREPARE_WORKSPACE";
    public static final String STEP_INVOKE_ENGINE = "INVOKE_ENGINE";
    public static final String STEP_INVOKE_MODEL = "INVOKE_MODEL";
    public static final String STEP_PERSIST_RESULT = "PERSIST_RESULT";

    public static final String CONCLUSION_PASS = "PASS";
    public static final String CONCLUSION_WARN = "WARN";
    public static final String CONCLUSION_BLOCK = "BLOCK";

    /** 事件来源：合并请求。 */
    public static final String EVENT_SOURCE_PR = "PR";
    /** 事件来源：推送。 */
    public static final String EVENT_SOURCE_PUSH = "PUSH";

    public static final String FAILURE_CONFIG_MISSING = "CONFIG_MISSING";
    public static final String FAILURE_CREDENTIAL_ERROR = "CREDENTIAL_ERROR";
    public static final String FAILURE_WORKSPACE_PREPARE = "WORKSPACE_PREPARE_FAILED";
    public static final String FAILURE_TIMEOUT = "TIMEOUT";
    public static final String FAILURE_ENGINE = "ENGINE_FAILED";
    public static final String FAILURE_MODEL = "MODEL_CALL_FAILED";
    public static final String FAILURE_RESULT_FORMAT = "RESULT_FORMAT_INVALID";
    public static final String FAILURE_CONCURRENCY = "CONCURRENCY_LIMIT";
    public static final String FAILURE_RATE_LIMIT = "RATE_LIMIT";
    public static final String FAILURE_DEPENDENCY_UNAVAILABLE = "DEPENDENCY_UNAVAILABLE";
    public static final String FAILURE_LEASE_EXPIRED = "LEASE_EXPIRED";
    public static final String FAILURE_WORKER_SHUTDOWN = "WORKER_SHUTDOWN";
    public static final String FAILURE_UNKNOWN = "UNKNOWN";

    public static final int MAX_RESULT_JSON_CHARS = 512_000;
    public static final int MAX_DIFF_CHARS = 400_000;

    /** 高影响扩展单文件全文拉取上限（字节）：超出按拉取失败降级，保留 L0 hunk。 */
    public static final int MAX_EXPANDED_FILE_BYTES = 256 * 1024;

    /** 单次执行允许拉取全文的扩展文件数量上限：超出部分不再拉取，记 FETCH_LIMIT_SKIPPED。 */
    public static final int MAX_EXPANDED_FETCH_COUNT = 30;

    private ReviewPipelineConstants()
    {
    }

    public static String normalizeReviewMode(String reviewMode)
    {
        if (REVIEW_MODE_OCR_PR_DIFF_LEGACY.equals(reviewMode))
        {
            return REVIEW_MODE_OCR_ENGINE;
        }
        return reviewMode;
    }

    public static boolean isOcrEngineMode(String reviewMode)
    {
        String normalized = normalizeReviewMode(reviewMode);
        return REVIEW_MODE_OCR_ENGINE.equals(normalized);
    }

    public static boolean isLlmDirectMode(String reviewMode)
    {
        return REVIEW_MODE_LLM_DIRECT.equals(normalizeReviewMode(reviewMode));
    }
}
