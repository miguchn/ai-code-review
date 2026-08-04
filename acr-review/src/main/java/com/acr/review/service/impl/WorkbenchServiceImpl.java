package com.acr.review.service.impl;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import com.acr.common.enums.LlmProviderCode;
import com.acr.common.utils.DateUtils;
import com.acr.common.utils.SecurityUtils;
import com.acr.common.utils.StringUtils;
import com.acr.review.delivery.ReviewDeliveryConstants;
import com.acr.review.domain.ReviewDeliveryRecord;
import com.acr.review.domain.ReviewIssue;
import com.acr.review.domain.ReviewIssueConstants;
import com.acr.review.domain.ReviewPipelineConstants;
import com.acr.review.domain.ReviewProject;
import com.acr.review.domain.ReviewTask;
import com.acr.review.domain.WorkbenchCard;
import com.acr.review.domain.WorkbenchConstants;
import com.acr.review.domain.WorkbenchModelItem;
import com.acr.review.domain.WorkbenchModels;
import com.acr.review.domain.WorkbenchRecentItem;
import com.acr.review.domain.WorkbenchScope;
import com.acr.review.domain.WorkbenchSummary;
import com.acr.review.domain.WorkbenchToday;
import com.acr.review.domain.WorkbenchTrend;
import com.acr.review.domain.WorkbenchTrendPoint;
import com.acr.review.domain.result.ReviewConclusionDailyStat;
import com.acr.review.service.IReviewDeliveryService;
import com.acr.review.service.IReviewIssueService;
import com.acr.review.service.IReviewProjectService;
import com.acr.review.service.IReviewRecordService;
import com.acr.review.service.IReviewTaskService;
import com.acr.review.service.IWorkbenchService;
import com.acr.system.domain.SysAiModelConfig;
import com.acr.system.service.ISysAiModelConfigService;

/**
 * 工作台汇总：按菜单权限裁剪卡片，计数走各列表同款 DataScope。
 */
@Service
public class WorkbenchServiceImpl implements IWorkbenchService
{
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern(DateUtils.YYYY_MM_DD);
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    /** 与 LlmCallServiceImpl 写入的 lastCheckResult 成功前缀一致。 */
    private static final String CHECK_SUCCESS_PREFIX = "成功";

    private final IReviewProjectService projectService;
    private final IReviewIssueService issueService;
    private final IReviewTaskService taskService;
    private final IReviewRecordService recordService;
    private final IReviewDeliveryService deliveryService;
    private final ISysAiModelConfigService aiModelConfigService;

    public WorkbenchServiceImpl(IReviewProjectService projectService,
                                IReviewIssueService issueService,
                                IReviewTaskService taskService,
                                IReviewRecordService recordService,
                                IReviewDeliveryService deliveryService,
                                ISysAiModelConfigService aiModelConfigService)
    {
        this.projectService = projectService;
        this.issueService = issueService;
        this.taskService = taskService;
        this.recordService = recordService;
        this.deliveryService = deliveryService;
        this.aiModelConfigService = aiModelConfigService;
    }

    @Override
    public WorkbenchSummary getSummary()
    {
        WorkbenchSummary summary = new WorkbenchSummary();
        summary.setScope(buildScope());
        summary.setCards(buildCards());
        summary.setToday(buildToday());
        summary.setRecent(buildRecent());
        return summary;
    }

    @Override
    public WorkbenchTrend getTrend(int days)
    {
        if (!SecurityUtils.hasPermi(WorkbenchConstants.PERM_RECORD_LIST))
        {
            return null;
        }
        int normalized = Math.min(Math.max(days, 1), WorkbenchConstants.TREND_MAX_DAYS);
        LocalDate end = LocalDate.now(ZONE);
        LocalDate begin = end.minusDays(normalized - 1L);

        ReviewTask query = new ReviewTask();
        query.getParams().put("beginTime", begin.format(DAY));
        query.getParams().put("endTime", end.format(DAY));
        List<ReviewConclusionDailyStat> stats = recordService.selectReviewConclusionTrend(query);

        Map<String, ReviewConclusionDailyStat> byDate = new HashMap<>();
        if (stats != null)
        {
            for (ReviewConclusionDailyStat stat : stats)
            {
                byDate.put(stat.getStatDate(), stat);
            }
        }
        List<WorkbenchTrendPoint> points = new ArrayList<>();
        for (int i = 0; i < normalized; i++)
        {
            String day = begin.plusDays(i).format(DAY);
            ReviewConclusionDailyStat stat = byDate.get(day);
            points.add(stat == null
                ? new WorkbenchTrendPoint(day, 0, 0, 0, 0)
                : new WorkbenchTrendPoint(day, stat.getPassCount(), stat.getWarnCount(),
                    stat.getBlockCount(), stat.getFailedCount()));
        }
        return new WorkbenchTrend(normalized, begin.format(DAY), end.format(DAY), points);
    }

    @Override
    public WorkbenchModels getModelHealth()
    {
        SysAiModelConfig query = new SysAiModelConfig();
        query.setEnabled("1");
        List<SysAiModelConfig> enabled = aiModelConfigService.selectSysAiModelConfigList(query);

        WorkbenchModels models = new WorkbenchModels();
        List<WorkbenchModelItem> items = new ArrayList<>();
        Date latestCheck = null;
        int online = 0;
        if (enabled != null)
        {
            for (SysAiModelConfig config : enabled)
            {
                WorkbenchModelItem item = new WorkbenchModelItem();
                item.setModelName(config.getModelName());
                item.setProvider(config.getProvider());
                item.setProviderLabel(providerLabel(config));
                item.setModel(config.getModel());
                item.setIsDefault("1".equals(config.getIsDefault()));
                String checkStatus = deriveCheckStatus(config);
                item.setCheckStatus(checkStatus);
                item.setLastCheckResult(config.getLastCheckResult());
                if (config.getLastCheckTime() != null)
                {
                    item.setLastCheckTime(DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM, config.getLastCheckTime()));
                    if (latestCheck == null || config.getLastCheckTime().after(latestCheck))
                    {
                        latestCheck = config.getLastCheckTime();
                    }
                }
                if (WorkbenchConstants.CHECK_STATUS_SUCCESS.equals(checkStatus))
                {
                    online++;
                }
                items.add(item);
            }
        }
        models.setEnabledCount(items.size());
        models.setOnlineCount(online);
        if (latestCheck != null)
        {
            models.setLastCheckTime(DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM, latestCheck));
        }
        models.setItems(items);
        return models;
    }

    static String deriveCheckStatus(SysAiModelConfig config)
    {
        if (config.getLastCheckTime() == null || StringUtils.isEmpty(config.getLastCheckResult()))
        {
            return WorkbenchConstants.CHECK_STATUS_NEVER;
        }
        return config.getLastCheckResult().startsWith(CHECK_SUCCESS_PREFIX)
            ? WorkbenchConstants.CHECK_STATUS_SUCCESS
            : WorkbenchConstants.CHECK_STATUS_FAILED;
    }

    private static String providerLabel(SysAiModelConfig config)
    {
        LlmProviderCode code = LlmProviderCode.fromCode(config.getProvider());
        if (code != null && !code.isCustom())
        {
            return code.getLabel();
        }
        if (StringUtils.isNotEmpty(config.getCustomProviderName()))
        {
            return config.getCustomProviderName();
        }
        return code != null ? code.getLabel() : "自定义";
    }

    private WorkbenchScope buildScope()
    {
        WorkbenchScope scope = new WorkbenchScope();
        if (SecurityUtils.hasPermi(WorkbenchConstants.PERM_PROJECT_LIST))
        {
            scope.setProjectCount(projectService.countReviewProjectList(new ReviewProject()));
        }
        if (SecurityUtils.hasPermi(WorkbenchConstants.PERM_TASK_LIST))
        {
            Date latest = taskService.selectLatestTaskTime(new ReviewTask());
            if (latest != null)
            {
                scope.setLatestTaskTime(DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM, latest));
            }
        }
        return scope;
    }

    private List<WorkbenchCard> buildCards()
    {
        List<WorkbenchCard> cards = new ArrayList<>();
        if (SecurityUtils.hasPermi(WorkbenchConstants.PERM_ISSUE_LIST))
        {
            cards.add(issueCard(
                WorkbenchConstants.CARD_ISSUE_AWAITING_CONFIRM,
                WorkbenchConstants.TITLE_ISSUE_AWAITING_CONFIRM,
                WorkbenchConstants.SUBTITLE_ORIGIN_NEW,
                ReviewIssueConstants.STATUS_AWAITING_CONFIRM,
                ReviewIssueConstants.ORIGIN_NEW));
            cards.add(issueCard(
                WorkbenchConstants.CARD_ISSUE_EXISTING_CONFIRM,
                WorkbenchConstants.TITLE_ISSUE_EXISTING_CONFIRM,
                WorkbenchConstants.SUBTITLE_ORIGIN_EXISTING,
                ReviewIssueConstants.STATUS_AWAITING_CONFIRM,
                ReviewIssueConstants.ORIGIN_EXISTING));
            cards.add(issueCard(
                WorkbenchConstants.CARD_ISSUE_AWAITING_FIX,
                WorkbenchConstants.TITLE_ISSUE_AWAITING_FIX,
                WorkbenchConstants.SUBTITLE_ALL_ORIGIN,
                ReviewIssueConstants.STATUS_AWAITING_FIX,
                null));
        }
        if (SecurityUtils.hasPermi(WorkbenchConstants.PERM_RECORD_LIST))
        {
            cards.add(highRiskCard());
        }
        if (SecurityUtils.hasPermi(WorkbenchConstants.PERM_TASK_LIST))
        {
            ReviewTask query = new ReviewTask();
            query.setTaskStatus(ReviewPipelineConstants.TASK_FAILED);
            Map<String, String> q = new LinkedHashMap<>();
            q.put("taskStatus", ReviewPipelineConstants.TASK_FAILED);
            cards.add(new WorkbenchCard(
                WorkbenchConstants.CARD_TASK_FAILED,
                WorkbenchConstants.TITLE_TASK_FAILED,
                taskService.countReviewTaskList(query),
                WorkbenchConstants.LINK_TASK,
                q));
        }
        if (SecurityUtils.hasPermi(WorkbenchConstants.PERM_DELIVERY_LIST))
        {
            ReviewDeliveryRecord query = new ReviewDeliveryRecord();
            query.setDeliveryStatus(ReviewDeliveryConstants.STATUS_FAILED);
            Map<String, String> q = new LinkedHashMap<>();
            q.put("deliveryStatus", ReviewDeliveryConstants.STATUS_FAILED);
            cards.add(new WorkbenchCard(
                WorkbenchConstants.CARD_DELIVERY_FAILED,
                WorkbenchConstants.TITLE_DELIVERY_FAILED,
                deliveryService.countDeliveryList(query),
                WorkbenchConstants.LINK_DELIVERY,
                q));
        }
        return cards;
    }

    private WorkbenchCard issueCard(String type, String title, String subtitle, String status, String origin)
    {
        ReviewIssue query = new ReviewIssue();
        query.setStatus(status);
        Map<String, String> q = new LinkedHashMap<>();
        q.put("status", status);
        if (origin != null)
        {
            query.setOrigin(origin);
            q.put("origin", origin);
        }
        return new WorkbenchCard(type, title, subtitle, issueService.countIssueList(query), WorkbenchConstants.LINK_ISSUE, q);
    }

    private WorkbenchCard highRiskCard()
    {
        LocalDate end = LocalDate.now(ZONE);
        LocalDate begin = end.minusDays(WorkbenchConstants.HIGH_RISK_WINDOW_DAYS - 1L);
        String beginTime = begin.format(DAY);
        String endTime = end.format(DAY);

        ReviewTask query = new ReviewTask();
        query.setReviewConclusion(ReviewPipelineConstants.CONCLUSION_BLOCK);
        query.getParams().put("beginTime", beginTime);
        query.getParams().put("endTime", endTime);

        Map<String, String> q = new LinkedHashMap<>();
        q.put("reviewConclusion", ReviewPipelineConstants.CONCLUSION_BLOCK);
        q.put("beginTime", beginTime);
        q.put("endTime", endTime);
        return new WorkbenchCard(
            WorkbenchConstants.CARD_HIGH_RISK_CONCLUSION,
            WorkbenchConstants.TITLE_HIGH_RISK_CONCLUSION,
            WorkbenchConstants.SUBTITLE_HIGH_RISK_WINDOW,
            recordService.countReviewRecordList(query),
            WorkbenchConstants.LINK_RECORD,
            q);
    }

    private WorkbenchToday buildToday()
    {
        WorkbenchToday today = new WorkbenchToday();
        if (SecurityUtils.hasPermi(WorkbenchConstants.PERM_TASK_LIST))
        {
            today.setNewTasks(taskService.countTodayNewTasks(new ReviewTask()));
            today.setSuccessTasks(taskService.countTodaySuccessTasks(new ReviewTask()));
            today.setFailedTasks(taskService.countTodayFailedTasks(new ReviewTask()));
        }
        if (SecurityUtils.hasPermi(WorkbenchConstants.PERM_ISSUE_LIST))
        {
            today.setClosedIssues(issueService.countClosedToday(new ReviewIssue()));
        }
        return today;
    }

    private List<WorkbenchRecentItem> buildRecent()
    {
        if (!SecurityUtils.hasPermi(WorkbenchConstants.PERM_TASK_LIST))
        {
            return List.of();
        }
        List<ReviewTask> tasks = taskService.selectRecentTasks(new ReviewTask());
        List<WorkbenchRecentItem> items = new ArrayList<>();
        if (tasks == null)
        {
            return items;
        }
        for (ReviewTask task : tasks)
        {
            String time = null;
            Date when = task.getFinishedTime() != null ? task.getFinishedTime() : task.getCreateTime();
            if (when != null)
            {
                time = DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS, when);
            }
            String link = task.getTaskId() == null
                ? WorkbenchConstants.LINK_TASK
                : WorkbenchConstants.LINK_TASK_DETAIL_PREFIX + task.getTaskId();
            items.add(new WorkbenchRecentItem(
                WorkbenchConstants.RECENT_TYPE_TASK,
                buildRecentTitle(task),
                time,
                link,
                deriveRecentConclusion(task)));
        }
        return items;
    }

    static String deriveRecentConclusion(ReviewTask task)
    {
        if (ReviewPipelineConstants.TASK_SUCCESS.equals(task.getTaskStatus()))
        {
            return task.getReviewConclusion();
        }
        if (ReviewPipelineConstants.TASK_FAILED.equals(task.getTaskStatus()))
        {
            return WorkbenchConstants.RECENT_CONCLUSION_FAILED;
        }
        return null;
    }

    static String buildRecentTitle(ReviewTask task)
    {
        String project = StringUtils.isNotEmpty(task.getProjectName()) ? task.getProjectName() : "未命名项目";
        String pr = task.getPrNumber() == null ? "合并请求 —" : "合并请求 #" + task.getPrNumber();
        return project + " · " + pr + " · " + taskStatusOrConclusionLabel(task);
    }

    static String taskStatusOrConclusionLabel(ReviewTask task)
    {
        if (ReviewPipelineConstants.TASK_SUCCESS.equals(task.getTaskStatus()))
        {
            if (ReviewPipelineConstants.CONCLUSION_PASS.equals(task.getReviewConclusion()))
            {
                return "通过";
            }
            if (ReviewPipelineConstants.CONCLUSION_WARN.equals(task.getReviewConclusion()))
            {
                return "建议修改";
            }
            if (ReviewPipelineConstants.CONCLUSION_BLOCK.equals(task.getReviewConclusion()))
            {
                return "高风险";
            }
            return "已完成";
        }
        if (ReviewPipelineConstants.TASK_FAILED.equals(task.getTaskStatus()))
        {
            return "已失败";
        }
        if (ReviewPipelineConstants.TASK_RUNNING.equals(task.getTaskStatus()))
        {
            return "执行中";
        }
        if (ReviewPipelineConstants.TASK_PENDING.equals(task.getTaskStatus()))
        {
            return "待执行";
        }
        if (ReviewPipelineConstants.TASK_CANCELLED.equals(task.getTaskStatus()))
        {
            return "已取消";
        }
        return StringUtils.isNotEmpty(task.getTaskStatus()) ? task.getTaskStatus() : "未知";
    }
}
