package com.acr.review.service;

import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * 组合最终审查 Prompt：剥离模板中冲突的旧输出协议，再追加平台公共协议。
 */
@Component
public class ReviewPromptComposer
{
    private static final Pattern OLD_JSON_OUTPUT_BLOCK = Pattern.compile(
        "(?s)(?:请用中文输出可执行建议。\\s*)?若可能，按 JSON 返回：\\s*\\{.*?\\}\\s*不要编造未在 Diff 中出现的文件或行号。?");

    private static final Pattern OLD_JSON_OUTPUT_BLOCK_ALT = Pattern.compile(
        "(?s)请按以下 JSON 格式返回[：:]\\s*\\{.*?\"comments\".*?\\}");

    private static final Pattern MARKDOWN_JSON_FENCE_HINT = Pattern.compile(
        "(?s)```json\\s*\\{\\s*\"summary\".*?```");

    public String stripConflictingOutputInstructions(String templateBody)
    {
        String content = templateBody == null ? "" : templateBody;
        content = OLD_JSON_OUTPUT_BLOCK.matcher(content).replaceAll("");
        content = OLD_JSON_OUTPUT_BLOCK_ALT.matcher(content).replaceAll("");
        content = MARKDOWN_JSON_FENCE_HINT.matcher(content).replaceAll("");
        return content.replaceAll("\\n{3,}", "\n\n").trim();
    }

    public String composeFinalPrompt(String templateBody, String renderedContextBody)
    {
        String focus = stripConflictingOutputInstructions(templateBody);
        String context = renderedContextBody == null ? "" : renderedContextBody.trim();
        StringBuilder builder = new StringBuilder();
        if (!focus.isEmpty())
        {
            builder.append(focus).append("\n\n");
        }
        if (!context.isEmpty())
        {
            builder.append(context).append("\n\n");
        }
        builder.append(ReviewScoringConstants.protocolAppendix());
        return builder.toString().trim();
    }

    /**
     * 模板快照正文可能已含占位符与上下文骨架；执行时先剥离冲突，再渲染占位符，最后追加协议。
     */
    public String composeFromSnapshotTemplate(String snapshotTemplateContent, String renderedWithPlaceholders)
    {
        String strippedRendered = stripConflictingOutputInstructions(renderedWithPlaceholders);
        String appendix = ReviewScoringConstants.protocolAppendix();
        if (strippedRendered.contains("平台公共评分标准与输出协议"))
        {
            return strippedRendered.trim();
        }
        return (strippedRendered.trim() + "\n\n" + appendix).trim();
    }
}
