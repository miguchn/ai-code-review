package com.acr.review.service;

import java.util.List;
import com.acr.review.domain.ReviewTask;
import com.acr.review.domain.ReviewTaskDetail;
import com.acr.review.domain.result.ReviewConclusionDailyStat;

/** 审查记录：已完成审查结果的查询视图（复用 review_task / review_task_run）。 */
public interface IReviewRecordService
{
    List<ReviewTask> selectReviewRecordList(ReviewTask query);

    ReviewTaskDetail selectReviewRecordDetail(Long taskId);

    /** 与审查记录列表同口径计数。 */
    int countReviewRecordList(ReviewTask query);

    /** 审查结论按天聚合（时间窗走 query.params.beginTime/endTime），与审查记录同 DataScope。 */
    List<ReviewConclusionDailyStat> selectReviewConclusionTrend(ReviewTask query);
}
