package com.acr.review.insight;

import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import com.acr.common.annotation.DataScope;
import com.acr.review.domain.ReviewProject;
import com.acr.review.mapper.ReviewMemberStatsDailyMapper;
import com.acr.review.mapper.ReviewProjectMapper;
import com.acr.review.mapper.ReviewStatsDailyMapper;
import com.acr.review.mapper.ReviewStatsSourceMapper;
import com.acr.system.domain.SysUserIdentity;
import com.acr.system.mapper.SysUserIdentityMapper;

/** 带 DataScope 的只读查询（独立 Bean，避免同类自调用绕过 AOP）。 */
@Component
public class InsightScopeQueries
{
    private final ReviewStatsDailyMapper dailyMapper;
    private final ReviewStatsSourceMapper sourceMapper;
    private final ReviewProjectMapper projectMapper;
    private final ReviewMemberStatsDailyMapper memberStatsMapper;
    private final SysUserIdentityMapper userIdentityMapper;

    public InsightScopeQueries(ReviewStatsDailyMapper dailyMapper,
                               ReviewStatsSourceMapper sourceMapper,
                               ReviewProjectMapper projectMapper,
                               ReviewMemberStatsDailyMapper memberStatsMapper,
                               SysUserIdentityMapper userIdentityMapper)
    {
        this.dailyMapper = dailyMapper;
        this.sourceMapper = sourceMapper;
        this.projectMapper = projectMapper;
        this.memberStatsMapper = memberStatsMapper;
        this.userIdentityMapper = userIdentityMapper;
    }

    @DataScope(deptAlias = "d", userAlias = "owner", permission = InsightConstants.PERM_OVERVIEW_VIEW)
    public List<ReviewStatsDaily> selectStatsOverview(ReviewStatsDaily query)
    {
        return dailyMapper.selectScopedRange(query);
    }

    @DataScope(deptAlias = "d", userAlias = "owner", permission = InsightConstants.PERM_PROJECT_VIEW)
    public List<ReviewStatsDaily> selectStatsProject(ReviewStatsDaily query)
    {
        return dailyMapper.selectScopedRange(query);
    }

    @DataScope(deptAlias = "d", userAlias = "owner", permission = InsightConstants.PERM_PROJECT_VIEW)
    public List<ReviewProject> selectProjects(ReviewProject query)
    {
        return projectMapper.selectReviewProjectList(query);
    }

    @DataScope(deptAlias = "d", userAlias = "owner", permission = InsightConstants.PERM_OVERVIEW_VIEW)
    public int countOpenFocusOverview(ReviewProject query)
    {
        return sourceMapper.countOpenFocusIssues(query);
    }

    @DataScope(deptAlias = "d", userAlias = "owner", permission = InsightConstants.PERM_PROJECT_VIEW)
    public int countOpenFocusProject(ReviewProject query)
    {
        return sourceMapper.countOpenFocusIssues(query);
    }

    @DataScope(deptAlias = "d", userAlias = "owner", permission = InsightConstants.PERM_OVERVIEW_VIEW)
    public List<Map<String, Object>> categoryOverview(ReviewProject query)
    {
        return sourceMapper.selectCategoryDistribution(query);
    }

    @DataScope(deptAlias = "d", userAlias = "owner", permission = InsightConstants.PERM_PROJECT_VIEW)
    public List<Map<String, Object>> categoryProject(ReviewProject query)
    {
        return sourceMapper.selectCategoryDistribution(query);
    }

    @DataScope(deptAlias = "d", userAlias = "owner", permission = InsightConstants.PERM_OVERVIEW_VIEW)
    public List<Map<String, Object>> deliveryOverview(ReviewProject query)
    {
        return sourceMapper.selectDeliveryChannelHealth(query);
    }

    @DataScope(deptAlias = "d", userAlias = "owner", permission = InsightConstants.PERM_PROJECT_VIEW)
    public List<Map<String, Object>> deliveryProject(ReviewProject query)
    {
        return sourceMapper.selectDeliveryChannelHealth(query);
    }

    @DataScope(deptAlias = "d", userAlias = "owner", permission = InsightConstants.PERM_TEAM_VIEW)
    public List<ReviewMemberStatsDaily> selectMemberStatsTeam(ReviewMemberStatsDaily query)
    {
        return memberStatsMapper.selectScopedRange(query);
    }

    @DataScope(deptAlias = "d", userAlias = "owner", permission = InsightConstants.PERM_TEAM_VIEW)
    public List<ReviewProject> selectProjectsTeam(ReviewProject query)
    {
        return projectMapper.selectReviewProjectList(query);
    }

    @DataScope(deptAlias = "d", userAlias = "u", permission = InsightConstants.PERM_IDENTITY_MANAGE)
    public List<SysUserIdentity> selectIdentitiesManage(SysUserIdentity query)
    {
        return userIdentityMapper.selectScopedList(query);
    }
}
