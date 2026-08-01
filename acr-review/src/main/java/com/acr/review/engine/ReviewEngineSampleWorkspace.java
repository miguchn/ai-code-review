package com.acr.review.engine;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;

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
        String[] command = new String[3 + args.length];
        command[0] = "git";
        command[1] = "-C";
        command[2] = workspace.toString();
        System.arraycopy(args, 0, command, 3, args.length);

        ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectErrorStream(true);
        Process process = builder.start();
        boolean finished = process.waitFor(30, TimeUnit.SECONDS);
        if (!finished)
        {
            process.destroyForcibly();
            throw new IOException("git 命令超时: git " + String.join(" ", args));
        }
        if (process.exitValue() != 0)
        {
            throw new IOException("git 命令失败: git " + String.join(" ", args));
        }
    }
}
