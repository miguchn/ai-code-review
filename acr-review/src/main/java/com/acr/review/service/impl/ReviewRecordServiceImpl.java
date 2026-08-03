package com.acr.review.service.impl;

import java.util.List;
import org.springframework.stereotype.Service;
import com.acr.common.annotation.DataScope;
import com.acr.common.exception.ServiceException;
import com.acr.review.domain.ReviewDeliveryRecord;
import com.acr.review.domain.ReviewProject;
import com.acr.review.domain.ReviewTask;
import com.acr.review.domain.ReviewTaskDetail;
import com.acr.review.domain.ReviewTaskRun;
import com.acr.review.mapper.ReviewProjectMapper;
import com.acr.review.mapper.ReviewTaskMapper;
import com.acr.review.mapper.ReviewTaskRunMapper;
import com.acr.review.service.IReviewDeliveryService;
import com.acr.review.service.IReviewIssueService;
import com.acr.review.service.IReviewRecordService;
import com.acr.system.service.ISysDeptService;

/** 审查记录查询：已结束任务（SUCCESS + FAILED），数据范围与审查任务一致。 */
@Service
public class ReviewRecordServiceImpl implements IReviewRecordService
{
    private final ReviewTaskMapper taskMapper;
    private final ReviewTaskRunMapper runMapper;
    private final ReviewProjectMapper projectMapper;
    private final ISysDeptService deptService;
    private final IReviewDeliveryService deliveryService;
    private final IReviewIssueService issueService;

    public ReviewRecordServiceImpl(ReviewTaskMapper taskMapper,
                                   ReviewTaskRunMapper runMapper,
                                   ReviewProjectMapper projectMapper,
                                   ISysDeptService deptService,
                                   IReviewDeliveryService deliveryService,
                                   IReviewIssueService issueService)
    {
        this.taskMapper = taskMapper;
        this.runMapper = runMapper;
        this.projectMapper = projectMapper;
        this.deptService = deptService;
        this.deliveryService = deliveryService;
        this.issueService = issueService;
    }

    @Override
    @DataScope(deptAlias = "d", userAlias = "owner", permission = "review:record:list")
    public List<ReviewTask> selectReviewRecordList(ReviewTask query)
    {
        return taskMapper.selectReviewRecordList(query);
    }

    @Override
    public ReviewTaskDetail selectReviewRecordDetail(Long taskId)
    {
        ReviewTask task = taskMapper.selectReviewTaskById(taskId);
        if (task == null)
        {
            throw new ServiceException("审查记录不存在");
        }
        if (!"SUCCESS".equals(task.getTaskStatus()) && !"FAILED".equals(task.getTaskStatus()))
        {
            throw new ServiceException("该任务尚未结束，请到审查任务中查看执行状态");
        }
        checkTaskDataScope(task);
        List<ReviewTaskRun> runs = runMapper.selectRunsByTaskId(taskId);
        issueService.enrichRuns(runs, task.getProjectId(), task.getPrNumber());
        ReviewDeliveryRecord delivery = deliveryService.selectSummaryDelivery(task.getProjectId(), task.getPrNumber());
        return new ReviewTaskDetail(task, runs, delivery);
    }

    private void checkTaskDataScope(ReviewTask task)
    {
        ReviewProject project = projectMapper.selectReviewProjectById(task.getProjectId());
        if (project == null)
        {
            throw new ServiceException("审查记录所属项目不存在");
        }
        deptService.checkDeptDataScope(project.getDeptId());
    }
}
