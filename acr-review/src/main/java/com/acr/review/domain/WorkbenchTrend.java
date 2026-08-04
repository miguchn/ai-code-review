package com.acr.review.domain;

import java.util.ArrayList;
import java.util.List;

/** 工作台审查结论趋势（近 N 天按天聚合，缺失日期补零）。 */
public class WorkbenchTrend
{
    private int days;
    private String beginDate;
    private String endDate;
    private List<WorkbenchTrendPoint> points = new ArrayList<>();

    public WorkbenchTrend()
    {
    }

    public WorkbenchTrend(int days, String beginDate, String endDate, List<WorkbenchTrendPoint> points)
    {
        this.days = days;
        this.beginDate = beginDate;
        this.endDate = endDate;
        if (points != null)
        {
            this.points = points;
        }
    }

    public int getDays()
    {
        return days;
    }

    public void setDays(int days)
    {
        this.days = days;
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

    public List<WorkbenchTrendPoint> getPoints()
    {
        return points;
    }

    public void setPoints(List<WorkbenchTrendPoint> points)
    {
        this.points = points;
    }
}
