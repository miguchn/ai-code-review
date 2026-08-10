package com.acr.review.mapper;

import com.acr.review.runtime.ReviewRuntimeBacklogItem;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;

/** 运行概览与积压清单的只读聚合查询。 */
public interface ReviewRuntimeStatsMapper
{
    List<Map<String, Object>> countTasksByStatus(@Param("statuses") List<String> statuses);

    Map<String, Object> selectOldestPendingTask();

    List<Map<String, Object>> countTerminalTasksSinceHours(@Param("hours") int hours);

    long countRetryEventsSinceHours(@Param("hours") int hours);

    long countTimeoutEventsSinceHours(@Param("hours") int hours);

    Map<String, Object> countTerminalOutcomesSinceMinutes(@Param("minutes") int minutes);

    long countDeliveriesByStatus(@Param("status") String status);

    Map<String, Object> selectOldestPendingDelivery();

    List<ReviewRuntimeBacklogItem> selectOverduePendingTasks(@Param("ageMinutes") int ageMinutes,
                                                            @Param("limit") int limit);

    List<ReviewRuntimeBacklogItem> selectLeaseExpiredTasks(@Param("limit") int limit);

    List<ReviewRuntimeBacklogItem> selectStuckDeliveries(@Param("ageMinutes") int ageMinutes,
                                                        @Param("limit") int limit);
}
