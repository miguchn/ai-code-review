package com.acr.review.engine;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ExternalProcessRunnerTest
{
    @TempDir
    Path tempDir;

    private ExternalProcessRunner runner;

    @BeforeEach
    void setUp()
    {
        runner = new ExternalProcessRunner();
    }

    @AfterEach
    void tearDown()
    {
        runner.destroy();
    }

    @Test
    void drainsOutputBeyondPipeCapacityWithoutHanging() throws Exception
    {
        Path script = executable("large-output.sh", """
            #!/bin/sh
            i=0
            while [ $i -lt 200000 ]; do
              printf 'x'
              i=$((i+1))
            done
            """);

        ExternalProcessRunner.ProcessExecution execution = org.junit.jupiter.api.Assertions.assertTimeoutPreemptively(
            Duration.ofSeconds(5), () -> runner.execute(List.of(script.toString()), tempDir, null, 4, 128));

        assertFalse(execution.timedOut());
        assertTrue(execution.stdout().contains("output truncated"));
    }

    @Test
    void timeoutTerminatesProcessTreeBeforeReturning() throws Exception
    {
        AtomicReference<Process> rootProcess = new AtomicReference<>();
        runner.destroy();
        runner = new ExternalProcessRunner()
        {
            @Override
            Process start(ProcessBuilder builder) throws IOException
            {
                Process process = super.start(builder);
                rootProcess.set(process);
                return process;
            }
        };
        Path childPidFile = tempDir.resolve("child.pid");
        Path script = executable("child-process.sh", """
            #!/bin/sh
            sleep 30 &
            child=$!
            echo "$child" > child.pid
            wait "$child"
            """);

        long started = System.nanoTime();
        CompletableFuture<ExternalProcessRunner.ProcessExecution> executionFuture = CompletableFuture.supplyAsync(() -> {
            try
            {
                return runner.execute(List.of(script.toString()), tempDir, null, 1, 128);
            }
            catch (IOException | InterruptedException ex)
            {
                throw new java.util.concurrent.CompletionException(ex);
            }
        });
        org.junit.jupiter.api.Assertions.assertTimeoutPreemptively(Duration.ofSeconds(1), () -> {
            while (!Files.exists(childPidFile))
            {
                Thread.sleep(10);
            }
        });
        long childPid = Long.parseLong(Files.readString(childPidFile).trim());
        ProcessHandle childProcess = ProcessHandle.of(childPid).orElseThrow();
        assertTrue(rootProcess.get().descendants().anyMatch(handle -> handle.pid() == childPid),
            "测试子进程必须位于受控进程树内");
        ExternalProcessRunner.ProcessExecution execution = executionFuture.get(4, TimeUnit.SECONDS);
        long durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);

        assertTrue(execution.timedOut());
        assertTrue(durationMs < 3_000, "硬超时应快速返回，实际 " + durationMs + "ms");
        assertFalse(childProcess.isAlive(),
            "子进程不应泄漏，清理失败计数=" + runner.orphanCleanupFailureCount());
    }

    @Test
    void outputReadFailureStillDestroysProcess()
    {
        FailingOutputProcess process = new FailingOutputProcess();
        runner.destroy();
        runner = new ExternalProcessRunner()
        {
            @Override
            Process start(ProcessBuilder builder)
            {
                return process;
            }
        };

        assertThrows(IOException.class,
            () -> runner.execute(List.of("fake-command"), tempDir, null, 1, 128));
        assertFalse(process.isAlive(), "读取异常后仍必须清理进程");
    }

    private Path executable(String name, String content) throws IOException
    {
        Path script = tempDir.resolve(name);
        Files.writeString(script, content);
        script.toFile().setExecutable(true);
        return script;
    }

    private static final class FailingOutputProcess extends Process
    {
        private volatile boolean alive = true;

        @Override
        public OutputStream getOutputStream()
        {
            return OutputStream.nullOutputStream();
        }

        @Override
        public InputStream getInputStream()
        {
            return new InputStream()
            {
                @Override
                public int read() throws IOException
                {
                    throw new IOException("simulated read failure");
                }
            };
        }

        @Override
        public InputStream getErrorStream()
        {
            return new ByteArrayInputStream(new byte[0]);
        }

        @Override
        public int waitFor()
        {
            return 0;
        }

        @Override
        public boolean waitFor(long timeout, TimeUnit unit)
        {
            return true;
        }

        @Override
        public int exitValue()
        {
            return 0;
        }

        @Override
        public void destroy()
        {
            alive = false;
        }

        @Override
        public Process destroyForcibly()
        {
            alive = false;
            return this;
        }

        @Override
        public boolean isAlive()
        {
            return alive;
        }
    }
}
