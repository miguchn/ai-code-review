package com.acr.review.service.impl;

import java.util.Date;
import java.util.List;
import org.springframework.stereotype.Service;
import com.acr.common.annotation.DataScope;
import com.acr.common.exception.ServiceException;
import com.acr.review.domain.ReviewDeliveryRecord;
import com.acr.review.domain.ReviewPipelineConstants;
import com.acr.review.domain.ReviewProject;
import com.acr.review.domain.ReviewTask;
import com.acr.review.domain.ReviewTaskDetail;
import com.acr.review.domain.ReviewTaskRun;
import com.acr.review.mapper.ReviewProjectMapper;
import com.acr.review.mapper.ReviewTaskMapper;
import com.acr.review.mapper.ReviewTaskRunMapper;
import com.acr.review.service.IReviewDeliveryService;
import com.acr.review.service.IReviewIssueService;
import com.acr.review.service.IReviewTaskExecutionService;
import com.acr.review.service.IReviewTaskService;
import com.acr.system.service.ISysDeptService;

/** 审查任务查询与重试。 */
@Service
public class ReviewTaskServiceImpl implements IReviewTaskService
{
    private final ReviewTaskMapper taskMapper;
    private final ReviewTaskRunMapper runMapper;
    private final ReviewProjectMapper projectMapper;
    private final ISysDeptService deptService;
    private final IReviewTaskExecutionService executionService;
    private final IReviewDeliveryService deliveryService;
    private final IReviewIssueService issueService;

    public ReviewTaskServiceImpl(ReviewTaskMapper taskMapper,
                                 ReviewTaskRunMapper runMapper,
                                 ReviewProjectMapper projectMapper,
                                 ISysDeptService deptService,
                                 IReviewTaskExecutionService executionService,
                                 IReviewDeliveryService deliveryService,
                                 IReviewIssueService issueService)
    {
        this.taskMapper = taskMapper;
        this.runMapper = runMapper;
        this.projectMapper = projectMapper;
        this.deptService = deptService;
        this.executionService = executionService;
        this.deliveryService = deliveryService;
        this.issueService = issueService;
    }

    @Override
    public ReviewTask selectReviewTaskById(Long taskId)
    {
        ReviewTask task = taskMapper.selectReviewTaskById(taskId);
        if (task == null)
        {
            throw new ServiceException("审查任务不存在");
        }
        return task;
    }

    @Override
    @DataScope(deptAlias = "d", userAlias = "owner", permission = "review:task:list")
    public List<ReviewTask> selectReviewTaskList(ReviewTask task)
    {
        return taskMapper.selectReviewTaskList(task);
    }

    @Override
    @DataScope(deptAlias = "d", userAlias = "owner", permission = "review:task:list")
    public int countReviewTaskList(ReviewTask task)
    {
        return taskMapper.countReviewTaskList(task);
    }

    @Override
    @DataScope(deptAlias = "d", userAlias = "owner", permission = "review:task:list")
    public int countTodayNewTasks(ReviewTask task)
    {
        return taskMapper.countTodayNewTasks(task);
    }

    @Override
    @DataScope(deptAlias = "d", userAlias = "owner", permission = "review:task:list")
    public int countTodaySuccessTasks(ReviewTask task)
    {
        return taskMapper.countTodaySuccessTasks(task);
    }

    @Override
    @DataScope(deptAlias = "d", userAlias = "owner", permission = "review:task:list")
    public int countTodayFailedTasks(ReviewTask task)
    {
        return taskMapper.countTodayFailedTasks(task);
    }

    @Override
    @DataScope(deptAlias = "d", userAlias = "owner", permission = "review:task:list")
    public Date selectLatestTaskTime(ReviewTask task)
    {
        return taskMapper.selectLatestTaskTime(task);
    }

    @Override
    @DataScope(deptAlias = "d", userAlias = "owner", permission = "review:task:list")
    public List<ReviewTask> selectRecentTasks(ReviewTask task)
    {
        return taskMapper.selectRecentTasks(task);
    }

    @Override
    public ReviewTaskDetail selectReviewTaskDetail(Long taskId)
    {
        ReviewTask task = selectReviewTaskById(taskId);
        checkTaskDataScope(task);
        List<ReviewTaskRun> runs = runMapper.selectRunsByTaskId(taskId);
        issueService.enrichRuns(runs, task.getProjectId(), task.getPrNumber(),
            ReviewIssueServiceImpl.resolveRefBranch(task));
        ReviewDeliveryRecord delivery = deliveryService.selectSummaryDelivery(task.getProjectId(), task.getPrNumber());
        return new ReviewTaskDetail(task, runs, delivery);
    }

    @Override
    public void retryTask(Long taskId)
    {
        ReviewTask task = selectReviewTaskById(taskId);
        checkTaskDataScope(task);
        executionService.retryTask(taskId);
    }

    @Override
    public void cancelTask(Long taskId)
    {
        ReviewTask task = selectReviewTaskById(taskId);
        checkTaskDataScope(task);
        String status = task.getTaskStatus();
        if (ReviewPipelineConstants.TASK_SUCCESS.equals(status)
            || ReviewPipelineConstants.TASK_FAILED.equals(status)
            || ReviewPipelineConstants.TASK_CANCELLED.equals(status)
            || ReviewPipelineConstants.TASK_SUPERSEDED.equals(status))
        {
            throw new ServiceException("当前状态不可终止：" + statusLabel(status));
        }
        String operator;
        try
        {
            operator = com.acr.common.utils.SecurityUtils.getUsername();
        }
        catch (RuntimeException ex)
        {
            operator = "system";
        }
        if (taskMapper.cancelTask(taskId, operator) != 1)
        {
            throw new ServiceException("终止失败，任务可能已被其他节点领取或状态已变更");
        }
    }

    private static String statusLabel(String status)
    {
        if (ReviewPipelineConstants.TASK_SUCCESS.equals(status))
        {
            return "已成功";
        }
        if (ReviewPipelineConstants.TASK_FAILED.equals(status))
        {
            return "已失败";
        }
        if (ReviewPipelineConstants.TASK_CANCELLED.equals(status))
        {
            return "已取消";
        }
        if (ReviewPipelineConstants.TASK_SUPERSEDED.equals(status))
        {
            return "已被替代";
        }
        return status == null ? "未知" : status;
    }

    /** 详情与重试不走列表的数据范围切面，按任务所属项目部门单独校验。 */
    private void checkTaskDataScope(ReviewTask task)
    {
        ReviewProject project = projectMapper.selectReviewProjectById(task.getProjectId());
        if (project == null)
        {
            throw new ServiceException("审查任务所属项目不存在");
        }
        deptService.checkDeptDataScope(project.getDeptId());
    }
}
