package com.acr.review.scheduling;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.Test;
import com.acr.review.engine.ReviewEngineWorkspaceManager;
import com.acr.system.service.ISysConfigService;

class ReviewResourceBudgetServiceTest
{
    @Test
    void budgetExhaustionDoesNotBleedAcrossPools()
    {
        ISysConfigService config = mock(ISysConfigService.class);
        when(config.selectConfigByKey(ReviewTaskRuntimeSettings.CONFIG_OCR_MAX_CONCURRENCY)).thenReturn("1");
        when(config.selectConfigByKey(ReviewTaskRuntimeSettings.CONFIG_LLM_MAX_CONCURRENCY)).thenReturn("1");
        when(config.selectConfigByKey(ReviewTaskRuntimeSettings.CONFIG_WORKSPACE_MAX_COUNT)).thenReturn("1");
        when(config.selectConfigByKey(ReviewTaskRuntimeSettings.CONFIG_WORKSPACE_MAX_DISK_MB)).thenReturn("10240");
        when(config.selectConfigByKey(ReviewTaskRuntimeSettings.CONFIG_PROJECT_MAX_CONCURRENCY)).thenReturn("2");
        ReviewTaskRuntimeSettings settings = new ReviewTaskRuntimeSettings(config);
        ReviewEngineWorkspaceManager workspaceManager = mock(ReviewEngineWorkspaceManager.class);
        when(workspaceManager.getWorkspaceRoot()).thenReturn(java.nio.file.Path.of(System.getProperty("java.io.tmpdir")));

        ReviewResourceBudgetService budgets = new ReviewResourceBudgetService(settings, workspaceManager);
        ReviewBudgetLease ocr = budgets.tryAcquireOcrExecution();
        assertNotNull(ocr);
        assertNull(budgets.tryAcquireOcrExecution());
        ReviewBudgetLease llm = budgets.tryAcquireLlmCall();
        assertNotNull(llm, "OCR 占满不得阻塞 LLM 预算池");
        ocr.close();
        llm.close();
        assertNotNull(budgets.tryAcquireOcrExecution());
    }

    @Test
    void llmBudgetRespectsGlobalLimit()
    {
        ISysConfigService config = mock(ISysConfigService.class);
        when(config.selectConfigByKey(ReviewTaskRuntimeSettings.CONFIG_LLM_MAX_CONCURRENCY)).thenReturn("1");
        when(config.selectConfigByKey(ReviewTaskRuntimeSettings.CONFIG_OCR_MAX_CONCURRENCY)).thenReturn("2");
        when(config.selectConfigByKey(ReviewTaskRuntimeSettings.CONFIG_WORKSPACE_MAX_COUNT)).thenReturn("4");
        when(config.selectConfigByKey(ReviewTaskRuntimeSettings.CONFIG_WORKSPACE_MAX_DISK_MB)).thenReturn("10240");
        when(config.selectConfigByKey(ReviewTaskRuntimeSettings.CONFIG_PROJECT_MAX_CONCURRENCY)).thenReturn("2");
        ReviewEngineWorkspaceManager workspaceManager = mock(ReviewEngineWorkspaceManager.class);
        when(workspaceManager.getWorkspaceRoot()).thenReturn(java.nio.file.Path.of(System.getProperty("java.io.tmpdir")));
        ReviewResourceBudgetService budgets = new ReviewResourceBudgetService(
            new ReviewTaskRuntimeSettings(config), workspaceManager);

        ReviewBudgetLease first = budgets.tryAcquireLlmCall();
        assertNotNull(first);
        assertNull(budgets.tryAcquireLlmCall());
        first.close();
        assertNotNull(budgets.tryAcquireLlmCall());
    }

    @Test
    void projectConcurrencyTracksHeldAndRejected()
    {
        ISysConfigService config = mock(ISysConfigService.class);
        when(config.selectConfigByKey(ReviewTaskRuntimeSettings.CONFIG_PROJECT_MAX_CONCURRENCY)).thenReturn("1");
        when(config.selectConfigByKey(ReviewTaskRuntimeSettings.CONFIG_OCR_MAX_CONCURRENCY)).thenReturn("2");
        when(config.selectConfigByKey(ReviewTaskRuntimeSettings.CONFIG_LLM_MAX_CONCURRENCY)).thenReturn("2");
        when(config.selectConfigByKey(ReviewTaskRuntimeSettings.CONFIG_WORKSPACE_MAX_COUNT)).thenReturn("4");
        when(config.selectConfigByKey(ReviewTaskRuntimeSettings.CONFIG_WORKSPACE_MAX_DISK_MB)).thenReturn("10240");
        ReviewEngineWorkspaceManager workspaceManager = mock(ReviewEngineWorkspaceManager.class);
        when(workspaceManager.getWorkspaceRoot()).thenReturn(java.nio.file.Path.of(System.getProperty("java.io.tmpdir")));
        ReviewResourceBudgetService budgets = new ReviewResourceBudgetService(
            new ReviewTaskRuntimeSettings(config), workspaceManager);

        assertTrue(budgets.tryAcquireProject(7L));
        assertTrue(!budgets.tryAcquireProject(7L));
        assertTrue(budgets.tryAcquireProject(8L));
        budgets.releaseProject(7L);
        assertTrue(budgets.tryAcquireProject(7L));
        ReviewResourceBudgetStatus status = budgets.snapshot();
        assertEquals(1, status.projectLimitPerProject());
        assertTrue(status.projectRejected() >= 1);
    }
}
