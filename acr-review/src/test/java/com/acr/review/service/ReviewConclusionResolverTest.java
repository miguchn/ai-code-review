package com.acr.review.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import com.acr.review.domain.ReviewPipelineConstants;

class ReviewConclusionResolverTest
{
    private final ReviewConclusionResolver resolver = new ReviewConclusionResolver();

    @Test
    void emptyResultIsPass()
    {
        assertEquals(ReviewPipelineConstants.CONCLUSION_PASS, resolver.resolve(Map.of()));
    }

    @Test
    void highSeverityBecomesBlock()
    {
        Map<String, Object> result = Map.of(
            "comments", List.of(Map.of("severity", "critical", "message", "sql injection")));
        assertEquals(ReviewPipelineConstants.CONCLUSION_BLOCK, resolver.resolve(result));
    }

    @Test
    void mediumSeverityBecomesWarn()
    {
        Map<String, Object> result = Map.of(
            "comments", List.of(Map.of("severity", "warning", "message", "naming")));
        assertEquals(ReviewPipelineConstants.CONCLUSION_WARN, resolver.resolve(result));
    }

    @Test
    void substringLookalikeIsNotBlock()
    {
        // "noncritical" 含 critical 子串，精确匹配下不得判为阻断
        Map<String, Object> result = Map.of(
            "comments", List.of(Map.of("severity", "noncritical", "message", "style")));
        assertEquals(ReviewPipelineConstants.CONCLUSION_WARN, resolver.resolve(result));
    }

    @Test
    void infoSeverityDoesNotEscalate()
    {
        Map<String, Object> result = Map.of(
            "comments", List.of(Map.of("severity", "info", "message", "hint")));
        assertEquals(ReviewPipelineConstants.CONCLUSION_PASS, resolver.resolve(result));
    }
}
