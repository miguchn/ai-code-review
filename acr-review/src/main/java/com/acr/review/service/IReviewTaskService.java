package com.acr.review.service;

import java.util.List;
import com.acr.review.domain.ReviewTask;

/** 审查任务查询用例。 */
public interface IReviewTaskService
{
    ReviewTask selectReviewTaskById(Long taskId);

    List<ReviewTask> selectReviewTaskList(ReviewTask task);
}
