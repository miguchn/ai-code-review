package com.acr.review.insight;

import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import com.acr.common.annotation.DataScope;
import com.acr.review.insight.dto.InsightTokenModelOption;
import com.acr.review.insight.dto.InsightTokenModelRow;
import com.acr.review.insight.dto.InsightTokenProjectRow;
import com.acr.review.insight.dto.InsightTokenRunRow;
import com.acr.review.insight.dto.InsightTokenTrendPoint;
import com.acr.review.insight.dto.TokenUsageProjectModelRow;
import com.acr.review.insight.dto.TokenUsageTotals;
import com.acr.review.domain.ReviewProject;
import com.acr.review.mapper.ReviewMemberStatsDailyMapper;
import com.acr.review.mapper.ReviewProjectMapper;
import com.acr.review.mapper.ReviewStatsDailyMapper;
import com.acr.review.mapper.ReviewStatsSourceMapper;
import com.acr.review.mapper.ReviewTokenUsageMapper;
import com.acr.review.service.ReviewProjectAccessService;
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
    private final ReviewTokenUsageMapper tokenUsageMapper;
    private final SysUserIdentityMapper userIdentityMapper;
    private final ReviewProjectAccessService projectAccessService;

    public InsightScopeQueries(ReviewStatsDailyMapper dailyMapper,
                               ReviewStatsSourceMapper sourceMapper,
                               ReviewProjectMapper projectMapper,
                               ReviewMemberStatsDailyMapper memberStatsMapper,
                               ReviewTokenUsageMapper tokenUsageMapper,
                               SysUserIdentityMapper userIdentityMapper,
                               ReviewProjectAccessService projectAccessService)
    {
        this.dailyMapper = dailyMapper;
        this.sourceMapper = sourceMapper;
        this.projectMapper = projectMapper;
        this.memberStatsMapper = memberStatsMapper;
        this.tokenUsageMapper = tokenUsageMapper;
        this.userIdentityMapper = userIdentityMapper;
        this.projectAccessService = projectAccessService;
    }

    @DataScope(deptAlias = "d", userAlias = "owner", permission = InsightConstants.PERM_OVERVIEW_VIEW)
    public List<ReviewStatsDaily> selectStatsOverview(ReviewStatsDaily query)
    {
        projectAccessService.applyQueryScope(query);
        return dailyMapper.selectScopedRange(query);
    }

    @DataScope(deptAlias = "d", userAlias = "owner", permission = InsightConstants.PERM_PROJECT_VIEW)
    public List<ReviewStatsDaily> selectStatsProject(ReviewStatsDaily query)
    {
        projectAccessService.applyQueryScope(query);
        return dailyMapper.selectScopedRange(query);
    }

    @DataScope(deptAlias = "d", userAlias = "owner", permission = InsightConstants.PERM_PROJECT_VIEW)
    public List<ReviewProject> selectProjects(ReviewProject query)
    {
        projectAccessService.applyQueryScope(query);
        return projectMapper.selectReviewProjectList(query);
    }

    @DataScope(deptAlias = "d", userAlias = "owner", permission = InsightConstants.PERM_OVERVIEW_VIEW)
    public int countOpenFocusOverview(ReviewProject query)
    {
        projectAccessService.applyQueryScope(query);
        return sourceMapper.countOpenFocusIssues(query);
    }

    @DataScope(deptAlias = "d", userAlias = "owner", permission = InsightConstants.PERM_PROJECT_VIEW)
    public int countOpenFocusProject(ReviewProject query)
    {
        projectAccessService.applyQueryScope(query);
        return sourceMapper.countOpenFocusIssues(query);
    }

    @DataScope(deptAlias = "d", userAlias = "owner", permission = InsightConstants.PERM_OVERVIEW_VIEW)
    public List<Map<String, Object>> categoryOverview(ReviewProject query)
    {
        projectAccessService.applyQueryScope(query);
        return sourceMapper.selectCategoryDistribution(query);
    }

    @DataScope(deptAlias = "d", userAlias = "owner", permission = InsightConstants.PERM_PROJECT_VIEW)
    public List<Map<String, Object>> categoryProject(ReviewProject query)
    {
        projectAccessService.applyQueryScope(query);
        return sourceMapper.selectCategoryDistribution(query);
    }

    @DataScope(deptAlias = "d", userAlias = "owner", permission = InsightConstants.PERM_OVERVIEW_VIEW)
    public List<Map<String, Object>> deliveryOverview(ReviewProject query)
    {
        projectAccessService.applyQueryScope(query);
        return sourceMapper.selectDeliveryChannelHealth(query);
    }

    @DataScope(deptAlias = "d", userAlias = "owner", permission = InsightConstants.PERM_PROJECT_VIEW)
    public List<Map<String, Object>> deliveryProject(ReviewProject query)
    {
        projectAccessService.applyQueryScope(query);
        return sourceMapper.selectDeliveryChannelHealth(query);
    }

    @DataScope(deptAlias = "d", userAlias = "owner", permission = InsightConstants.PERM_TEAM_VIEW)
    public List<ReviewMemberStatsDaily> selectMemberStatsTeam(ReviewMemberStatsDaily query)
    {
        projectAccessService.applyQueryScope(query);
        return memberStatsMapper.selectScopedRange(query);
    }

    @DataScope(deptAlias = "d", userAlias = "owner", permission = InsightConstants.PERM_TEAM_VIEW)
    public List<ReviewProject> selectProjectsTeam(ReviewProject query)
    {
        projectAccessService.applyQueryScope(query);
        return projectMapper.selectReviewProjectList(query);
    }

    @DataScope(deptAlias = "d", userAlias = "u", permission = InsightConstants.PERM_IDENTITY_MANAGE)
    public List<SysUserIdentity> selectIdentitiesManage(SysUserIdentity query)
    {
        return userIdentityMapper.selectScopedList(query);
    }

    @DataScope(deptAlias = "d", userAlias = "owner", permission = InsightConstants.PERM_TOKEN_VIEW)
    public TokenUsageTotals selectTokenTotals(TokenUsageQuery query)
    {
        projectAccessService.applyQueryScope(query);
        return tokenUsageMapper.selectTotals(query);
    }

    @DataScope(deptAlias = "d", userAlias = "owner", permission = InsightConstants.PERM_TOKEN_VIEW)
    public java.util.Date selectTokenDataSince(TokenUsageQuery query)
    {
        projectAccessService.applyQueryScope(query);
        return tokenUsageMapper.selectEarliestTokenTime(query);
    }

    @DataScope(deptAlias = "d", userAlias = "owner", permission = InsightConstants.PERM_TOKEN_VIEW)
    public List<InsightTokenTrendPoint> selectTokenTrend(TokenUsageQuery query)
    {
        projectAccessService.applyQueryScope(query);
        return tokenUsageMapper.selectTrend(query);
    }

    @DataScope(deptAlias = "d", userAlias = "owner", permission = InsightConstants.PERM_TOKEN_VIEW)
    public List<InsightTokenModelRow> selectTokenModels(TokenUsageQuery query)
    {
        projectAccessService.applyQueryScope(query);
        return tokenUsageMapper.selectModels(query);
    }

    @DataScope(deptAlias = "d", userAlias = "owner", permission = InsightConstants.PERM_TOKEN_VIEW)
    public List<InsightTokenModelOption> selectTokenModelOptions(TokenUsageQuery query)
    {
        projectAccessService.applyQueryScope(query);
        return tokenUsageMapper.selectModelOptions(query);
    }

    @DataScope(deptAlias = "d", userAlias = "owner", permission = InsightConstants.PERM_TOKEN_VIEW)
    public List<InsightTokenProjectRow> selectTokenProjects(TokenUsageQuery query)
    {
        projectAccessService.applyQueryScope(query);
        return tokenUsageMapper.selectProjects(query);
    }

    @DataScope(deptAlias = "d", userAlias = "owner", permission = InsightConstants.PERM_TOKEN_VIEW)
    public List<TokenUsageProjectModelRow> selectTokenProjectModels(TokenUsageQuery query)
    {
        projectAccessService.applyQueryScope(query);
        return tokenUsageMapper.selectProjectModels(query);
    }

    @DataScope(deptAlias = "d", userAlias = "owner", permission = InsightConstants.PERM_TOKEN_VIEW)
    public List<InsightTokenRunRow> selectTokenRuns(TokenUsageQuery query)
    {
        projectAccessService.applyQueryScope(query);
        return tokenUsageMapper.selectRuns(query);
    }

    @DataScope(deptAlias = "d", userAlias = "owner", permission = InsightConstants.PERM_TOKEN_VIEW)
    public List<ReviewProject> selectProjectsToken(ReviewProject query)
    {
        projectAccessService.applyQueryScope(query);
        return projectMapper.selectReviewProjectList(query);
    }
}
