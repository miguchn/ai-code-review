package com.acr.review.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.acr.review.domain.ReviewIssue;
import com.acr.review.domain.ReviewIssueStats;

public interface ReviewIssueMapper
{
    ReviewIssue selectIssueById(Long issueId);

    List<ReviewIssue> selectIssueList(ReviewIssue query);

    /**
     * 按项目 + PR 编号查询问题；refBranch 非 null 时额外按参考分支过滤（含空串）。
     * PR 关闭联动等跨分支场景可传 null 表示不过滤 ref_branch。
     */
    List<ReviewIssue> selectByProjectAndPr(@Param("projectId") Long projectId,
                                          @Param("prNumber") Integer prNumber,
                                          @Param("refBranch") String refBranch);

    int insertIssue(ReviewIssue issue);

    int updateIssueSnapshot(ReviewIssue issue);

    int updateIssueDisposition(ReviewIssue issue);

    int countOpenNewByProject(@Param("projectId") Long projectId);

    /** 与 selectIssueList 同筛选、同 DataScope 的计数。 */
    int countIssueList(ReviewIssue query);

    /** 今日关闭问题数（status=CLOSED 且 DATE(closed_time)=CURDATE()）。 */
    int countClosedToday(ReviewIssue query);

    /** 状态总览计数（与列表同 DataScope，不含列表筛选条件）。 */
    ReviewIssueStats selectIssueStats(ReviewIssue query);
}
