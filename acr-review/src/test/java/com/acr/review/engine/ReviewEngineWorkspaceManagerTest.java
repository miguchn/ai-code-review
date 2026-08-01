package com.acr.review.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import com.acr.review.engine.config.ReviewEngineProperties;

class ReviewEngineWorkspaceManagerTest
{
    @TempDir
    Path tempDir;

    private ReviewEngineProperties properties;
    private ReviewEngineWorkspaceManager workspaceManager;

    @BeforeEach
    void setUp()
    {
        properties = new ReviewEngineProperties();
        properties.setWorkspaceRoot(tempDir.resolve("engine-root").toString());
        workspaceManager = new ReviewEngineWorkspaceManager(properties);
    }

    @Test
    void createsAndCleansIsolatedWorkspace() throws IOException
    {
        Path workspace = workspaceManager.createIsolatedWorkspace();
        assertTrue(Files.exists(workspace));
        Files.writeString(workspace.resolve("marker.txt"), "test");
        workspaceManager.cleanup(workspace);
        assertFalse(Files.exists(workspace));
    }

    @Test
    void rejectsDirectoryOutsideRoot() throws IOException
    {
        Path outside = tempDir.resolve("outside");
        Files.createDirectories(outside);
        assertThrows(Exception.class, () -> workspaceManager.validateWithinRoot(outside));
    }
}
