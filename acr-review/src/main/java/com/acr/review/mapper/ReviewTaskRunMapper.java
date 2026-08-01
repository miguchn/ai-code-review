package com.acr.review.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.acr.review.domain.ReviewTaskRun;

/** 审查任务执行记录数据访问。 */
public interface ReviewTaskRunMapper
{
    int insertReviewTaskRun(ReviewTaskRun run);

    int updateReviewTaskRun(ReviewTaskRun run);

    ReviewTaskRun selectReviewTaskRunById(Long runId);

    List<ReviewTaskRun> selectRunsByTaskId(@Param("taskId") Long taskId);

    Integer selectMaxAttemptNo(@Param("taskId") Long taskId);
}
