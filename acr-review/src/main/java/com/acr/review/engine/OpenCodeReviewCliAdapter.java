package com.acr.review.engine;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import com.acr.review.engine.config.ReviewEngineProperties;

/** 本地 open-code-review CLI 适配器。 */
@Component
public class OpenCodeReviewCliAdapter implements ReviewEngine
{
    private static final Pattern VERSION_PATTERN = Pattern.compile("open-code-review v([\\d.]+)");

    private final ReviewEngineProperties properties;
    private final ReviewEngineProcessRunner processRunner;
    private final ReviewEngineWorkspaceManager workspaceManager;
    private final ReviewEngineOutputParser outputParser;

    public OpenCodeReviewCliAdapter(ReviewEngineProperties properties, ReviewEngineProcessRunner processRunner,
        ReviewEngineWorkspaceManager workspaceManager, ReviewEngineOutputParser outputParser)
    {
        this.properties = properties;
        this.processRunner = processRunner;
        this.workspaceManager = workspaceManager;
        this.outputParser = outputParser;
    }

    @Override
    public ReviewEngineResult execute(ReviewEngineRequest request)
    {
        String engineName = properties.getEngineName();
        int timeoutSeconds = request.getTimeoutSeconds() > 0
            ? request.getTimeoutSeconds()
            : properties.getDefaultTimeoutSeconds();
        Path workingDirectory = resolveWorkingDirectory(request);
        List<String> command = buildCommand(request, workingDirectory);

        try
        {
            Map<String, String> environment = withPlainTextTerminal(request.getModelEnvironment());
            ReviewEngineProcessRunner.ProcessExecution execution = processRunner.execute(
                command, workingDirectory, environment, timeoutSeconds);
            String stdout = AnsiTextCleaner.strip(execution.stdout());
            String stderr = AnsiTextCleaner.strip(execution.stderr());
            String version = parseVersionFromOutput(stdout);
            if (request.getInvocationType() == ReviewEngineInvocationType.VERSION && version == null)
            {
                version = parseVersionFromOutput(stdout + "\n" + stderr);
            }

            if (execution.timedOut())
            {
                return ReviewEngineResult.failure(engineName, version, execution.durationMs(),
                    stdout, stderr, execution.exitCode(),
                    ReviewEngineFailureType.TIMEOUT, "CLI 执行超时");
            }

            Map<String, Object> structured;
            try
            {
                structured = outputParser.parse(stdout, request.getInvocationType());
            }
            catch (IllegalArgumentException ex)
            {
                return ReviewEngineResult.failure(engineName, version, execution.durationMs(),
                    stdout, stderr, execution.exitCode(),
                    ReviewEngineFailureType.OUTPUT_FORMAT_ERROR, ex.getMessage());
            }

            if (execution.exitCode() != 0)
            {
                ReviewEngineFailureType failureType = ReviewEngineProcessRunner.classifyExitFailure(
                    execution.exitCode(), stdout, stderr, request.getInvocationType());
                return ReviewEngineResult.failure(engineName, version, execution.durationMs(),
                    stdout, stderr, execution.exitCode(), failureType,
                    summarizeFailure(stderr, stdout));
            }

            return ReviewEngineResult.success(engineName, version, execution.durationMs(),
                stdout, stderr, structured, execution.exitCode());
        }
        catch (IOException ex)
        {
            ReviewEngineFailureType failureType = ReviewEngineProcessRunner.classifyStartupFailure(ex);
            return ReviewEngineResult.failure(engineName, null, 0, "", ex.getMessage(), null,
                failureType, failureType.getLabel());
        }
        catch (InterruptedException ex)
        {
            Thread.currentThread().interrupt();
            return ReviewEngineResult.failure(engineName, null, 0, "", ex.getMessage(), null,
                ReviewEngineFailureType.UNKNOWN, "CLI 执行被中断");
        }
    }

    private Path resolveWorkingDirectory(ReviewEngineRequest request)
    {
        if (request.getWorkingDirectory() == null || request.getWorkingDirectory().isBlank())
        {
            throw new IllegalArgumentException("工作目录不能为空");
        }
        Path path = Path.of(request.getWorkingDirectory());
        workspaceManager.validateWithinRoot(path);
        return workspaceManager.resolveExisting(path);
    }

    List<String> buildCommand(ReviewEngineRequest request, Path workingDirectory)
    {
        List<String> command = new ArrayList<>();
        command.add(properties.getExecutablePath());
        switch (request.getInvocationType())
        {
            case VERSION -> command.add("version");
            case LLM_TEST -> command.addAll(List.of("llm", "test"));
            case REVIEW_PREVIEW -> command.addAll(buildReviewArgs(request, workingDirectory, true));
            case REVIEW -> command.addAll(buildReviewArgs(request, workingDirectory, false));
            default -> throw new IllegalArgumentException("不支持的调用类型");
        }
        return command;
    }

    private List<String> buildReviewArgs(ReviewEngineRequest request, Path workingDirectory, boolean preview)
    {
        List<String> args = new ArrayList<>();
        args.add("review");
        if (preview)
        {
            args.add("--preview");
        }
        args.add("--format");
        args.add("json");
        args.add("--audience");
        args.add("agent");
        args.add("--repo");
        args.add(workingDirectory.toString());
        if (!preview && request.getBaseSha() != null && !request.getBaseSha().isBlank()
            && request.getHeadSha() != null && !request.getHeadSha().isBlank())
        {
            args.add("--from");
            args.add(request.getBaseSha());
            args.add("--to");
            args.add(request.getHeadSha());
        }
        if (!preview && request.getDiffContent() != null && !request.getDiffContent().isBlank())
        {
            writeDiffPatch(workingDirectory, request.getDiffContent());
        }
        return args;
    }

    private void writeDiffPatch(Path workingDirectory, String diffContent)
    {
        try
        {
            Files.writeString(workingDirectory.resolve(".acr-review.patch"), diffContent, StandardCharsets.UTF_8);
        }
        catch (IOException ex)
        {
            throw new IllegalStateException("写入 diff 补丁失败", ex);
        }
    }

    static String parseVersionFromOutput(String output)
    {
        if (output == null)
        {
            return null;
        }
        Matcher matcher = VERSION_PATTERN.matcher(output);
        if (matcher.find())
        {
            return matcher.group(1);
        }
        return null;
    }

    private String summarizeFailure(String stderr, String stdout)
    {
        if (stderr != null && !stderr.isBlank())
        {
            return stderr.lines().findFirst().orElse(stderr).trim();
        }
        if (stdout != null && !stdout.isBlank())
        {
            return stdout.lines().findFirst().orElse(stdout).trim();
        }
        return "CLI 执行失败";
    }

    /** 尽量关闭 CLI 颜色输出；即使 CLI 仍输出 ANSI，也会在返回前再剥离一次。 */
    private Map<String, String> withPlainTextTerminal(Map<String, String> modelEnvironment)
    {
        Map<String, String> environment = new HashMap<>();
        if (modelEnvironment != null)
        {
            environment.putAll(modelEnvironment);
        }
        environment.put("NO_COLOR", "1");
        environment.put("TERM", "dumb");
        environment.put("CLICOLOR", "0");
        environment.put("FORCE_COLOR", "0");
        return environment;
    }
}
