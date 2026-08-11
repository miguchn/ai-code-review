package com.acr.review.mapper;

import java.util.Date;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;
import com.acr.review.insight.ReviewMemberStatsDaily;

public interface ReviewMemberStatsDailyMapper
{
    int upsert(ReviewMemberStatsDaily row);

    List<ReviewMemberStatsDaily> selectScopedRange(ReviewMemberStatsDaily query);

    List<Map<String, Object>> selectMemberAgg(@Param("beginDate") Date beginDate,
                                              @Param("endDate") Date endDate,
                                              @Param("authorKeys") List<String> authorKeys,
                                              @Param("projectIds") List<Long> projectIds);

    Date selectEarliestStatDate(@Param("authorKeys") List<String> authorKeys,
                                @Param("projectIds") List<Long> projectIds);
}
