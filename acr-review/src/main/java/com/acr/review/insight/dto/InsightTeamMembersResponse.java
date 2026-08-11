package com.acr.review.insight.dto;

import java.util.ArrayList;
import java.util.List;

public class InsightTeamMembersResponse
{
    private String beginDate;
    private String endDate;
    private String metricsVersion;
    private String dataSince;
    private String disclaimer = "用于团队管理与辅导，不作为绩效评价直接输入";
    private List<InsightTeamProjectOption> projectOptions = new ArrayList<>();
    private List<InsightTeamMemberRow> members = new ArrayList<>();
    private List<InsightTeamMemberRow> unbound = new ArrayList<>();
    private List<InsightCommitTrendPoint> stackedTrend = new ArrayList<>();

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

    public String getDisclaimer()
    {
        return disclaimer;
    }

    public void setDisclaimer(String disclaimer)
    {
        this.disclaimer = disclaimer;
    }

    public List<InsightTeamProjectOption> getProjectOptions()
    {
        return projectOptions;
    }

    public void setProjectOptions(List<InsightTeamProjectOption> projectOptions)
    {
        this.projectOptions = projectOptions;
    }

    public List<InsightTeamMemberRow> getMembers()
    {
        return members;
    }

    public void setMembers(List<InsightTeamMemberRow> members)
    {
        this.members = members;
    }

    public List<InsightTeamMemberRow> getUnbound()
    {
        return unbound;
    }

    public void setUnbound(List<InsightTeamMemberRow> unbound)
    {
        this.unbound = unbound;
    }

    public List<InsightCommitTrendPoint> getStackedTrend()
    {
        return stackedTrend;
    }

    public void setStackedTrend(List<InsightCommitTrendPoint> stackedTrend)
    {
        this.stackedTrend = stackedTrend;
    }
}
