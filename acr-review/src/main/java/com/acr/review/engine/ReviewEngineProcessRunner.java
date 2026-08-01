package com.acr.review.engine;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
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
    private final ExecutorService streamExecutor = Executors.newCachedThreadPool(r -> {
        Thread thread = new Thread(r, "review-engine-stream");
        thread.setDaemon(true);
        return thread;
    });

    public ReviewEngineProcessRunner(ReviewEngineProperties properties)
    {
        this.properties = properties;
    }

    public ProcessExecution execute(List<String> command, Path workingDirectory, Map<String, String> environment,
        int timeoutSeconds) throws IOException, InterruptedException
    {
        long started = System.currentTimeMillis();
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(workingDirectory.toFile());
        builder.redirectErrorStream(false);
        if (environment != null)
        {
            builder.environment().putAll(environment);
        }

        Process process = builder.start();
        Future<StreamCapture> stdoutFuture = streamExecutor.submit(capture(process.getInputStream()));
        Future<StreamCapture> stderrFuture = streamExecutor.submit(capture(process.getErrorStream()));

        boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
        StreamCapture stdoutCapture = readCapture(stdoutFuture);
        StreamCapture stderrCapture = readCapture(stderrFuture);
        String stdout = stdoutCapture.text();
        String stderr = stderrCapture.text();

        if (!finished)
        {
            destroyProcessTree(process);
            return ProcessExecution.timedOut(System.currentTimeMillis() - started, stdout, stderr);
        }

        return new ProcessExecution(process.exitValue(), System.currentTimeMillis() - started, stdout, stderr, false);
    }

    private StreamCapture readCapture(Future<StreamCapture> future) throws IOException
    {
        try
        {
            return future.get(5, TimeUnit.SECONDS);
        }
        catch (ExecutionException | TimeoutException ex)
        {
            throw new IOException("读取 CLI 输出失败", ex);
        }
        catch (InterruptedException ex)
        {
            Thread.currentThread().interrupt();
            throw new IOException("读取 CLI 输出被中断", ex);
        }
    }

    private Callable<StreamCapture> capture(InputStream inputStream)
    {
        return () -> {
            StringBuilder builder = new StringBuilder();
            boolean truncated = false;
            byte[] buffer = new byte[4096];
            int total = 0;
            int read;
            while ((read = inputStream.read(buffer)) >= 0)
            {
                if (total >= properties.getMaxOutputBytes())
                {
                    truncated = true;
                    continue;
                }
                int allowed = Math.min(read, properties.getMaxOutputBytes() - total);
                builder.append(new String(buffer, 0, allowed, StandardCharsets.UTF_8));
                total += allowed;
                if (read > allowed)
                {
                    truncated = true;
                }
            }
            inputStream.close();
            return new StreamCapture(builder.toString(), truncated);
        };
    }

    private void destroyProcessTree(Process process)
    {
        process.descendants().forEach(ProcessHandle::destroyForcibly);
        process.destroyForcibly();
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

    private record StreamCapture(String content, boolean truncated)
    {
        String text()
        {
            return truncated ? content + "\n...[output truncated]" : content;
        }
    }
}
