package com.acr.review.service.impl;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
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
import com.acr.review.domain.WorkbenchRecentItem;
import com.acr.review.domain.WorkbenchScope;
import com.acr.review.domain.WorkbenchSummary;
import com.acr.review.domain.WorkbenchToday;
import com.acr.review.service.IReviewDeliveryService;
import com.acr.review.service.IReviewIssueService;
import com.acr.review.service.IReviewProjectService;
import com.acr.review.service.IReviewRecordService;
import com.acr.review.service.IReviewTaskService;
import com.acr.review.service.IWorkbenchService;

/**
 * 工作台汇总：按菜单权限裁剪卡片，计数走各列表同款 DataScope。
 */
@Service
public class WorkbenchServiceImpl implements IWorkbenchService
{
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern(DateUtils.YYYY_MM_DD);
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private final IReviewProjectService projectService;
    private final IReviewIssueService issueService;
    private final IReviewTaskService taskService;
    private final IReviewRecordService recordService;
    private final IReviewDeliveryService deliveryService;

    public WorkbenchServiceImpl(IReviewProjectService projectService,
                                IReviewIssueService issueService,
                                IReviewTaskService taskService,
                                IReviewRecordService recordService,
                                IReviewDeliveryService deliveryService)
    {
        this.projectService = projectService;
        this.issueService = issueService;
        this.taskService = taskService;
        this.recordService = recordService;
        this.deliveryService = deliveryService;
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
                scope.setLatestTaskTime(DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS, latest));
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
                ReviewIssueConstants.STATUS_AWAITING_CONFIRM));
            cards.add(issueCard(
                WorkbenchConstants.CARD_ISSUE_AWAITING_FIX,
                WorkbenchConstants.TITLE_ISSUE_AWAITING_FIX,
                ReviewIssueConstants.STATUS_AWAITING_FIX));
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

    private WorkbenchCard issueCard(String type, String title, String status)
    {
        ReviewIssue query = new ReviewIssue();
        query.setStatus(status);
        query.setOrigin(ReviewIssueConstants.ORIGIN_NEW);
        Map<String, String> q = new LinkedHashMap<>();
        q.put("status", status);
        q.put("origin", ReviewIssueConstants.ORIGIN_NEW);
        return new WorkbenchCard(type, title, issueService.countIssueList(query), WorkbenchConstants.LINK_ISSUE, q);
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
                link));
        }
        return items;
    }

    static String buildRecentTitle(ReviewTask task)
    {
        String project = StringUtils.isNotEmpty(task.getProjectName()) ? task.getProjectName() : "未命名项目";
        String pr = task.getPrNumber() == null ? "PR —" : "PR #" + task.getPrNumber();
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
        if ("CANCELLED".equals(task.getTaskStatus()))
        {
            return "已取消";
        }
        return StringUtils.isNotEmpty(task.getTaskStatus()) ? task.getTaskStatus() : "未知";
    }
}
