package com.acr.review.service;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;
import com.acr.review.domain.ReviewPipelineConstants;
import com.acr.review.domain.ReviewTask;

/** 将提示词模板渲染为可调用模型的完整 Prompt（占位符替换，不含公共协议追加）。 */
@Component
public class ReviewPromptRenderer
{
    /** 单趟扫描替换，替换值中即使含 {{...}} 字面量也不会被二次展开。 */
    private static final java.util.regex.Pattern PLACEHOLDER = java.util.regex.Pattern.compile("\\{\\{(\\w+)\\}\\}");

    public String render(String template, ReviewTask task, String diffContent)
    {
        return render(template, task, diffContent, null, null);
    }

    public String render(String template, ReviewTask task, String diffContent,
                         String prDescription, String commitMessages)
    {
        String content = template == null ? "" : template;
        String diff = diffContent == null ? "" : diffContent;
        if (diff.length() > ReviewPipelineConstants.MAX_DIFF_CHARS)
        {
            diff = diff.substring(0, ReviewPipelineConstants.MAX_DIFF_CHARS)
                + "\n\n/* Diff 过长，已截断，仅审查前序变更 */\n";
        }
        Map<String, String> values = new LinkedHashMap<>();
        values.put("pr_title", nullToEmpty(task == null ? null : task.getPrTitle()));
        values.put("pr_description", truncate(nullToEmpty(prDescription), ReviewScoringConstants.MAX_PR_DESCRIPTION_CHARS));
        values.put("commit_messages", truncate(nullToEmpty(commitMessages), ReviewScoringConstants.MAX_COMMIT_MESSAGES_CHARS));
        values.put("source_branch", nullToEmpty(task == null ? null : task.getSourceBranch()));
        values.put("target_branch", nullToEmpty(task == null ? null : task.getTargetBranch()));
        values.put("base_sha", nullToEmpty(task == null ? null : task.getBaseSha()));
        values.put("head_sha", nullToEmpty(task == null ? null : task.getHeadSha()));
        values.put("diff", diff);

        java.util.regex.Matcher matcher = PLACEHOLDER.matcher(content);
        StringBuilder rendered = new StringBuilder();
        while (matcher.find())
        {
            String value = values.get(matcher.group(1));
            // 未知占位符原样保留；替换值按字面量处理，不二次解析
            matcher.appendReplacement(rendered,
                java.util.regex.Matcher.quoteReplacement(value == null ? matcher.group(0) : value));
        }
        matcher.appendTail(rendered);
        return rendered.toString();
    }

    private String nullToEmpty(String value)
    {
        return value == null ? "" : value;
    }

    private String truncate(String value, int max)
    {
        if (value == null)
        {
            return "";
        }
        return value.length() <= max ? value : value.substring(0, max) + "\n/* 已截断 */";
    }
}
