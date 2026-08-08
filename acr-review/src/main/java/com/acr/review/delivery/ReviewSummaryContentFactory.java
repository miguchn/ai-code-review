package com.acr.review.delivery;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import com.acr.common.utils.StringUtils;
import com.acr.review.domain.ReviewPipelineConstants;
import com.acr.review.domain.ReviewProject;
import com.acr.review.domain.ReviewRoundReconcileResult;
import com.acr.review.domain.ReviewTask;
import com.acr.review.domain.ReviewTaskRun;
import com.acr.review.domain.result.ReviewScopeStats;
import com.acr.review.domain.result.ReviewTopIssue;
import com.acr.review.git.GitProviderCodes;
import com.acr.system.service.ISysConfigService;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;

/** 从 task/run/project 装配 {@link ReviewSummaryContent}。 */
@Component
public class ReviewSummaryContentFactory
{
    private static final Pattern GITHUB_SSH_URL = Pattern.compile(
        "^git@github\\.com:([^/]+)/([^/]+?)(?:\\.git)?/?$", Pattern.CASE_INSENSITIVE);

    private final ISysConfigService configService;

    public ReviewSummaryContentFactory(ISysConfigService configService)
    {
        this.configService = configService;
    }

    public ReviewSummaryContent build(ReviewTask task, ReviewTaskRun run, ReviewProject project)
    {
        return build(task, run, project, null);
    }

    public ReviewSummaryContent build(ReviewTask task, ReviewTaskRun run, ReviewProject project,
                                      ReviewRoundReconcileResult reconcile)
    {
        String conclusion = task == null ? null : task.getReviewConclusion();
        String headSha = shortSha(task == null ? null : task.getHeadSha());
        if (StringUtils.isEmpty(headSha) && run != null)
        {
            headSha = shortSha(run.getSnapshotHeadSha());
        }

        ReviewSummaryContent.Builder builder = ReviewSummaryContent.builder()
            .taskStatus(task == null ? null : task.getTaskStatus())
            .taskId(task == null ? null : task.getTaskId())
            .conclusion(conclusion)
            .conclusionLabel(ReviewCommentBodyRenderer.conclusionLabel(conclusion))
            .totalScore(firstNonNull(
                task == null ? null : task.getTotalScore(),
                run == null ? null : run.getTotalScore()))
            .headShaShort(StringUtils.isEmpty(headSha) ? null : headSha)
            .prNumber(task == null ? null : task.getPrNumber())
            .prTitle(task == null ? null : task.getPrTitle())
            .prAuthor(task == null ? null : task.getPrAuthor())
            .sourceBranch(task == null ? null : task.getSourceBranch())
            .targetBranch(task == null ? null : task.getTargetBranch())
            .changedFiles(task == null ? null : task.getChangedFiles())
            .additions(task == null ? null : task.getAdditions())
            .deletions(task == null ? null : task.getDeletions())
            .topIssues(resolveTopIssues(run))
            .recheckingTitles(reconcile == null ? List.of() : reconcile.recheckingTitles())
            .scopeStats(resolveScopeStats(run))
            .failureType(task == null ? null : task.getFailureType())
            .failureTypeLabel(failureTypeLabel(task == null ? null : task.getFailureType()))
            .commitMessage(resolveCommitMessage(run))
            .summaryText(run == null ? null : run.getResultSummary())
            .reviewTime(run == null ? null : run.getFinishedTime());

        if (project != null)
        {
            builder.repositoryOwner(project.getRepositoryOwner())
                .repositoryName(project.getRepositoryName());
            builder.prUrl(buildMergeRequestUrl(project, task == null ? null : task.getPrNumber()));
        }

        builder.projectName(firstNonEmpty(
                project == null ? null : project.getProjectName(),
                task == null ? null : task.getProjectName()))
            .businessSystemName(firstNonEmpty(
                project == null ? null : project.getBusinessSystemName(),
                task == null ? null : task.getBusinessSystemName()));

        builder.detailUrl(buildDetailUrl(task));
        return builder.build();
    }

    /** 从 run 解析 Top3（优先 topIssuesJson 列，否则 resultJson.topIssues）。 */
    public static List<ReviewTopIssue> resolveTopIssues(ReviewTaskRun run)
    {
        if (run == null)
        {
            return List.of();
        }
        List<ReviewTopIssue> fromColumn = parseTopIssues(run.getTopIssuesJson());
        if (!fromColumn.isEmpty())
        {
            return fromColumn;
        }
        JSONObject result = parseJsonObject(run.getResultJson());
        if (result == null)
        {
            return List.of();
        }
        JSONArray array = result.getJSONArray("topIssues");
        return array == null ? List.of() : parseTopIssues(array.toJSONString());
    }

    /** 从 run.resultJson 解析 scopeStats。 */
    static ReviewScopeStats resolveScopeStats(ReviewTaskRun run)
    {
        if (run == null || StringUtils.isEmpty(run.getResultJson()))
        {
            return null;
        }
        JSONObject result = parseJsonObject(run.getResultJson());
        if (result == null || !result.containsKey("scopeStats"))
        {
            return null;
        }
        try
        {
            return result.getObject("scopeStats", ReviewScopeStats.class);
        }
        catch (Exception ex)
        {
            return null;
        }
    }

    /** 取 commitMessages 首行非空文本作为提交信息展示。 */
    static String resolveCommitMessage(ReviewTaskRun run)
    {
        if (run == null || StringUtils.isEmpty(run.getCommitMessages()))
        {
            return null;
        }
        for (String line : run.getCommitMessages().split("\\R"))
        {
            if (StringUtils.isNotEmpty(line) && !line.isBlank())
            {
                return line.trim();
            }
        }
        return null;
    }

    static String failureTypeLabel(String failureType)
    {
        if (StringUtils.isEmpty(failureType))
        {
            return "未知";
        }
        return switch (failureType.trim())
        {
            case ReviewPipelineConstants.FAILURE_CONFIG_MISSING -> "配置缺失";
            case ReviewPipelineConstants.FAILURE_CREDENTIAL_ERROR -> "凭据错误";
            case ReviewPipelineConstants.FAILURE_WORKSPACE_PREPARE -> "工作区准备失败";
            case ReviewPipelineConstants.FAILURE_TIMEOUT -> "引擎超时";
            case ReviewPipelineConstants.FAILURE_ENGINE -> "引擎调用失败";
            case ReviewPipelineConstants.FAILURE_MODEL -> "模型调用失败";
            case ReviewPipelineConstants.FAILURE_RESULT_FORMAT -> "结果格式异常";
            case ReviewPipelineConstants.FAILURE_CONCURRENCY -> "并发超限";
            case ReviewPipelineConstants.FAILURE_RATE_LIMIT -> "API 限流";
            case ReviewPipelineConstants.FAILURE_UNKNOWN -> "未知错误";
            default -> failureType;
        };
    }

    public static String shortSha(String sha)
    {
        if (StringUtils.isEmpty(sha))
        {
            return "";
        }
        return sha.length() <= 7 ? sha : sha.substring(0, 7);
    }

    /**
     * IM/评论标题分数段：有总分时返回 {@code " · N/100"}，否则空串。
     * 避免 OCR 等无评分路径渲染出「· —」。
     */
    public static String scoreHeaderSuffix(Integer totalScore)
    {
        if (totalScore == null)
        {
            return "";
        }
        return " · " + totalScore + "/100";
    }

    /**
     * 评论表格「总分」单元格；{@code null} 表示整行不渲染。
     */
    public static String scoreTableCell(Integer totalScore)
    {
        if (totalScore == null)
        {
            return null;
        }
        return totalScore + " / 100";
    }

    private String buildDetailUrl(ReviewTask task)
    {
        if (task == null || task.getTaskId() == null)
        {
            return null;
        }
        String baseUrl = configService.selectConfigByKey(ReviewDeliveryConstants.UI_BASE_URL_CONFIG_KEY);
        if (StringUtils.isEmpty(baseUrl))
        {
            return null;
        }
        String normalizedBase = baseUrl.replaceAll("/+$", "");
        String path = detailPath(task.getTaskStatus(), task.getTaskId());
        return normalizedBase + path;
    }

    static String detailPath(String taskStatus, Long taskId)
    {
        if (ReviewPipelineConstants.TASK_SUCCESS.equals(taskStatus)
            || ReviewPipelineConstants.TASK_FAILED.equals(taskStatus))
        {
            return "/review/record-detail/index/" + taskId;
        }
        return "/review/task-detail/index/" + taskId;
    }

    static String buildMergeRequestUrl(ReviewProject project, Integer prNumber)
    {
        if (project == null || prNumber == null || prNumber <= 0)
        {
            return null;
        }
        String repoBase = resolveRepositoryWebBase(project);
        if (repoBase == null)
        {
            return null;
        }
        if (GitProviderCodes.GITLAB.equalsIgnoreCase(project.getProvider()))
        {
            return repoBase + "/-/merge_requests/" + prNumber;
        }
        // Gitee/Gitea 的 Web 路径为 /pulls/{n}；GitHub 为 /pull/{n}
        if (GitProviderCodes.GITEE.equalsIgnoreCase(project.getProvider())
            || GitProviderCodes.GITEA.equalsIgnoreCase(project.getProvider()))
        {
            return repoBase + "/pulls/" + prNumber;
        }
        return repoBase + "/pull/" + prNumber;
    }

    /** @deprecated 使用 {@link #buildMergeRequestUrl(ReviewProject, Integer)} */
    @Deprecated
    static String buildGithubPrUrl(ReviewProject project, Integer prNumber)
    {
        return buildMergeRequestUrl(project, prNumber);
    }

    static String resolveRepositoryWebBase(ReviewProject project)
    {
        if (project == null)
        {
            return null;
        }
        String fromUrl = resolveRepositoryWebBase(project.getRepositoryUrl());
        if (fromUrl != null)
        {
            return fromUrl;
        }
        String owner = project.getRepositoryOwner();
        String name = project.getRepositoryName();
        if (StringUtils.isEmpty(owner) || StringUtils.isEmpty(name))
        {
            return null;
        }
        if (GitProviderCodes.GITHUB.equalsIgnoreCase(project.getProvider()))
        {
            return "https://github.com/" + owner + "/" + name;
        }
        if (GitProviderCodes.GITEE.equalsIgnoreCase(project.getProvider()))
        {
            return "https://gitee.com/" + owner + "/" + name;
        }
        return null;
    }

    static String resolveRepositoryWebBase(String repositoryUrl)
    {
        if (StringUtils.isEmpty(repositoryUrl))
        {
            return null;
        }
        String value = repositoryUrl.trim();
        Matcher sshMatcher = GITHUB_SSH_URL.matcher(value);
        if (sshMatcher.matches())
        {
            return "https://github.com/" + sshMatcher.group(1) + "/" + stripGitSuffix(sshMatcher.group(2));
        }
        try
        {
            URI uri = new URI(value);
            if (uri.getHost() == null || uri.getScheme() == null)
            {
                return null;
            }
            String path = uri.getPath();
            while (path != null && (path.endsWith("/") || path.endsWith(".git")))
            {
                path = path.endsWith("/")
                    ? path.substring(0, path.length() - 1)
                    : path.substring(0, path.length() - 4);
            }
            StringBuilder base = new StringBuilder();
            base.append(uri.getScheme().toLowerCase()).append("://").append(uri.getHost().toLowerCase());
            if (uri.getPort() > 0)
            {
                base.append(':').append(uri.getPort());
            }
            if (path != null && !path.isBlank())
            {
                base.append(path);
            }
            return base.toString();
        }
        catch (URISyntaxException ex)
        {
            return null;
        }
    }

    /** @deprecated 使用 {@link #resolveRepositoryWebBase(String)} */
    @Deprecated
    static String resolveGithubWebBase(String repositoryUrl)
    {
        return resolveRepositoryWebBase(repositoryUrl);
    }

    private static List<ReviewTopIssue> parseTopIssues(String json)
    {
        if (StringUtils.isEmpty(json))
        {
            return List.of();
        }
        try
        {
            List<ReviewTopIssue> list = JSON.parseArray(json, ReviewTopIssue.class);
            return list == null ? List.of() : list;
        }
        catch (Exception ex)
        {
            return List.of();
        }
    }

    private static JSONObject parseJsonObject(String json)
    {
        if (StringUtils.isEmpty(json))
        {
            return null;
        }
        try
        {
            Object parsed = JSON.parse(json);
            if (parsed instanceof JSONObject object)
            {
                return object;
            }
            return null;
        }
        catch (Exception ex)
        {
            return null;
        }
    }

    private static String stripGitSuffix(String repository)
    {
        if (repository == null)
        {
            return "";
        }
        return repository.endsWith(".git") ? repository.substring(0, repository.length() - 4) : repository;
    }

    private static Integer firstNonNull(Integer a, Integer b)
    {
        return a != null ? a : b;
    }

    private static String firstNonEmpty(String a, String b)
    {
        if (StringUtils.isNotEmpty(a))
        {
            return a.trim();
        }
        if (StringUtils.isNotEmpty(b))
        {
            return b.trim();
        }
        return null;
    }
}
