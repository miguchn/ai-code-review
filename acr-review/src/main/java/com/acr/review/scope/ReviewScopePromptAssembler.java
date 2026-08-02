package com.acr.review.scope;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import com.acr.review.domain.ReviewPipelineConstants;

/**
 * 范围 Prompt 拼装：在决策服务输出的 L0 scoped diff 之后，按剩余预算追加高影响扩展文件全文段。
 * 纯逻辑无 I/O：全文由执行层拉取后作为入参传入。
 *
 * 两段式预算：决策服务已按 MAX_DIFF_CHARS 截断 L0；本类在剩余预算内按决策优先级顺序
 * 整文件纳入或整文件跳过（BUDGET_SKIPPED），不半切文件内容误导模型。
 * 拉取失败（DEGRADED）与数量上限（FETCH_LIMIT_SKIPPED）只影响全文段，L0 hunk 始终保留。
 */
@Component
public class ReviewScopePromptAssembler
{
    /** 新增文件：完整内容已在 Diff hunk 中，无需拉取。 */
    public static final String STATUS_IN_DIFF = "IN_DIFF";
    /** 全文已纳入 Prompt。 */
    public static final String STATUS_FULL = "FULL";
    /** 全文拉取成功但剩余预算不足，整文件跳过。 */
    public static final String STATUS_BUDGET_SKIPPED = "BUDGET_SKIPPED";
    /** 全文拉取失败，降级保留 L0。 */
    public static final String STATUS_DEGRADED = "DEGRADED";
    /** 超出单次拉取数量上限，未尝试拉取。 */
    public static final String STATUS_FETCH_LIMIT_SKIPPED = "FETCH_LIMIT_SKIPPED";

    /** 需要执行层拉取全文的扩展文件清单（决策优先级顺序，数量受 MAX_EXPANDED_FETCH_COUNT 保护）。 */
    public List<ReviewScopeDecision.ExpandedFile> planFetches(ReviewScopeDecision decision)
    {
        if (decision == null)
        {
            return List.of();
        }
        List<ReviewScopeDecision.ExpandedFile> plan = new ArrayList<>();
        for (ReviewScopeDecision.ExpandedFile file : decision.expandedFiles())
        {
            if (!file.needsFullContent())
            {
                continue;
            }
            if (plan.size() >= ReviewPipelineConstants.MAX_EXPANDED_FETCH_COUNT)
            {
                break;
            }
            plan.add(file);
        }
        return plan;
    }

    /**
     * 拼装最终注入 {{diff}} 的内容：scoped diff + 预算内的扩展全文段。
     *
     * @param fetchedContents 拉取成功的全文（path → content），仅包含本次实际尝试的文件
     * @param fetchFailures   拉取失败原因（path → reason）
     */
    public ReviewScopeAssembly assemble(ReviewScopeDecision decision,
                                        Map<String, String> fetchedContents,
                                        Map<String, String> fetchFailures)
    {
        StringBuilder diff = new StringBuilder(decision.scopedDiff());
        long remaining = ReviewPipelineConstants.MAX_DIFF_CHARS - diff.length();
        List<ExpandedFileDisposition> dispositions = new ArrayList<>();

        for (ReviewScopeDecision.ExpandedFile file : decision.expandedFiles())
        {
            if (!file.needsFullContent())
            {
                dispositions.add(new ExpandedFileDisposition(file.path(), file.rule(), STATUS_IN_DIFF, null, 0));
                continue;
            }
            String failure = fetchFailures == null ? null : fetchFailures.get(file.path());
            if (failure != null)
            {
                dispositions.add(new ExpandedFileDisposition(file.path(), file.rule(), STATUS_DEGRADED, failure, 0));
                continue;
            }
            String content = fetchedContents == null ? null : fetchedContents.get(file.path());
            if (content == null)
            {
                dispositions.add(new ExpandedFileDisposition(file.path(), file.rule(), STATUS_FETCH_LIMIT_SKIPPED, null, 0));
                continue;
            }
            String section = fullContentSection(file, content);
            if (section.length() > remaining)
            {
                dispositions.add(new ExpandedFileDisposition(file.path(), file.rule(), STATUS_BUDGET_SKIPPED, null, 0));
                continue;
            }
            diff.append(section);
            remaining -= section.length();
            dispositions.add(new ExpandedFileDisposition(file.path(), file.rule(), STATUS_FULL, null, content.length()));
        }
        return new ReviewScopeAssembly(diff.toString(), dispositions);
    }

    /** 全文段统一追加在 scoped diff 末尾，标题明确标记规则与路径，供模型与排障识别。 */
    private String fullContentSection(ReviewScopeDecision.ExpandedFile file, String content)
    {
        return "\n\n===== 高影响扩展文件完整内容（规则：" + file.rule() + "，变更行见上方 Diff）：" + file.path()
            + " =====\n" + content;
    }

    /** 拼装输出。 */
    public record ReviewScopeAssembly(String diffForPrompt, List<ExpandedFileDisposition> dispositions)
    {
        public ReviewScopeAssembly
        {
            dispositions = List.copyOf(dispositions);
        }

        /** 是否存在成功纳入全文的扩展文件（决定范围指令块是否含扩展说明段）。 */
        public boolean hasFullContent()
        {
            return dispositions.stream().anyMatch(d -> STATUS_FULL.equals(d.status()));
        }
    }

    /** 单个扩展文件的最终处置。 */
    public record ExpandedFileDisposition(String path, String rule, String status, String reason, int chars)
    {
    }
}
