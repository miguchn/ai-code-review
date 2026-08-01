package com.acr.review.engine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.UUID;
import org.springframework.stereotype.Component;
import com.acr.common.exception.ServiceException;
import com.acr.review.engine.config.ReviewEngineProperties;

/** 审查引擎独立工作目录管理，限制在配置根路径内。 */
@Component
public class ReviewEngineWorkspaceManager
{
    private final ReviewEngineProperties properties;
    private final Path workspaceRoot;

    public ReviewEngineWorkspaceManager(ReviewEngineProperties properties)
    {
        this.properties = properties;
        this.workspaceRoot = Paths.get(properties.getWorkspaceRoot()).toAbsolutePath().normalize();
    }

    public void ensureRootReady() throws IOException
    {
        Files.createDirectories(workspaceRoot);
    }

    public Path createIsolatedWorkspace() throws IOException
    {
        ensureRootReady();
        Path workspace = workspaceRoot.resolve("run-" + UUID.randomUUID());
        Files.createDirectories(workspace);
        return workspace.toAbsolutePath().normalize();
    }

    public void validateWithinRoot(Path workingDirectory)
    {
        if (workingDirectory == null)
        {
            throw new ServiceException("工作目录不能为空");
        }
        Path normalized = workingDirectory.toAbsolutePath().normalize();
        if (!normalized.startsWith(workspaceRoot))
        {
            throw new ServiceException("工作目录必须位于审查引擎根路径内");
        }
    }

    public Path resolveExisting(Path workingDirectory)
    {
        validateWithinRoot(workingDirectory);
        return workingDirectory.toAbsolutePath().normalize();
    }

    public void cleanup(Path workspace)
    {
        if (workspace == null)
        {
            return;
        }
        Path normalized = workspace.toAbsolutePath().normalize();
        if (!normalized.startsWith(workspaceRoot))
        {
            return;
        }
        try (var paths = Files.walk(normalized))
        {
            paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                try
                {
                    Files.deleteIfExists(path);
                }
                catch (IOException ignored)
                {
                    // best effort cleanup
                }
            });
        }
        catch (IOException ignored)
        {
            // best effort cleanup
        }
    }

    public Path getWorkspaceRoot()
    {
        return workspaceRoot;
    }

    public String getWorkspaceRootText()
    {
        return workspaceRoot.toString();
    }
}
