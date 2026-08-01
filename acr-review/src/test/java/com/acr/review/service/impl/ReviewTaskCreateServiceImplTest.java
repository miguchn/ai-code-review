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
import com.acr.review.domain.ReviewWebhookEvent;
import com.acr.review.git.GitPullRequestEvent;
import com.acr.review.mapper.ReviewTaskMapper;
import com.acr.review.mapper.ReviewWebhookEventMapper;

class ReviewTaskCreateServiceImplTest
{
    private final ReviewTaskMapper taskMapper = mock(ReviewTaskMapper.class);
    private final ReviewWebhookEventMapper eventMapper = mock(ReviewWebhookEventMapper.class);
    private final ReviewTaskCreateServiceImpl service = new ReviewTaskCreateServiceImpl(taskMapper, eventMapper);

    @Test
    void createsPendingTaskAndAcceptsEvent()
    {
        ReviewProject project = new ReviewProject();
        project.setProjectId(1L);
        ReviewWebhookEvent event = new ReviewWebhookEvent();
        event.setEventId(10L);
        event.setProvider("GITHUB");
        GitPullRequestEvent prEvent = new GitPullRequestEvent(
            "d-1", "opened", "miguchn", "demo", 12, "feat: login",
            "feature/login", "dev", "aaaabbbbccccddddeeeeffff0000111122223333", "ffffeeeeddddccccbbbbaaaa3333222211110000");
        when(taskMapper.insertReviewTask(any())).thenAnswer(invocation -> {
            ReviewTask task = invocation.getArgument(0);
            task.setTaskId(100L);
            return 1;
        });

        Long taskId = service.createTaskFromEvent(project, event, prEvent);

        assertEquals(100L, taskId);
        verify(taskMapper).insertReviewTask(org.mockito.ArgumentMatchers.argThat(task ->
            task.getProjectId().equals(1L) && task.getEventId().equals(10L)
                && "PENDING".equals(task.getTaskStatus()) && "WEBHOOK".equals(task.getTriggerType())
                && Integer.valueOf(12).equals(task.getPrNumber())
                && "feature/login".equals(task.getSourceBranch()) && "dev".equals(task.getTargetBranch())
                && task.getBaseSha().startsWith("aaaa") && task.getHeadSha().startsWith("ffff")));
        verify(eventMapper).updateProcessResult(org.mockito.ArgumentMatchers.argThat(e ->
            "ACCEPTED".equals(e.getProcessStatus()) && Long.valueOf(100L).equals(e.getTaskId())));
    }

    @Test
    void rejectsDuplicateTaskForSameEvent()
    {
        ReviewProject project = new ReviewProject();
        project.setProjectId(1L);
        ReviewWebhookEvent event = new ReviewWebhookEvent();
        event.setEventId(10L);
        event.setProvider("GITHUB");
        GitPullRequestEvent prEvent = new GitPullRequestEvent(
            "d-1", "opened", "miguchn", "demo", 12, "t", "a", "dev", "b", "h");
        when(taskMapper.insertReviewTask(any())).thenThrow(new DuplicateKeyException("uk_task_event"));

        assertThrows(ServiceException.class, () -> service.createTaskFromEvent(project, event, prEvent));
    }
}
