package com.acr.review.insight;

import java.sql.Date;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import com.acr.common.utils.StringUtils;
import com.acr.review.mapper.ReviewCommitFactMapper;
import com.acr.review.mapper.ReviewMemberStatsDailyMapper;
import com.acr.review.mapper.ReviewStatsDailyMapper;
import com.acr.review.mapper.ReviewStatsSourceMapper;

/**
 * 日聚合覆盖式重算。只读事实表 → UPSERT review_stats_daily / review_member_stats_daily；同键重算结果不变。
 */
@Service("reviewStatsAggregationService")
public class ReviewStatsAggregationService
{
    private static final Logger log = LoggerFactory.getLogger(ReviewStatsAggregationService.class);
    private static final ZoneId ZONE = ZoneId.of(InsightConstants.ZONE_ID);

    private final ReviewStatsSourceMapper sourceMapper;
    private final ReviewStatsDailyMapper dailyMapper;
    private final ReviewCommitFactMapper commitFactMapper;
    private final ReviewMemberStatsDailyMapper memberStatsMapper;

    public ReviewStatsAggregationService(ReviewStatsSourceMapper sourceMapper,
                                         ReviewStatsDailyMapper dailyMapper,
                                         ReviewCommitFactMapper commitFactMapper,
                                         ReviewMemberStatsDailyMapper memberStatsMapper)
    {
        this.sourceMapper = sourceMapper;
        this.dailyMapper = dailyMapper;
        this.commitFactMapper = commitFactMapper;
        this.memberStatsMapper = memberStatsMapper;
    }

    /** 重算近 35 天（含今日）。 */
    public int fullRecalc()
    {
        LocalDate today = LocalDate.now(ZONE);
        return recalculateRange(today.minusDays(InsightConstants.FULL_RECALC_DAYS - 1L), today);
    }

    /** 重算昨日+今日。 */
    public int refreshRecent()
    {
        LocalDate today = LocalDate.now(ZONE);
        return recalculateRange(today.minusDays(1L), today);
    }

    /**
     * 按日覆盖式重算 [from, to]（含端点）。返回写入/更新的行数。
     */
    public int recalculateRange(LocalDate from, LocalDate to)
    {
        if (from == null || to == null || to.isBefore(from))
        {
            throw new IllegalArgumentException("无效的聚合日期范围");
        }
        Date begin = Date.valueOf(from);
        Date end = Date.valueOf(to);
        List<Long> projectIds = sourceMapper.selectActiveProjectIds();
        int written = 0;
        for (Long projectId : projectIds)
        {
            written += recalculateProject(projectId, begin, end, from, to);
            written += recalculateMemberProject(projectId, begin, end, from, to);
        }
        log.info("insight stats recalculated projects={} range={}..{} rows={}",
            projectIds.size(), from, to, written);
        return written;
    }

    private int recalculateProject(Long projectId, Date begin, Date end, LocalDate from, LocalDate to)
    {
        Map<LocalDate, ReviewStatsDaily> rows = new HashMap<>();
        for (LocalDate day = from; !day.isAfter(to); day = day.plusDays(1))
        {
            rows.put(day, emptyRow(projectId, day));
        }

        mergeTaskAgg(rows, sourceMapper.selectTaskAggByDay(projectId, begin, end));
        mergeCovered(rows, sourceMapper.selectTaskCoveredAggByDay(projectId, begin, end));
        mergeIssueNew(rows, sourceMapper.selectIssueNewAggByDay(projectId, begin, end));
        mergeIssueClosed(rows, sourceMapper.selectIssueClosedAggByDay(projectId, begin, end));
        mergeIssueConfirmed(rows, sourceMapper.selectIssueConfirmedAggByDay(projectId, begin, end));
        mergeDelivery(rows, sourceMapper.selectDeliveryAggByDay(projectId, begin, end));
        mergeEvent(rows, sourceMapper.selectEventAggByDay(projectId, begin, end));

        int written = 0;
        for (Map.Entry<LocalDate, ReviewStatsDaily> entry : rows.entrySet())
        {
            ReviewStatsDaily row = entry.getValue();
            List<Long> durations = sourceMapper.selectSuccessDurations(projectId, Date.valueOf(entry.getKey()));
            row.setDurationP95Ms(InsightMetrics.percentile95(durations));
            // 空日同样覆盖写入，确保窗口内空日被清零（幂等冻结）
            dailyMapper.upsert(row);
            written++;
        }
        return written;
    }

    private int recalculateMemberProject(Long projectId, Date begin, Date end, LocalDate from, LocalDate to)
    {
        Map<String, ReviewMemberStatsDaily> rows = new HashMap<>();
        mergeMemberCommits(rows, projectId, commitFactMapper.selectCommitCountByAuthorDay(projectId, begin, end));
        mergeMemberInt(rows, projectId, sourceMapper.selectTasksReviewedByAuthorDay(projectId, begin, end),
            "tasks_reviewed");
        mergeMemberLineStats(rows, projectId, sourceMapper.selectLineStatsByAuthorDay(projectId, begin, end));
        mergeMemberInt(rows, projectId, sourceMapper.selectIssuesNewByAuthorDay(projectId, begin, end),
            "issues_new");

        for (LocalDate day = from; !day.isAfter(to); day = day.plusDays(1))
        {
            LocalDate dayKey = day;
            Date asOf = Date.valueOf(dayKey);
            for (Map<String, Object> open : safe(sourceMapper.selectIssuesOpenByAuthorAsOf(projectId, asOf)))
            {
                String authorKey = str(open.get("author_key"));
                if (StringUtils.isEmpty(authorKey))
                {
                    continue;
                }
                ReviewMemberStatsDaily row = rows.computeIfAbsent(dayKey + "|" + authorKey,
                    k -> emptyMemberRow(projectId, authorKey, dayKey));
                row.setIssuesOpen(intVal(open.get("issues_open")));
            }
        }

        int written = 0;
        for (ReviewMemberStatsDaily row : rows.values())
        {
            if (StringUtils.isEmpty(row.getAuthorKey()))
            {
                continue;
            }
            memberStatsMapper.upsert(row);
            written++;
        }
        return written;
    }

    private void mergeMemberCommits(Map<String, ReviewMemberStatsDaily> rows, Long projectId,
                                    List<Map<String, Object>> agg)
    {
        for (Map<String, Object> item : safe(agg))
        {
            LocalDate day = toLocalDate(item.get("stat_date"));
            String authorKey = str(item.get("author_key"));
            if (day == null || StringUtils.isEmpty(authorKey))
            {
                continue;
            }
            ReviewMemberStatsDaily row = rows.computeIfAbsent(day + "|" + authorKey,
                k -> emptyMemberRow(projectId, authorKey, day));
            row.setAuthorName(str(item.get("author_name")));
            row.setCommitCount(intVal(item.get("commit_count")));
        }
    }

    private void mergeMemberInt(Map<String, ReviewMemberStatsDaily> rows, Long projectId,
                                List<Map<String, Object>> agg, String field)
    {
        for (Map<String, Object> item : safe(agg))
        {
            LocalDate day = toLocalDate(item.get("stat_date"));
            String authorKey = str(item.get("author_key"));
            if (day == null || StringUtils.isEmpty(authorKey))
            {
                continue;
            }
            ReviewMemberStatsDaily row = rows.computeIfAbsent(day + "|" + authorKey,
                k -> emptyMemberRow(projectId, authorKey, day));
            int value = intVal(item.get(field));
            if ("tasks_reviewed".equals(field))
            {
                row.setTasksReviewed(value);
            }
            else if ("issues_new".equals(field))
            {
                row.setIssuesNew(value);
            }
        }
    }

    private void mergeMemberLineStats(Map<String, ReviewMemberStatsDaily> rows, Long projectId,
                                      List<Map<String, Object>> agg)
    {
        for (Map<String, Object> item : safe(agg))
        {
            LocalDate day = toLocalDate(item.get("stat_date"));
            String authorKey = str(item.get("author_key"));
            if (day == null || StringUtils.isEmpty(authorKey))
            {
                continue;
            }
            ReviewMemberStatsDaily row = rows.computeIfAbsent(day + "|" + authorKey,
                k -> emptyMemberRow(projectId, authorKey, day));
            row.setAdditionsSum(intVal(item.get("additions_sum")));
            row.setDeletionsSum(intVal(item.get("deletions_sum")));
        }
    }

    private static ReviewMemberStatsDaily emptyMemberRow(Long projectId, String authorKey, LocalDate day)
    {
        ReviewMemberStatsDaily row = new ReviewMemberStatsDaily();
        row.setProjectId(projectId);
        row.setAuthorKey(authorKey);
        row.setStatDate(Date.valueOf(day));
        row.setCommitCount(0);
        row.setTasksReviewed(0);
        row.setAdditionsSum(0);
        row.setDeletionsSum(0);
        row.setIssuesNew(0);
        row.setIssuesOpen(0);
        return row;
    }

    /**
     * 成员增删行数口径（与 selectLineStatsByAuthorDay 一致）：仅 SUCCESS；
     * additions/deletions 各自非空才计入对应合计。
     *
     * @return int[2] {additionsSum, deletionsSum}
     */
    static int[] sumMemberTaskLines(List<TaskLineSample> samples)
    {
        int additions = 0;
        int deletions = 0;
        if (samples == null)
        {
            return new int[] {0, 0};
        }
        for (TaskLineSample sample : samples)
        {
            if (sample == null || !"SUCCESS".equals(sample.taskStatus))
            {
                continue;
            }
            if (sample.additions != null)
            {
                additions += sample.additions;
            }
            if (sample.deletions != null)
            {
                deletions += sample.deletions;
            }
        }
        return new int[] {additions, deletions};
    }

    /** 供口径单测的任务行数样本。 */
    static final class TaskLineSample
    {
        final String taskStatus;
        final Integer additions;
        final Integer deletions;

        TaskLineSample(String taskStatus, Integer additions, Integer deletions)
        {
            this.taskStatus = taskStatus;
            this.additions = additions;
            this.deletions = deletions;
        }
    }

    private static String str(Object value)
    {
        return value == null ? null : String.valueOf(value);
    }

    private static ReviewStatsDaily emptyRow(Long projectId, LocalDate day)
    {
        ReviewStatsDaily row = new ReviewStatsDaily();
        row.setProjectId(projectId);
        row.setStatDate(Date.valueOf(day));
        row.setTaskTotal(0);
        row.setTaskSuccess(0);
        row.setTaskFailed(0);
        row.setTaskPush(0);
        row.setTaskCovered(0);
        row.setDurationP95Ms(0L);
        row.setIssueNew(0);
        row.setIssueCritical(0);
        row.setIssueHigh(0);
        row.setIssueMedium(0);
        row.setIssueLow(0);
        row.setIssueClosed(0);
        row.setIssueConfirmed(0);
        row.setIssueFalsePositive(0);
        row.setDeliveryTotal(0);
        row.setDeliverySuccess(0);
        row.setEventAccepted(0);
        row.setEventIgnored(0);
        return row;
    }

    private void mergeTaskAgg(Map<LocalDate, ReviewStatsDaily> rows, List<Map<String, Object>> agg)
    {
        for (Map<String, Object> item : safe(agg))
        {
            ReviewStatsDaily row = rows.get(toLocalDate(item.get("stat_date")));
            if (row == null)
            {
                continue;
            }
            row.setTaskTotal(intVal(item.get("task_total")));
            row.setTaskSuccess(intVal(item.get("task_success")));
            row.setTaskFailed(intVal(item.get("task_failed")));
            row.setTaskPush(intVal(item.get("task_push")));
        }
    }

    private void mergeCovered(Map<LocalDate, ReviewStatsDaily> rows, List<Map<String, Object>> agg)
    {
        for (Map<String, Object> item : safe(agg))
        {
            ReviewStatsDaily row = rows.get(toLocalDate(item.get("stat_date")));
            if (row != null)
            {
                row.setTaskCovered(intVal(item.get("task_covered")));
            }
        }
    }

    private void mergeIssueNew(Map<LocalDate, ReviewStatsDaily> rows, List<Map<String, Object>> agg)
    {
        for (Map<String, Object> item : safe(agg))
        {
            ReviewStatsDaily row = rows.get(toLocalDate(item.get("stat_date")));
            if (row == null)
            {
                continue;
            }
            row.setIssueNew(intVal(item.get("issue_new")));
            row.setIssueCritical(intVal(item.get("issue_critical")));
            row.setIssueHigh(intVal(item.get("issue_high")));
            row.setIssueMedium(intVal(item.get("issue_medium")));
            row.setIssueLow(intVal(item.get("issue_low")));
        }
    }

    private void mergeIssueClosed(Map<LocalDate, ReviewStatsDaily> rows, List<Map<String, Object>> agg)
    {
        for (Map<String, Object> item : safe(agg))
        {
            ReviewStatsDaily row = rows.get(toLocalDate(item.get("stat_date")));
            if (row == null)
            {
                continue;
            }
            row.setIssueClosed(intVal(item.get("issue_closed")));
            row.setIssueFalsePositive(intVal(item.get("issue_false_positive")));
        }
    }

    private void mergeIssueConfirmed(Map<LocalDate, ReviewStatsDaily> rows, List<Map<String, Object>> agg)
    {
        for (Map<String, Object> item : safe(agg))
        {
            ReviewStatsDaily row = rows.get(toLocalDate(item.get("stat_date")));
            if (row != null)
            {
                row.setIssueConfirmed(intVal(item.get("issue_confirmed")));
            }
        }
    }

    private void mergeDelivery(Map<LocalDate, ReviewStatsDaily> rows, List<Map<String, Object>> agg)
    {
        for (Map<String, Object> item : safe(agg))
        {
            ReviewStatsDaily row = rows.get(toLocalDate(item.get("stat_date")));
            if (row == null)
            {
                continue;
            }
            row.setDeliveryTotal(intVal(item.get("delivery_total")));
            row.setDeliverySuccess(intVal(item.get("delivery_success")));
        }
    }

    private void mergeEvent(Map<LocalDate, ReviewStatsDaily> rows, List<Map<String, Object>> agg)
    {
        for (Map<String, Object> item : safe(agg))
        {
            ReviewStatsDaily row = rows.get(toLocalDate(item.get("stat_date")));
            if (row == null)
            {
                continue;
            }
            row.setEventAccepted(intVal(item.get("event_accepted")));
            row.setEventIgnored(intVal(item.get("event_ignored")));
        }
    }

    private static List<Map<String, Object>> safe(List<Map<String, Object>> list)
    {
        return list == null ? List.of() : list;
    }

    private static LocalDate toLocalDate(Object value)
    {
        if (value == null)
        {
            return null;
        }
        if (value instanceof LocalDate localDate)
        {
            return localDate;
        }
        if (value instanceof java.sql.Date sqlDate)
        {
            return sqlDate.toLocalDate();
        }
        if (value instanceof java.util.Date utilDate)
        {
            return utilDate.toInstant().atZone(ZONE).toLocalDate();
        }
        return LocalDate.parse(String.valueOf(value));
    }

    private static int intVal(Object value)
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

    /** 供单测：从内存事实汇总一行（不落库）。 */
    static ReviewStatsDaily buildRowForTest(Long projectId, LocalDate day,
                                            List<Long> successDurations,
                                            int taskSuccess, int taskFailed, int taskPush,
                                            int taskCovered,
                                            int issueNew, int issueCritical, int issueHigh,
                                            int issueMedium, int issueLow,
                                            int issueClosed, int issueConfirmed, int issueFalsePositive,
                                            int deliveryTotal, int deliverySuccess,
                                            int eventAccepted, int eventIgnored)
    {
        ReviewStatsDaily row = emptyRow(projectId, day);
        row.setTaskSuccess(taskSuccess);
        row.setTaskFailed(taskFailed);
        row.setTaskTotal(taskSuccess + taskFailed);
        row.setTaskPush(taskPush);
        row.setTaskCovered(taskCovered);
        row.setDurationP95Ms(InsightMetrics.percentile95(successDurations));
        row.setIssueNew(issueNew);
        row.setIssueCritical(issueCritical);
        row.setIssueHigh(issueHigh);
        row.setIssueMedium(issueMedium);
        row.setIssueLow(issueLow);
        row.setIssueClosed(issueClosed);
        row.setIssueConfirmed(issueConfirmed);
        row.setIssueFalsePositive(issueFalsePositive);
        row.setDeliveryTotal(deliveryTotal);
        row.setDeliverySuccess(deliverySuccess);
        row.setEventAccepted(eventAccepted);
        row.setEventIgnored(eventIgnored);
        return row;
    }

    /** 供单测：比较两行关键指标是否一致。 */
    static boolean sameMetrics(ReviewStatsDaily a, ReviewStatsDaily b)
    {
        if (a == null || b == null)
        {
            return a == b;
        }
        Set<String> keys = new HashSet<>();
        keys.add("x");
        return eq(a.getTaskTotal(), b.getTaskTotal())
            && eq(a.getTaskSuccess(), b.getTaskSuccess())
            && eq(a.getTaskFailed(), b.getTaskFailed())
            && eq(a.getTaskCovered(), b.getTaskCovered())
            && eq(a.getDurationP95Ms(), b.getDurationP95Ms())
            && eq(a.getIssueNew(), b.getIssueNew())
            && eq(a.getIssueCritical(), b.getIssueCritical())
            && eq(a.getEventAccepted(), b.getEventAccepted())
            && eq(a.getDeliverySuccess(), b.getDeliverySuccess());
    }

    private static boolean eq(Number a, Number b)
    {
        long left = a == null ? 0L : a.longValue();
        long right = b == null ? 0L : b.longValue();
        return left == right;
    }
}
