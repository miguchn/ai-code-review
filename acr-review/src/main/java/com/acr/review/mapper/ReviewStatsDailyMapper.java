package com.acr.review.mapper;

import java.util.Date;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.acr.review.insight.ReviewStatsDaily;

/** review_stats_daily 读写。 */
public interface ReviewStatsDailyMapper
{
    int upsert(ReviewStatsDaily row);

    List<ReviewStatsDaily> selectByRange(@Param("projectIds") List<Long> projectIds,
                                         @Param("beginDate") Date beginDate,
                                         @Param("endDate") Date endDate);

    List<ReviewStatsDaily> selectScopedRange(ReviewStatsDaily query);

    Date selectEarliestStatDate(@Param("projectIds") List<Long> projectIds);
}
