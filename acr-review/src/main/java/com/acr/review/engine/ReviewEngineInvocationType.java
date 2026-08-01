package com.acr.review.engine;

/** 受控的 CLI 调用模式，禁止任意命令注入。 */
public enum ReviewEngineInvocationType
{
    /** 读取 CLI 版本 */
    VERSION,
    /** 测试 LLM 连通性 */
    LLM_TEST,
    /** 预览待审文件（不调用模型） */
    REVIEW_PREVIEW,
    /** 完整 diff 审查 */
    REVIEW
}
