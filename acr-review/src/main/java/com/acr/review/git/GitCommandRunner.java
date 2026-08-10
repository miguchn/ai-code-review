package com.acr.review.git;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import com.acr.review.engine.ExternalProcessRunner;

/** 审查域内 Git 子命令的统一受控执行入口。 */
@Component
public class GitCommandRunner
{
    private final ExternalProcessRunner processRunner;

    public GitCommandRunner(ExternalProcessRunner processRunner)
    {
        this.processRunner = processRunner;
    }

    public GitCommandResult execute(Path workspace, Map<String, String> environment, int timeoutSeconds,
        String... args) throws IOException, InterruptedException
    {
        List<String> command = new ArrayList<>();
        command.add("git");
        command.add("-C");
        command.add(workspace.toString());
        if (args != null)
        {
            command.addAll(List.of(args));
        }

        Map<String, String> processEnvironment = new HashMap<>();
        processEnvironment.put("GIT_TERMINAL_PROMPT", "0");
        if (environment != null)
        {
            processEnvironment.putAll(environment);
        }
        ExternalProcessRunner.ProcessExecution execution = processRunner.execute(
            command, workspace, processEnvironment, timeoutSeconds);
        return new GitCommandResult(execution.exitCode(), execution.durationMs(), execution.combinedOutput(),
            execution.timedOut());
    }

    public record GitCommandResult(int exitCode, long durationMs, String output, boolean timedOut)
    {
        public boolean successful()
        {
            return !timedOut && exitCode == 0;
        }

        /** Git 远端临时故障的稳定分类；输入/SHA/权限类错误不在此列。 */
        public boolean transientDependencyFailure()
        {
            if (output == null || output.isBlank())
            {
                return false;
            }
            String normalized = output.toLowerCase(java.util.Locale.ROOT);
            return normalized.contains("could not resolve host")
                || normalized.contains("failed to connect")
                || normalized.contains("connection timed out")
                || normalized.contains("connection reset")
                || normalized.contains("remote end hung up")
                || normalized.contains("the remote end hung up")
                || normalized.contains("http 502")
                || normalized.contains("http 503")
                || normalized.contains("http 504");
        }
    }
}
