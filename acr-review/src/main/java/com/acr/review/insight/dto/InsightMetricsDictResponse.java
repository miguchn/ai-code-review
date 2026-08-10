package com.acr.review.insight.dto;

import java.util.ArrayList;
import java.util.List;
import com.acr.review.insight.InsightMetricDef;

public class InsightMetricsDictResponse
{
    private String version;
    private List<InsightMetricDef> metrics = new ArrayList<>();

    public String getVersion()
    {
        return version;
    }

    public void setVersion(String version)
    {
        this.version = version;
    }

    public List<InsightMetricDef> getMetrics()
    {
        return metrics;
    }

    public void setMetrics(List<InsightMetricDef> metrics)
    {
        this.metrics = metrics;
    }
}
