package com.acr.review.insight;

import java.util.List;
import com.acr.review.insight.dto.InsightTokenModelRow;
import com.acr.review.insight.dto.InsightTokenOverviewResponse;
import com.acr.review.insight.dto.InsightTokenProjectRow;
import com.acr.review.insight.dto.InsightTokenRunRow;
import com.acr.review.insight.dto.InsightTokenTrendPoint;

public interface ITokenUsageInsightService
{
    InsightTokenOverviewResponse getOverview(String beginDate, String endDate, Integer days,
                                             Long projectId, Long modelId);

    List<InsightTokenTrendPoint> getTrend(String beginDate, String endDate, Integer days,
                                          Long projectId, Long modelId);

    List<InsightTokenModelRow> listModels(String beginDate, String endDate, Integer days,
                                          Long projectId, Long modelId);

    List<InsightTokenProjectRow> listProjects(String beginDate, String endDate, Integer days,
                                              Long projectId, Long modelId);

    List<InsightTokenRunRow> listRuns(String beginDate, String endDate, Integer days,
                                      Long projectId, Long modelId);
}
