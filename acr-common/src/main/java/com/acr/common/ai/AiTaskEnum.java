package com.acr.common.ai;

/**
 * AI 任务类型枚举
 */
public enum AiTaskEnum
{
    /** 文档解析 — 非标准格式转接口结构 */
    PARSE("文档解析"),
    /** 业务域标注 — 自动打标签 */
    LABEL("业务域标注"),
    /** 缺失补全 — 生成缺失字段建议值 */
    FILL("缺失补全"),
    /** 语义推荐 — 根据需求描述推荐接口 */
    RECOMMEND("语义推荐"),
    /** 自然语言查询 — 翻译用户问题为查询条件 */
    CHAT("对话查询"),
    /** 连接测试 */
    TEST("连接测试");

    private final String description;

    AiTaskEnum(String description)
    {
        this.description = description;
    }

    public String getDescription()
    {
        return description;
    }
}
