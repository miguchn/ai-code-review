package com.acr.review.mapper;

import java.util.List;
import com.acr.review.domain.ReviewTask;
import org.apache.ibatis.annotations.Param;

/** 审查任务数据访问。 */
public interface ReviewTaskMapper
{
    int insertReviewTask(ReviewTask task);

    ReviewTask selectReviewTaskById(Long taskId);

    List<ReviewTask> selectReviewTaskList(ReviewTask task);

    /** 按事件 ID 查询（幂等校验）。 */
    ReviewTask selectByEventId(@Param("eventId") Long eventId);
}
