package com.acr.review.engine;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;

/**
 * 审查域统一的外部进程隔离器。
 * 负责并行消费输出、硬超时、进程树清理、输出上限和执行器生命周期。
 */
@Component
public class ExternalProcessRunner implements DisposableBean
{
    public static final int DEFAULT_MAX_OUTPUT_BYTES = 1_048_576;

    private static final Logger log = LoggerFactory.getLogger(ExternalProcessRunner.class);
    private static final int STREAM_DRAIN_TIMEOUT_SECONDS = 2;
    private static final int PROCESS_TERMINATION_TIMEOUT_MILLIS = 1_000;

    private final ExecutorService streamExecutor;
    private final AtomicLong orphanCleanupFailures = new AtomicLong();

    public ExternalProcessRunner(ObjectProvider<MeterRegistry> meterRegistryProvider)
    {
        this(createStreamExecutor(), meterRegistryProvider.getIfAvailable());
    }

    ExternalProcessRunner()
    {
        this(createStreamExecutor(), null);
    }

    ExternalProcessRunner(ExecutorService streamExecutor, MeterRegistry meterRegistry)
    {
        this.streamExecutor = streamExecutor;
        if (meterRegistry != null)
        {
            Gauge.builder("acr.review.external.process.orphan.cleanup.failures", orphanCleanupFailures,
                AtomicLong::get)
                .description("外部进程树清理后仍存活的进程数量")
                .register(meterRegistry);
        }
    }

    public ProcessExecution execute(List<String> command, Path workingDirectory, Map<String, String> environment,
        int timeoutSeconds) throws IOException, InterruptedException
    {
        return execute(command, workingDirectory, environment, timeoutSeconds, DEFAULT_MAX_OUTPUT_BYTES);
    }

    public ProcessExecution execute(List<String> command, Path workingDirectory, Map<String, String> environment,
        int timeoutSeconds, int maxOutputBytes) throws IOException, InterruptedException
    {
        if (command == null || command.isEmpty())
        {
            throw new IllegalArgumentException("外部进程命令不能为空");
        }
        int effectiveTimeoutSeconds = Math.max(1, timeoutSeconds);
        int effectiveMaxOutputBytes = Math.max(1, maxOutputBytes);
        long startedNanos = System.nanoTime();

        ProcessBuilder builder = new ProcessBuilder(command);
        if (workingDirectory != null)
        {
            builder.directory(workingDirectory.toFile());
        }
        builder.redirectErrorStream(false);
        if (environment != null && !environment.isEmpty())
        {
            builder.environment().putAll(environment);
        }

        Process process = start(builder);
        Future<StreamCapture> stdoutFuture;
        Future<StreamCapture> stderrFuture;
        try
        {
            stdoutFuture = streamExecutor.submit(capture(process.getInputStream(), effectiveMaxOutputBytes));
            stderrFuture = streamExecutor.submit(capture(process.getErrorStream(), effectiveMaxOutputBytes));
        }
        catch (RuntimeException ex)
        {
            destroyProcessTree(process);
            throw new IOException("无法启动外部进程输出读取任务", ex);
        }

        try
        {
            boolean finished = process.waitFor(effectiveTimeoutSeconds, TimeUnit.SECONDS);
            if (!finished)
            {
                destroyProcessTree(process);
                StreamCapture stdout = readCaptureQuietly(stdoutFuture, "stdout");
                StreamCapture stderr = readCaptureQuietly(stderrFuture, "stderr");
                return ProcessExecution.timedOut(elapsedMillis(startedNanos), stdout.text(), stderr.text());
            }

            StreamCapture stdout = readCapture(stdoutFuture, "stdout");
            StreamCapture stderr = readCapture(stderrFuture, "stderr");
            return new ProcessExecution(process.exitValue(), elapsedMillis(startedNanos),
                stdout.text(), stderr.text(), false);
        }
        catch (InterruptedException ex)
        {
            destroyProcessTree(process);
            cancelCapture(stdoutFuture);
            cancelCapture(stderrFuture);
            Thread.currentThread().interrupt();
            throw ex;
        }
        catch (IOException | RuntimeException ex)
        {
            destroyProcessTree(process);
            cancelCapture(stdoutFuture);
            cancelCapture(stderrFuture);
            throw ex;
        }
        finally
        {
            if (process.isAlive())
            {
                destroyProcessTree(process);
            }
        }
    }

    Process start(ProcessBuilder builder) throws IOException
    {
        return builder.start();
    }

    long orphanCleanupFailureCount()
    {
        return orphanCleanupFailures.get();
    }

    private StreamCapture readCapture(Future<StreamCapture> future, String streamName) throws IOException
    {
        try
        {
            return future.get(STREAM_DRAIN_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        }
        catch (ExecutionException ex)
        {
            throw new IOException("读取外部进程 " + streamName + " 失败", ex.getCause());
        }
        catch (TimeoutException ex)
        {
            cancelCapture(future);
            throw new IOException("读取外部进程 " + streamName + " 超时", ex);
        }
        catch (InterruptedException ex)
        {
            cancelCapture(future);
            Thread.currentThread().interrupt();
            throw new IOException("读取外部进程 " + streamName + " 被中断", ex);
        }
    }

    private StreamCapture readCaptureQuietly(Future<StreamCapture> future, String streamName)
    {
        try
        {
            return readCapture(future, streamName);
        }
        catch (IOException ex)
        {
            log.warn("外部进程超时后读取 {} 失败: {}", streamName, ex.getMessage());
            return StreamCapture.empty();
        }
    }

    private static Callable<StreamCapture> capture(InputStream inputStream, int maxOutputBytes)
    {
        return () -> {
            try (InputStream input = inputStream; ByteArrayOutputStream output = new ByteArrayOutputStream())
            {
                boolean truncated = false;
                byte[] buffer = new byte[4096];
                int read;
                while ((read = input.read(buffer)) >= 0)
                {
                    int remaining = maxOutputBytes - output.size();
                    if (remaining > 0)
                    {
                        int allowed = Math.min(read, remaining);
                        output.write(buffer, 0, allowed);
                        if (allowed < read)
                        {
                            truncated = true;
                        }
                    }
                    else
                    {
                        truncated = true;
                    }
                }
                return new StreamCapture(output.toString(StandardCharsets.UTF_8), truncated);
            }
        };
    }

    private void destroyProcessTree(Process process)
    {
        long terminationDeadlineNanos = System.nanoTime()
            + TimeUnit.MILLISECONDS.toNanos(PROCESS_TERMINATION_TIMEOUT_MILLIS);
        List<ProcessHandle> descendants = new ArrayList<>();
        try
        {
            descendants.addAll(process.descendants().toList());
        }
        catch (RuntimeException ex)
        {
            log.warn("枚举外部进程子进程失败, pid={}: {}", safePid(process), ex.getMessage());
        }

        for (int i = descendants.size() - 1; i >= 0; i--)
        {
            destroyForcibly(descendants.get(i));
        }
        for (ProcessHandle descendant : descendants)
        {
            awaitHandleExit(descendant, terminationDeadlineNanos);
        }
        try
        {
            long remainingMillis = remainingMillis(terminationDeadlineNanos);
            if (process.isAlive() && (remainingMillis == 0
                || !process.waitFor(remainingMillis, TimeUnit.MILLISECONDS)))
            {
                process.destroyForcibly();
                process.waitFor(Math.max(1L, remainingMillis(terminationDeadlineNanos)), TimeUnit.MILLISECONDS);
            }
            if (process.isAlive())
            {
                process.destroyForcibly();
                process.waitFor(Math.max(1L, remainingMillis(terminationDeadlineNanos)), TimeUnit.MILLISECONDS);
            }
        }
        catch (InterruptedException ex)
        {
            Thread.currentThread().interrupt();
        }
        catch (RuntimeException ex)
        {
            log.warn("强制终止外部进程失败, pid={}: {}", safePid(process), ex.getMessage());
        }

        closeProcessStreams(process);
        long aliveCount = 0L;
        for (ProcessHandle descendant : descendants)
        {
            ProcessHandle current = ProcessHandle.of(descendant.pid()).orElse(null);
            if (current != null && current.isAlive())
            {
                destroyForcibly(current);
                awaitHandleExit(current, terminationDeadlineNanos);
                if (ProcessHandle.of(descendant.pid()).map(ProcessHandle::isAlive).orElse(false))
                {
                    aliveCount++;
                }
            }
        }
        if (process.isAlive())
        {
            aliveCount++;
        }
        if (aliveCount > 0)
        {
            orphanCleanupFailures.addAndGet(aliveCount);
            log.error("外部进程树清理后仍有 {} 个进程存活, rootPid={}", aliveCount, safePid(process));
        }
    }

    private static void destroyForcibly(ProcessHandle handle)
    {
        try
        {
            if (handle.isAlive())
            {
                handle.destroyForcibly();
            }
        }
        catch (RuntimeException ex)
        {
            log.warn("强制终止子进程失败, pid={}: {}", handle.pid(), ex.getMessage());
        }
    }

    private static void awaitHandleExit(ProcessHandle handle, long deadlineNanos)
    {
        if (!handle.isAlive())
        {
            return;
        }
        try
        {
            long remainingMillis = remainingMillis(deadlineNanos);
            if (remainingMillis == 0)
            {
                destroyForcibly(handle);
                return;
            }
            handle.onExit().get(remainingMillis, TimeUnit.MILLISECONDS);
        }
        catch (InterruptedException ex)
        {
            Thread.currentThread().interrupt();
        }
        catch (ExecutionException | TimeoutException ex)
        {
            if (handle.isAlive())
            {
                destroyForcibly(handle);
            }
        }
    }

    private static long remainingMillis(long deadlineNanos)
    {
        long remainingNanos = deadlineNanos - System.nanoTime();
        if (remainingNanos <= 0)
        {
            return 0L;
        }
        return Math.max(1L, TimeUnit.NANOSECONDS.toMillis(remainingNanos));
    }

    private static void closeProcessStreams(Process process)
    {
        try
        {
            process.getOutputStream().close();
        }
        catch (IOException ignored)
        {
        }
        try
        {
            process.getInputStream().close();
        }
        catch (IOException ignored)
        {
        }
        try
        {
            process.getErrorStream().close();
        }
        catch (IOException ignored)
        {
        }
    }

    private static long safePid(Process process)
    {
        try
        {
            return process.pid();
        }
        catch (RuntimeException ex)
        {
            return -1L;
        }
    }

    private static void cancelCapture(Future<StreamCapture> future)
    {
        if (future != null && !future.isDone())
        {
            future.cancel(true);
        }
    }

    private static long elapsedMillis(long startedNanos)
    {
        return TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedNanos);
    }

    private static ExecutorService createStreamExecutor()
    {
        AtomicInteger sequence = new AtomicInteger();
        return Executors.newCachedThreadPool(task -> {
            Thread thread = new Thread(task, "acr-external-process-stream-" + sequence.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        });
    }

    @Override
    public void destroy()
    {
        streamExecutor.shutdownNow();
        try
        {
            if (!streamExecutor.awaitTermination(2, TimeUnit.SECONDS))
            {
                log.warn("外部进程输出读取线程池未在期限内退出");
            }
        }
        catch (InterruptedException ex)
        {
            Thread.currentThread().interrupt();
        }
    }

    public record ProcessExecution(int exitCode, long durationMs, String stdout, String stderr, boolean timedOut)
    {
        public static ProcessExecution timedOut(long durationMs, String stdout, String stderr)
        {
            return new ProcessExecution(-1, durationMs, stdout, stderr, true);
        }

        public String combinedOutput()
        {
            String out = stdout == null ? "" : stdout.trim();
            String err = stderr == null ? "" : stderr.trim();
            if (out.isEmpty())
            {
                return err;
            }
            if (err.isEmpty())
            {
                return out;
            }
            return out + "\n" + err;
        }
    }

    private record StreamCapture(String content, boolean truncated)
    {
        static StreamCapture empty()
        {
            return new StreamCapture("", false);
        }

        String text()
        {
            return truncated ? content + "\n...[output truncated]" : content;
        }
    }
}
