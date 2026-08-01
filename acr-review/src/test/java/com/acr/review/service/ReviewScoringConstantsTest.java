package com.acr.review.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import com.acr.review.domain.ReviewPlatformRules;

class ReviewScoringConstantsTest
{
    @Test
    void scoreWeightsDerivedFromSharedDimensions()
    {
        Map<String, Integer> weights = ReviewScoringConstants.scoreWeights();
        assertEquals(40, weights.get(ReviewScoringConstants.DIM_CORRECTNESS));
        assertEquals(30, weights.get(ReviewScoringConstants.DIM_SECURITY));
        assertEquals(20, weights.get(ReviewScoringConstants.DIM_PRACTICE));
        assertEquals(5, weights.get(ReviewScoringConstants.DIM_PERFORMANCE));
        assertEquals(5, weights.get(ReviewScoringConstants.DIM_COMMIT_QUALITY));
        assertEquals(100, weights.values().stream().mapToInt(Integer::intValue).sum());
        assertEquals(ReviewScoringConstants.requiredDimensions(), List.copyOf(weights.keySet()));
    }

    @Test
    void protocolAppendixAndUiRulesShareDimensionSource()
    {
        String appendix = ReviewScoringConstants.protocolAppendix();
        ReviewPlatformRules rules = ReviewScoringConstants.platformRulesForUi();

        assertEquals("平台统一审查规则", rules.getTitle());
        assertEquals(ReviewScoringConstants.PROTOCOL_VERSION, rules.getProtocolVersion());
        assertEquals(ReviewScoringConstants.MAX_TOP_ISSUES, rules.getTopIssuesMax());
        assertEquals(ReviewScoringConstants.MAX_TOTAL, rules.getTotalMaxScore());
        assertEquals(ReviewScoringConstants.protocolUiHint(), rules.getUiHint());
        assertTrue(rules.getTopIssuesHint().contains("Top 3"));
        assertFalse(rules.getUiHint().isBlank());

        assertEquals(5, rules.getDimensions().size());
        for (ReviewPlatformRules.Dimension dimension : rules.getDimensions())
        {
            assertTrue(appendix.contains(dimension.getCode()));
            assertTrue(appendix.contains(dimension.getName()));
            assertTrue(appendix.contains("满分 " + dimension.getMaxScore()));
            assertTrue(appendix.contains(dimension.getDescription()));
        }

        // 内部 JSON Schema 不对普通用户展示；附录可含 JSON 字段要求供模型执行
        assertFalse(rules.toString().toLowerCase().contains("schema"));
        assertFalse(String.valueOf(rules.getUiHint()).toLowerCase().contains("schema"));
        assertTrue(appendix.contains("protocolVersion"));
    }
}
