package com.acr.review.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import com.acr.review.engine.config.ReviewEngineProperties;

class ReviewEngineProcessRunnerTest
{
    @TempDir
    Path tempDir;

    private ReviewEngineProperties properties;
    private ReviewEngineProcessRunner runner;
    private ExternalProcessRunner externalProcessRunner;

    @BeforeEach
    void setUp() throws IOException
    {
        Path slowScript = tempDir.resolve("slow.sh");
        Files.writeString(slowScript, """
            #!/bin/sh
            sleep 3
            echo done
            """);
        slowScript.toFile().setExecutable(true);

        properties = new ReviewEngineProperties();
        properties.setMaxOutputBytes(128);
        externalProcessRunner = new ExternalProcessRunner();
        runner = new ReviewEngineProcessRunner(properties, externalProcessRunner);
    }

    @AfterEach
    void tearDown()
    {
        externalProcessRunner.destroy();
    }

    @Test
    void timesOutLongRunningProcess() throws Exception
    {
        Path script = tempDir.resolve("slow.sh");
        var execution = runner.execute(List.of(script.toString()), tempDir, null, 1);
        assertTrue(execution.timedOut());
    }

    @Test
    void truncatesLargeOutput() throws Exception
    {
        Path echoScript = tempDir.resolve("echo.sh");
        Files.writeString(echoScript, """
            #!/bin/sh
            i=0
            while [ $i -lt 200 ]; do
              printf 'x'
              i=$((i+1))
            done
            """);
        echoScript.toFile().setExecutable(true);

        var execution = runner.execute(List.of(echoScript.toString()), tempDir, null, 5);
        assertEquals(0, execution.exitCode());
        assertTrue(execution.stdout().contains("truncated"));
    }

    @Test
    void classifiesMissingExecutable()
    {
        var failure = ReviewEngineProcessRunner.classifyStartupFailure(
            new IOException("Cannot run program \"/missing/ocr\": error=2, No such file or directory"));
        assertEquals(ReviewEngineFailureType.CLI_NOT_FOUND, failure);
    }
}
