package com.acr.review.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import static org.mockito.Mockito.inOrder;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import com.acr.common.exception.ServiceException;
import com.acr.review.domain.ReviewPipelineConstants;
import com.acr.review.domain.ReviewProject;
import com.acr.review.domain.ReviewTask;
import com.acr.review.domain.ReviewTemplate;
import com.acr.review.domain.ReviewWebhookEvent;
import com.acr.review.engine.config.ReviewEngineProperties;
import com.acr.review.git.GitPullRequestEvent;
import com.acr.review.git.GitPushEvent;
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
                && ReviewPipelineConstants.EVENT_SOURCE_PR.equals(task.getEventSource())
                && "PR#12".equals(task.getChangeKey())
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
        verify(taskMapper).supersedePendingByChangeKey(1L, "PR#12", 100L);
        verify(eventMapper).updateProcessResult(org.mockito.ArgumentMatchers.argThat(e ->
            "ACCEPTED".equals(e.getProcessStatus()) && Long.valueOf(100L).equals(e.getTaskId())));
        verify(executionService).scheduleExecution(100L);
    }

    @Test
    void consecutivePrHeadsSupersedePriorPendingTowardLatestTask()
    {
        ReviewProject project = new ReviewProject();
        project.setProjectId(9L);
        project.setReviewMode("LLM_DIRECT");
        project.setModelId(3L);
        project.setTemplateId(4L);
        when(modelConfigService.selectRuntimeConfigById(3L)).thenReturn(enabledModel());
        when(templateService.selectEnabledTemplateById(4L)).thenReturn(enabledTemplate());
        when(taskMapper.supersedePendingByChangeKey(eq(9L), eq("PR#12"), org.mockito.ArgumentMatchers.anyLong()))
            .thenReturn(1);
        when(taskMapper.insertReviewTask(any())).thenAnswer(invocation -> {
            ReviewTask task = invocation.getArgument(0);
            task.setTaskId(1000L + task.getEventId() - 100L);
            return 1;
        });

        for (int i = 0; i < 10; i++)
        {
            ReviewWebhookEvent event = new ReviewWebhookEvent();
            event.setEventId(100L + i);
            event.setProvider("GITHUB");
            GitPullRequestEvent prEvent = new GitPullRequestEvent(
                "d-" + i, "synchronize", "miguchn", "demo", 12, "feat: login",
                "feature/login", "dev", "base" + i, "head" + i, "alice", 1, 1);
            assertEquals(1000L + i, service.createTaskFromEvent(project, event, prEvent));
        }

        ArgumentCaptor<Long> supersededBy = ArgumentCaptor.forClass(Long.class);
        verify(taskMapper, times(10)).supersedePendingByChangeKey(eq(9L), eq("PR#12"), supersededBy.capture());
        assertEquals(java.util.List.of(1000L, 1001L, 1002L, 1003L, 1004L, 1005L, 1006L, 1007L, 1008L, 1009L),
            supersededBy.getAllValues());
        InOrder order = inOrder(taskMapper);
        for (long taskId = 1000L; taskId <= 1009L; taskId++)
        {
            order.verify(taskMapper).supersedePendingByChangeKey(9L, "PR#12", taskId);
        }
    }

    @Test
    void consecutivePushHeadsSupersedePriorPendingTowardLatestTask()
    {
        ReviewProject project = new ReviewProject();
        project.setProjectId(9L);
        project.setReviewMode("LLM_DIRECT");
        project.setModelId(3L);
        project.setTemplateId(4L);
        when(modelConfigService.selectRuntimeConfigById(3L)).thenReturn(enabledModel());
        when(templateService.selectEnabledTemplateById(4L)).thenReturn(enabledTemplate());
        when(taskMapper.insertReviewTask(any())).thenAnswer(invocation -> {
            ReviewTask task = invocation.getArgument(0);
            task.setTaskId(2000L + task.getEventId() - 200L);
            return 1;
        });

        for (int i = 0; i < 10; i++)
        {
            ReviewWebhookEvent event = new ReviewWebhookEvent();
            event.setEventId(200L + i);
            event.setProvider("GITHUB");
            GitPushEvent pushEvent = new GitPushEvent(
                "p-" + i, "miguchn", "demo", "miguchn/demo", "main",
                "before" + i, "after" + i, "bob", 1, "msg", false, false, java.util.List.of());
            assertEquals(2000L + i, service.createTaskFromPushEvent(project, event, pushEvent));
        }

        ArgumentCaptor<Long> supersededBy = ArgumentCaptor.forClass(Long.class);
        verify(taskMapper, times(10)).supersedePendingByChangeKey(eq(9L), eq("PUSH#main"), supersededBy.capture());
        assertEquals(java.util.List.of(2000L, 2001L, 2002L, 2003L, 2004L, 2005L, 2006L, 2007L, 2008L, 2009L),
            supersededBy.getAllValues());
    }

    @Test
    void createsPushTaskWithSentinelPrNumberAndSchedulesExecution()
    {
        ReviewProject project = new ReviewProject();
        project.setProjectId(1L);
        project.setReviewMode("LLM_DIRECT");
        project.setModelId(3L);
        project.setTemplateId(4L);
        ReviewWebhookEvent event = new ReviewWebhookEvent();
        event.setEventId(11L);
        event.setProvider("GITHUB");
        GitPushEvent pushEvent = new GitPushEvent(
            "d-push", "miguchn", "demo", "miguchn/demo", "main",
            "aaaabbbbccccddddeeeeffff0000111122223333", "ffffeeeeddddccccbbbbaaaa3333222211110000",
            "bob", 3, "fix: cache\n\ndetails", false, false, java.util.List.of());
        when(taskMapper.insertReviewTask(any())).thenAnswer(invocation -> {
            ReviewTask task = invocation.getArgument(0);
            task.setTaskId(200L);
            return 1;
        });
        when(modelConfigService.selectRuntimeConfigById(3L)).thenReturn(enabledModel());
        when(templateService.selectEnabledTemplateById(4L)).thenReturn(enabledTemplate());

        Long taskId = service.createTaskFromPushEvent(project, event, pushEvent);

        assertEquals(200L, taskId);
        verify(taskMapper).insertReviewTask(org.mockito.ArgumentMatchers.argThat(task ->
            ReviewPipelineConstants.EVENT_SOURCE_PUSH.equals(task.getEventSource())
                && "PUSH#main".equals(task.getChangeKey())
                && Integer.valueOf(0).equals(task.getPrNumber())
                && "fix: cache".equals(task.getPrTitle())
                && "bob".equals(task.getPrAuthor())
                && "main".equals(task.getSourceBranch()) && "main".equals(task.getTargetBranch())
                && task.getAdditions() == null && task.getDeletions() == null && task.getChangedFiles() == null
                && "WEBHOOK".equals(task.getTriggerType())
                && "PENDING".equals(task.getTaskStatus())));
        verify(taskMapper).supersedePendingByChangeKey(1L, "PUSH#main", 200L);
        verify(executionService).scheduleExecution(200L);
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
