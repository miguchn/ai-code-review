package com.acr.review.insight;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.acr.review.domain.ReviewProject;
import com.acr.review.insight.dto.InsightTeamMembersResponse;
import com.acr.review.insight.dto.InsightTeamProjectOption;
import com.acr.review.mapper.ReviewCommitFactMapper;
import com.acr.review.mapper.ReviewMemberStatsDailyMapper;
import com.acr.review.mapper.ReviewStatsDailyMapper;
import com.acr.review.mapper.ReviewStatsSourceMapper;
import com.acr.system.domain.SysUserIdentity;
import com.acr.system.service.ISysConfigService;
import com.acr.system.service.ISysUserIdentityService;
import com.acr.system.service.ISysUserService;

/**
 * getTeamMembers 的 projectId 筛选路径：命中过滤 vs 越权空集（选项与统计范围分离）。
 */
@ExtendWith(MockitoExtension.class)
class TeamMembersProjectFilterTest
{
    @Mock
    private InsightScopeQueries scopeQueries;
    @Mock
    private ReviewStatsDailyMapper dailyMapper;
    @Mock
    private ReviewStatsSourceMapper sourceMapper;
    @Mock
    private ReviewCommitFactMapper commitFactMapper;
    @Mock
    private ReviewMemberStatsDailyMapper memberStatsMapper;
    @Mock
    private ISysUserIdentityService userIdentityService;
    @Mock
    private ISysUserService userService;
    @Mock
    private ISysConfigService configService;

    private ReviewInsightServiceImpl service;

    @BeforeEach
    void setUp()
    {
        service = new ReviewInsightServiceImpl(scopeQueries, dailyMapper, sourceMapper, commitFactMapper,
            memberStatsMapper, userIdentityService, userService, configService);
        when(configService.selectConfigByKey(any())).thenReturn(null);
    }

    @Test
    void teamMembers_projectIdHit_filtersStatsAndKeepsFullOptions()
    {
        when(scopeQueries.selectProjectsTeam(any())).thenAnswer(inv -> {
            ReviewProject query = inv.getArgument(0);
            if (Long.valueOf(1L).equals(query.getProjectId()))
            {
                return List.of(project(1L, "Alpha"));
            }
            // 选项分离的第二次调用：无过滤条件 → 全量可见范围
            if (query.getProjectId() == null && query.getBusinessSystemId() == null)
            {
                return List.of(project(1L, "Alpha"), project(2L, "Beta"));
            }
            return List.of();
        });

        ReviewMemberStatsDaily row = new ReviewMemberStatsDaily();
        row.setProjectId(1L);
        row.setAuthorKey("a@x.com");
        row.setAuthorName("老王");
        row.setCommitCount(3);
        row.setTasksReviewed(2);
        row.setAdditionsSum(10);
        row.setDeletionsSum(4);
        row.setIssuesNew(1);
        row.setIssuesOpen(1);
        when(scopeQueries.selectMemberStatsTeam(any())).thenReturn(List.of(row));

        SysUserIdentity binding = new SysUserIdentity();
        binding.setUserId(10L);
        binding.setIdentifier("a@x.com");
        binding.setNickName("老王");
        when(userIdentityService.listByType(SysUserIdentity.TYPE_GIT_COMMIT)).thenReturn(List.of(binding));
        when(commitFactMapper.selectCommitTrendByAuthorKeys(any(), any(), any(), any())).thenReturn(List.of());
        when(memberStatsMapper.selectEarliestStatDate(any(), any())).thenReturn(null);

        InsightTeamMembersResponse resp = service.getTeamMembers(
            "2026-08-01", "2026-08-07", null, null, 1L);

        assertEquals(1, resp.getMembers().size());
        assertEquals(10L, resp.getMembers().get(0).getUserId());
        assertEquals(3, resp.getMembers().get(0).getCommitCount());
        assertEquals(2, resp.getMembers().get(0).getTasksReviewed());
        assertTrue(resp.getUnbound().isEmpty());

        assertEquals(2, resp.getProjectOptions().size());
        Set<Long> optionIds = resp.getProjectOptions().stream()
            .map(InsightTeamProjectOption::getProjectId)
            .collect(Collectors.toSet());
        assertEquals(Set.of(1L, 2L), optionIds);

        ArgumentCaptor<ReviewMemberStatsDaily> statsCaptor = ArgumentCaptor.forClass(ReviewMemberStatsDaily.class);
        verify(scopeQueries).selectMemberStatsTeam(statsCaptor.capture());
        assertEquals(1L, statsCaptor.getValue().getParams().get("projectId"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Long>> projectIdsCaptor = ArgumentCaptor.forClass(List.class);
        verify(memberStatsMapper).selectEarliestStatDate(isNull(), projectIdsCaptor.capture());
        assertEquals(List.of(1L), projectIdsCaptor.getValue());
    }

    @Test
    void teamMembers_projectIdOutOfScope_returnsEmpty()
    {
        when(scopeQueries.selectProjectsTeam(any())).thenAnswer(inv -> {
            ReviewProject query = inv.getArgument(0);
            if (Long.valueOf(999L).equals(query.getProjectId()))
            {
                return List.of();
            }
            if (query.getProjectId() == null && query.getBusinessSystemId() == null)
            {
                return List.of(project(1L, "Alpha"), project(2L, "Beta"));
            }
            return List.of();
        });
        when(scopeQueries.selectMemberStatsTeam(any())).thenReturn(List.of());
        when(userIdentityService.listByType(SysUserIdentity.TYPE_GIT_COMMIT)).thenReturn(List.of());
        when(memberStatsMapper.selectEarliestStatDate(any(), any())).thenReturn(null);

        InsightTeamMembersResponse resp = service.getTeamMembers(
            "2026-08-01", "2026-08-07", null, null, 999L);

        assertNotNull(resp.getMembers());
        assertTrue(resp.getMembers().isEmpty());
        assertTrue(resp.getUnbound().isEmpty());
        assertTrue(resp.getStackedTrend().isEmpty());

        ArgumentCaptor<ReviewMemberStatsDaily> statsCaptor = ArgumentCaptor.forClass(ReviewMemberStatsDaily.class);
        verify(scopeQueries).selectMemberStatsTeam(statsCaptor.capture());
        assertEquals(999L, statsCaptor.getValue().getParams().get("projectId"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Long>> projectIdsCaptor = ArgumentCaptor.forClass(List.class);
        verify(memberStatsMapper).selectEarliestStatDate(isNull(), projectIdsCaptor.capture());
        // 空 list → Mapper AND 1=0；不得传 null（null 会退化为无条件全量）
        assertNotNull(projectIdsCaptor.getValue());
        assertTrue(projectIdsCaptor.getValue().isEmpty());

        verify(commitFactMapper, never()).selectCommitTrendByAuthorKeys(any(), any(), any(), any());
    }

    private static ReviewProject project(Long id, String name)
    {
        ReviewProject p = new ReviewProject();
        p.setProjectId(id);
        p.setProjectName(name);
        return p;
    }
}
