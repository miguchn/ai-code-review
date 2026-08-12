package com.acr.review.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.List;
import org.junit.jupiter.api.Test;
import com.acr.common.exception.ServiceException;
import com.acr.review.domain.ReviewProject;
import com.acr.review.domain.ReviewTask;
import com.acr.review.domain.ReviewTaskDetail;
import com.acr.review.domain.ReviewTaskRun;
import com.acr.review.mapper.ReviewProjectMapper;
import com.acr.review.mapper.ReviewTaskMapper;
import com.acr.review.mapper.ReviewTaskRunMapper;
import com.acr.review.service.IReviewDeliveryService;
import com.acr.review.service.IReviewIssueService;
import com.acr.review.service.ReviewProjectAccessService;
import com.acr.system.service.ISysDeptService;

class ReviewRecordServiceImplTest
{
    private final ReviewTaskMapper taskMapper = mock(ReviewTaskMapper.class);
    private final ReviewTaskRunMapper runMapper = mock(ReviewTaskRunMapper.class);
    private final ReviewProjectMapper projectMapper = mock(ReviewProjectMapper.class);
    private final ISysDeptService deptService = mock(ISysDeptService.class);
    private final IReviewDeliveryService deliveryService = mock(IReviewDeliveryService.class);
    private final IReviewIssueService issueService = mock(IReviewIssueService.class);
    private final ReviewProjectAccessService projectAccessService = mock(ReviewProjectAccessService.class);
    private final ReviewRecordServiceImpl service = new ReviewRecordServiceImpl(
        taskMapper, runMapper, projectMapper, deptService, deliveryService, issueService, projectAccessService);

    @Test
    void returnsDetailForSuccessTaskWithinDataScope()
    {
        ReviewTask task = new ReviewTask();
        task.setTaskId(9L);
        task.setProjectId(3L);
        task.setPrNumber(1);
        task.setTaskStatus("SUCCESS");
        ReviewProject project = new ReviewProject();
        project.setProjectId(3L);
        project.setDeptId(88L);
        ReviewTaskRun run = new ReviewTaskRun();
        run.setRunId(1L);
        when(taskMapper.selectReviewTaskById(9L)).thenReturn(task);
        when(projectMapper.selectReviewProjectById(3L)).thenReturn(project);
        when(runMapper.selectRunsByTaskId(9L)).thenReturn(List.of(run));
        doNothing().when(deptService).checkDeptDataScope(88L);

        ReviewTaskDetail detail = service.selectReviewRecordDetail(9L);

        assertEquals(9L, detail.getTask().getTaskId());
        assertEquals(1, detail.getRuns().size());
        verify(projectAccessService).requireView(3L);
    }

    @Test
    void rejectsNonFinishedTask()
    {
        ReviewTask task = new ReviewTask();
        task.setTaskId(9L);
        task.setTaskStatus("RUNNING");
        when(taskMapper.selectReviewTaskById(9L)).thenReturn(task);

        ServiceException ex = assertThrows(ServiceException.class, () -> service.selectReviewRecordDetail(9L));
        assertEquals("该任务尚未结束，请到审查任务中查看执行状态", ex.getMessage());
    }

    @Test
    void returnsDetailForFailedTask()
    {
        ReviewTask task = new ReviewTask();
        task.setTaskId(10L);
        task.setProjectId(3L);
        task.setPrNumber(1);
        task.setTaskStatus("FAILED");
        ReviewProject project = new ReviewProject();
        project.setProjectId(3L);
        project.setDeptId(88L);
        when(taskMapper.selectReviewTaskById(10L)).thenReturn(task);
        when(projectMapper.selectReviewProjectById(3L)).thenReturn(project);
        when(runMapper.selectRunsByTaskId(10L)).thenReturn(List.of());
        doNothing().when(deptService).checkDeptDataScope(88L);

        ReviewTaskDetail detail = service.selectReviewRecordDetail(10L);
        assertEquals("FAILED", detail.getTask().getTaskStatus());
    }

    @Test
    void rejectsMissingTask()
    {
        when(taskMapper.selectReviewTaskById(1L)).thenReturn(null);
        assertThrows(ServiceException.class, () -> service.selectReviewRecordDetail(1L));
    }
}
