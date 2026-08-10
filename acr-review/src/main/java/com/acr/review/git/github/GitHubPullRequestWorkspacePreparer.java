package com.acr.review.git.github;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.acr.review.domain.ReviewPipelineConstants;
import com.acr.review.git.GitCommandRunner;
import com.acr.review.git.GitPullRequestWorkspacePreparer;
import com.acr.review.git.GitPullRequestWorkspaceRequest;
import com.acr.review.git.GitPullRequestWorkspaceResult;

/**
 * 使用项目 PAT 按需 fetch base/head SHA，为 OCR --from/--to 准备真实 Git 工作区。
 * Token 仅通过进程环境变量注入 git 配置（GIT_CONFIG_*），不出现在命令行参数、日志或落盘文件中。
 */
@Component
public class GitHubPullRequestWorkspacePreparer implements GitPullRequestWorkspacePreparer
{
    private static final java.util.regex.Pattern SHA_PATTERN = java.util.regex.Pattern.compile("^[0-9a-fA-F]{4,64}$");
    private static final java.util.regex.Pattern TOKEN_PATTERN = java.util.regex.Pattern.compile(
        "(ghp|gho|ghu|ghs|ghr)_[A-Za-z0-9_]{20,}|github_pat_[A-Za-z0-9_]{20,}");

    private final int prepareTimeoutSeconds;
    private final GitCommandRunner gitCommandRunner;

    public GitHubPullRequestWorkspacePreparer(GitCommandRunner gitCommandRunner,
        @Value("${review.github.workspace-prepare-timeout-seconds:180}") int prepareTimeoutSeconds)
    {
        this.gitCommandRunner = gitCommandRunner;
        this.prepareTimeoutSeconds = Math.max(30, prepareTimeoutSeconds);
    }

    @Override
    public String providerCode()
    {
        return "GITHUB";
    }

    @Override
    public GitPullRequestWorkspaceResult prepare(GitPullRequestWorkspaceRequest request)
    {
        if (request == null || request.repository() == null)
        {
            return GitPullRequestWorkspaceResult.fail(ReviewPipelineConstants.FAILURE_WORKSPACE_PREPARE, "仓库信息不完整");
        }
        String token;
        try
        {
            token = request.access().requireToken();
        }
        catch (IllegalArgumentException ex)
        {
            return GitPullRequestWorkspaceResult.fail(ReviewPipelineConstants.FAILURE_CREDENTIAL_ERROR, "GitHub 凭据不可用");
        }
        if (!isValidSha(request.baseSha()) || !isValidSha(request.headSha()))
        {
            return GitPullRequestWorkspaceResult.fail(ReviewPipelineConstants.FAILURE_WORKSPACE_PREPARE, "base/head SHA 格式非法，无法准备审查范围");
        }
        if (request.workingDirectory() == null || request.workingDirectory().isBlank())
        {
            return GitPullRequestWorkspaceResult.fail(ReviewPipelineConstants.FAILURE_WORKSPACE_PREPARE, "工作目录不能为空");
        }

        Path workspace = Path.of(request.workingDirectory()).toAbsolutePath().normalize();
        String remoteUrl = resolveRemoteUrl(request.repository());
        try
        {
            Files.createDirectories(workspace);
            runGit(workspace, null, "init");
            runGit(workspace, null, "remote", "add", "origin", remoteUrl);
            runGit(workspace, null, "config", "core.sparseCheckout", "false");
            fetchCommit(workspace, token, request.headSha());
            if (!request.baseSha().equals(request.headSha()))
            {
                fetchCommit(workspace, token, request.baseSha());
            }
            runGit(workspace, null, "checkout", "--force", request.headSha());
            ensureCommitExists(workspace, request.baseSha());
            ensureCommitExists(workspace, request.headSha());
            return GitPullRequestWorkspaceResult.ok(workspace.toString(), request.baseSha(), request.headSha());
        }
        catch (IOException | InterruptedException ex)
        {
            if (ex instanceof InterruptedException)
            {
                Thread.currentThread().interrupt();
            }
            return GitPullRequestWorkspaceResult.fail(
                ReviewPipelineConstants.FAILURE_WORKSPACE_PREPARE,
                sanitize("准备 GitHub 审查工作区失败: " + ex.getMessage(), token));
        }
        catch (WorkspacePrepareException ex)
        {
            return GitPullRequestWorkspaceResult.fail(ex.failureType(), sanitize(ex.getMessage(), token));
        }
    }

    private static boolean isValidSha(String sha)
    {
        return sha != null && SHA_PATTERN.matcher(sha).matches();
    }

    /** 远端地址以项目接入时校验过的仓库 URL 为准，兼容 GitHub Enterprise；缺失时回退 github.com。 */
    static String resolveRemoteUrl(com.acr.review.git.GitRepositoryCoordinates repository)
    {
        String canonical = repository.canonicalUrl();
        if (canonical != null && !canonical.isBlank())
        {
            String url = canonical.trim();
            while (url.endsWith("/"))
            {
                url = url.substring(0, url.length() - 1);
            }
            if (url.endsWith(".git"))
            {
                url = url.substring(0, url.length() - 4);
            }
            return url + ".git";
        }
        return "https://github.com/" + repository.owner() + "/" + repository.repository() + ".git";
    }

    private void fetchCommit(Path workspace, String token, String sha)
        throws IOException, InterruptedException, WorkspacePrepareException
    {
        // 禁止 --depth：浅拉取会使 base/head 成为互不连通的浅根，OCR 无法选择 base..head 变更。
        runGit(workspace, token, buildFetchArgs("origin", sha));
    }

    /** 完整按 SHA fetch；供单测断言不含 --depth。 */
    static String[] buildFetchArgs(String remote, String sha)
    {
        return new String[] { "fetch", remote, sha };
    }

    private void ensureCommitExists(Path workspace, String sha)
        throws IOException, InterruptedException, WorkspacePrepareException
    {
        runGit(workspace, null, "cat-file", "-e", sha + "^{commit}");
    }

    private void runGit(Path workspace, String token, String... args)
        throws IOException, InterruptedException, WorkspacePrepareException
    {
        Map<String, String> environment = new HashMap<>();
        if (token != null && !token.isBlank())
        {
            // 通过环境变量注入 git 配置，避免 PAT 出现在进程命令行参数中（ps 可见）。
            // GitHub git smart HTTP 接受 Basic(x-access-token:PAT)，不接受 REST 常用的 Bearer。
            environment.put("GIT_CONFIG_COUNT", "1");
            environment.put("GIT_CONFIG_KEY_0", "http.extraHeader");
            environment.put("GIT_CONFIG_VALUE_0", buildAuthorizationExtraHeader(token));
        }
        GitCommandRunner.GitCommandResult result = gitCommandRunner.execute(
            workspace, environment, prepareTimeoutSeconds, args);
        if (result.timedOut())
        {
            throw new WorkspacePrepareException(ReviewPipelineConstants.FAILURE_TIMEOUT,
                "准备审查工作区超时（" + prepareTimeoutSeconds + " 秒）");
        }
        if (!result.successful())
        {
            String output = result.output() == null ? "" : result.output().trim();
            String detail = output.isBlank() ? "git 命令执行失败" : output.lines().findFirst().orElse(output);
            throw new WorkspacePrepareException(result.transientDependencyFailure()
                ? ReviewPipelineConstants.FAILURE_DEPENDENCY_UNAVAILABLE
                : ReviewPipelineConstants.FAILURE_WORKSPACE_PREPARE,
                "git " + String.join(" ", args) + " 失败: " + detail);
        }
    }

    /**
     * GitHub git over HTTPS 的 http.extraHeader 值：
     * {@code Authorization: Basic base64("x-access-token:" + token)}。
     */
    static String buildAuthorizationExtraHeader(String token)
    {
        String credentials = "x-access-token:" + token;
        String encoded = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
        return "Authorization: Basic " + encoded;
    }

    static String sanitize(String message, String token)
    {
        if (message == null)
        {
            return "准备审查工作区失败";
        }
        String sanitized = message;
        if (token != null && !token.isBlank())
        {
            sanitized = sanitized.replace(token, "***");
        }
        // 兜底脱敏：按 GitHub Token 常见格式再做一次正则替换，防止片段或变体泄露
        sanitized = TOKEN_PATTERN.matcher(sanitized).replaceAll("***");
        return sanitized.length() > 480 ? sanitized.substring(0, 480) : sanitized;
    }

    private static final class WorkspacePrepareException extends Exception
    {
        private final String failureType;

        private WorkspacePrepareException(String failureType, String message)
        {
            super(message);
            this.failureType = failureType;
        }

        private String failureType()
        {
            return failureType;
        }
    }
}
