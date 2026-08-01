package com.acr.review.service.impl;

import java.util.List;
import org.springframework.stereotype.Service;
import com.acr.common.annotation.DataScope;
import com.acr.common.exception.ServiceException;
import com.acr.review.domain.ReviewTask;
import com.acr.review.mapper.ReviewTaskMapper;
import com.acr.review.service.IReviewTaskService;

/** 审查任务查询。 */
@Service
public class ReviewTaskServiceImpl implements IReviewTaskService
{
    private final ReviewTaskMapper taskMapper;

    public ReviewTaskServiceImpl(ReviewTaskMapper taskMapper)
    {
        this.taskMapper = taskMapper;
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
}
