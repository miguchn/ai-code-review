package com.acr.review.mapper;

import java.util.Date;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;
import com.acr.review.insight.ReviewCommitFact;

public interface ReviewCommitFactMapper
{
    int insertIgnore(ReviewCommitFact fact);

    int insertIgnoreBatch(@Param("list") List<ReviewCommitFact> list);

    List<Map<String, Object>> selectCommitCountByAuthorDay(@Param("projectId") Long projectId,
                                                           @Param("beginDate") Date beginDate,
                                                           @Param("endDate") Date endDate);

    List<Map<String, Object>> selectProjectCommitTrend(@Param("projectId") Long projectId,
                                                       @Param("beginDate") Date beginDate,
                                                       @Param("endDate") Date endDate);

    List<ReviewCommitFact> selectCandidateIdentities(@Param("email") String email,
                                                     @Param("userName") String userName,
                                                     @Param("limit") int limit);

    List<Map<String, Object>> selectCommitTrendByAuthorKeys(@Param("authorKeys") List<String> authorKeys,
                                                            @Param("beginDate") Date beginDate,
                                                            @Param("endDate") Date endDate,
                                                            @Param("projectIds") List<Long> projectIds);
}
