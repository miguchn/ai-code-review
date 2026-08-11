package com.acr.review.insight;

import java.util.List;
import com.acr.review.insight.dto.InsightIdentityBindRequest;
import com.acr.review.insight.dto.InsightIdentityCandidateVo;
import com.acr.review.insight.dto.InsightMemberMineResponse;
import com.acr.review.insight.dto.InsightMetricsDictResponse;
import com.acr.review.insight.dto.InsightOverviewResponse;
import com.acr.review.insight.dto.InsightProjectDetailResponse;
import com.acr.review.insight.dto.InsightProjectRow;
import com.acr.review.insight.dto.InsightTeamIdentitiesResponse;
import com.acr.review.insight.dto.InsightTeamMembersResponse;
import com.acr.review.insight.dto.InsightUserOption;

public interface IReviewInsightService
{
    InsightOverviewResponse getOverview(String beginDate, String endDate, Integer days, Long businessSystemId);

    List<InsightProjectRow> listProjects(String beginDate, String endDate, Integer days,
                                         Long businessSystemId, Long projectId, String orderBy);

    InsightProjectDetailResponse getProjectDetail(Long projectId, String beginDate, String endDate, Integer days);

    InsightMetricsDictResponse getMetricsDictionary();

    InsightMemberMineResponse getMemberMine(String beginDate, String endDate, Integer days);

    InsightTeamMembersResponse getTeamMembers(String beginDate, String endDate, Integer days,
                                              Long businessSystemId, Long projectId);

    List<InsightIdentityCandidateVo> listIdentityCandidates();

    InsightTeamIdentitiesResponse listTeamIdentities();

    /** 指派弹窗用户候选；DataScope 由 selectUserList 承担，最多 20 条。 */
    List<InsightUserOption> listIdentityUserOptions(String keyword);

    /** @return true 若发生了改派 */
    boolean bindTeamIdentity(InsightIdentityBindRequest request);

    void unbindTeamIdentity(Long id);
}
