package com.acr.review.scope;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Component;
import com.acr.review.domain.ReviewPipelineConstants;

/**
 * 审查范围决策：把解析后的 Diff 文件分类为 记录类 / 排除 / 高影响扩展 / 普通纳入，
 * 拼装 scoped diff 并按两段式预算截断。纯确定性逻辑，无 I/O、不调用模型。
 *
 * 分类顺序（排除优先于高影响）：
 * 记录类（删除/二进制/纯改名/gitlink/仅 mode/空文件）→ 默认排除 → 项目排除 → 测试文件 → 高影响 → 普通。
 * 预算：L0 hunk 优先保留，超限时从普通文件起按文件边界整丢并记录；扩展全文由执行层竞争剩余预算。
 */
@Component
public class ReviewScopeDecisionService
{
    /** 高影响规则截断优先级：SECURITY > DEPENDENCY > DB_SCRIPT > CONFIG > SIGNATURE > NEW_FILE。 */
    private static final List<String> RULE_PRIORITY = List.of(
        ReviewScopeRules.RULE_SECURITY,
        ReviewScopeRules.RULE_DEPENDENCY,
        ReviewScopeRules.RULE_DB_SCRIPT,
        ReviewScopeRules.RULE_CONFIG,
        ReviewScopeRules.RULE_SIGNATURE,
        ReviewScopeRules.RULE_NEW_FILE);

    public ReviewScopeDecision decide(DiffParseResult parsed, ReviewScopeConfig config)
    {
        ReviewScopeConfig effective = config == null ? ReviewScopeConfig.defaults() : config;
        DiffParseResult source = parsed == null
            ? new DiffParseResult(List.of(), List.of())
            : parsed;

        List<ReviewScopeDecision.ExcludedFile> excludedFiles = new ArrayList<>();
        List<ReviewScopeDecision.RecordOnlyFile> recordOnlyFiles = new ArrayList<>();
        List<Candidate> expanded = new ArrayList<>();
        List<Candidate> normal = new ArrayList<>();

        for (DiffFileChange file : source.files())
        {
            String path = file.effectivePath();
            if (path == null)
            {
                continue;
            }

            String recordReason = recordOnlyReason(file);
            if (recordReason != null)
            {
                recordOnlyFiles.add(new ReviewScopeDecision.RecordOnlyFile(path, recordReason));
                continue;
            }

            String excludeReason = excludeReason(file, effective);
            if (excludeReason != null)
            {
                excludedFiles.add(new ReviewScopeDecision.ExcludedFile(path, excludeReason));
                continue;
            }

            if (effective.expandEnabled())
            {
                String rule = highImpactRule(file);
                if (rule != null)
                {
                    boolean needsFullContent = file.changeType() != DiffChangeType.ADDED;
                    expanded.add(new Candidate(file, rule, needsFullContent));
                    continue;
                }
            }
            normal.add(new Candidate(file, null, false));
        }

        expanded.sort(Comparator.comparingInt(candidate -> RULE_PRIORITY.indexOf(candidate.rule())));

        // L0 预算两段式（设计 4.1①「从普通文件起丢」）：
        // 段 A：expanded 按规则优先级占预算，单/累计超限的整文件丢弃并置 expandedOverflow；
        // 段 B：仅当 expanded 未溢出时，普通文件在剩余预算内纳入（溢出整文件丢）；
        // expanded 一旦溢出，普通文件全部让位记 dropped——保证审查聚焦高影响，不顶占漏审的扩展预算。
        StringBuilder scopedDiff = new StringBuilder();
        List<String> includedFiles = new ArrayList<>();
        List<String> droppedFiles = new ArrayList<>();
        boolean expandedOverflow = false;
        for (Candidate candidate : expanded)
        {
            String section = candidate.file().rawSection();
            int nextLength = scopedDiff.length() + section.length() + (scopedDiff.length() == 0 ? 0 : 1);
            if (nextLength > ReviewPipelineConstants.MAX_DIFF_CHARS)
            {
                droppedFiles.add(candidate.path());
                expandedOverflow = true;
                continue;
            }
            if (scopedDiff.length() > 0)
            {
                scopedDiff.append('\n');
            }
            scopedDiff.append(section);
            includedFiles.add(candidate.path());
        }
        if (!expandedOverflow)
        {
            for (Candidate candidate : normal)
            {
                String section = candidate.file().rawSection();
                int nextLength = scopedDiff.length() + section.length() + (scopedDiff.length() == 0 ? 0 : 1);
                if (nextLength > ReviewPipelineConstants.MAX_DIFF_CHARS)
                {
                    droppedFiles.add(candidate.path());
                    continue;
                }
                if (scopedDiff.length() > 0)
                {
                    scopedDiff.append('\n');
                }
                scopedDiff.append(section);
                includedFiles.add(candidate.path());
            }
        }
        else
        {
            // expanded 溢出：普通文件让位，整文件丢弃不挤占扩展预算
            for (Candidate candidate : normal)
            {
                droppedFiles.add(candidate.path());
            }
        }

        List<ReviewScopeDecision.ExpandedFile> expandedFiles = expanded.stream()
            .filter(candidate -> !droppedFiles.contains(candidate.path()))
            .map(candidate -> new ReviewScopeDecision.ExpandedFile(
                candidate.path(), candidate.rule(), candidate.needsFullContent()))
            .toList();

        return new ReviewScopeDecision(
            scopedDiff.toString(), includedFiles, excludedFiles, expandedFiles, recordOnlyFiles,
            droppedFiles, !droppedFiles.isEmpty(),
            source.warnings() == null ? List.of() : source.warnings());
    }

    /** 记录类判定：删除、二进制、gitlink、仅 mode 变更、无内容的新增空文件、无 hunk 的纯改名。 */
    private String recordOnlyReason(DiffFileChange file)
    {
        if (file.changeType() == DiffChangeType.DELETED)
        {
            return ReviewScopeRules.RECORD_DELETED;
        }
        if (file.binary())
        {
            return ReviewScopeRules.RECORD_BINARY;
        }
        if (file.gitlink())
        {
            return ReviewScopeRules.RECORD_GITLINK;
        }
        if (file.modeOnly())
        {
            return ReviewScopeRules.RECORD_MODE_ONLY;
        }
        if (!file.hasHunks())
        {
            if (file.changeType() == DiffChangeType.RENAMED)
            {
                return ReviewScopeRules.RECORD_RENAME_ONLY;
            }
            if (file.changeType() == DiffChangeType.ADDED)
            {
                return ReviewScopeRules.RECORD_EMPTY;
            }
        }
        return null;
    }

    private String excludeReason(DiffFileChange file, ReviewScopeConfig config)
    {
        String path = file.effectivePath();
        if (ReviewScopeRules.matchesAny(ReviewScopeRules.DEFAULT_EXCLUDE_GLOBS, path))
        {
            return ReviewScopeRules.EXCLUDE_DEFAULT;
        }
        if (ReviewScopeRules.matchesAny(config.excludePatterns(), path))
        {
            return ReviewScopeRules.EXCLUDE_PROJECT;
        }
        if (!config.includeTests() && ReviewScopeRules.matchesAny(ReviewScopeRules.TEST_FILE_GLOBS, path))
        {
            return ReviewScopeRules.EXCLUDE_TEST;
        }
        return null;
    }

    /** 高影响规则判定（首中即停，优先级同 RULE_PRIORITY）；未命中且为新增文件时记 NEW_FILE。 */
    private String highImpactRule(DiffFileChange file)
    {
        String path = file.effectivePath();
        if (ReviewScopeRules.isSecurityPath(path))
        {
            return ReviewScopeRules.RULE_SECURITY;
        }
        if (ReviewScopeRules.isDependencyManifest(path))
        {
            return ReviewScopeRules.RULE_DEPENDENCY;
        }
        if (ReviewScopeRules.matchesAny(ReviewScopeRules.DB_SCRIPT_GLOBS, path))
        {
            return ReviewScopeRules.RULE_DB_SCRIPT;
        }
        if (ReviewScopeRules.matchesAny(ReviewScopeRules.CONFIG_FILE_GLOBS, path))
        {
            return ReviewScopeRules.RULE_CONFIG;
        }
        if (file.changeType() != DiffChangeType.ADDED
            && ReviewScopeRules.hitsPublicSignature(path, file.changedLines()))
        {
            return ReviewScopeRules.RULE_SIGNATURE;
        }
        if (file.changeType() == DiffChangeType.ADDED)
        {
            return ReviewScopeRules.RULE_NEW_FILE;
        }
        return null;
    }

    private record Candidate(DiffFileChange file, String rule, boolean needsFullContent)
    {
        private String path()
        {
            return file.effectivePath();
        }
    }
}
