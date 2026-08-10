package com.acr.review.mapper;

import java.util.Date;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;

/**
 * 聚合用事实表只读查询（按项目×日）。
 * 返回行字段使用 Map，键与 SQL 别名一致。
 */
public interface ReviewStatsSourceMapper
{
    List<Long> selectActiveProjectIds();

    List<Map<String, Object>> selectTaskAggByDay(@Param("projectId") Long projectId,
                                                 @Param("beginDate") Date beginDate,
                                                 @Param("endDate") Date endDate);

    List<Long> selectSuccessDurations(@Param("projectId") Long projectId,
                                      @Param("statDate") Date statDate);

    List<Map<String, Object>> selectIssueNewAggByDay(@Param("projectId") Long projectId,
                                                     @Param("beginDate") Date beginDate,
                                                     @Param("endDate") Date endDate);

    List<Map<String, Object>> selectIssueClosedAggByDay(@Param("projectId") Long projectId,
                                                        @Param("beginDate") Date beginDate,
                                                        @Param("endDate") Date endDate);

    List<Map<String, Object>> selectIssueConfirmedAggByDay(@Param("projectId") Long projectId,
                                                           @Param("beginDate") Date beginDate,
                                                           @Param("endDate") Date endDate);

    List<Map<String, Object>> selectDeliveryAggByDay(@Param("projectId") Long projectId,
                                                     @Param("beginDate") Date beginDate,
                                                     @Param("endDate") Date endDate);

    List<Map<String, Object>> selectEventAggByDay(@Param("projectId") Long projectId,
                                                  @Param("beginDate") Date beginDate,
                                                  @Param("endDate") Date endDate);

    List<Map<String, Object>> selectTaskCoveredAggByDay(@Param("projectId") Long projectId,
                                                        @Param("beginDate") Date beginDate,
                                                        @Param("endDate") Date endDate);

    int countOpenFocusIssues(com.acr.review.domain.ReviewProject query);

    List<Map<String, Object>> selectCategoryDistribution(com.acr.review.domain.ReviewProject query);

    List<Map<String, Object>> selectDeliveryChannelHealth(com.acr.review.domain.ReviewProject query);

    Date selectProjectLastReviewTime(@Param("projectId") Long projectId);

    List<Map<String, Object>> selectTasksReviewedByAuthorDay(@Param("projectId") Long projectId,
                                                             @Param("beginDate") Date beginDate,
                                                             @Param("endDate") Date endDate);

    List<Map<String, Object>> selectIssuesNewByAuthorDay(@Param("projectId") Long projectId,
                                                         @Param("beginDate") Date beginDate,
                                                         @Param("endDate") Date endDate);

    List<Map<String, Object>> selectIssuesOpenByAuthorAsOf(@Param("projectId") Long projectId,
                                                           @Param("asOfDate") Date asOfDate);

    List<Map<String, Object>> selectOpenIssuesByAuthorKeys(@Param("authorKeys") List<String> authorKeys,
                                                           @Param("limit") int limit);
}
