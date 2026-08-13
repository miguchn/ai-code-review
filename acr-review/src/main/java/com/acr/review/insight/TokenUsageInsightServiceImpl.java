package com.acr.review.insight;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import com.acr.review.domain.ReviewProject;
import com.acr.review.insight.dto.InsightKpiCard;
import com.acr.review.insight.dto.InsightTeamProjectOption;
import com.acr.review.insight.dto.InsightTokenModelOption;
import com.acr.review.insight.dto.InsightTokenModelRow;
import com.acr.review.insight.dto.InsightTokenOverviewResponse;
import com.acr.review.insight.dto.InsightTokenProjectRow;
import com.acr.review.insight.dto.InsightTokenRunRow;
import com.acr.review.insight.dto.InsightTokenTrendPoint;
import com.acr.review.insight.dto.TokenUsageProjectModelRow;
import com.acr.review.insight.dto.TokenUsageTotals;
import com.acr.system.domain.SysAiModelConfig;
import com.acr.system.service.ISysAiModelConfigService;

@Service
public class TokenUsageInsightServiceImpl implements ITokenUsageInsightService
{
    private static final DateTimeFormatter DAY = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final ZoneId ZONE = ZoneId.of(InsightConstants.ZONE_ID);

    private final InsightScopeQueries scopeQueries;
    private final ISysAiModelConfigService modelConfigService;

    public TokenUsageInsightServiceImpl(InsightScopeQueries scopeQueries,
                                        ISysAiModelConfigService modelConfigService)
    {
        this.scopeQueries = scopeQueries;
        this.modelConfigService = modelConfigService;
    }

    @Override
    public InsightTokenOverviewResponse getOverview(String beginDate, String endDate, Integer days,
                                                    Long projectId, Long modelId)
    {
        InsightRange range = InsightRange.of(beginDate, endDate, days);
        TokenUsageTotals current = nvl(scopeQueries.selectTokenTotals(query(range, projectId, modelId)));
        TokenUsageTotals previous = nvl(scopeQueries.selectTokenTotals(
            query(range.getPrevBegin(), range.getPrevEnd(), projectId, modelId)));

        InsightTokenOverviewResponse resp = new InsightTokenOverviewResponse();
        resp.setBeginDate(range.beginText());
        resp.setEndDate(range.endText());
        java.util.Date since = scopeQueries.selectTokenDataSince(new TokenUsageQuery());
        resp.setDataSince(formatDay(since));
        long callCount = n(current.getCallCount());
        resp.setEmpty(callCount == 0 && n(current.getTotalTokens()) == 0);
        if (resp.isEmpty())
        {
            resp.setEmptyReason("选定范围内暂无 Token 用量数据；采集上线前的审查记录不会回填");
        }
        long success = n(current.getSuccessCount());
        if (success > 0)
        {
            resp.setDataGapRatio(InsightMetrics.ratio(n(current.getSuccessMissingTokens()), success));
        }

        Map<Long, SysAiModelConfig> prices = priceMap();
        Double curCost = totalCost(scopeQueries.selectTokenModels(query(range, projectId, modelId)), prices);
        Double prevCost = totalCost(scopeQueries.selectTokenModels(
            query(range.getPrevBegin(), range.getPrevEnd(), projectId, modelId)), prices);

        resp.getKpis().add(kpi("totalTokens", "总 Token", (double) n(current.getTotalTokens()),
            (double) n(previous.getTotalTokens()), "count"));
        resp.getKpis().add(kpi("inputTokens", "输入 Token", (double) n(current.getInputTokens()),
            (double) n(previous.getInputTokens()), "count"));
        resp.getKpis().add(kpi("outputTokens", "输出 Token", (double) n(current.getOutputTokens()),
            (double) n(previous.getOutputTokens()), "count"));
        resp.getKpis().add(kpi("callCount", "调用次数", (double) callCount,
            (double) n(previous.getCallCount()), "count"));
        resp.getKpis().add(kpiNullable("estimatedCost", "估算成本", curCost, prevCost, "yuan"));

        TokenUsageQuery optionQuery = query(range, projectId, null);
        List<InsightTokenModelOption> modelOptions = scopeQueries.selectTokenModelOptions(optionQuery);
        resp.setModelOptions(modelOptions == null ? List.of() : modelOptions);
        List<ReviewProject> projects = scopeQueries.selectProjectsToken(new ReviewProject());
        List<InsightTeamProjectOption> projectOptions = new ArrayList<>();
        if (projects != null)
        {
            for (ReviewProject project : projects)
            {
                projectOptions.add(new InsightTeamProjectOption(project.getProjectId(), project.getProjectName(),
                    project.getBusinessSystemId(), project.getBusinessSystemName()));
            }
        }
        resp.setProjectOptions(projectOptions);
        return resp;
    }

    @Override
    public List<InsightTokenTrendPoint> getTrend(String beginDate, String endDate, Integer days,
                                                 Long projectId, Long modelId)
    {
        InsightRange range = InsightRange.of(beginDate, endDate, days);
        List<InsightTokenTrendPoint> rows = scopeQueries.selectTokenTrend(query(range, projectId, modelId));
        Map<String, InsightTokenTrendPoint> map = new HashMap<>();
        for (LocalDate day = range.getBegin(); !day.isAfter(range.getEnd()); day = day.plusDays(1))
        {
            InsightTokenTrendPoint point = new InsightTokenTrendPoint();
            point.setDate(day.format(DAY));
            point.setInputTokens(0L);
            point.setOutputTokens(0L);
            map.put(point.getDate(), point);
        }
        if (rows != null)
        {
            for (InsightTokenTrendPoint row : rows)
            {
                if (row.getDate() == null)
                {
                    continue;
                }
                String key = String.valueOf(row.getDate());
                InsightTokenTrendPoint point = map.get(key);
                if (point == null)
                {
                    continue;
                }
                point.setInputTokens(n(row.getInputTokens()));
                point.setOutputTokens(n(row.getOutputTokens()));
            }
        }
        return map.values().stream()
            .sorted(Comparator.comparing(InsightTokenTrendPoint::getDate))
            .toList();
    }

    @Override
    public List<InsightTokenModelRow> listModels(String beginDate, String endDate, Integer days,
                                                 Long projectId, Long modelId)
    {
        InsightRange range = InsightRange.of(beginDate, endDate, days);
        List<InsightTokenModelRow> rows = scopeQueries.selectTokenModels(query(range, projectId, modelId));
        if (rows == null)
        {
            return List.of();
        }
        Map<Long, SysAiModelConfig> prices = priceMap();
        long total = 0L;
        for (InsightTokenModelRow row : rows)
        {
            total += n(row.getTotalTokens());
            applyModelCost(row, prices);
        }
        for (InsightTokenModelRow row : rows)
        {
            row.setShare(InsightMetrics.ratio(n(row.getTotalTokens()), total));
        }
        return rows;
    }

    @Override
    public List<InsightTokenProjectRow> listProjects(String beginDate, String endDate, Integer days,
                                                     Long projectId, Long modelId)
    {
        InsightRange range = InsightRange.of(beginDate, endDate, days);
        TokenUsageQuery q = query(range, projectId, modelId);
        List<InsightTokenProjectRow> rows = scopeQueries.selectTokenProjects(q);
        if (rows == null)
        {
            return List.of();
        }
        Map<Long, Double> costByProject = projectCosts(scopeQueries.selectTokenProjectModels(q), priceMap());
        for (InsightTokenProjectRow row : rows)
        {
            row.setEstimatedCost(costByProject.get(row.getProjectId()));
        }
        return rows;
    }

    @Override
    public List<InsightTokenRunRow> listRuns(String beginDate, String endDate, Integer days,
                                             Long projectId, Long modelId)
    {
        InsightRange range = InsightRange.of(beginDate, endDate, days);
        List<InsightTokenRunRow> rows = scopeQueries.selectTokenRuns(query(range, projectId, modelId));
        if (rows == null)
        {
            return List.of();
        }
        Map<Long, SysAiModelConfig> prices = priceMap();
        for (InsightTokenRunRow row : rows)
        {
            SysAiModelConfig config = row.getModelId() == null ? null : prices.get(row.getModelId());
            BigDecimal cost = TokenCostCalculator.estimate(row.getInputTokens(), row.getOutputTokens(),
                config == null ? null : config.getInputPricePer1k(),
                config == null ? null : config.getOutputPricePer1k());
            row.setEstimatedCost(TokenCostCalculator.toDouble(cost));
        }
        return rows;
    }

    private TokenUsageQuery query(InsightRange range, Long projectId, Long modelId)
    {
        return query(range.getBegin(), range.getEnd(), projectId, modelId);
    }

    private TokenUsageQuery query(LocalDate begin, LocalDate end, Long projectId, Long modelId)
    {
        TokenUsageQuery query = new TokenUsageQuery();
        query.setProjectId(projectId);
        query.setModelId(modelId);
        query.getParams().put("beginDate", Date.valueOf(begin));
        query.getParams().put("endDateExclusive", Date.valueOf(end.plusDays(1)));
        return query;
    }

    private void applyModelCost(InsightTokenModelRow row, Map<Long, SysAiModelConfig> prices)
    {
        SysAiModelConfig config = row.getModelId() == null ? null : prices.get(row.getModelId());
        Integer input = row.getInputTokens() == null ? null : intExact(row.getInputTokens());
        Integer output = row.getOutputTokens() == null ? null : intExact(row.getOutputTokens());
        row.setEstimatedCost(TokenCostCalculator.toDouble(TokenCostCalculator.estimate(input, output,
            config == null ? null : config.getInputPricePer1k(),
            config == null ? null : config.getOutputPricePer1k())));
    }

    private Double totalCost(List<InsightTokenModelRow> models, Map<Long, SysAiModelConfig> prices)
    {
        if (models == null || models.isEmpty())
        {
            return 0d;
        }
        BigDecimal sum = BigDecimal.ZERO;
        boolean any = false;
        for (InsightTokenModelRow row : models)
        {
            applyModelCost(row, prices);
            if (row.getEstimatedCost() == null)
            {
                return null;
            }
            sum = sum.add(BigDecimal.valueOf(row.getEstimatedCost()));
            any = true;
        }
        return any ? sum.doubleValue() : 0d;
    }

    private Map<Long, Double> projectCosts(List<TokenUsageProjectModelRow> rows, Map<Long, SysAiModelConfig> prices)
    {
        Map<Long, Double> result = new HashMap<>();
        if (rows == null)
        {
            return result;
        }
        Map<Long, BigDecimal> sums = new HashMap<>();
        for (TokenUsageProjectModelRow row : rows)
        {
            if (result.containsKey(row.getProjectId()) && result.get(row.getProjectId()) == null)
            {
                continue;
            }
            SysAiModelConfig config = row.getModelId() == null ? null : prices.get(row.getModelId());
            Integer input = row.getInputTokens() == null ? null : intExact(row.getInputTokens());
            Integer output = row.getOutputTokens() == null ? null : intExact(row.getOutputTokens());
            BigDecimal cost = TokenCostCalculator.estimate(input, output,
                config == null ? null : config.getInputPricePer1k(),
                config == null ? null : config.getOutputPricePer1k());
            if (cost == null)
            {
                result.put(row.getProjectId(), null);
                sums.remove(row.getProjectId());
                continue;
            }
            sums.merge(row.getProjectId(), cost, BigDecimal::add);
            result.put(row.getProjectId(), sums.get(row.getProjectId()).doubleValue());
        }
        return result;
    }

    private Map<Long, SysAiModelConfig> priceMap()
    {
        List<SysAiModelConfig> configs = modelConfigService.selectSysAiModelConfigList(new SysAiModelConfig());
        Map<Long, SysAiModelConfig> map = new HashMap<>();
        if (configs == null)
        {
            return map;
        }
        for (SysAiModelConfig config : configs)
        {
            if (config.getModelId() != null)
            {
                map.put(config.getModelId(), config);
            }
        }
        return map;
    }

    private static TokenUsageTotals nvl(TokenUsageTotals totals)
    {
        return totals == null ? new TokenUsageTotals() : totals;
    }

    private static long n(Long value)
    {
        return value == null ? 0L : value;
    }

    private static String formatDay(java.util.Date value)
    {
        if (value == null)
        {
            return null;
        }
        if (value instanceof Date sqlDate)
        {
            return sqlDate.toLocalDate().format(DAY);
        }
        return value.toInstant().atZone(ZONE).toLocalDate().format(DAY);
    }

    private static Integer intExact(Long value)
    {
        if (value == null)
        {
            return null;
        }
        if (value > Integer.MAX_VALUE)
        {
            return Integer.MAX_VALUE;
        }
        return value.intValue();
    }

    private static InsightKpiCard kpi(String code, String name, double value, double previous, String unit)
    {
        return kpiNullable(code, name, value, previous, unit);
    }

    private static InsightKpiCard kpiNullable(String code, String name, Double value, Double previous, String unit)
    {
        InsightKpiCard card = new InsightKpiCard();
        card.setCode(code);
        card.setName(name);
        card.setValue(value);
        card.setPreviousValue(previous);
        card.setUnit(unit);
        if (value != null && previous != null)
        {
            card.setChangeRatio(InsightMetrics.periodChangeRatio(value, previous));
        }
        return card;
    }
}
