package com.acr.review.engine;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.stereotype.Component;
import com.acr.review.git.GitCommandRunner;

/** 内置测试样例：最小 Git 仓库，供引擎测试调用使用。 */
@Component
public class ReviewEngineSampleWorkspace
{
    private static final String SAMPLE_JAVA = """
        public class SampleService {
            public int divide(int a, int b) {
                return a / b;
            }
        }
        """;

    private final GitCommandRunner gitCommandRunner;

    public ReviewEngineSampleWorkspace(GitCommandRunner gitCommandRunner)
    {
        this.gitCommandRunner = gitCommandRunner;
    }

    public Path prepare(Path workspace) throws IOException, InterruptedException
    {
        Files.createDirectories(workspace);
        Path sourceFile = workspace.resolve("SampleService.java");
        Files.writeString(sourceFile, SAMPLE_JAVA, StandardCharsets.UTF_8);

        runGit(workspace, "init");
        runGit(workspace, "config", "user.email", "acr-test@example.com");
        runGit(workspace, "config", "user.name", "ACR Test");
        runGit(workspace, "add", "SampleService.java");
        runGit(workspace, "commit", "-m", "initial sample");
        Files.writeString(sourceFile, SAMPLE_JAVA.replace("return a / b;", "return a / b; // TODO review"), StandardCharsets.UTF_8);
        runGit(workspace, "add", "SampleService.java");
        return workspace;
    }

    private void runGit(Path workspace, String... args) throws IOException, InterruptedException
    {
        GitCommandRunner.GitCommandResult result = gitCommandRunner.execute(workspace, null, 30, args);
        if (result.timedOut())
        {
            throw new IOException("git 命令超时: git " + String.join(" ", args));
        }
        if (!result.successful())
        {
            throw new IOException("git 命令失败: git " + String.join(" ", args));
        }
    }
}
