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

    /** 审查记录列表：已结束任务（SUCCESS + FAILED）。 */
    List<ReviewTask> selectReviewRecordList(ReviewTask task);

    /** 按事件 ID 查询（幂等校验）。 */
    ReviewTask selectByEventId(@Param("eventId") Long eventId);

    /** 某项目某 PR 最近一次 SUCCESS 任务（投递重试渲染来源）。 */
    ReviewTask selectLatestSuccessByProjectAndPr(@Param("projectId") Long projectId,
                                                 @Param("prNumber") Integer prNumber);

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

    /** 与 selectReviewTaskList 同筛选、同 DataScope 的计数。 */
    int countReviewTaskList(ReviewTask task);

    /** 与 selectReviewRecordList 同筛选、同 DataScope 的计数。 */
    int countReviewRecordList(ReviewTask task);

    /** 今日新建任务数（DATE(create_time)=CURDATE()）。 */
    int countTodayNewTasks(ReviewTask task);

    /** 今日成功任务数。 */
    int countTodaySuccessTasks(ReviewTask task);

    /** 今日失败任务数。 */
    int countTodayFailedTasks(ReviewTask task);

    /** 可见范围内最近任务时间（ifnull(finished_time, create_time)）。 */
    Date selectLatestTaskTime(ReviewTask task);

    /** 最近审查任务（按 task_id 倒序，LIMIT 5）。 */
    List<ReviewTask> selectRecentTasks(ReviewTask task);
}
