package com.acr.review.mapper;

import java.util.Date;
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

    /**
     * 原子领取：PENDING/FAILED 可进入 RUNNING；RUNNING 超过回收阈值（上次执行已中断）也可重新领取。
     * @return 更新行数，1 表示领取成功
     */
    int claimTask(@Param("taskId") Long taskId,
                  @Param("currentStep") String currentStep,
                  @Param("startedTime") Date startedTime,
                  @Param("staleRunningMinutes") int staleRunningMinutes);

    int updateTaskExecution(ReviewTask task);

    /** 仅更新执行快照列：历史任务（快照冻结上线前建单）执行前补冻结时使用。 */
    int updateTaskSnapshot(ReviewTask task);
}
