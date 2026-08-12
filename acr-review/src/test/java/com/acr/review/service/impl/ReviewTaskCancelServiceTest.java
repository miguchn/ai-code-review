package com.acr.review.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.Test;
import com.acr.common.exception.ServiceException;
import com.acr.review.domain.ReviewPipelineConstants;
import com.acr.review.domain.ReviewProject;
import com.acr.review.domain.ReviewTask;
import com.acr.review.mapper.ReviewProjectMapper;
import com.acr.review.mapper.ReviewTaskMapper;
import com.acr.review.mapper.ReviewTaskRunMapper;
import com.acr.review.service.IReviewDeliveryService;
import com.acr.review.service.IReviewIssueService;
import com.acr.review.service.IReviewTaskExecutionService;
import com.acr.review.service.ReviewProjectAccessService;
import com.acr.system.service.ISysDeptService;

class ReviewTaskCancelServiceTest
{
    @Test
    void cancelTaskSetsCancelledAndRejectsTerminalStates()
    {
        ReviewTaskMapper taskMapper = mock(ReviewTaskMapper.class);
        ReviewProjectMapper projectMapper = mock(ReviewProjectMapper.class);
        ISysDeptService deptService = mock(ISysDeptService.class);
        ReviewTaskServiceImpl service = new ReviewTaskServiceImpl(
            taskMapper, mock(ReviewTaskRunMapper.class), projectMapper, deptService,
            mock(IReviewTaskExecutionService.class), mock(IReviewDeliveryService.class),
            mock(IReviewIssueService.class), mock(ReviewProjectAccessService.class));

        ReviewTask running = new ReviewTask();
        running.setTaskId(7L);
        running.setProjectId(3L);
        running.setTaskStatus(ReviewPipelineConstants.TASK_RUNNING);
        running.setExecutionEpoch(2L);
        when(taskMapper.selectReviewTaskById(7L)).thenReturn(running);
        ReviewProject project = new ReviewProject();
        project.setProjectId(3L);
        project.setDeptId(10L);
        when(projectMapper.selectReviewProjectById(3L)).thenReturn(project);
        when(taskMapper.cancelTask(eq(7L), any())).thenReturn(1);

        service.cancelTask(7L);
        verify(taskMapper).cancelTask(eq(7L), any());

        ReviewTask success = new ReviewTask();
        success.setTaskId(8L);
        success.setProjectId(3L);
        success.setTaskStatus(ReviewPipelineConstants.TASK_SUCCESS);
        when(taskMapper.selectReviewTaskById(8L)).thenReturn(success);
        ServiceException ex = assertThrows(ServiceException.class, () -> service.cancelTask(8L));
        assertTrueContains(ex.getMessage(), "不可终止");
        verify(taskMapper, never()).cancelTask(eq(8L), any());
    }

    private static void assertTrueContains(String actual, String expected)
    {
        assertEquals(true, actual != null && actual.contains(expected), actual);
    }
}
