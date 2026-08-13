package com.acr.review.insight.dto;

import java.util.ArrayList;
import java.util.List;

/** Token 用量总览。 */
public class InsightTokenOverviewResponse
{
    private String beginDate;
    private String endDate;
    private String dataSince;
    private Double dataGapRatio;
    private boolean empty;
    private String emptyReason;
    private List<InsightKpiCard> kpis = new ArrayList<>();
    private List<InsightTeamProjectOption> projectOptions = new ArrayList<>();
    private List<InsightTokenModelOption> modelOptions = new ArrayList<>();

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

    public String getDataSince()
    {
        return dataSince;
    }

    public void setDataSince(String dataSince)
    {
        this.dataSince = dataSince;
    }

    public Double getDataGapRatio()
    {
        return dataGapRatio;
    }

    public void setDataGapRatio(Double dataGapRatio)
    {
        this.dataGapRatio = dataGapRatio;
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

    public List<InsightTeamProjectOption> getProjectOptions()
    {
        return projectOptions;
    }

    public void setProjectOptions(List<InsightTeamProjectOption> projectOptions)
    {
        this.projectOptions = projectOptions;
    }

    public List<InsightTokenModelOption> getModelOptions()
    {
        return modelOptions;
    }

    public void setModelOptions(List<InsightTokenModelOption> modelOptions)
    {
        this.modelOptions = modelOptions;
    }
}
