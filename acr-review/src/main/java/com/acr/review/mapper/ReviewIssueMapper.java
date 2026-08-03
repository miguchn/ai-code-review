package com.acr.review.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.acr.review.domain.ReviewIssue;

public interface ReviewIssueMapper
{
    ReviewIssue selectIssueById(Long issueId);

    ReviewIssue selectByProjectPrFingerprint(@Param("projectId") Long projectId,
                                             @Param("prNumber") Integer prNumber,
                                             @Param("fingerprint") String fingerprint);

    List<ReviewIssue> selectIssueList(ReviewIssue query);

    List<ReviewIssue> selectByProjectAndPr(@Param("projectId") Long projectId,
                                          @Param("prNumber") Integer prNumber);

    int insertIssue(ReviewIssue issue);

    int updateIssueSnapshot(ReviewIssue issue);

    int updateIssueDisposition(ReviewIssue issue);

    int countOpenNewByProject(@Param("projectId") Long projectId);
}
