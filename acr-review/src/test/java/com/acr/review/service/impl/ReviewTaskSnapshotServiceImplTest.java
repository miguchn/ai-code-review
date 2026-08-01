package com.acr.review.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.Test;
import com.acr.common.exception.ServiceException;
import com.acr.review.domain.ReviewProject;
import com.acr.review.domain.ReviewTask;
import com.acr.review.domain.ReviewTemplate;
import com.acr.review.engine.config.ReviewEngineProperties;
import com.acr.review.service.IReviewTemplateService;
import com.acr.system.domain.SysAiModelConfig;
import com.acr.system.service.ISysAiModelConfigService;

class ReviewTaskSnapshotServiceImplTest
{
    private final IReviewTemplateService templateService = mock(IReviewTemplateService.class);
    private final ISysAiModelConfigService modelConfigService = mock(ISysAiModelConfigService.class);
    private final ReviewTaskSnapshotServiceImpl service = new ReviewTaskSnapshotServiceImpl(
        templateService, modelConfigService, new ReviewEngineProperties());

    @Test
    void freezeLlmSnapshotCopiesModelAndTemplate()
    {
        ReviewProject project = llmProject();
        when(modelConfigService.selectRuntimeConfigById(3L)).thenReturn(enabledModel());
        when(templateService.selectEnabledTemplateById(4L)).thenReturn(enabledTemplate());
        ReviewTask task = new ReviewTask();

        service.freezeExecutionSnapshot(project, task);

        assertEquals("LLM_DIRECT", task.getSnapshotReviewMode());
        assertEquals(Long.valueOf(3L), task.getSnapshotModelId());
        assertEquals("DeepSeek", task.getSnapshotModelName());
        assertEquals("deepseek", task.getSnapshotModelProvider());
        assertEquals("deepseek-chat", task.getSnapshotModel());
        assertEquals(Long.valueOf(4L), task.getSnapshotTemplateId());
        assertEquals("builtin_java", task.getSnapshotTemplateCode());
        assertEquals(Integer.valueOf(2), task.getSnapshotTemplateVersion());
        assertEquals("模板正文", task.getSnapshotPromptContent());
        assertNull(task.getSnapshotEngineCode());
    }

    @Test
    void freezeRejectsProjectWithoutReviewMode()
    {
        ReviewProject project = new ReviewProject();
        ServiceException ex = assertThrows(ServiceException.class,
            () -> service.freezeExecutionSnapshot(project, new ReviewTask()));
        assertTrue(ex.getMessage().contains("未配置有效的审查方式"));
    }

    @Test
    void freezeRejectsLlmWithoutModel()
    {
        ReviewProject project = new ReviewProject();
        project.setReviewMode("LLM_DIRECT");
        project.setTemplateId(4L);
        ServiceException ex = assertThrows(ServiceException.class,
            () -> service.freezeExecutionSnapshot(project, new ReviewTask()));
        assertTrue(ex.getMessage().contains("未配置模型"));
    }

    @Test
    void freezeOcrEngineSetsEngineFieldsAndClearsLlm()
    {
        ReviewProject project = new ReviewProject();
        project.setReviewMode("OCR_PR_DIFF");
        ReviewTask task = new ReviewTask();
        task.setSnapshotModelId(3L);
        task.setSnapshotPromptContent("旧值");

        service.freezeExecutionSnapshot(project, task);

        assertEquals("OCR_ENGINE", task.getSnapshotReviewMode());
        assertEquals("OPEN_CODE_REVIEW", task.getSnapshotEngineCode());
        assertEquals("open-code-review", task.getSnapshotEngineName());
        assertNull(task.getSnapshotModelId());
        assertNull(task.getSnapshotPromptContent());
    }

    private ReviewProject llmProject()
    {
        ReviewProject project = new ReviewProject();
        project.setReviewMode("LLM_DIRECT");
        project.setModelId(3L);
        project.setTemplateId(4L);
        return project;
    }

    private SysAiModelConfig enabledModel()
    {
        SysAiModelConfig model = new SysAiModelConfig();
        model.setModelId(3L);
        model.setModelName("DeepSeek");
        model.setProvider("deepseek");
        model.setModel("deepseek-chat");
        model.setEnabled("1");
        model.setApiKey("secret");
        return model;
    }

    private ReviewTemplate enabledTemplate()
    {
        ReviewTemplate template = new ReviewTemplate();
        template.setTemplateId(4L);
        template.setTemplateName("Java");
        template.setTemplateCode("builtin_java");
        template.setVersionNo(2);
        template.setContent("模板正文");
        template.setStatus("0");
        return template;
    }
}
