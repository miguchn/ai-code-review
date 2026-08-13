package com.acr.review.insight;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.acr.review.insight.dto.InsightKpiCard;
import com.acr.review.insight.dto.InsightTokenModelRow;
import com.acr.review.insight.dto.InsightTokenOverviewResponse;
import com.acr.review.insight.dto.InsightTokenRunRow;
import com.acr.review.insight.dto.TokenUsageTotals;
import com.acr.system.domain.SysAiModelConfig;
import com.acr.system.service.ISysAiModelConfigService;

@ExtendWith(MockitoExtension.class)
class TokenUsageInsightServiceImplTest
{
    @Mock
    private InsightScopeQueries scopeQueries;
    @Mock
    private ISysAiModelConfigService modelConfigService;

    private TokenUsageInsightServiceImpl service;

    @BeforeEach
    void setUp()
    {
        service = new TokenUsageInsightServiceImpl(scopeQueries, modelConfigService);
    }

    @Test
    void overviewCostIsNullWhenPriceMissing()
    {
        TokenUsageTotals totals = new TokenUsageTotals();
        totals.setTotalTokens(1500L);
        totals.setInputTokens(1000L);
        totals.setOutputTokens(500L);
        totals.setCallCount(2L);
        totals.setSuccessCount(2L);
        totals.setSuccessMissingTokens(0L);
        when(scopeQueries.selectTokenTotals(any())).thenReturn(totals);
        when(scopeQueries.selectTokenDataSince(any())).thenReturn(null);
        InsightTokenModelRow model = modelRow(7L, 1000L, 500L, 1500L);
        when(scopeQueries.selectTokenModels(any())).thenReturn(List.of(model));
        when(scopeQueries.selectTokenModelOptions(any())).thenReturn(List.of());
        when(scopeQueries.selectProjectsToken(any())).thenReturn(List.of());
        SysAiModelConfig config = new SysAiModelConfig();
        config.setModelId(7L);
        when(modelConfigService.selectSysAiModelConfigList(any())).thenReturn(List.of(config));

        InsightTokenOverviewResponse resp = service.getOverview(null, null, 7, null, null);

        InsightKpiCard cost = kpi(resp, "estimatedCost");
        assertNull(cost.getValue());
        assertEquals("yuan", cost.getUnit());
        verify(scopeQueries, atLeastOnce()).selectTokenTotals(any());
    }

    @Test
    void overviewCostUsesCurrentUnitPrice()
    {
        TokenUsageTotals totals = new TokenUsageTotals();
        totals.setTotalTokens(1500L);
        totals.setInputTokens(1000L);
        totals.setOutputTokens(500L);
        totals.setCallCount(1L);
        totals.setSuccessCount(1L);
        totals.setSuccessMissingTokens(0L);
        when(scopeQueries.selectTokenTotals(any())).thenReturn(totals);
        when(scopeQueries.selectTokenDataSince(any())).thenReturn(null);
        when(scopeQueries.selectTokenModels(any())).thenReturn(List.of(modelRow(7L, 1000L, 500L, 1500L)));
        when(scopeQueries.selectTokenModelOptions(any())).thenReturn(List.of());
        when(scopeQueries.selectProjectsToken(any())).thenReturn(List.of());
        SysAiModelConfig config = new SysAiModelConfig();
        config.setModelId(7L);
        config.setInputPricePer1k(new BigDecimal("0.0020"));
        config.setOutputPricePer1k(new BigDecimal("0.0080"));
        when(modelConfigService.selectSysAiModelConfigList(any())).thenReturn(List.of(config));

        InsightTokenOverviewResponse resp = service.getOverview(null, null, 7, null, null);

        assertEquals(0.006d, kpi(resp, "estimatedCost").getValue(), 0.0001);
        assertEquals(1500d, kpi(resp, "totalTokens").getValue());
        assertEquals(0d, resp.getDataGapRatio());
    }

    @Test
    void emptyScopeHidesUnauthorizedProjectUsage()
    {
        when(scopeQueries.selectTokenTotals(any())).thenReturn(new TokenUsageTotals());
        when(scopeQueries.selectTokenDataSince(any())).thenReturn(null);
        when(scopeQueries.selectTokenModels(any())).thenReturn(List.of());
        when(scopeQueries.selectTokenModelOptions(any())).thenReturn(List.of());
        when(scopeQueries.selectProjectsToken(any())).thenReturn(List.of());

        InsightTokenOverviewResponse resp = service.getOverview(null, null, 7, 99L, null);

        assertTrue(resp.isEmpty());
        ArgumentCaptor<TokenUsageQuery> captor = ArgumentCaptor.forClass(TokenUsageQuery.class);
        verify(scopeQueries, atLeastOnce()).selectTokenTotals(captor.capture());
        assertEquals(99L, captor.getValue().getProjectId());
        verify(scopeQueries, never()).selectTokenRuns(any());
    }

    @Test
    void runCostIsNullWhenPriceMissingAndFilledWhenPriced()
    {
        InsightTokenRunRow priced = new InsightTokenRunRow();
        priced.setModelId(7L);
        priced.setInputTokens(1000);
        priced.setOutputTokens(500);
        InsightTokenRunRow unpriced = new InsightTokenRunRow();
        unpriced.setModelId(8L);
        unpriced.setInputTokens(100);
        unpriced.setOutputTokens(20);
        when(scopeQueries.selectTokenRuns(any())).thenReturn(List.of(priced, unpriced));
        SysAiModelConfig config = new SysAiModelConfig();
        config.setModelId(7L);
        config.setInputPricePer1k(new BigDecimal("1.0000"));
        config.setOutputPricePer1k(new BigDecimal("2.0000"));
        when(modelConfigService.selectSysAiModelConfigList(any())).thenReturn(List.of(config));

        List<InsightTokenRunRow> rows = service.listRuns(null, null, 7, null, null);

        assertEquals(2.0d, rows.get(0).getEstimatedCost(), 0.0001);
        assertNull(rows.get(1).getEstimatedCost());
    }

    private static InsightTokenModelRow modelRow(Long modelId, long input, long output, long total)
    {
        InsightTokenModelRow row = new InsightTokenModelRow();
        row.setModelId(modelId);
        row.setInputTokens(input);
        row.setOutputTokens(output);
        row.setTotalTokens(total);
        row.setCallCount(1L);
        return row;
    }

    private static InsightKpiCard kpi(InsightTokenOverviewResponse resp, String code)
    {
        return resp.getKpis().stream().filter(c -> code.equals(c.getCode())).findFirst().orElseThrow();
    }
}
