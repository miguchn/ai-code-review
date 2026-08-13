package com.acr.review.mapper;

import java.util.Date;
import java.util.List;
import com.acr.review.insight.TokenUsageQuery;
import com.acr.review.insight.dto.InsightTokenModelOption;
import com.acr.review.insight.dto.InsightTokenModelRow;
import com.acr.review.insight.dto.InsightTokenProjectRow;
import com.acr.review.insight.dto.InsightTokenRunRow;
import com.acr.review.insight.dto.InsightTokenTrendPoint;
import com.acr.review.insight.dto.TokenUsageProjectModelRow;
import com.acr.review.insight.dto.TokenUsageTotals;

/** Token 用量直查 review_task_run（JOIN 项目归属 + DataScope）。 */
public interface ReviewTokenUsageMapper
{
    TokenUsageTotals selectTotals(TokenUsageQuery query);

    Date selectEarliestTokenTime(TokenUsageQuery query);

    List<InsightTokenTrendPoint> selectTrend(TokenUsageQuery query);

    List<InsightTokenModelRow> selectModels(TokenUsageQuery query);

    List<InsightTokenModelOption> selectModelOptions(TokenUsageQuery query);

    List<InsightTokenProjectRow> selectProjects(TokenUsageQuery query);

    List<TokenUsageProjectModelRow> selectProjectModels(TokenUsageQuery query);

    List<InsightTokenRunRow> selectRuns(TokenUsageQuery query);
}
