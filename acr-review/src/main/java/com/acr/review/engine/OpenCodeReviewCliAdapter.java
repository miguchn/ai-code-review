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

    /**
     * OCR --background 语言指令（平台行为，硬编码常量）。
     * CLI 无独立 language 参数时以此注入输出语言约束。
     */
    static final String BACKGROUND_LANGUAGE_INSTRUCTION =
        "输出语言要求：所有审查发现必须使用简体中文，包括问题标题、问题描述、修复建议；"
            + "severity 与 category 字段保持英文枚举值不变。";

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
        // M3.2 步 6：平台范围决策的排除规则经 CLI 原生 --exclude 传入（逗号分隔 gitignore 风格）。
        // 含逗号的 glob 无法在该参数中表达，由执行层提前剔除，此处不再转义。
        if (request.getExcludePatterns() != null && !request.getExcludePatterns().isEmpty())
        {
            args.add("--exclude");
            args.add(String.join(",", request.getExcludePatterns()));
        }
        if (!preview && request.getDiffContent() != null && !request.getDiffContent().isBlank())
        {
            writeDiffPatch(workingDirectory, request.getDiffContent());
        }
        args.add("--background");
        args.add(BACKGROUND_LANGUAGE_INSTRUCTION);
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
        return summarizeFailureMessage(stderr, stdout);
    }

    /**
     * 优先从 OCR JSON 错误输出提取 {@code message}；否则取完整输出截断至 480 字符。
     * 不再只取首行（JSON 报错首行常为「{」，信息不可用）。
     */
    static String summarizeFailureMessage(String stderr, String stdout)
    {
        String fromJson = extractJsonMessage(stderr);
        if (fromJson == null || fromJson.isBlank())
        {
            fromJson = extractJsonMessage(stdout);
        }
        if (fromJson != null && !fromJson.isBlank())
        {
            return truncateFailure(fromJson);
        }
        if (stderr != null && !stderr.isBlank())
        {
            return truncateFailure(stderr.trim());
        }
        if (stdout != null && !stdout.isBlank())
        {
            return truncateFailure(stdout.trim());
        }
        return "CLI 执行失败";
    }

    private static String extractJsonMessage(String text)
    {
        if (text == null || text.isBlank())
        {
            return null;
        }
        String trimmed = text.trim();
        int start = trimmed.indexOf('{');
        if (start < 0)
        {
            return null;
        }
        String candidate = trimmed.substring(start);
        try
        {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = new com.fasterxml.jackson.databind.ObjectMapper()
                .readValue(candidate, Map.class);
            Object message = map.get("message");
            if (message == null)
            {
                return null;
            }
            String value = String.valueOf(message).trim();
            return value.isEmpty() ? null : value;
        }
        catch (Exception ignored)
        {
            return null;
        }
    }

    private static String truncateFailure(String message)
    {
        if (message.length() <= 480)
        {
            return message;
        }
        return message.substring(0, 480);
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
