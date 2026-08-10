package com.acr.review.insight;

import java.util.List;
import com.acr.review.insight.dto.InsightMemberClaimRequest;
import com.acr.review.insight.dto.InsightMemberMineResponse;
import com.acr.review.insight.dto.InsightMetricsDictResponse;
import com.acr.review.insight.dto.InsightOverviewResponse;
import com.acr.review.insight.dto.InsightProjectDetailResponse;
import com.acr.review.insight.dto.InsightProjectRow;
import com.acr.review.insight.dto.InsightTeamMembersResponse;

public interface IReviewInsightService
{
    InsightOverviewResponse getOverview(String beginDate, String endDate, Integer days, Long businessSystemId);

    List<InsightProjectRow> listProjects(String beginDate, String endDate, Integer days,
                                         Long businessSystemId, Long projectId, String orderBy);

    InsightProjectDetailResponse getProjectDetail(Long projectId, String beginDate, String endDate, Integer days);

    InsightMetricsDictResponse getMetricsDictionary();

    InsightMemberMineResponse getMemberMine(String beginDate, String endDate, Integer days);

    void claimMemberIdentity(InsightMemberClaimRequest request);

    InsightTeamMembersResponse getTeamMembers(String beginDate, String endDate, Integer days, Long businessSystemId);
}
