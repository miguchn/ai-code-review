package com.acr.review.engine;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import com.acr.review.engine.config.ReviewEngineProperties;

/** 安全的外部 CLI 进程执行器。 */
@Component
public class ReviewEngineProcessRunner
{
    private static final Logger log = LoggerFactory.getLogger(ReviewEngineProcessRunner.class);

    private final ReviewEngineProperties properties;
    private final ExternalProcessRunner processRunner;

    public ReviewEngineProcessRunner(ReviewEngineProperties properties, ExternalProcessRunner processRunner)
    {
        this.properties = properties;
        this.processRunner = processRunner;
    }

    public ProcessExecution execute(List<String> command, Path workingDirectory, Map<String, String> environment,
        int timeoutSeconds) throws IOException, InterruptedException
    {
        ExternalProcessRunner.ProcessExecution execution = processRunner.execute(
            command, workingDirectory, environment, timeoutSeconds, properties.getMaxOutputBytes());
        return new ProcessExecution(execution.exitCode(), execution.durationMs(),
            execution.stdout(), execution.stderr(), execution.timedOut());
    }

    public static ReviewEngineFailureType classifyStartupFailure(IOException ex)
    {
        String message = ex.getMessage() != null ? ex.getMessage().toLowerCase() : "";
        if (message.contains("no such file") || message.contains("cannot find") || message.contains("not found"))
        {
            return ReviewEngineFailureType.CLI_NOT_FOUND;
        }
        if (message.contains("permission denied") || message.contains("access is denied"))
        {
            return ReviewEngineFailureType.PERMISSION_DENIED;
        }
        log.warn("Review engine CLI startup failed: {}", ex.getMessage());
        return ReviewEngineFailureType.UNKNOWN;
    }

    public static ReviewEngineFailureType classifyExitFailure(int exitCode, String stdout, String stderr,
        ReviewEngineInvocationType invocationType)
    {
        String combined = (stdout + "\n" + stderr).toLowerCase();
        if (invocationType == ReviewEngineInvocationType.LLM_TEST
            || combined.contains("llm") || combined.contains("endpoint") || combined.contains("api key")
            || combined.contains("auth") || combined.contains("model"))
        {
            if (combined.contains("timeout") || combined.contains("timed out"))
            {
                return ReviewEngineFailureType.TIMEOUT;
            }
            if (combined.contains("no valid llm") || combined.contains("resolve llm")
                || combined.contains("401") || combined.contains("403") || combined.contains("unauthorized")
                || combined.contains("invalid api key") || combined.contains("authentication"))
            {
                return ReviewEngineFailureType.MODEL_CALL_FAILED;
            }
        }
        if (exitCode != 0)
        {
            return ReviewEngineFailureType.ABNORMAL_EXIT;
        }
        return ReviewEngineFailureType.UNKNOWN;
    }

    public record ProcessExecution(int exitCode, long durationMs, String stdout, String stderr, boolean timedOut)
    {
        public static ProcessExecution timedOut(long durationMs, String stdout, String stderr)
        {
            return new ProcessExecution(-1, durationMs, stdout, stderr, true);
        }
    }
}
