package com.acr.review.service;

import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;
import com.acr.review.domain.ReviewPipelineConstants;

/**
 * 从引擎结构化结果推导平台基础结论。
 * 这是运营可读的基础判定，不是正式质量门禁；后续门禁切片可替换策略而不改任务状态语义。
 */
@Component
public class ReviewConclusionResolver
{
    /** 精确匹配的严重度集合；避免 "noncritical" 之类子串误判。 */
    private static final java.util.Set<String> BLOCK_SEVERITIES = java.util.Set.of(
        "critical", "error", "high", "blocker", "严重", "阻断", "高危");
    private static final java.util.Set<String> INFO_SEVERITIES = java.util.Set.of(
        "info", "nit", "note", "提示", "建议");

    public String resolve(Map<String, Object> structuredResult)
    {
        int[] counts = countIssues(structuredResult);
        if (counts[0] == 0 && counts[1] == 0)
        {
            Object issuesCount = structuredResult == null ? null : structuredResult.get("issueCount");
            if (issuesCount instanceof Number number && number.intValue() > 0)
            {
                return ReviewPipelineConstants.CONCLUSION_WARN;
            }
            return ReviewPipelineConstants.CONCLUSION_PASS;
        }
        if (counts[0] > 0)
        {
            return ReviewPipelineConstants.CONCLUSION_BLOCK;
        }
        return ReviewPipelineConstants.CONCLUSION_WARN;
    }

    public String summarize(Map<String, Object> structuredResult, String conclusion)
    {
        int[] counts = countIssues(structuredResult);
        String conclusionLabel = switch (conclusion)
        {
            case ReviewPipelineConstants.CONCLUSION_BLOCK -> "阻断";
            case ReviewPipelineConstants.CONCLUSION_WARN -> "警告";
            default -> "通过";
        };
        return "审查结论：" + conclusionLabel + "；高风险 " + counts[0] + " 项，警告 " + counts[1] + " 项";
    }

    private int[] countIssues(Map<String, Object> structuredResult)
    {
        int[] counts = new int[2];
        if (structuredResult == null || structuredResult.isEmpty())
        {
            return counts;
        }
        countSeverities(structuredResult.get("comments"), counts);
        countSeverities(structuredResult.get("issues"), counts);
        countSeverities(structuredResult.get("findings"), counts);
        return counts;
    }

    private void countSeverities(Object node, int[] counts)
    {
        if (node instanceof Collection<?> collection)
        {
            for (Object item : collection)
            {
                countSeverities(item, counts);
            }
            return;
        }
        if (!(node instanceof Map<?, ?> map))
        {
            return;
        }
        Object severity = firstNonNull(map.get("severity"), map.get("level"), map.get("priority"));
        if (severity == null)
        {
            // 有评论对象但无级别时按警告计，避免“有意见却显示通过”
            counts[1]++;
            return;
        }
        String value = String.valueOf(severity).trim().toLowerCase(Locale.ROOT);
        if (BLOCK_SEVERITIES.contains(value))
        {
            counts[0]++;
        }
        else if (INFO_SEVERITIES.contains(value))
        {
            // 信息级不抬升结论
        }
        else
        {
            counts[1]++;
        }
    }

    private Object firstNonNull(Object... values)
    {
        for (Object value : values)
        {
            if (value != null)
            {
                return value;
            }
        }
        return null;
    }
}
