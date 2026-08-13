package com.acr.review.insight;

import com.acr.common.core.domain.BaseEntity;

/** Token 用量直查条件（时间窗放在 params.beginDate / params.endDate）。 */
public class TokenUsageQuery extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long projectId;
    private Long modelId;

    public Long getProjectId()
    {
        return projectId;
    }

    public void setProjectId(Long projectId)
    {
        this.projectId = projectId;
    }

    public Long getModelId()
    {
        return modelId;
    }

    public void setModelId(Long modelId)
    {
        this.modelId = modelId;
    }
}
