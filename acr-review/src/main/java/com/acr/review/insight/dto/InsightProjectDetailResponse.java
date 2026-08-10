package com.acr.review.insight.dto;

import java.util.ArrayList;
import java.util.List;

/** 单项目趋势详情（一期不含提交趋势）。 */
public class InsightProjectDetailResponse
{
    private Long projectId;
    private String projectName;
    private String businessSystemName;
    private String ownerName;
    private String beginDate;
    private String endDate;
    private String metricsVersion;
    private String dataSince;
    private boolean empty;
    private String emptyReason;
    private List<InsightKpiCard> kpis = new ArrayList<>();
    private List<InsightTrendPoint> taskTrend = new ArrayList<>();
    private List<InsightTrendPoint> issueTrend = new ArrayList<>();
    private List<InsightCommitTrendPoint> commitTrend = new ArrayList<>();
    private List<InsightNamedCount> severityDistribution = new ArrayList<>();
    private List<InsightNamedCount> categoryDistribution = new ArrayList<>();
    private InsightDispositionFunnel dispositionFunnel = new InsightDispositionFunnel();

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

    public String getBusinessSystemName()
    {
        return businessSystemName;
    }

    public void setBusinessSystemName(String businessSystemName)
    {
        this.businessSystemName = businessSystemName;
    }

    public String getOwnerName()
    {
        return ownerName;
    }

    public void setOwnerName(String ownerName)
    {
        this.ownerName = ownerName;
    }

    public String getBeginDate()
    {
        return beginDate;
    }

    public void setBeginDate(String beginDate)
    {
        this.beginDate = beginDate;
    }

    public String getEndDate()
    {
        return endDate;
    }

    public void setEndDate(String endDate)
    {
        this.endDate = endDate;
    }

    public String getMetricsVersion()
    {
        return metricsVersion;
    }

    public void setMetricsVersion(String metricsVersion)
    {
        this.metricsVersion = metricsVersion;
    }

    public String getDataSince()
    {
        return dataSince;
    }

    public void setDataSince(String dataSince)
    {
        this.dataSince = dataSince;
    }

    public boolean isEmpty()
    {
        return empty;
    }

    public void setEmpty(boolean empty)
    {
        this.empty = empty;
    }

    public String getEmptyReason()
    {
        return emptyReason;
    }

    public void setEmptyReason(String emptyReason)
    {
        this.emptyReason = emptyReason;
    }

    public List<InsightKpiCard> getKpis()
    {
        return kpis;
    }

    public void setKpis(List<InsightKpiCard> kpis)
    {
        this.kpis = kpis;
    }

    public List<InsightTrendPoint> getTaskTrend()
    {
        return taskTrend;
    }

    public void setTaskTrend(List<InsightTrendPoint> taskTrend)
    {
        this.taskTrend = taskTrend;
    }

    public List<InsightTrendPoint> getIssueTrend()
    {
        return issueTrend;
    }

    public void setIssueTrend(List<InsightTrendPoint> issueTrend)
    {
        this.issueTrend = issueTrend;
    }

    public List<InsightCommitTrendPoint> getCommitTrend()
    {
        return commitTrend;
    }

    public void setCommitTrend(List<InsightCommitTrendPoint> commitTrend)
    {
        this.commitTrend = commitTrend;
    }

    public List<InsightNamedCount> getSeverityDistribution()
    {
        return severityDistribution;
    }

    public void setSeverityDistribution(List<InsightNamedCount> severityDistribution)
    {
        this.severityDistribution = severityDistribution;
    }

    public List<InsightNamedCount> getCategoryDistribution()
    {
        return categoryDistribution;
    }

    public void setCategoryDistribution(List<InsightNamedCount> categoryDistribution)
    {
        this.categoryDistribution = categoryDistribution;
    }

    public InsightDispositionFunnel getDispositionFunnel()
    {
        return dispositionFunnel;
    }

    public void setDispositionFunnel(InsightDispositionFunnel dispositionFunnel)
    {
        this.dispositionFunnel = dispositionFunnel;
    }
}
