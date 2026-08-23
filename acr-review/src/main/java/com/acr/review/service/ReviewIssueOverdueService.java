package com.acr.review.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import com.acr.common.utils.StringUtils;
import com.acr.review.delivery.ReviewDeliveryIntentService;
import com.acr.review.domain.ReviewIssue;
import com.acr.review.domain.ReviewIssueConstants;
import com.acr.review.domain.ReviewProject;
import com.acr.review.domain.ReviewTask;
import com.acr.review.mapper.ReviewIssueMapper;
import com.acr.review.mapper.ReviewProjectMapper;
import com.acr.review.mapper.ReviewTaskMapper;
import com.acr.system.service.ISysConfigService;

/** 逾期标志重算与聚合提醒入队。 */
@Service
public class ReviewIssueOverdueService
{
    private static final Logger log = LoggerFactory.getLogger(ReviewIssueOverdueService.class);
    private static final long MILLIS_PER_DAY = 24L * 60L * 60L * 1000L;

    private final ReviewIssueMapper issueMapper;
    private final ReviewProjectMapper projectMapper;
    private final ReviewTaskMapper taskMapper;
    private final ReviewDeliveryIntentService deliveryIntentService;
    private final ISysConfigService configService;

    public ReviewIssueOverdueService(ReviewIssueMapper issueMapper,
                                     ReviewProjectMapper projectMapper,
                                     ReviewTaskMapper taskMapper,
                                     ReviewDeliveryIntentService deliveryIntentService,
                                     ISysConfigService configService)
    {
        this.issueMapper = issueMapper;
        this.projectMapper = projectMapper;
        this.taskMapper = taskMapper;
        this.deliveryIntentService = deliveryIntentService;
        this.configService = configService;
    }

    public int scan()
    {
        int highDays = parseDays(ReviewIssueConstants.CONFIG_OVERDUE_HIGH_DAYS,
            ReviewIssueConstants.DEFAULT_OVERDUE_HIGH_DAYS);
        int normalDays = parseDays(ReviewIssueConstants.CONFIG_OVERDUE_NORMAL_DAYS,
            ReviewIssueConstants.DEFAULT_OVERDUE_NORMAL_DAYS);
        Date now = new Date();
        List<ReviewIssue> issues = issueMapper.selectActiveIssuesForOverdueScan();
        if (issues == null || issues.isEmpty())
        {
            return 0;
        }
        Map<Long, List<ReviewIssue>> newlyByProject = new LinkedHashMap<>();
        for (ReviewIssue issue : issues)
        {
            boolean overdue = isOverdue(issue, now, highDays, normalDays);
            String next = overdue ? "Y" : "N";
            String prev = StringUtils.defaultIfEmpty(issue.getOverdueFlag(), "N");
            if (!next.equals(prev))
            {
                issueMapper.updateOverdueFlag(issue.getIssueId(), next);
            }
            if (overdue && !"Y".equals(prev))
            {
                newlyByProject.computeIfAbsent(issue.getProjectId(), key -> new ArrayList<>()).add(issue);
            }
        }
        for (Map.Entry<Long, List<ReviewIssue>> entry : newlyByProject.entrySet())
        {
            enqueueProjectRemind(entry.getKey());
        }
        return issues.size();
    }

    static boolean isOverdue(ReviewIssue issue, Date now, int highDays, int normalDays)
    {
        if (issue == null || issue.getStageEnteredTime() == null || now == null)
        {
            return false;
        }
        int days = isHighSeverity(issue.getSeverity()) ? highDays : normalDays;
        return now.getTime() - issue.getStageEnteredTime().getTime() >= days * MILLIS_PER_DAY;
    }

    static boolean isHighSeverity(String severity)
    {
        if (severity == null)
        {
            return false;
        }
        String normalized = severity.trim().toUpperCase(Locale.ROOT);
        return "CRITICAL".equals(normalized) || "HIGH".equals(normalized);
    }

    private void enqueueProjectRemind(Long projectId)
    {
        ReviewProject project = projectMapper.selectReviewProjectById(projectId);
        if (project == null || !"Y".equals(project.getNotifyEnabled()))
        {
            return;
        }
        ReviewTask anchor = taskMapper.selectLatestByProjectId(projectId);
        if (anchor == null)
        {
            log.warn("逾期提醒跳过：项目无审查任务可挂载投递记录, projectId={}", projectId);
            return;
        }
        deliveryIntentService.enqueueOverdueRemind(project, anchor, ReviewIssueConstants.OPERATOR_SYSTEM);
    }

    private int parseDays(String key, int fallback)
    {
        try
        {
            String raw = configService == null ? null : configService.selectConfigByKey(key);
            if (StringUtils.isEmpty(raw))
            {
                return fallback;
            }
            int value = Integer.parseInt(raw.trim());
            return value > 0 ? value : fallback;
        }
        catch (Exception ignored)
        {
            return fallback;
        }
    }
}
