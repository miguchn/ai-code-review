package com.acr.review.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import com.acr.common.exception.ServiceException;
import com.acr.review.domain.ReviewProject;
import com.acr.review.domain.ReviewTask;
import com.acr.review.domain.ReviewTemplate;
import com.acr.review.domain.ReviewWebhookEvent;
import com.acr.review.engine.config.ReviewEngineProperties;
import com.acr.review.git.GitPullRequestEvent;
import com.acr.review.mapper.ReviewTaskMapper;
import com.acr.review.mapper.ReviewWebhookEventMapper;
import com.acr.review.service.IReviewTaskExecutionService;
import com.acr.review.service.IReviewTemplateService;
import com.acr.system.domain.SysAiModelConfig;
import com.acr.system.service.ISysAiModelConfigService;

class ReviewTaskCreateServiceImplTest
{
    private final ReviewTaskMapper taskMapper = mock(ReviewTaskMapper.class);
    private final ReviewWebhookEventMapper eventMapper = mock(ReviewWebhookEventMapper.class);
    private final IReviewTaskExecutionService executionService = mock(IReviewTaskExecutionService.class);
    private final IReviewTemplateService templateService = mock(IReviewTemplateService.class);
    private final ISysAiModelConfigService modelConfigService = mock(ISysAiModelConfigService.class);
    private final ReviewTaskCreateServiceImpl service = new ReviewTaskCreateServiceImpl(
        taskMapper, eventMapper, executionService,
        new ReviewTaskSnapshotServiceImpl(templateService, modelConfigService, new ReviewEngineProperties()));

    @Test
    void createsPendingTaskAcceptsEventAndSchedulesExecution()
    {
        ReviewProject project = new ReviewProject();
        project.setProjectId(1L);
        project.setReviewMode("LLM_DIRECT");
        project.setModelId(3L);
        project.setTemplateId(4L);
        ReviewWebhookEvent event = new ReviewWebhookEvent();
        event.setEventId(10L);
        event.setProvider("GITHUB");
        GitPullRequestEvent prEvent = new GitPullRequestEvent(
            "d-1", "opened", "miguchn", "demo", 12, "feat: login",
            "feature/login", "dev", "aaaabbbbccccddddeeeeffff0000111122223333", "ffffeeeeddddccccbbbbaaaa3333222211110000",
            "alice", 15, 2);
        when(taskMapper.insertReviewTask(any())).thenAnswer(invocation -> {
            ReviewTask task = invocation.getArgument(0);
            task.setTaskId(100L);
            return 1;
        });
        when(modelConfigService.selectRuntimeConfigById(3L)).thenReturn(enabledModel());
        when(templateService.selectEnabledTemplateById(4L)).thenReturn(enabledTemplate());

        Long taskId = service.createTaskFromEvent(project, event, prEvent);

        assertEquals(100L, taskId);
        verify(taskMapper).insertReviewTask(org.mockito.ArgumentMatchers.argThat(task ->
            task.getProjectId().equals(1L) && task.getEventId().equals(10L)
                && "PENDING".equals(task.getTaskStatus()) && "WEBHOOK".equals(task.getTriggerType())
                && Integer.valueOf(12).equals(task.getPrNumber())
                && "alice".equals(task.getPrAuthor())
                && Integer.valueOf(15).equals(task.getAdditions())
                && Integer.valueOf(2).equals(task.getDeletions())
                && "feature/login".equals(task.getSourceBranch()) && "dev".equals(task.getTargetBranch())
                && task.getBaseSha().startsWith("aaaa") && task.getHeadSha().startsWith("ffff")
                && "LLM_DIRECT".equals(task.getSnapshotReviewMode())
                && Long.valueOf(3L).equals(task.getSnapshotModelId())
                && Long.valueOf(4L).equals(task.getSnapshotTemplateId())
                && "builtin_java".equals(task.getSnapshotTemplateCode())
                && Integer.valueOf(2).equals(task.getSnapshotTemplateVersion())
                && "模板正文".equals(task.getSnapshotPromptContent())));
        verify(eventMapper).updateProcessResult(org.mockito.ArgumentMatchers.argThat(e ->
            "ACCEPTED".equals(e.getProcessStatus()) && Long.valueOf(100L).equals(e.getTaskId())));
        verify(executionService).scheduleExecution(100L);
    }

    @Test
    void rejectsDuplicateTaskForSameEvent()
    {
        ReviewProject project = new ReviewProject();
        project.setProjectId(1L);
        project.setReviewMode("OCR_ENGINE");
        ReviewWebhookEvent event = new ReviewWebhookEvent();
        event.setEventId(10L);
        event.setProvider("GITHUB");
        GitPullRequestEvent prEvent = new GitPullRequestEvent(
            "d-1", "opened", "miguchn", "demo", 12, "t", "a", "dev", "b", "h");
        when(taskMapper.insertReviewTask(any())).thenThrow(new DuplicateKeyException("uk_task_event"));

        assertThrows(ServiceException.class, () -> service.createTaskFromEvent(project, event, prEvent));
    }

    @Test
    void rejectsLlmTaskWithoutEnabledTemplateBeforeInsert()
    {
        ReviewProject project = new ReviewProject();
        project.setProjectId(1L);
        project.setReviewMode("LLM_DIRECT");
        project.setModelId(3L);
        project.setTemplateId(4L);
        when(modelConfigService.selectRuntimeConfigById(3L)).thenReturn(enabledModel());
        when(templateService.selectEnabledTemplateById(4L)).thenThrow(new ServiceException("审查模板已停用"));

        assertThrows(ServiceException.class, () -> service.createTaskFromEvent(project, new ReviewWebhookEvent(),
            new GitPullRequestEvent("d", "opened", "owner", "repo", 1, "t", "s", "t", "b", "h")));
        verify(taskMapper, org.mockito.Mockito.never()).insertReviewTask(any());
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
