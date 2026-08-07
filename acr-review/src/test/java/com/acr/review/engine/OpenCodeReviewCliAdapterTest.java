package com.acr.review.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import com.acr.review.engine.config.ReviewEngineProperties;

class OpenCodeReviewCliAdapterTest
{
    @TempDir
    Path tempDir;

    private ReviewEngineProperties properties;
    private OpenCodeReviewCliAdapter adapter;

    @BeforeEach
    void setUp() throws IOException
    {
        Path fakeExecutable = tempDir.resolve("fake-ocr.sh");
        Files.writeString(fakeExecutable, """
            #!/bin/sh
            case "$1" in
              version)
                echo "open-code-review v9.9.9 (test) darwin/arm64"
                exit 0
                ;;
              llm)
                echo "LLM test failed: unauthorized"
                exit 1
                ;;
              review)
                echo '{"files":["SampleService.java"]}'
                exit 0
                ;;
              *)
                echo "unknown"
                exit 2
                ;;
            esac
            """);
        fakeExecutable.toFile().setExecutable(true);

        properties = new ReviewEngineProperties();
        properties.setExecutablePath(fakeExecutable.toString());
        properties.setWorkspaceRoot(tempDir.resolve("workspaces").toString());
        properties.setDefaultTimeoutSeconds(5);
        properties.setMaxOutputBytes(4096);

        adapter = new OpenCodeReviewCliAdapter(
            properties,
            new ReviewEngineProcessRunner(properties),
            new ReviewEngineWorkspaceManager(properties),
            new ReviewEngineOutputParser());
    }

    @Test
    void detectsVersionFromCliOutput()
    {
        ReviewEngineRequest request = new ReviewEngineRequest();
        request.setWorkingDirectory(createWorkspace().toString());
        request.setInvocationType(ReviewEngineInvocationType.VERSION);

        ReviewEngineResult result = adapter.execute(request);
        assertTrue(result.isSuccess());
        assertEquals("9.9.9", result.getEngineVersion());
    }

    @Test
    void classifiesModelFailureFromExitCode()
    {
        ReviewEngineRequest request = new ReviewEngineRequest();
        request.setWorkingDirectory(createWorkspace().toString());
        request.setInvocationType(ReviewEngineInvocationType.LLM_TEST);

        ReviewEngineResult result = adapter.execute(request);
        assertFalse(result.isSuccess());
        assertEquals(ReviewEngineFailureType.MODEL_CALL_FAILED, result.getFailureType());
    }

    @Test
    void parsesPreviewJsonOutput()
    {
        ReviewEngineRequest request = new ReviewEngineRequest();
        Path workspace = createWorkspace();
        request.setWorkingDirectory(workspace.toString());
        request.setInvocationType(ReviewEngineInvocationType.REVIEW_PREVIEW);

        ReviewEngineResult result = adapter.execute(request);
        assertTrue(result.isSuccess());
        assertTrue(result.getStructuredResult().containsKey("files"));
    }

    @Test
    void buildsFixedCommandWithoutShellConcatenation()
    {
        Path workspace = createWorkspace();
        ReviewEngineRequest request = new ReviewEngineRequest();
        request.setWorkingDirectory(workspace.toString());
        request.setInvocationType(ReviewEngineInvocationType.REVIEW_PREVIEW);

        List<String> command = adapter.buildCommand(request, workspace);
        assertEquals(properties.getExecutablePath(), command.get(0));
        assertEquals("review", command.get(1));
        assertEquals("--preview", command.get(2));
        assertFalse(String.join(" ", command).contains(";"));
        assertFalse(String.join(" ", command).contains("|"));
    }

    @Test
    void appendsExcludePatternsAsSingleCommaJoinedArgument()
    {
        // M3.2 步 6：平台排除规则经 CLI 原生 --exclude 传入，逗号分隔、单参数（不拼 shell）
        Path workspace = createWorkspace();
        ReviewEngineRequest request = new ReviewEngineRequest();
        request.setWorkingDirectory(workspace.toString());
        request.setInvocationType(ReviewEngineInvocationType.REVIEW);
        request.setBaseSha("abc1234");
        request.setHeadSha("def5678");
        request.setExcludePatterns(List.of("**/package-lock.json", "**/src/test/**", "docs/**"));

        List<String> command = adapter.buildCommand(request, workspace);
        int excludeIndex = command.indexOf("--exclude");
        assertTrue(excludeIndex > 0, "应包含 --exclude 参数: " + command);
        assertEquals("**/package-lock.json,**/src/test/**,docs/**", command.get(excludeIndex + 1));
    }

    @Test
    void omitsExcludeFlagWhenPatternsEmpty()
    {
        Path workspace = createWorkspace();
        ReviewEngineRequest request = new ReviewEngineRequest();
        request.setWorkingDirectory(workspace.toString());
        request.setInvocationType(ReviewEngineInvocationType.REVIEW);
        request.setExcludePatterns(List.of());

        List<String> command = adapter.buildCommand(request, workspace);
        assertFalse(command.contains("--exclude"));
    }

    @Test
    void summarizeFailurePrefersJsonMessageOverFirstLineBrace()
    {
        String json = "{\n  \"status\": \"failed\",\n  \"message\": \"Review failed (input): no files selected\"\n}";
        assertEquals("Review failed (input): no files selected",
            OpenCodeReviewCliAdapter.summarizeFailureMessage(json, ""));
        assertEquals("Review failed (input): no files selected",
            OpenCodeReviewCliAdapter.summarizeFailureMessage("", json));
    }

    @Test
    void summarizeFailureTruncatesPlainTextOutput()
    {
        String longText = "fatal: " + "x".repeat(500);
        String summary = OpenCodeReviewCliAdapter.summarizeFailureMessage(longText, null);
        assertEquals(480, summary.length());
        assertTrue(summary.startsWith("fatal: "));
        assertFalse(summary.contains("\n") && summary.indexOf('\n') == 0);
    }

    private Path createWorkspace()
    {
        try
        {
            return new ReviewEngineWorkspaceManager(properties).createIsolatedWorkspace();
        }
        catch (IOException ex)
        {
            throw new IllegalStateException(ex);
        }
    }
}
