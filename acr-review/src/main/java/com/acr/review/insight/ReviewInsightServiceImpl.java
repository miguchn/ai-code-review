package com.acr.review.insight;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import com.acr.common.core.domain.entity.SysUser;
import com.acr.common.core.domain.model.LoginUser;
import com.acr.common.exception.ServiceException;
import com.acr.common.utils.SecurityUtils;
import com.acr.common.utils.StringUtils;
import com.acr.review.domain.ReviewProject;
import com.acr.review.insight.dto.InsightChannelHealth;
import com.acr.review.insight.dto.InsightCommitTrendPoint;
import com.acr.review.insight.dto.InsightDispositionFunnel;
import com.acr.review.insight.dto.InsightIdentityBindRequest;
import com.acr.review.insight.dto.InsightIdentityCandidate;
import com.acr.review.insight.dto.InsightIdentityCandidateVo;
import com.acr.review.insight.dto.InsightKpiCard;
import com.acr.review.insight.dto.InsightMemberMineResponse;
import com.acr.review.insight.dto.InsightMetricsDictResponse;
import com.acr.review.insight.dto.InsightNamedCount;
import com.acr.review.insight.dto.InsightOverviewResponse;
import com.acr.review.insight.dto.InsightProjectDetailResponse;
import com.acr.review.insight.dto.InsightProjectRow;
import com.acr.review.insight.dto.InsightTeamIdentitiesResponse;
import com.acr.review.insight.dto.InsightTeamMemberRow;
import com.acr.review.insight.dto.InsightTeamMembersResponse;
import com.acr.review.insight.dto.InsightTeamProjectOption;
import com.acr.review.insight.dto.InsightTrendPoint;
import com.acr.review.insight.dto.InsightUnboundIdentity;
import com.acr.review.insight.dto.InsightUserOption;
import com.acr.review.mapper.ReviewCommitFactMapper;
import com.acr.review.mapper.ReviewMemberStatsDailyMapper;
import com.acr.review.mapper.ReviewStatsDailyMapper;
import com.acr.review.mapper.ReviewStatsSourceMapper;
import com.acr.system.domain.SysUserIdentity;
import com.acr.system.service.ISysConfigService;
import com.acr.system.service.ISysUserIdentityService;
import com.acr.system.service.ISysUserService;

/** 看板查询：聚合表为主，类别/渠道/未处置重点为受限只读聚合。 */
@Service
public class ReviewInsightServiceImpl implements IReviewInsightService
{
    private static final DateTimeFormatter DAY = DateTimeFormatter.ISO_LOCAL_DATE;

    private final InsightScopeQueries scopeQueries;
    private final ReviewStatsDailyMapper dailyMapper;
    private final ReviewStatsSourceMapper sourceMapper;
    private final ReviewCommitFactMapper commitFactMapper;
    private final ReviewMemberStatsDailyMapper memberStatsMapper;
    private final ISysUserIdentityService userIdentityService;
    private final ISysUserService userService;
    private final ISysConfigService configService;

    public ReviewInsightServiceImpl(InsightScopeQueries scopeQueries,
                                    ReviewStatsDailyMapper dailyMapper,
                                    ReviewStatsSourceMapper sourceMapper,
                                    ReviewCommitFactMapper commitFactMapper,
                                    ReviewMemberStatsDailyMapper memberStatsMapper,
                                    ISysUserIdentityService userIdentityService,
                                    ISysUserService userService,
                                    ISysConfigService configService)
    {
        this.scopeQueries = scopeQueries;
        this.dailyMapper = dailyMapper;
        this.sourceMapper = sourceMapper;
        this.commitFactMapper = commitFactMapper;
        this.memberStatsMapper = memberStatsMapper;
        this.userIdentityService = userIdentityService;
        this.userService = userService;
        this.configService = configService;
    }

    @Override
    public InsightOverviewResponse getOverview(String beginDate, String endDate, Integer days, Long businessSystemId)
    {
        InsightRange range = InsightRange.of(beginDate, endDate, days);
        List<ReviewStatsDaily> current = scopeQueries.selectStatsOverview(
            statsQuery(range.getBegin(), range.getEnd(), businessSystemId, null));
        List<ReviewStatsDaily> previous = scopeQueries.selectStatsOverview(
            statsQuery(range.getPrevBegin(), range.getPrevEnd(), businessSystemId, null));

        Totals cur = sum(current);
        Totals prev = sum(previous);

        InsightOverviewResponse resp = new InsightOverviewResponse();
        resp.setBeginDate(range.beginText());
        resp.setEndDate(range.endText());
        resp.setMetricsVersion(metricsVersion());
        resp.setDataSince(formatDate(dailyMapper.selectEarliestStatDate(null)));
        resp.setEmpty(cur.taskTotal == 0 && cur.issueNew == 0 && cur.eventAccepted == 0);
        if (resp.isEmpty())
        {
            resp.setEmptyReason("选定范围内暂无审查聚合数据；请确认项目已接入并完成夜间/近期聚合刷新");
        }

        int openFocus = scopeQueries.countOpenFocusOverview(focusQuery(businessSystemId, null));
        resp.getKpis().add(kpi("coverageRate", "有效审查覆盖率",
            InsightMetrics.ratio(cur.taskCovered, cur.eventAccepted),
            InsightMetrics.ratio(prev.taskCovered, prev.eventAccepted), "ratio"));
        resp.getKpis().add(kpi("successRate", "审查成功率",
            InsightMetrics.ratio(cur.taskSuccess, cur.taskTotal),
            InsightMetrics.ratio(prev.taskSuccess, prev.taskTotal), "ratio"));
        resp.getKpis().add(kpi("durationP95Ms", "P95 审查时延",
            (double) cur.durationP95Max, (double) prev.durationP95Max, "ms"));
        resp.getKpis().add(kpi("openFocusIssues", "未处置重点问题数",
            (double) openFocus, (double) openFocus, "count"));

        resp.setTaskTrend(buildTaskTrend(range.getBegin(), range.getEnd(), current));
        resp.setIssueTrend(buildIssueTrend(range.getBegin(), range.getEnd(), current));
        resp.setCategoryDistribution(toNamedCounts(
            scopeQueries.categoryOverview(scopeQuery(range, businessSystemId, null))));
        resp.setDeliveryHealth(toChannelHealth(
            scopeQueries.deliveryOverview(scopeQuery(range, businessSystemId, null))));
        return resp;
    }

    @Override
    public List<InsightProjectRow> listProjects(String beginDate, String endDate, Integer days,
                                                Long businessSystemId, Long projectId, String orderBy)
    {
        InsightRange range = InsightRange.of(beginDate, endDate, days);
        ReviewProject projectQuery = new ReviewProject();
        if (businessSystemId != null)
        {
            projectQuery.setBusinessSystemId(businessSystemId);
        }
        if (projectId != null)
        {
            projectQuery.setProjectId(projectId);
        }
        List<ReviewProject> projects = scopeQueries.selectProjects(projectQuery);
        List<ReviewStatsDaily> stats = scopeQueries.selectStatsProject(
            statsQuery(range.getBegin(), range.getEnd(), businessSystemId, projectId));
        Map<Long, Totals> byProject = new HashMap<>();
        for (ReviewStatsDaily row : stats)
        {
            byProject.computeIfAbsent(row.getProjectId(), id -> new Totals()).add(row);
        }

        List<InsightProjectRow> rows = new ArrayList<>();
        for (ReviewProject project : projects)
        {
            Totals t = byProject.getOrDefault(project.getProjectId(), new Totals());
            InsightProjectRow row = new InsightProjectRow();
            row.setProjectId(project.getProjectId());
            row.setProjectName(project.getProjectName());
            row.setBusinessSystemName(project.getBusinessSystemName());
            row.setOwnerName(project.getOwnerName());
            row.setTaskTotal(t.taskTotal);
            row.setSuccessRate(InsightMetrics.ratio(t.taskSuccess, t.taskTotal));
            row.setIssueNew(t.issueNew);
            row.setOpenFocusIssues(scopeQueries.countOpenFocusProject(focusQuery(null, project.getProjectId())));
            if (t.issueNew == 0)
            {
                row.setDispositionRate(0d);
            }
            else
            {
                row.setDispositionRate(InsightMetrics.ratio(t.issueConfirmed + t.issueClosed, t.issueNew));
            }
            row.setLastReviewTime(sourceMapper.selectProjectLastReviewTime(project.getProjectId()));
            rows.add(row);
        }
        sortProjectRows(rows, orderBy);
        return rows;
    }

    @Override
    public InsightProjectDetailResponse getProjectDetail(Long projectId, String beginDate, String endDate, Integer days)
    {
        if (projectId == null)
        {
            throw new ServiceException("项目ID不能为空");
        }
        ReviewProject filter = new ReviewProject();
        filter.setProjectId(projectId);
        List<ReviewProject> scoped = scopeQueries.selectProjects(filter);
        if (scoped.isEmpty())
        {
            throw new ServiceException("没有权限访问该项目或项目不存在");
        }
        ReviewProject project = scoped.get(0);
        InsightRange range = InsightRange.of(beginDate, endDate, days);
        List<ReviewStatsDaily> current = scopeQueries.selectStatsProject(
            statsQuery(range.getBegin(), range.getEnd(), null, projectId));
        List<ReviewStatsDaily> previous = scopeQueries.selectStatsProject(
            statsQuery(range.getPrevBegin(), range.getPrevEnd(), null, projectId));
        Totals cur = sum(current);
        Totals prev = sum(previous);

        InsightProjectDetailResponse resp = new InsightProjectDetailResponse();
        resp.setProjectId(project.getProjectId());
        resp.setProjectName(project.getProjectName());
        resp.setBusinessSystemName(project.getBusinessSystemName());
        resp.setOwnerName(project.getOwnerName());
        resp.setBeginDate(range.beginText());
        resp.setEndDate(range.endText());
        resp.setMetricsVersion(metricsVersion());
        resp.setDataSince(formatDate(dailyMapper.selectEarliestStatDate(List.of(projectId))));
        resp.setEmpty(cur.taskTotal == 0 && cur.issueNew == 0);
        if (resp.isEmpty())
        {
            resp.setEmptyReason("该项目在选定范围内暂无审查数据；请确认 Webhook 已接入并已产生审查任务");
        }

        int openFocus = scopeQueries.countOpenFocusProject(focusQuery(null, projectId));
        resp.getKpis().add(kpi("coverageRate", "有效审查覆盖率",
            InsightMetrics.ratio(cur.taskCovered, cur.eventAccepted),
            InsightMetrics.ratio(prev.taskCovered, prev.eventAccepted), "ratio"));
        resp.getKpis().add(kpi("successRate", "审查成功率",
            InsightMetrics.ratio(cur.taskSuccess, cur.taskTotal),
            InsightMetrics.ratio(prev.taskSuccess, prev.taskTotal), "ratio"));
        double curDisp = cur.issueNew == 0 ? 0d
            : InsightMetrics.ratio(cur.issueConfirmed + cur.issueClosed, cur.issueNew);
        double prevDisp = prev.issueNew == 0 ? 0d
            : InsightMetrics.ratio(prev.issueConfirmed + prev.issueClosed, prev.issueNew);
        resp.getKpis().add(kpi("dispositionRate", "处置率", curDisp, prevDisp, "ratio"));
        resp.getKpis().add(kpi("openFocusIssues", "未处置重点问题数", (double) openFocus, (double) openFocus, "count"));

        resp.setTaskTrend(buildTaskTrend(range.getBegin(), range.getEnd(), current));
        resp.setIssueTrend(buildIssueTrend(range.getBegin(), range.getEnd(), current));
        resp.getSeverityDistribution().add(new InsightNamedCount("CRITICAL", cur.issueCritical));
        resp.getSeverityDistribution().add(new InsightNamedCount("HIGH", cur.issueHigh));
        resp.getSeverityDistribution().add(new InsightNamedCount("MEDIUM", cur.issueMedium));
        resp.getSeverityDistribution().add(new InsightNamedCount("LOW", cur.issueLow));
        resp.setCategoryDistribution(toNamedCounts(
            scopeQueries.categoryProject(scopeQuery(range, null, projectId))));

        InsightDispositionFunnel funnel = new InsightDispositionFunnel();
        funnel.setIssueNew(cur.issueNew);
        funnel.setConfirmed(cur.issueConfirmed);
        funnel.setClosed(cur.issueClosed);
        resp.setDispositionFunnel(funnel);
        resp.setCommitTrend(buildProjectCommitTrend(projectId, range));
        return resp;
    }

    @Override
    public InsightMetricsDictResponse getMetricsDictionary()
    {
        InsightMetricsDictResponse resp = new InsightMetricsDictResponse();
        resp.setVersion(metricsVersion());
        resp.setMetrics(InsightMetrics.dictionary());
        return resp;
    }

    @Override
    public InsightMemberMineResponse getMemberMine(String beginDate, String endDate, Integer days)
    {
        InsightRange range = InsightRange.of(beginDate, endDate, days);
        LoginUser loginUser = SecurityUtils.getLoginUser();
        Long userId = loginUser.getUserId();
        List<SysUserIdentity> identities = userIdentityService.listMineGit(userId);
        InsightMemberMineResponse resp = new InsightMemberMineResponse();
        resp.setMetricsVersion(metricsVersion());
        if (identities == null || identities.isEmpty())
        {
            resp.setClaimed(false);
            return resp;
        }
        resp.setClaimed(true);
        List<String> authorKeys = new ArrayList<>();
        for (SysUserIdentity identity : identities)
        {
            String key = identity.getIdentifier();
            authorKeys.add(key);
            resp.getClaimedIdentities().add(new InsightIdentityCandidate(
                key, identity.getDisplayName(), key));
        }
        resp.setDataSince(formatDate(memberStatsMapper.selectEarliestStatDate(authorKeys, null)));
        List<Map<String, Object>> trend = commitFactMapper.selectCommitTrendByAuthorKeys(
            authorKeys, Date.valueOf(range.getBegin()), Date.valueOf(range.getEnd()), null);
        resp.setCommitTrend(fillCommitTrendGaps(mergeTrendByDay(toCommitTrend(trend)), range, null));
        List<Map<String, Object>> agg = memberStatsMapper.selectMemberAgg(
            Date.valueOf(range.getBegin()), Date.valueOf(range.getEnd()), authorKeys, null);
        int tasks = 0;
        int additions = 0;
        int deletions = 0;
        int issuesNew = 0;
        int issuesOpen = 0;
        for (Map<String, Object> row : agg)
        {
            tasks += intOf(row.get("tasks_reviewed"));
            additions += intOf(row.get("additions_sum"));
            deletions += intOf(row.get("deletions_sum"));
            issuesNew += intOf(row.get("issues_new"));
            issuesOpen = Math.max(issuesOpen, intOf(row.get("issues_open")));
        }
        resp.setTasksReviewed(tasks);
        resp.setAdditionsSum(additions);
        resp.setDeletionsSum(deletions);
        resp.setIssuesNew(issuesNew);
        resp.setIssuesOpen(issuesOpen);
        for (Map<String, Object> issue : sourceMapper.selectOpenIssuesByAuthorKeys(authorKeys, 20))
        {
            resp.getOpenIssueTitles().add(new InsightNamedCount(
                String.valueOf(issue.get("title")), intOf(issue.get("cnt"))));
        }
        return resp;
    }

    @Override
    public InsightTeamMembersResponse getTeamMembers(String beginDate, String endDate, Integer days,
                                                     Long businessSystemId, Long projectId)
    {
        InsightRange range = InsightRange.of(beginDate, endDate, days);
        ReviewProject projectFilter = new ReviewProject();
        if (businessSystemId != null)
        {
            projectFilter.setBusinessSystemId(businessSystemId);
        }
        if (projectId != null)
        {
            projectFilter.setProjectId(projectId);
        }
        List<ReviewProject> projects = scopeQueries.selectProjectsTeam(projectFilter);
        List<ReviewProject> projectOptions = projects;
        if (businessSystemId != null || projectId != null)
        {
            // 筛选结果与下拉选项分离；选项仍只来自当前用户可见的项目范围。
            projectOptions = scopeQueries.selectProjectsTeam(new ReviewProject());
        }
        List<Long> projectIds = projects.stream().map(ReviewProject::getProjectId).collect(Collectors.toList());

        ReviewMemberStatsDaily query = new ReviewMemberStatsDaily();
        query.getParams().put("beginDate", Date.valueOf(range.getBegin()));
        query.getParams().put("endDate", Date.valueOf(range.getEnd()));
        if (businessSystemId != null)
        {
            query.getParams().put("businessSystemId", businessSystemId);
        }
        if (projectId != null)
        {
            query.getParams().put("projectId", projectId);
        }
        List<ReviewMemberStatsDaily> dailyRows = scopeQueries.selectMemberStatsTeam(query);

        List<SysUserIdentity> bindings = userIdentityService.listByType(SysUserIdentity.TYPE_GIT_COMMIT);
        Map<String, SysUserIdentity> identityByKey = new HashMap<>();
        for (SysUserIdentity binding : bindings)
        {
            identityByKey.put(binding.getIdentifier(), binding);
        }

        Map<Long, InsightTeamMemberRow> boundMembers = new LinkedHashMap<>();
        Map<String, InsightTeamMemberRow> unboundMembers = new LinkedHashMap<>();

        for (ReviewMemberStatsDaily row : dailyRows)
        {
            String authorKey = row.getAuthorKey();
            if (StringUtils.isEmpty(authorKey))
            {
                continue;
            }
            SysUserIdentity binding = identityByKey.get(authorKey);
            if (binding != null)
            {
                InsightTeamMemberRow member = boundMembers.computeIfAbsent(binding.getUserId(), uid -> {
                    InsightTeamMemberRow m = emptyMemberRow();
                    m.setUserId(uid);
                    m.setAuthorName(StringUtils.isNotEmpty(binding.getNickName())
                        ? binding.getNickName() : binding.getUserName());
                    m.setAuthorKey("user:" + uid);
                    return m;
                });
                if (!member.getIdentities().contains(authorKey))
                {
                    member.getIdentities().add(authorKey);
                }
                accumulate(member, row);
            }
            else
            {
                InsightTeamMemberRow member = unboundMembers.computeIfAbsent(authorKey, key -> {
                    InsightTeamMemberRow m = emptyMemberRow();
                    m.setAuthorKey(key);
                    m.setAuthorName(row.getAuthorName());
                    m.getIdentities().add(key);
                    return m;
                });
                accumulate(member, row);
                if (StringUtils.isEmpty(member.getAuthorName()) && StringUtils.isNotEmpty(row.getAuthorName()))
                {
                    member.setAuthorName(row.getAuthorName());
                }
            }
        }

        List<String> allKeys = new ArrayList<>();
        for (InsightTeamMemberRow m : boundMembers.values())
        {
            allKeys.addAll(m.getIdentities());
        }
        allKeys.addAll(unboundMembers.keySet());
        List<Map<String, Object>> trendRows = allKeys.isEmpty() ? List.of()
            : commitFactMapper.selectCommitTrendByAuthorKeys(
                allKeys, Date.valueOf(range.getBegin()), Date.valueOf(range.getEnd()),
                projectIds.isEmpty() ? null : projectIds);
        List<InsightCommitTrendPoint> stacked = toCommitTrend(trendRows);
        Map<String, List<InsightCommitTrendPoint>> byAuthor = stacked.stream()
            .collect(Collectors.groupingBy(p -> p.getAuthorKey() == null ? "" : p.getAuthorKey()));

        List<InsightCommitTrendPoint> stackedTrend = new ArrayList<>();
        for (InsightTeamMemberRow member : boundMembers.values())
        {
            List<InsightCommitTrendPoint> merged = new ArrayList<>();
            for (String key : member.getIdentities())
            {
                merged.addAll(byAuthor.getOrDefault(key, List.of()));
            }
            List<InsightCommitTrendPoint> dayMerged = mergeTrendByDay(merged);
            List<InsightCommitTrendPoint> filled = fillCommitTrendGaps(dayMerged, range, member.getAuthorKey());
            member.setCommitTrend(filled);
            stackedTrend.addAll(filled);
        }
        for (InsightTeamMemberRow member : unboundMembers.values())
        {
            List<InsightCommitTrendPoint> points = byAuthor.getOrDefault(member.getAuthorKey(), List.of());
            // 未关联成员保留原始 authorKey，区间补零后并入全量 stackedTrend
            List<InsightCommitTrendPoint> filled = fillCommitTrendGaps(points, range, member.getAuthorKey());
            member.setCommitTrend(filled);
            stackedTrend.addAll(filled);
        }

        InsightTeamMembersResponse resp = new InsightTeamMembersResponse();
        resp.setBeginDate(range.beginText());
        resp.setEndDate(range.endText());
        resp.setMetricsVersion(metricsVersion());
        resp.setDataSince(formatDate(memberStatsMapper.selectEarliestStatDate(null, projectIds)));
        resp.setProjectOptions(projectOptions.stream()
            .map(project -> new InsightTeamProjectOption(project.getProjectId(), project.getProjectName(),
                project.getBusinessSystemId(), project.getBusinessSystemName()))
            .sorted(Comparator.comparing(InsightTeamProjectOption::getProjectName,
                Comparator.nullsFirst(String::compareToIgnoreCase)))
            .collect(Collectors.toList()));
        resp.setMembers(boundMembers.values().stream()
            .sorted(Comparator.comparing(InsightTeamMemberRow::getCommitCount,
                Comparator.nullsFirst(Integer::compareTo)).reversed())
            .collect(Collectors.toList()));
        resp.setUnbound(unboundMembers.values().stream()
            .sorted(Comparator.comparing(InsightTeamMemberRow::getCommitCount,
                Comparator.nullsFirst(Integer::compareTo)).reversed())
            .collect(Collectors.toList()));
        resp.setStackedTrend(stackedTrend);
        return resp;
    }

    @Override
    public List<InsightUserOption> listIdentityUserOptions(String keyword)
    {
        SysUser query = new SysUser();
        if (StringUtils.isNotEmpty(keyword))
        {
            query.setUserName(keyword.trim());
        }
        List<SysUser> users = userService.selectUserList(query);
        if (users == null || users.isEmpty())
        {
            return List.of();
        }
        int limit = Math.min(20, users.size());
        List<InsightUserOption> options = new ArrayList<>(limit);
        for (int i = 0; i < limit; i++)
        {
            SysUser user = users.get(i);
            options.add(new InsightUserOption(user.getUserId(), user.getUserName(), user.getNickName()));
        }
        return options;
    }

    @Override
    public List<InsightIdentityCandidateVo> listIdentityCandidates()
    {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        SysUser user = loginUser.getUser();
        List<SysUserIdentity> mine = userIdentityService.listMineGit(loginUser.getUserId());
        Set<String> exclude = new HashSet<>();
        for (SysUserIdentity identity : mine)
        {
            exclude.add(identity.getIdentifier());
        }
        List<Map<String, Object>> rows = commitFactMapper.selectMatchCandidateIdentities(
            user.getEmail(), user.getUserName(), user.getNickName(), 30);
        List<IdentityCandidateMatcher.CommitIdentity> commits = new ArrayList<>();
        for (Map<String, Object> row : rows)
        {
            commits.add(new IdentityCandidateMatcher.CommitIdentity(
                str(row.get("author_email")), str(row.get("author_name")), str(row.get("author_key"))));
        }
        List<IdentityCandidateMatcher.Match> matches = IdentityCandidateMatcher.match(
            user.getEmail(), user.getUserName(), user.getNickName(), commits, exclude);
        List<InsightIdentityCandidateVo> result = new ArrayList<>();
        for (IdentityCandidateMatcher.Match match : matches)
        {
            InsightIdentityCandidateVo vo = new InsightIdentityCandidateVo();
            vo.setIdentifier(match.authorKey);
            vo.setDisplayName(match.authorName);
            vo.setMatchType(match.matchType);
            Map<String, Object> sample = commitFactMapper.selectLatestCommitSample(match.authorKey);
            if (sample != null)
            {
                vo.setSampleProjectName(str(sample.get("project_name")));
                vo.setSampleMessage(str(sample.get("message_first_line")));
                vo.setSampleTime(formatDateTime(sample.get("commit_time")));
                if (StringUtils.isEmpty(vo.getDisplayName()))
                {
                    vo.setDisplayName(str(sample.get("author_name")));
                }
            }
            result.add(vo);
        }
        return result;
    }

    @Override
    public InsightTeamIdentitiesResponse listTeamIdentities()
    {
        SysUserIdentity query = new SysUserIdentity();
        query.setIdentityType(SysUserIdentity.TYPE_GIT_COMMIT);
        List<SysUserIdentity> bindings = scopeQueries.selectIdentitiesManage(query);
        Set<String> boundKeys = new HashSet<>();
        for (SysUserIdentity binding : bindings)
        {
            boundKeys.add(binding.getIdentifier());
        }
        InsightTeamIdentitiesResponse resp = new InsightTeamIdentitiesResponse();
        resp.setBindings(bindings);
        List<String> allKeys = commitFactMapper.selectDistinctAuthorKeys();
        for (String key : allKeys)
        {
            if (boundKeys.contains(key))
            {
                continue;
            }
            InsightUnboundIdentity unbound = new InsightUnboundIdentity();
            unbound.setIdentifier(key);
            Map<String, Object> sample = commitFactMapper.selectLatestCommitSample(key);
            if (sample != null)
            {
                unbound.setDisplayName(str(sample.get("author_name")));
                unbound.setSampleProjectName(str(sample.get("project_name")));
                unbound.setSampleMessage(str(sample.get("message_first_line")));
                unbound.setSampleTime(formatDateTime(sample.get("commit_time")));
            }
            resp.getUnbound().add(unbound);
        }
        return resp;
    }

    @Override
    public boolean bindTeamIdentity(InsightIdentityBindRequest request)
    {
        if (request == null)
        {
            return false;
        }
        LoginUser loginUser = SecurityUtils.getLoginUser();
        return userIdentityService.bindAdmin(request.getUserId(), request.getIdentifier(),
            request.getDisplayName(), loginUser.getUsername());
    }

    @Override
    public void unbindTeamIdentity(Long id)
    {
        userIdentityService.unbindAdmin(id);
    }

    private static InsightTeamMemberRow emptyMemberRow()
    {
        InsightTeamMemberRow m = new InsightTeamMemberRow();
        m.setCommitCount(0);
        m.setTasksReviewed(0);
        m.setAdditionsSum(0);
        m.setDeletionsSum(0);
        m.setIssuesNew(0);
        m.setIssuesOpen(0);
        return m;
    }

    private static void accumulate(InsightTeamMemberRow member, ReviewMemberStatsDaily row)
    {
        member.setCommitCount(n(member.getCommitCount()) + n(row.getCommitCount()));
        member.setTasksReviewed(n(member.getTasksReviewed()) + n(row.getTasksReviewed()));
        member.setAdditionsSum(n(member.getAdditionsSum()) + n(row.getAdditionsSum()));
        member.setDeletionsSum(n(member.getDeletionsSum()) + n(row.getDeletionsSum()));
        member.setIssuesNew(n(member.getIssuesNew()) + n(row.getIssuesNew()));
        member.setIssuesOpen(Math.max(n(member.getIssuesOpen()), n(row.getIssuesOpen())));
    }

    private static List<InsightCommitTrendPoint> mergeTrendByDay(List<InsightCommitTrendPoint> points)
    {
        Map<String, Integer> byDay = new LinkedHashMap<>();
        for (InsightCommitTrendPoint p : points)
        {
            if (p == null || p.getDate() == null)
            {
                continue;
            }
            byDay.merge(p.getDate(), n(p.getCommitCount()), Integer::sum);
        }
        List<InsightCommitTrendPoint> merged = new ArrayList<>();
        for (Map.Entry<String, Integer> e : byDay.entrySet())
        {
            InsightCommitTrendPoint point = new InsightCommitTrendPoint();
            point.setDate(e.getKey());
            point.setCommitCount(e.getValue());
            merged.add(point);
        }
        merged.sort(Comparator.comparing(InsightCommitTrendPoint::getDate));
        return merged;
    }

    /**
     * 按 InsightRange 的 begin..end 生成连续日期序列，缺失日期 commitCount=0（对齐 WorkbenchTrend）。
     * authorKey 写入整段序列，保证成员趋势与 stackedTrend 口径一致。
     */
    static List<InsightCommitTrendPoint> fillCommitTrendGaps(List<InsightCommitTrendPoint> points,
                                                             InsightRange range, String authorKey)
    {
        Map<String, Integer> byDay = new HashMap<>();
        if (points != null)
        {
            for (InsightCommitTrendPoint p : points)
            {
                if (p == null || p.getDate() == null)
                {
                    continue;
                }
                byDay.merge(p.getDate(), n(p.getCommitCount()), Integer::sum);
            }
        }
        List<InsightCommitTrendPoint> filled = new ArrayList<>();
        for (LocalDate day = range.getBegin(); !day.isAfter(range.getEnd()); day = day.plusDays(1))
        {
            InsightCommitTrendPoint point = new InsightCommitTrendPoint();
            point.setDate(day.format(DAY));
            point.setCommitCount(byDay.getOrDefault(point.getDate(), 0));
            if (authorKey != null)
            {
                point.setAuthorKey(authorKey);
            }
            filled.add(point);
        }
        return filled;
    }

    private static String str(Object value)
    {
        return value == null ? null : String.valueOf(value);
    }

    private static String formatDateTime(Object value)
    {
        if (value == null)
        {
            return null;
        }
        if (value instanceof java.util.Date date)
        {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm").format(date);
        }
        return String.valueOf(value);
    }

    private List<InsightCommitTrendPoint> buildProjectCommitTrend(Long projectId, InsightRange range)
    {
        List<Map<String, Object>> rows = commitFactMapper.selectProjectCommitTrend(
            projectId, Date.valueOf(range.getBegin()), Date.valueOf(range.getEnd()));
        Map<String, Integer> byDay = new HashMap<>();
        for (Map<String, Object> row : rows)
        {
            byDay.put(formatDate((java.util.Date) row.get("stat_date")), intOf(row.get("commit_count")));
        }
        List<InsightCommitTrendPoint> points = new ArrayList<>();
        for (LocalDate day = range.getBegin(); !day.isAfter(range.getEnd()); day = day.plusDays(1))
        {
            InsightCommitTrendPoint point = new InsightCommitTrendPoint();
            point.setDate(day.format(DAY));
            point.setCommitCount(byDay.getOrDefault(point.getDate(), 0));
            points.add(point);
        }
        return points;
    }

    private static List<InsightCommitTrendPoint> toCommitTrend(List<Map<String, Object>> rows)
    {
        List<InsightCommitTrendPoint> points = new ArrayList<>();
        if (rows == null)
        {
            return points;
        }
        for (Map<String, Object> row : rows)
        {
            InsightCommitTrendPoint point = new InsightCommitTrendPoint();
            Object date = row.get("stat_date");
            point.setDate(date instanceof java.util.Date d ? formatDate(d) : String.valueOf(date));
            point.setCommitCount(intOf(row.get("commit_count")));
            Object authorKey = row.get("author_key");
            if (authorKey != null)
            {
                point.setAuthorKey(String.valueOf(authorKey));
            }
            points.add(point);
        }
        return points;
    }

    private static int intOf(Object value)
    {
        if (value == null)
        {
            return 0;
        }
        if (value instanceof Number number)
        {
            return number.intValue();
        }
        return Integer.parseInt(String.valueOf(value));
    }

    private ReviewStatsDaily statsQuery(LocalDate begin, LocalDate end, Long businessSystemId, Long projectId)
    {
        ReviewStatsDaily query = new ReviewStatsDaily();
        query.setProjectId(projectId);
        query.getParams().put("beginDate", Date.valueOf(begin));
        query.getParams().put("endDate", Date.valueOf(end));
        if (businessSystemId != null)
        {
            query.getParams().put("businessSystemId", businessSystemId);
        }
        return query;
    }

    private ReviewProject focusQuery(Long businessSystemId, Long projectId)
    {
        ReviewProject query = new ReviewProject();
        query.setProjectId(projectId);
        if (businessSystemId != null)
        {
            query.getParams().put("businessSystemId", businessSystemId);
        }
        return query;
    }

    private ReviewProject scopeQuery(InsightRange range, Long businessSystemId, Long projectId)
    {
        ReviewProject query = new ReviewProject();
        query.setProjectId(projectId);
        query.getParams().put("beginDate", Date.valueOf(range.getBegin()));
        query.getParams().put("endDate", Date.valueOf(range.getEnd()));
        if (businessSystemId != null)
        {
            query.getParams().put("businessSystemId", businessSystemId);
        }
        return query;
    }

    private String metricsVersion()
    {
        String version = configService.selectConfigByKey(InsightConstants.CONFIG_METRICS_VERSION);
        return StringUtils.isEmpty(version) ? InsightConstants.DEFAULT_METRICS_VERSION : version;
    }

    private static List<InsightNamedCount> toNamedCounts(List<Map<String, Object>> rows)
    {
        List<InsightNamedCount> list = new ArrayList<>();
        if (rows == null)
        {
            return list;
        }
        for (Map<String, Object> row : rows)
        {
            Object cnt = row.get("cnt");
            list.add(new InsightNamedCount(String.valueOf(row.get("category")),
                cnt == null ? 0 : ((Number) cnt).intValue()));
        }
        return list;
    }

    private static List<InsightChannelHealth> toChannelHealth(List<Map<String, Object>> rows)
    {
        List<InsightChannelHealth> list = new ArrayList<>();
        if (rows == null)
        {
            return list;
        }
        for (Map<String, Object> row : rows)
        {
            int total = ((Number) row.get("delivery_total")).intValue();
            int success = ((Number) row.get("delivery_success")).intValue();
            InsightChannelHealth item = new InsightChannelHealth();
            item.setChannel(String.valueOf(row.get("channel")));
            item.setTotal(total);
            item.setSuccess(success);
            item.setSuccessRate(InsightMetrics.ratio(success, total));
            list.add(item);
        }
        return list;
    }

    private static InsightKpiCard kpi(String code, String name, double value, double previous, String unit)
    {
        InsightKpiCard card = new InsightKpiCard();
        card.setCode(code);
        card.setName(name);
        card.setValue(value);
        card.setPreviousValue(previous);
        card.setChangeRatio(InsightMetrics.periodChangeRatio(value, previous));
        card.setUnit(unit);
        return card;
    }

    private static Totals sum(List<ReviewStatsDaily> rows)
    {
        Totals t = new Totals();
        for (ReviewStatsDaily row : rows)
        {
            t.add(row);
        }
        return t;
    }

    private static List<InsightTrendPoint> buildTaskTrend(LocalDate begin, LocalDate end, List<ReviewStatsDaily> rows)
    {
        Map<String, InsightTrendPoint> map = new HashMap<>();
        for (LocalDate day = begin; !day.isAfter(end); day = day.plusDays(1))
        {
            InsightTrendPoint point = new InsightTrendPoint();
            point.setDate(day.format(DAY));
            point.setSuccess(0);
            point.setFailed(0);
            map.put(point.getDate(), point);
        }
        for (ReviewStatsDaily row : rows)
        {
            String key = formatDate(row.getStatDate());
            InsightTrendPoint point = map.get(key);
            if (point == null)
            {
                continue;
            }
            point.setSuccess(point.getSuccess() + n(row.getTaskSuccess()));
            point.setFailed(point.getFailed() + n(row.getTaskFailed()));
        }
        return map.values().stream().sorted(Comparator.comparing(InsightTrendPoint::getDate)).collect(Collectors.toList());
    }

    private static List<InsightTrendPoint> buildIssueTrend(LocalDate begin, LocalDate end, List<ReviewStatsDaily> rows)
    {
        Map<String, InsightTrendPoint> map = new HashMap<>();
        for (LocalDate day = begin; !day.isAfter(end); day = day.plusDays(1))
        {
            InsightTrendPoint point = new InsightTrendPoint();
            point.setDate(day.format(DAY));
            point.setCritical(0);
            point.setHigh(0);
            point.setMedium(0);
            point.setLow(0);
            point.setIssueNew(0);
            map.put(point.getDate(), point);
        }
        for (ReviewStatsDaily row : rows)
        {
            String key = formatDate(row.getStatDate());
            InsightTrendPoint point = map.get(key);
            if (point == null)
            {
                continue;
            }
            point.setCritical(point.getCritical() + n(row.getIssueCritical()));
            point.setHigh(point.getHigh() + n(row.getIssueHigh()));
            point.setMedium(point.getMedium() + n(row.getIssueMedium()));
            point.setLow(point.getLow() + n(row.getIssueLow()));
            point.setIssueNew(point.getIssueNew() + n(row.getIssueNew()));
        }
        return map.values().stream().sorted(Comparator.comparing(InsightTrendPoint::getDate)).collect(Collectors.toList());
    }

    private static void sortProjectRows(List<InsightProjectRow> rows, String orderBy)
    {
        if (StringUtils.isEmpty(orderBy))
        {
            rows.sort(Comparator.comparing(InsightProjectRow::getTaskTotal,
                Comparator.nullsFirst(Integer::compareTo)).reversed());
            return;
        }
        boolean desc = orderBy.startsWith("-");
        String field = desc ? orderBy.substring(1) : orderBy;
        Comparator<InsightProjectRow> cmp = switch (field)
        {
            case "successRate" -> Comparator.comparing(InsightProjectRow::getSuccessRate,
                Comparator.nullsFirst(Double::compareTo));
            case "issueNew" -> Comparator.comparing(InsightProjectRow::getIssueNew,
                Comparator.nullsFirst(Integer::compareTo));
            case "openFocusIssues" -> Comparator.comparing(InsightProjectRow::getOpenFocusIssues,
                Comparator.nullsFirst(Integer::compareTo));
            case "dispositionRate" -> Comparator.comparing(InsightProjectRow::getDispositionRate,
                Comparator.nullsFirst(Double::compareTo));
            case "lastReviewTime" -> Comparator.comparing(InsightProjectRow::getLastReviewTime,
                Comparator.nullsFirst(java.util.Date::compareTo));
            default -> Comparator.comparing(InsightProjectRow::getTaskTotal,
                Comparator.nullsFirst(Integer::compareTo));
        };
        if (desc)
        {
            cmp = cmp.reversed();
        }
        rows.sort(cmp);
    }

    private static String formatDate(java.util.Date date)
    {
        if (date == null)
        {
            return null;
        }
        if (date instanceof Date sqlDate)
        {
            return sqlDate.toLocalDate().format(DAY);
        }
        return date.toInstant().atZone(java.time.ZoneId.of(InsightConstants.ZONE_ID)).toLocalDate().format(DAY);
    }

    private static int n(Integer value)
    {
        return value == null ? 0 : value;
    }

    private static final class Totals
    {
        private int taskTotal;
        private int taskSuccess;
        private int taskFailed;
        private int taskCovered;
        private int issueNew;
        private int issueCritical;
        private int issueHigh;
        private int issueMedium;
        private int issueLow;
        private int issueClosed;
        private int issueConfirmed;
        private int eventAccepted;
        private long durationP95Max;

        private void add(ReviewStatsDaily row)
        {
            taskTotal += n(row.getTaskTotal());
            taskSuccess += n(row.getTaskSuccess());
            taskFailed += n(row.getTaskFailed());
            taskCovered += n(row.getTaskCovered());
            issueNew += n(row.getIssueNew());
            issueCritical += n(row.getIssueCritical());
            issueHigh += n(row.getIssueHigh());
            issueMedium += n(row.getIssueMedium());
            issueLow += n(row.getIssueLow());
            issueClosed += n(row.getIssueClosed());
            issueConfirmed += n(row.getIssueConfirmed());
            eventAccepted += n(row.getEventAccepted());
            if (row.getDurationP95Ms() != null)
            {
                durationP95Max = Math.max(durationP95Max, row.getDurationP95Ms());
            }
        }
    }
}
