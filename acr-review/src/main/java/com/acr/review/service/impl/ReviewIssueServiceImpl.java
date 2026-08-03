package com.acr.review.service.impl;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.acr.common.annotation.DataScope;
import com.acr.common.exception.ServiceException;
import com.acr.common.utils.SecurityUtils;
import com.acr.common.utils.StringUtils;
import com.acr.review.delivery.ReviewSummaryContentFactory;
import com.acr.review.domain.ReviewCommentSyncResult;
import com.acr.review.domain.ReviewIssue;
import com.acr.review.domain.ReviewIssueAction;
import com.acr.review.domain.ReviewIssueConstants;
import com.acr.review.domain.ReviewIssueDetail;
import com.acr.review.domain.ReviewProject;
import com.acr.review.domain.ReviewTask;
import com.acr.review.domain.ReviewTaskRun;
import com.acr.review.domain.result.ReviewTopIssue;
import com.acr.review.mapper.ReviewIssueActionMapper;
import com.acr.review.mapper.ReviewIssueMapper;
import com.acr.review.mapper.ReviewProjectMapper;
import com.acr.review.mapper.ReviewTaskMapper;
import com.acr.review.service.IReviewDeliveryService;
import com.acr.review.service.IReviewIssueService;
import com.acr.review.service.ReviewIssueDispositionEnricher;
import com.acr.review.service.ReviewIssueFingerprint;
import com.acr.system.service.ISysDeptService;
import com.alibaba.fastjson2.JSON;

/** 问题台账用例：指纹物化、状态机、处置后评论重渲染。 */
@Service
public class ReviewIssueServiceImpl implements IReviewIssueService
{
    private static final Logger log = LoggerFactory.getLogger(ReviewIssueServiceImpl.class);

    private final ReviewIssueMapper issueMapper;
    private final ReviewIssueActionMapper actionMapper;
    private final ReviewProjectMapper projectMapper;
    private final ReviewTaskMapper taskMapper;
    private final ISysDeptService deptService;
    private final IReviewDeliveryService deliveryService;

    public ReviewIssueServiceImpl(ReviewIssueMapper issueMapper,
                                  ReviewIssueActionMapper actionMapper,
                                  ReviewProjectMapper projectMapper,
                                  ReviewTaskMapper taskMapper,
                                  ISysDeptService deptService,
                                  IReviewDeliveryService deliveryService)
    {
        this.issueMapper = issueMapper;
        this.actionMapper = actionMapper;
        this.projectMapper = projectMapper;
        this.taskMapper = taskMapper;
        this.deptService = deptService;
        this.deliveryService = deliveryService;
    }

    @Override
    public void materializeAfterSuccess(ReviewTask task, ReviewTaskRun run)
    {
        if (task == null || task.getTaskId() == null || task.getProjectId() == null || task.getPrNumber() == null)
        {
            return;
        }
        try
        {
            List<ReviewTopIssue> topIssues = ReviewSummaryContentFactory.resolveTopIssues(run);
            if (topIssues.isEmpty())
            {
                return;
            }
            String operator = safeOperator();
            Set<String> used = new HashSet<>();
            int batchIndex = 0;
            for (ReviewTopIssue top : topIssues)
            {
                batchIndex++;
                String baseFp = ReviewIssueFingerprint.of(top);
                String fp = baseFp;
                if (used.contains(fp))
                {
                    fp = ReviewIssueFingerprint.withBatchSuffix(baseFp, batchIndex);
                }
                used.add(fp);

                ReviewIssue existing = issueMapper.selectByProjectPrFingerprint(
                    task.getProjectId(), task.getPrNumber(), fp);
                if (existing == null)
                {
                    ReviewIssue created = buildNewIssue(task, run, top, fp, operator);
                    issueMapper.insertIssue(created);
                    continue;
                }
                if (ReviewIssueConstants.isTerminal(existing.getStatus()))
                {
                    continue;
                }
                applySnapshot(existing, task, run, top, operator);
                issueMapper.updateIssueSnapshot(existing);
            }
        }
        catch (Exception ex)
        {
            log.warn("问题台账物化失败（不影响任务状态）, taskId={}", task.getTaskId(), ex);
        }
    }

    @Override
    @DataScope(deptAlias = "d", userAlias = "owner", permission = "review:issue:list")
    public List<ReviewIssue> selectIssueList(ReviewIssue query)
    {
        return issueMapper.selectIssueList(query);
    }

    @Override
    public ReviewIssueDetail selectIssueDetail(Long issueId)
    {
        ReviewIssue issue = requireIssue(issueId);
        checkIssueDataScope(issue);
        ReviewIssueDetail detail = new ReviewIssueDetail();
        detail.setIssue(issue);
        if (issue.getLastTaskId() != null)
        {
            detail.setSourceTask(taskMapper.selectReviewTaskById(issue.getLastTaskId()));
        }
        detail.setActions(actionMapper.selectByIssueId(issueId));
        detail.setSummaryDelivery(deliveryService.selectSummaryDelivery(issue.getProjectId(), issue.getPrNumber()));
        return detail;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReviewCommentSyncResult confirm(Long issueId)
    {
        ReviewIssue issue = requireIssue(issueId);
        checkIssueDataScope(issue);
        if (!ReviewIssueConstants.STATUS_AWAITING_CONFIRM.equals(issue.getStatus()))
        {
            throw new ServiceException(terminalOrIllegalMessage(issue.getStatus(), "确认"));
        }
        transition(issue, ReviewIssueConstants.ACTION_CONFIRM,
            ReviewIssueConstants.STATUS_AWAITING_FIX, null);
        return scheduleCommentRerender(issue);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReviewCommentSyncResult close(Long issueId, String resolveNote)
    {
        ReviewIssue issue = requireIssue(issueId);
        checkIssueDataScope(issue);
        if (!ReviewIssueConstants.isOpen(issue.getStatus()))
        {
            throw new ServiceException(terminalOrIllegalMessage(issue.getStatus(), "关闭"));
        }
        String note = normalizeNote(resolveNote, false);
        transition(issue, ReviewIssueConstants.ACTION_CLOSE,
            ReviewIssueConstants.STATUS_CLOSED, note);
        return scheduleCommentRerender(issue);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReviewCommentSyncResult dismiss(Long issueId, String dismissType, String resolveNote)
    {
        ReviewIssue issue = requireIssue(issueId);
        checkIssueDataScope(issue);
        if (!ReviewIssueConstants.isOpen(issue.getStatus()))
        {
            throw new ServiceException(terminalOrIllegalMessage(issue.getStatus(), "忽略/误报"));
        }
        if (!ReviewIssueConstants.STATUS_IGNORED.equals(dismissType)
            && !ReviewIssueConstants.STATUS_FALSE_POSITIVE.equals(dismissType))
        {
            throw new ServiceException("dismissType 必须为 IGNORED 或 FALSE_POSITIVE");
        }
        String note = normalizeNote(resolveNote, true);
        transition(issue, ReviewIssueConstants.ACTION_DISMISS, dismissType, note);
        return scheduleCommentRerender(issue);
    }

    @Override
    public Map<String, ReviewIssue> mapByFingerprint(Long projectId, Integer prNumber)
    {
        Map<String, ReviewIssue> map = new HashMap<>();
        if (projectId == null || prNumber == null)
        {
            return map;
        }
        List<ReviewIssue> list = issueMapper.selectByProjectAndPr(projectId, prNumber);
        if (list == null)
        {
            return map;
        }
        for (ReviewIssue issue : list)
        {
            map.put(issue.getFingerprint(), issue);
        }
        return map;
    }

    @Override
    public void enrichTopIssues(List<ReviewTopIssue> topIssues, Long projectId, Integer prNumber)
    {
        if (topIssues == null || topIssues.isEmpty() || projectId == null || prNumber == null)
        {
            return;
        }
        Map<String, ReviewIssue> byFp = mapByFingerprint(projectId, prNumber);
        ReviewIssueDispositionEnricher.enrich(topIssues, byFp);
    }

    @Override
    public void enrichRuns(List<ReviewTaskRun> runs, Long projectId, Integer prNumber)
    {
        if (runs == null || runs.isEmpty() || projectId == null || prNumber == null)
        {
            return;
        }
        for (ReviewTaskRun run : runs)
        {
            List<ReviewTopIssue> issues = ReviewSummaryContentFactory.resolveTopIssues(run);
            if (issues.isEmpty())
            {
                continue;
            }
            enrichTopIssues(issues, projectId, prNumber);
            run.setTopIssuesJson(JSON.toJSONString(issues));
        }
    }

    @Override
    public int countOpenNewByProject(Long projectId)
    {
        if (projectId == null)
        {
            return 0;
        }
        return issueMapper.countOpenNewByProject(projectId);
    }

    @Override
    @DataScope(deptAlias = "d", userAlias = "owner", permission = "review:issue:list")
    public int countIssueList(ReviewIssue query)
    {
        return issueMapper.countIssueList(query);
    }

    @Override
    @DataScope(deptAlias = "d", userAlias = "owner", permission = "review:issue:list")
    public int countClosedToday(ReviewIssue query)
    {
        return issueMapper.countClosedToday(query);
    }

    private void transition(ReviewIssue issue, String actionType, String toStatus, String resolveNote)
    {
        if (ReviewIssueConstants.STATUS_RECHECKING.equals(toStatus)
            || ReviewIssueConstants.STATUS_RECHECKING.equals(issue.getStatus()))
        {
            throw new ServiceException("复核中状态本期未开放");
        }
        String from = issue.getStatus();
        String operator = safeOperator();
        Date now = new Date();

        issue.setStatus(toStatus);
        issue.setResolveNote(resolveNote);
        issue.setUpdateBy(operator);
        if (ReviewIssueConstants.isTerminal(toStatus))
        {
            issue.setCloseSource(ReviewIssueConstants.CLOSE_SOURCE_MANUAL);
            issue.setClosedBy(operator);
            issue.setClosedTime(now);
        }
        issueMapper.updateIssueDisposition(issue);

        ReviewIssueAction action = new ReviewIssueAction();
        action.setIssueId(issue.getIssueId());
        action.setOperator(operator);
        action.setActionType(actionType);
        action.setFromStatus(from);
        action.setToStatus(toStatus);
        action.setResolveNote(resolveNote);
        action.setCreateTime(now);
        actionMapper.insertAction(action);
    }

    private ReviewCommentSyncResult scheduleCommentRerender(ReviewIssue issue)
    {
        // 处置已在本事务写入；评论失败由 delivery 内部落 FAILED 并返回，不回滚处置。
        return deliveryService.rerenderSummaryComment(issue.getProjectId(), issue.getPrNumber());
    }

    private ReviewIssue requireIssue(Long issueId)
    {
        if (issueId == null)
        {
            throw new ServiceException("问题不存在");
        }
        ReviewIssue issue = issueMapper.selectIssueById(issueId);
        if (issue == null)
        {
            throw new ServiceException("问题不存在");
        }
        return issue;
    }

    private void checkIssueDataScope(ReviewIssue issue)
    {
        ReviewProject project = projectMapper.selectReviewProjectById(issue.getProjectId());
        if (project == null)
        {
            throw new ServiceException("问题所属项目不存在");
        }
        deptService.checkDeptDataScope(project.getDeptId());
    }

    private static ReviewIssue buildNewIssue(ReviewTask task, ReviewTaskRun run, ReviewTopIssue top,
                                            String fingerprint, String operator)
    {
        ReviewIssue issue = new ReviewIssue();
        issue.setProjectId(task.getProjectId());
        issue.setProvider(StringUtils.defaultIfEmpty(task.getProvider(), ReviewIssueConstants.PROVIDER_GITHUB));
        issue.setPrNumber(task.getPrNumber());
        issue.setFingerprint(fingerprint);
        issue.setFirstTaskId(task.getTaskId());
        issue.setFirstRunId(run == null ? null : run.getRunId());
        issue.setLastTaskId(task.getTaskId());
        issue.setLastRunId(run == null ? null : run.getRunId());
        applySnapshotFields(issue, top);
        issue.setStatus(ReviewIssueConstants.STATUS_AWAITING_CONFIRM);
        issue.setCreateBy(operator);
        issue.setUpdateBy(operator);
        return issue;
    }

    private static void applySnapshot(ReviewIssue issue, ReviewTask task, ReviewTaskRun run,
                                      ReviewTopIssue top, String operator)
    {
        issue.setLastTaskId(task.getTaskId());
        issue.setLastRunId(run == null ? null : run.getRunId());
        applySnapshotFields(issue, top);
        issue.setUpdateBy(operator);
    }

    private static void applySnapshotFields(ReviewIssue issue, ReviewTopIssue top)
    {
        issue.setIssueRank(top.getRank());
        issue.setSeverity(top.getSeverity());
        issue.setCategory(top.getCategory());
        issue.setTitle(StringUtils.isEmpty(top.getTitle())
            ? ReviewIssueConstants.DEFAULT_TITLE : top.getTitle());
        issue.setDescription(top.getDescription());
        issue.setFilePath(top.getFilePath());
        issue.setStartLine(top.getStartLine());
        issue.setEndLine(top.getEndLine());
        issue.setEvidence(top.getEvidence());
        issue.setSuggestion(top.getSuggestion());
        String origin = top.getOrigin();
        if (StringUtils.isEmpty(origin))
        {
            origin = ReviewIssueConstants.ORIGIN_NEW;
        }
        else if (ReviewIssueConstants.ORIGIN_EXISTING.equalsIgnoreCase(origin.trim()))
        {
            origin = ReviewIssueConstants.ORIGIN_EXISTING;
        }
        else
        {
            origin = ReviewIssueConstants.ORIGIN_NEW;
        }
        issue.setOrigin(origin);
    }

    private static String normalizeNote(String resolveNote, boolean required)
    {
        String note = resolveNote == null ? null : resolveNote.trim();
        if (required && StringUtils.isEmpty(note))
        {
            throw new ServiceException("忽略/误报必须填写原因");
        }
        if (note != null && note.length() > ReviewIssueConstants.MAX_RESOLVE_NOTE_CHARS)
        {
            note = note.substring(0, ReviewIssueConstants.MAX_RESOLVE_NOTE_CHARS);
        }
        return StringUtils.isEmpty(note) ? null : note;
    }

    private static String terminalOrIllegalMessage(String status, String action)
    {
        if (ReviewIssueConstants.isTerminal(status))
        {
            return "问题已终态（" + ReviewIssueConstants.statusLabel(status) + "），不可再" + action;
        }
        if (ReviewIssueConstants.STATUS_RECHECKING.equals(status))
        {
            return "复核中状态本期未开放";
        }
        return "当前状态不允许" + action;
    }

    private static String safeOperator()
    {
        try
        {
            return SecurityUtils.getUsername();
        }
        catch (Exception ex)
        {
            return "system";
        }
    }
}
