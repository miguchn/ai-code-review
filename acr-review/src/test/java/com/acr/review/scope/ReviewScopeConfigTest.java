package com.acr.review.scope;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import com.acr.review.domain.ReviewTask;

class ReviewScopeConfigTest
{
    @Test
    void fromTaskSnapshotNullTaskFallsBackToDefaults()
    {
        ReviewScopeConfig config = ReviewScopeConfig.fromTaskSnapshot(null);

        assertTrue(config.excludePatterns().isEmpty());
        assertFalse(config.includeTests());
        assertFalse(config.reportExisting());
        assertTrue(config.expandEnabled());
    }

    @Test
    void fromTaskSnapshotNullColumnsFallBackToDefaults()
    {
        // M3.2 快照冻结上线前建单的历史任务：四列全 NULL，行为必须与平台默认一致
        ReviewTask task = new ReviewTask();

        ReviewScopeConfig config = ReviewScopeConfig.fromTaskSnapshot(task);

        assertTrue(config.excludePatterns().isEmpty());
        assertFalse(config.includeTests());
        assertFalse(config.reportExisting());
        assertTrue(config.expandEnabled());
    }

    @Test
    void fromTaskSnapshotReadsFrozenValues()
    {
        ReviewTask task = new ReviewTask();
        task.setSnapshotScopeExcludePatterns("docs/**\n\n *.md \n");
        task.setSnapshotScopeIncludeTests("Y");
        task.setSnapshotScopeReportExisting("Y");
        task.setSnapshotScopeExpandEnabled("N");

        ReviewScopeConfig config = ReviewScopeConfig.fromTaskSnapshot(task);

        assertEquals(java.util.List.of("docs/**", "*.md"), config.excludePatterns());
        assertTrue(config.includeTests());
        assertTrue(config.reportExisting());
        assertFalse(config.expandEnabled());
    }

    @Test
    void fromTaskSnapshotExpandEnabledDefaultsTrueUnlessExplicitN()
    {
        ReviewTask task = new ReviewTask();
        task.setSnapshotScopeExpandEnabled("");

        assertTrue(ReviewScopeConfig.fromTaskSnapshot(task).expandEnabled());

        task.setSnapshotScopeExpandEnabled("N");
        assertFalse(ReviewScopeConfig.fromTaskSnapshot(task).expandEnabled());
    }
}
