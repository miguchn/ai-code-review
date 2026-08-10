package com.acr.review.service;

import java.util.Date;
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

    /** 人工终止：置 CANCELLED，旧执行由 epoch/租约围栏拒绝后续写入。 */
    void cancelTask(Long taskId);

    /** 与任务列表同口径计数。 */
    int countReviewTaskList(ReviewTask task);

    /** 以下今日/最近查询的 task 参数仅承载 DataScope。 */
    int countTodayNewTasks(ReviewTask task);

    int countTodaySuccessTasks(ReviewTask task);

    int countTodayFailedTasks(ReviewTask task);

    Date selectLatestTaskTime(ReviewTask task);

    List<ReviewTask> selectRecentTasks(ReviewTask task);
}
