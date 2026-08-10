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

    /** 对账后固化运行结果与台账问题的关联，不改动原始结果正文。 */
    int updateTopIssuesJson(@Param("runId") Long runId, @Param("topIssuesJson") String topIssuesJson);

    Integer selectMaxAttemptNo(@Param("taskId") Long taskId);

    /** 新 epoch 开始前，将上次因租约过期遗留的 RUNNING 执行记录闭合。 */
    int failInterruptedRuns(@Param("taskId") Long taskId,
                            @Param("failureType") String failureType,
                            @Param("failureMessage") String failureMessage);
}
