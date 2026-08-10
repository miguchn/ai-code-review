package com.acr.review.insight.dto;

import java.util.ArrayList;
import java.util.List;

/** 总览看板响应。 */
public class InsightOverviewResponse
{
    private String beginDate;
    private String endDate;
    private String metricsVersion;
    private String dataSince;
    private boolean empty;
    private String emptyReason;
    private List<InsightKpiCard> kpis = new ArrayList<>();
    private List<InsightTrendPoint> taskTrend = new ArrayList<>();
    private List<InsightTrendPoint> issueTrend = new ArrayList<>();
    private List<InsightNamedCount> categoryDistribution = new ArrayList<>();
    private List<InsightChannelHealth> deliveryHealth = new ArrayList<>();

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

    public List<InsightNamedCount> getCategoryDistribution()
    {
        return categoryDistribution;
    }

    public void setCategoryDistribution(List<InsightNamedCount> categoryDistribution)
    {
        this.categoryDistribution = categoryDistribution;
    }

    public List<InsightChannelHealth> getDeliveryHealth()
    {
        return deliveryHealth;
    }

    public void setDeliveryHealth(List<InsightChannelHealth> deliveryHealth)
    {
        this.deliveryHealth = deliveryHealth;
    }
}
