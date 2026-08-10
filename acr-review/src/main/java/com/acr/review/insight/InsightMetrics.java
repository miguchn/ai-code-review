package com.acr.review.insight;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** 指标字典与聚合口径工具。 */
public final class InsightMetrics
{
    private InsightMetrics()
    {
    }

    /**
     * 成功任务 duration_ms 的 P95：升序样本取 ceil(n*0.95)-1 下标；空样本返回 0。
     */
    public static long percentile95(List<Long> ascendingDurations)
    {
        if (ascendingDurations == null || ascendingDurations.isEmpty())
        {
            return 0L;
        }
        List<Long> sorted = new ArrayList<>(ascendingDurations);
        Collections.sort(sorted);
        int index = (int) Math.ceil(sorted.size() * 0.95d) - 1;
        if (index < 0)
        {
            index = 0;
        }
        if (index >= sorted.size())
        {
            index = sorted.size() - 1;
        }
        Long value = sorted.get(index);
        return value == null ? 0L : value;
    }

    public static double ratio(long numerator, long denominator)
    {
        if (denominator <= 0L)
        {
            return 0d;
        }
        return (double) numerator / (double) denominator;
    }

    /** 环比变化率：(current - previous) / previous；上期为 0 时返回 null。 */
    public static Double periodChangeRatio(double current, double previous)
    {
        if (previous == 0d)
        {
            return null;
        }
        return (current - previous) / previous;
    }

    public static List<InsightMetricDef> dictionary()
    {
        List<InsightMetricDef> list = new ArrayList<>();
        list.add(def("coverageRate", "有效审查覆盖率",
            "SUCCESS 且至少一次投递 SUCCESS 的任务数 / ACCEPTED 事件数", "ratio"));
        list.add(def("successRate", "审查成功率",
            "SUCCESS 任务数 / 终态任务数（SUCCESS+FAILED）", "ratio"));
        list.add(def("durationP95Ms", "P95 审查时延",
            "成功任务 duration_ms 升序后取 95 分位", "duration"));
        list.add(def("openFocusIssues", "未处置重点问题数",
            "CRITICAL+HIGH 且状态为待确认/待修复/待复核的问题数（当前库存）", "count"));
        list.add(def("issueNew", "新增问题数",
            "origin=NEW 的物化问题数，按日/严重度拆分", "count"));
        list.add(def("dispositionRate", "处置率",
            "(确认动作数 + 关闭数) / 新增问题数（窗口内）", "ratio"));
        list.add(def("falsePositiveRate", "误报率",
            "FALSE_POSITIVE 关闭数 / 全部关闭数（含误报）", "ratio"));
        list.add(def("deliverySuccessRate", "交付成功率",
            "投递 SUCCESS / 全部终态投递尝试", "ratio"));
        list.add(def("recheckRate", "复核流转率",
            "进入 RECHECKING 的问题占比（二期完善；一期仅字典占位）", "ratio"));
        list.add(def("commitActivity", "提交活跃度",
            "commit_fact 按日/项目/身份计数（二期）", "count"));
        return list;
    }

    private static InsightMetricDef def(String code, String name, String definition, String valueType)
    {
        InsightMetricDef item = new InsightMetricDef();
        item.setCode(code);
        item.setName(name);
        item.setDefinition(definition);
        item.setValueType(valueType);
        return item;
    }
}
