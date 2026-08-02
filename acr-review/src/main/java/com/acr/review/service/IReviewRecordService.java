package com.acr.review.service;

import java.util.List;
import com.acr.review.domain.ReviewTask;
import com.acr.review.domain.ReviewTaskDetail;

/** 审查记录：已完成审查结果的查询视图（复用 review_task / review_task_run）。 */
public interface IReviewRecordService
{
    List<ReviewTask> selectReviewRecordList(ReviewTask query);

    ReviewTaskDetail selectReviewRecordDetail(Long taskId);
}
