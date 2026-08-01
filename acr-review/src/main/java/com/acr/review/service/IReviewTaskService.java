package com.acr.review.service;

import java.util.List;
import com.acr.review.domain.ReviewTask;
import com.acr.review.domain.ReviewTaskDetail;

/** 审查任务查询与运维操作。 */
public interface IReviewTaskService
{
    ReviewTask selectReviewTaskById(Long taskId);

    List<ReviewTask> selectReviewTaskList(ReviewTask task);

    ReviewTaskDetail selectReviewTaskDetail(Long taskId);

    void retryTask(Long taskId);
}
