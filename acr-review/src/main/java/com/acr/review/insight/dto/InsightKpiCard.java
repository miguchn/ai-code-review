package com.acr.review.insight.dto;

/** KPI 卡片（含环比）。 */
public class InsightKpiCard
{
    private String code;
    private String name;
    private Double value;
    private String unit;
    private Double previousValue;
    private Double changeRatio;

    public String getCode()
    {
        return code;
    }

    public void setCode(String code)
    {
        this.code = code;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public Double getValue()
    {
        return value;
    }

    public void setValue(Double value)
    {
        this.value = value;
    }

    public String getUnit()
    {
        return unit;
    }

    public void setUnit(String unit)
    {
        this.unit = unit;
    }

    public Double getPreviousValue()
    {
        return previousValue;
    }

    public void setPreviousValue(Double previousValue)
    {
        this.previousValue = previousValue;
    }

    public Double getChangeRatio()
    {
        return changeRatio;
    }

    public void setChangeRatio(Double changeRatio)
    {
        this.changeRatio = changeRatio;
    }
}
