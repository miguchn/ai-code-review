package com.acr.review.domain;

/** 审查引擎测试调用请求。 */
public class ReviewEngineTestRequest
{
    /** 可选；为空时使用默认启用模型 */
    private Long modelId;

    public Long getModelId()
    {
        return modelId;
    }

    public void setModelId(Long modelId)
    {
        this.modelId = modelId;
    }
}
