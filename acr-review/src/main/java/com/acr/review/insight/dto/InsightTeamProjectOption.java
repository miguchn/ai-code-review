package com.acr.review.insight.dto;

/** 团队成员分析中当前数据权限范围内的项目筛选项。 */
public class InsightTeamProjectOption
{
    private Long projectId;
    private String projectName;
    private Long businessSystemId;
    private String businessSystemName;

    public InsightTeamProjectOption()
    {
    }

    public InsightTeamProjectOption(Long projectId, String projectName,
                                    Long businessSystemId, String businessSystemName)
    {
        this.projectId = projectId;
        this.projectName = projectName;
        this.businessSystemId = businessSystemId;
        this.businessSystemName = businessSystemName;
    }

    public Long getProjectId()
    {
        return projectId;
    }

    public void setProjectId(Long projectId)
    {
        this.projectId = projectId;
    }

    public String getProjectName()
    {
        return projectName;
    }

    public void setProjectName(String projectName)
    {
        this.projectName = projectName;
    }

    public Long getBusinessSystemId()
    {
        return businessSystemId;
    }

    public void setBusinessSystemId(Long businessSystemId)
    {
        this.businessSystemId = businessSystemId;
    }

    public String getBusinessSystemName()
    {
        return businessSystemName;
    }

    public void setBusinessSystemName(String businessSystemName)
    {
        this.businessSystemName = businessSystemName;
    }
}
