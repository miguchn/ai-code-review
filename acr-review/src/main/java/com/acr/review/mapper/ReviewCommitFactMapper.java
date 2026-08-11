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

    /** 按邮箱精确 / 名称精确拉取候选身份（去重 author_key）。 */
    List<Map<String, Object>> selectMatchCandidateIdentities(@Param("email") String email,
                                                             @Param("userName") String userName,
                                                             @Param("nickName") String nickName,
                                                             @Param("limit") int limit);

    /** 某 author_key 最近一笔提交样例（含项目名）。 */
    Map<String, Object> selectLatestCommitSample(@Param("authorKey") String authorKey);

    /** 全部 distinct author_key（用于未关联分组）。 */
    List<String> selectDistinctAuthorKeys();

    List<Map<String, Object>> selectCommitTrendByAuthorKeys(@Param("authorKeys") List<String> authorKeys,
                                                            @Param("beginDate") Date beginDate,
                                                            @Param("endDate") Date endDate,
                                                            @Param("projectIds") List<Long> projectIds);
}
