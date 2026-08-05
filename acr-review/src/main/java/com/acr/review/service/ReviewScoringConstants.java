package com.acr.review.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.acr.review.domain.ReviewPlatformRules;

/** 平台统一评分与结果协议常量（代码管理，不进模板正文）。 */
public final class ReviewScoringConstants
{
    public static final String PROTOCOL_VERSION = "1.2";

    /**
     * 可兼容解析的协议版本。1.0/1.1 是 1.2 的子集，
     * 模型偶发回写旧版本号时按兼容解析，归属缺省视为 NEW。
     */
    public static final java.util.Set<String> COMPATIBLE_PROTOCOL_VERSIONS =
        java.util.Set.of("1.0", "1.1", "1.2");

    public static final String PARSE_SUCCESS = "SUCCESS";
    public static final String PARSE_FAILED = "FAILED";

    /** 问题归属（v1.1+）：本次变更引入 / 存量。 */
    public static final String ORIGIN_NEW = "NEW";
    public static final String ORIGIN_EXISTING = "EXISTING";

    public static final String DIM_CORRECTNESS = "CORRECTNESS";
    public static final String DIM_SECURITY = "SECURITY";
    public static final String DIM_PRACTICE = "PRACTICE";
    public static final String DIM_PERFORMANCE = "PERFORMANCE";
    public static final String DIM_COMMIT_QUALITY = "COMMIT_QUALITY";

    public static final int MAX_CORRECTNESS = 40;
    public static final int MAX_SECURITY = 30;
    public static final int MAX_PRACTICE = 20;
    public static final int MAX_PERFORMANCE = 5;
    public static final int MAX_COMMIT_QUALITY = 5;
    public static final int MAX_TOTAL = 100;
    /** 展示层 Top N（总结评论 / IM / 记录详情）。 */
    public static final int MAX_TOP_ISSUES = 3;
    /** 协议 v1.2 单轮问题清单解析上限。 */
    public static final int MAX_ISSUES = 20;

    public static final String CONFIG_MAX_ISSUES = "review.protocol.maxIssues";

    public static final String SEVERITY_CRITICAL = "CRITICAL";
    public static final String SEVERITY_HIGH = "HIGH";
    public static final String SEVERITY_MEDIUM = "MEDIUM";
    public static final String SEVERITY_LOW = "LOW";
    public static final String SEVERITY_INFO = "INFO";

    public static final int MAX_RAW_RESPONSE_CHARS = 8_000;
    public static final int MAX_RENDERED_PROMPT_CHARS = 512_000;
    public static final int MAX_PR_DESCRIPTION_CHARS = 4_000;
    public static final int MAX_COMMIT_MESSAGES_CHARS = 4_000;

    /** 评分维度定义：执行 Prompt、权重快照与模板页展示共用同一数据源。 */
    public record ScoreDimensionDef(String code, String displayName, int maxScore, String description)
    {
    }

    private static final List<ScoreDimensionDef> DIMENSIONS = List.of(
        new ScoreDimensionDef(DIM_CORRECTNESS, "功能正确性与健壮性", MAX_CORRECTNESS,
            "主要评估功能实现、边界条件、异常处理和容错能力"),
        new ScoreDimensionDef(DIM_SECURITY, "安全性与潜在风险", MAX_SECURITY,
            "主要评估注入、越权、敏感信息泄露及其他安全风险"),
        new ScoreDimensionDef(DIM_PRACTICE, "最佳实践与可维护性", MAX_PRACTICE,
            "主要评估代码结构、复杂度、重复代码、命名和维护成本"),
        new ScoreDimensionDef(DIM_PERFORMANCE, "性能与资源利用", MAX_PERFORMANCE,
            "主要评估明显性能问题、资源泄漏和无效消耗"),
        new ScoreDimensionDef(DIM_COMMIT_QUALITY, "提交信息质量", MAX_COMMIT_QUALITY,
            "主要评估 PR 描述和 Commit Message 的清晰度与完整性"));

    private static final String TOP_ISSUES_HINT = "展示层仅呈现最重要的 Top " + MAX_TOP_ISSUES + " 问题；协议输出全量清单。";

    private static final String UI_HINT =
        "模板正文只需维护当前技术栈的专项审查重点；平台会在执行时自动追加上述公共评分标准、全量问题清单规则与统一输出协议，无需在模板中重复编写评分标准或输出格式。";

    private ReviewScoringConstants()
    {
    }

    public static List<ScoreDimensionDef> scoreDimensions()
    {
        return DIMENSIONS;
    }

    public static Map<String, Integer> scoreWeights()
    {
        Map<String, Integer> weights = new LinkedHashMap<>();
        for (ScoreDimensionDef dimension : DIMENSIONS)
        {
            weights.put(dimension.code(), dimension.maxScore());
        }
        return weights;
    }

    public static List<String> requiredDimensions()
    {
        List<String> codes = new ArrayList<>(DIMENSIONS.size());
        for (ScoreDimensionDef dimension : DIMENSIONS)
        {
            codes.add(dimension.code());
        }
        return List.copyOf(codes);
    }

    /**
     * 审查范围指令块（M3.2）：约束模型只报告本次变更引入的问题。
     * 归属（origin）输出要求随协议另行追加，本块保持协议版本中立。
     *
     * @param scopeApplied    范围决策是否生效（降级全量 Diff 时为 false，不出现"已筛选"表述）
     * @param hasFullContent  是否附有高影响扩展文件全文段
     */
    public static String scopeInstructionBlock(boolean scopeApplied, boolean hasFullContent)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("【审查范围说明 — 必须遵守】\n");
        int index = 1;
        if (scopeApplied)
        {
            sb.append(index++).append(". 上方 Diff 已经过平台范围筛选，仅包含需要审查的变更文件；")
                .append("锁文件、生成代码与项目配置的排除路径已被移除，无需评论其内容。\n");
        }
        sb.append(index++).append(". 只报告本次变更引入的问题：问题必须定位在 Diff 的新增/修改行（+ 行）上；")
            .append("未变更的上下文行仅用于理解代码结构，禁止就其中的历史存量问题输出意见。\n");
        if (hasFullContent)
        {
            sb.append(index++).append(". Diff 后附「高影响扩展文件完整内容」段：这些文件命中高影响规则")
                .append("（新增文件/公共签名/安全逻辑/配置/依赖/数据库脚本），整个文件都在审查范围内，可报告其中的问题。\n");
        }
        sb.append(index).append(". 禁止编造未在提供内容中出现的文件路径或行号；无法确定位置时对应字段必须为 null。\n");
        return sb.toString();
    }

    /** 追加到最终 Prompt 的公共协议附录（中文，含模型输出技术要求）。 */
    public static String protocolAppendix()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("【平台公共评分标准与输出协议 v").append(PROTOCOL_VERSION).append(" — 不可忽略】\n");
        sb.append("你必须仅输出一个严格 JSON 对象，禁止 Markdown 代码块、解释性前缀或其它自由文本。\n\n");
        sb.append("评分维度（各维度直接按满分给分，不做百分制二次加权；总分由平台重算，勿依赖你自报的 totalScore）：\n");
        int index = 1;
        for (ScoreDimensionDef dimension : DIMENSIONS)
        {
            sb.append(index++).append(". ")
                .append(dimension.code()).append(' ')
                .append(dimension.displayName()).append("：满分 ")
                .append(dimension.maxScore()).append("。")
                .append(dimension.description());
            if (DIM_COMMIT_QUALITY.equals(dimension.code()))
            {
                sb.append("（必须结合 PR 标题、PR 描述与 Commit Message 判断）");
            }
            sb.append('\n');
        }
        sb.append("总分满分 ").append(MAX_TOTAL).append("。\n\n");
        sb.append("问题清单规则：\n");
        sb.append("- 输出发现的全部问题，按影响程度从高到低排序，最多 ").append(MAX_ISSUES).append(" 条；\n");
        sb.append("- focusIssueCount 可自报，平台会按 NEW 问题中 CRITICAL/HIGH 数量重算；\n");
        sb.append("- 每个问题字段：rank、severity、category、title、description、filePath、startLine、endLine、evidence、suggestion、origin；\n");
        sb.append("- severity 取值：CRITICAL|HIGH|MEDIUM|LOW|INFO；\n");
        sb.append("- origin 取值：NEW（本次变更引入）|EXISTING（存量问题）；平台以后端 Diff 行号映射为准覆写该字段，")
            .append("EXISTING 问题不计 focusIssueCount、不影响评分与结论；\n");
        sb.append("- 文件路径与行号无法准确确定时必须为 null，禁止伪造位置。\n\n");
        sb.append("JSON 必须包含字段：\n");
        sb.append("protocolVersion（固定 \"").append(PROTOCOL_VERSION).append("\"）、\n");
        sb.append("scores（上述五维，每项含 dimension、score、maxScore、reason）、\n");
        sb.append("totalScore（可填，平台会忽略并重算）、\n");
        sb.append("summary、\n");
        sb.append("topIssues、\n");
        sb.append("focusIssueCount、\n");
        sb.append("hasCriticalSecurityIssue（布尔）。\n");
        return sb.toString();
    }

    /** 模板管理页只读说明（与 {@link #platformRulesForUi()} 同源）。 */
    public static String protocolUiHint()
    {
        return UI_HINT;
    }

    /** 模板新增/编辑/详情页展示用的平台统一审查规则（不含 JSON Schema）。 */
    public static ReviewPlatformRules platformRulesForUi()
    {
        ReviewPlatformRules rules = new ReviewPlatformRules();
        rules.setProtocolVersion(PROTOCOL_VERSION);
        rules.setTitle("平台统一审查规则");
        rules.setUiHint(UI_HINT);
        rules.setTotalMaxScore(MAX_TOTAL);
        rules.setTopIssuesMax(MAX_TOP_ISSUES);
        rules.setTopIssuesHint(TOP_ISSUES_HINT);
        List<ReviewPlatformRules.Dimension> dimensions = new ArrayList<>(DIMENSIONS.size());
        for (ScoreDimensionDef dimension : DIMENSIONS)
        {
            dimensions.add(new ReviewPlatformRules.Dimension(
                dimension.code(),
                dimension.displayName(),
                dimension.maxScore(),
                dimension.description()));
        }
        rules.setDimensions(dimensions);
        return rules;
    }
}
