package com.acr.review.service.impl;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.Test;
import com.acr.common.exception.ServiceException;
import com.acr.review.domain.ReviewProject;
import com.acr.review.domain.ReviewTask;
import com.acr.review.domain.ReviewTaskDetail;
import com.acr.review.mapper.ReviewProjectMapper;
import com.acr.review.mapper.ReviewTaskMapper;
import com.acr.review.mapper.ReviewTaskRunMapper;
import com.acr.review.service.IReviewTaskExecutionService;
import com.acr.system.service.ISysDeptService;

/** 任务详情/重试必须按项目部门做数据范围校验，不能绕过列表的 @DataScope。 */
class ReviewTaskServiceImplTest
{
    private final ReviewTaskMapper taskMapper = mock(ReviewTaskMapper.class);
    private final ReviewTaskRunMapper runMapper = mock(ReviewTaskRunMapper.class);
    private final ReviewProjectMapper projectMapper = mock(ReviewProjectMapper.class);
    private final ISysDeptService deptService = mock(ISysDeptService.class);
    private final IReviewTaskExecutionService executionService = mock(IReviewTaskExecutionService.class);
    private final ReviewTaskServiceImpl service = new ReviewTaskServiceImpl(
        taskMapper, runMapper, projectMapper, deptService, executionService);

    @Test
    void detailChecksDeptDataScope()
    {
        ReviewTask task = task(9L, 3L);
        ReviewProject project = project(3L, 100L);
        when(taskMapper.selectReviewTaskById(9L)).thenReturn(task);
        when(projectMapper.selectReviewProjectById(3L)).thenReturn(project);

        ReviewTaskDetail detail = service.selectReviewTaskDetail(9L);

        verify(deptService).checkDeptDataScope(100L);
        org.junit.jupiter.api.Assertions.assertEquals(9L, detail.getTask().getTaskId());
    }

    @Test
    void detailRejectedWhenDeptOutOfScope()
    {
        ReviewTask task = task(9L, 3L);
        when(taskMapper.selectReviewTaskById(9L)).thenReturn(task);
        when(projectMapper.selectReviewProjectById(3L)).thenReturn(project(3L, 100L));
        org.mockito.Mockito.doThrow(new ServiceException("没有权限访问部门数据"))
            .when(deptService).checkDeptDataScope(100L);

        assertThrows(ServiceException.class, () -> service.selectReviewTaskDetail(9L));
        verify(runMapper, never()).selectRunsByTaskId(org.mockito.ArgumentMatchers.anyLong());
    }

    @Test
    void retryChecksDeptDataScope()
    {
        ReviewTask task = task(9L, 3L);
        when(taskMapper.selectReviewTaskById(9L)).thenReturn(task);
        when(projectMapper.selectReviewProjectById(3L)).thenReturn(project(3L, 100L));

        service.retryTask(9L);

        verify(deptService).checkDeptDataScope(100L);
        verify(executionService).retryTask(9L);
    }

    @Test
    void retryRejectedWhenDeptOutOfScope()
    {
        ReviewTask task = task(9L, 3L);
        when(taskMapper.selectReviewTaskById(9L)).thenReturn(task);
        when(projectMapper.selectReviewProjectById(3L)).thenReturn(project(3L, 100L));
        org.mockito.Mockito.doThrow(new ServiceException("没有权限访问部门数据"))
            .when(deptService).checkDeptDataScope(100L);

        assertThrows(ServiceException.class, () -> service.retryTask(9L));
        verify(executionService, never()).retryTask(org.mockito.ArgumentMatchers.anyLong());
    }

    private ReviewTask task(Long taskId, Long projectId)
    {
        ReviewTask task = new ReviewTask();
        task.setTaskId(taskId);
        task.setProjectId(projectId);
        return task;
    }

    private ReviewProject project(Long projectId, Long deptId)
    {
        ReviewProject project = new ReviewProject();
        project.setProjectId(projectId);
        project.setDeptId(deptId);
        return project;
    }
}
