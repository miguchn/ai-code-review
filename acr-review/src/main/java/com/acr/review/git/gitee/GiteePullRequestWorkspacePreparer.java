package com.acr.review.git.gitee;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.acr.review.domain.ReviewPipelineConstants;
import com.acr.review.git.GitProviderCodes;
import com.acr.review.git.GitPullRequestWorkspacePreparer;
import com.acr.review.git.GitPullRequestWorkspaceRequest;
import com.acr.review.git.GitPullRequestWorkspaceResult;

/**
 * 使用项目 Token 按需 fetch base/head SHA，为 OCR --from/--to 准备真实 Git 工作区。
 * 远端使用 oauth2 Token 注入 HTTPS URL，不出现在命令行参数与日志中。
 */
@Component
public class GiteePullRequestWorkspacePreparer implements GitPullRequestWorkspacePreparer
{
    private static final java.util.regex.Pattern SHA_PATTERN = java.util.regex.Pattern.compile("^[0-9a-fA-F]{4,64}$");
    private static final java.util.regex.Pattern TOKEN_PATTERN = java.util.regex.Pattern.compile("[A-Za-z0-9_]{16,}");

    private final int prepareTimeoutSeconds;

    public GiteePullRequestWorkspacePreparer(
        @Value("${review.gitee.workspace-prepare-timeout-seconds:180}") int prepareTimeoutSeconds)
    {
        this.prepareTimeoutSeconds = Math.max(30, prepareTimeoutSeconds);
    }

    @Override
    public String providerCode()
    {
        return GitProviderCodes.GITEE;
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
            return GitPullRequestWorkspaceResult.fail(ReviewPipelineConstants.FAILURE_CREDENTIAL_ERROR, "Gitee 凭据不可用");
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
        String remoteUrl = resolveRemoteUrl(request.repository(), token);
        try
        {
            Files.createDirectories(workspace);
            runGit(workspace, null, "init");
            runGit(workspace, null, "remote", "add", "origin", remoteUrl);
            runGit(workspace, null, "config", "core.sparseCheckout", "false");
            fetchCommit(workspace, remoteUrl, request.headSha());
            if (!request.baseSha().equals(request.headSha()))
            {
                fetchCommit(workspace, remoteUrl, request.baseSha());
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
                sanitize("准备 Gitee 审查工作区失败: " + ex.getMessage(), token));
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

    /** oauth2 风格 HTTPS 远端，Token 嵌入 URL 供 git fetch 使用。 */
    static String resolveRemoteUrl(com.acr.review.git.GitRepositoryCoordinates repository, String token)
    {
        String host = GitProviderCodes.DEFAULT_GITEE_SERVER.replace("https://", "");
        String path = repository.owner() + "/" + repository.repository() + ".git";
        if (token == null || token.isBlank())
        {
            return GitProviderCodes.DEFAULT_GITEE_SERVER + "/" + path;
        }
        return "https://oauth2:" + token + "@" + host + "/" + path;
    }

    private void fetchCommit(Path workspace, String remoteUrl, String sha)
        throws IOException, InterruptedException, WorkspacePrepareException
    {
        // 禁止 --depth：浅拉取会使 base/head 成为互不连通的浅根，OCR 无法选择 base..head 变更。
        runGit(workspace, null, buildFetchArgs(remoteUrl, sha));
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
        List<String> command = new ArrayList<>();
        command.add("git");
        command.add("-C");
        command.add(workspace.toString());
        for (String arg : args)
        {
            command.add(arg);
        }

        ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectErrorStream(true);
        builder.environment().put("GIT_TERMINAL_PROMPT", "0");
        Process process = builder.start();
        boolean finished = process.waitFor(prepareTimeoutSeconds, TimeUnit.SECONDS);
        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
        if (!finished)
        {
            process.destroyForcibly();
            throw new WorkspacePrepareException(ReviewPipelineConstants.FAILURE_TIMEOUT,
                "准备审查工作区超时（" + prepareTimeoutSeconds + " 秒）");
        }
        if (process.exitValue() != 0)
        {
            String detail = output.isBlank() ? "git 命令执行失败" : output.lines().findFirst().orElse(output);
            throw new WorkspacePrepareException(ReviewPipelineConstants.FAILURE_WORKSPACE_PREPARE,
                "git " + String.join(" ", args) + " 失败: " + detail);
        }
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
