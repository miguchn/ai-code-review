package com.acr.review.mapper;

import java.util.Date;
import java.util.List;
import com.acr.review.domain.ReviewTask;
import com.acr.review.domain.result.ReviewConclusionDailyStat;
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

    /** 查询到期的 PENDING/RETRYING 候选；调用方按项目轮询后再提交执行。 */
    List<ReviewTask> selectDispatchableTasks(@Param("limit") int limit);

    /**
     * 建单事务内：将同 project_id+change_key 下尚未启动的 PENDING/RETRYING 任务条件更新为 SUPERSEDED。
     * 返回影响行数；不使用 SELECT FOR UPDATE。
     */
    int supersedePendingByChangeKey(@Param("projectId") Long projectId,
                                    @Param("changeKey") String changeKey,
                                    @Param("supersededBy") Long supersededBy);

    /**
     * 统计同变更下 task_id 更大的任务数；返回 0 表示本任务仍是最新。
     * 用于对账/评论 head 围栏。
     */
    int countNewerTasksByChangeKey(@Param("projectId") Long projectId,
                                   @Param("changeKey") String changeKey,
                                   @Param("taskId") Long taskId);

    /** 原子领取到期任务，并写入数据库租约、心跳和新 execution epoch。 */
    int claimTask(@Param("taskId") Long taskId,
                  @Param("currentStep") String currentStep,
                  @Param("leaseOwner") String leaseOwner,
                  @Param("leaseSeconds") int leaseSeconds);

    /**
     * 队列拒绝/项目并发限流：保留 PENDING/RETRYING，按退避延迟 next_run_at，不丢任务。
     */
    int deferDispatchableTask(@Param("taskId") Long taskId,
                              @Param("delaySeconds") int delaySeconds,
                              @Param("reason") String reason);

    /** 仅当前 owner + epoch 可续租；全部时间计算均在数据库完成。 */
    int renewTaskLease(@Param("taskId") Long taskId,
                       @Param("executionEpoch") Long executionEpoch,
                       @Param("leaseOwner") String leaseOwner,
                       @Param("leaseSeconds") int leaseSeconds);

    /** 恢复扫描：将未超过上限的到期 RUNNING 任务转回 RETRYING。 */
    int requeueExpiredTasks(@Param("maxRetries") int maxRetries,
                            @Param("retryDelaySeconds") int retryDelaySeconds);

    /** 恢复扫描：到期且超过恢复上限的 RUNNING 任务转人工失败终态。 */
    int terminalizeExpiredTasks(@Param("maxRetries") int maxRetries);

    /** 人工重试 FAILED：重置自动重试计数并立即进入统一 RETRYING 状态机。 */
    int requeueFailedTask(@Param("taskId") Long taskId);

    /** 人工接管数据库时钟判定已到期的 RUNNING 任务。 */
    int requeueExpiredTask(@Param("taskId") Long taskId);

    /** 停机时停止持有本实例租约，任务事实保留并可由其他实例接管。 */
    int releaseWorkerLeases(@Param("leaseOwner") String leaseOwner);

    /**
     * 优雅停机超时：仅将本实例持有的 RUNNING 租约置为已过期，由恢复扫描接管。
     * 不立刻改写 task_status，保留租约围栏语义。
     */
    int expireWorkerLeases(@Param("leaseOwner") String leaseOwner);

    /** 人工终止：置 CANCELLED 并递增 epoch，使旧执行后续写入被围栏拒绝。 */
    int cancelTask(@Param("taskId") Long taskId, @Param("operator") String operator);

    /** 带 owner + epoch fencing 的执行事实更新。 */
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

    /** 审查结论按天聚合（与 countReviewRecordList 同 join、同 DataScope；时间窗走 params.beginTime/endTime）。 */
    List<ReviewConclusionDailyStat> selectReviewConclusionTrend(ReviewTask task);
}
