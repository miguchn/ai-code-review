package com.acr.review.service.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
import com.acr.review.domain.ReviewPipelineConstants;
import com.acr.review.domain.ReviewProject;
import com.acr.review.domain.ReviewRoundReconcileResult;
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
import com.acr.review.service.ReviewScoringConstants;
import com.acr.system.service.ISysConfigService;
import com.acr.system.service.ISysDeptService;
import com.alibaba.fastjson2.JSON;

/** 问题台账用例：轮次对账、状态机、处置后评论重渲染。 */
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
    private final ISysConfigService configService;

    public ReviewIssueServiceImpl(ReviewIssueMapper issueMapper,
                                  ReviewIssueActionMapper actionMapper,
                                  ReviewProjectMapper projectMapper,
                                  ReviewTaskMapper taskMapper,
                                  ISysDeptService deptService,
                                  IReviewDeliveryService deliveryService,
                                  ISysConfigService configService)
    {
        this.issueMapper = issueMapper;
        this.actionMapper = actionMapper;
        this.projectMapper = projectMapper;
        this.taskMapper = taskMapper;
        this.deptService = deptService;
        this.deliveryService = deliveryService;
        this.configService = configService;
    }

    @Override
    public ReviewRoundReconcileResult reconcileAfterSuccess(ReviewTask task, ReviewTaskRun run)
    {
        if (task == null || task.getTaskId() == null || task.getProjectId() == null || task.getPrNumber() == null)
        {
            return ReviewRoundReconcileResult.empty();
        }
        if (!shouldReconcile(task, run))
        {
            return ReviewRoundReconcileResult.empty();
        }
        List<ReviewTopIssue> roundIssues = ReviewSummaryContentFactory.resolveTopIssues(run);
        List<ReviewIssue> allIssues = issueMapper.selectByProjectAndPr(task.getProjectId(), task.getPrNumber());
        if (allIssues == null)
        {
            allIssues = List.of();
        }
        ensureFamilyKeys(allIssues);

        String headSha = resolveHeadSha(task, run);
        Long runId = run == null ? null : run.getRunId();
        int roundNo = resolveRoundNumber(allIssues, task.getTaskId());
        int threshold = resolveMissedThreshold();

        Set<Long> hitIssueIds = new HashSet<>();
        Set<Integer> consumedRoundIndexes = new HashSet<>();
        List<ReviewIssue> reopened = new ArrayList<>();
        List<ReviewIssue> newlyMaterialized = new ArrayList<>();
        List<ReviewIssue> movedToRechecking = new ArrayList<>();

        // Pass 1 精确命中
        for (int i = 0; i < roundIssues.size(); i++)
        {
            ReviewTopIssue top = roundIssues.get(i);
            String fp = ReviewIssueFingerprint.of(top);
            ReviewIssue existing = findByFingerprint(allIssues, fp);
            if (existing == null)
            {
                continue;
            }
            consumedRoundIndexes.add(i);
            if (ReviewIssueConstants.isTerminal(existing.getStatus()))
            {
                continue;
            }
            applyHit(existing, task, run, top, headSha, ReviewIssueConstants.OPERATOR_SYSTEM);
            if (ReviewIssueConstants.STATUS_RECHECKING.equals(existing.getStatus()))
            {
                String from = existing.getStatus();
                existing.setStatus(ReviewIssueConstants.STATUS_AWAITING_FIX);
                issueMapper.updateIssueSnapshot(existing);
                insertSystemAction(existing.getIssueId(), ReviewIssueConstants.ACTION_AUTO_REOPEN, from,
                    ReviewIssueConstants.STATUS_AWAITING_FIX,
                    "第 " + roundNo + " 轮审查再次命中，修复未生效");
                reopened.add(existing);
            }
            else
            {
                issueMapper.updateIssueSnapshot(existing);
            }
            hitIssueIds.add(existing.getIssueId());
        }

        // Pass 2 单义族合并
        for (int i = 0; i < roundIssues.size(); i++)
        {
            if (consumedRoundIndexes.contains(i))
            {
                continue;
            }
            ReviewTopIssue top = roundIssues.get(i);
            String fk = ReviewIssueFingerprint.familyKey(top);
            List<ReviewIssue> familyActive = new ArrayList<>();
            for (ReviewIssue issue : allIssues)
            {
                if (hitIssueIds.contains(issue.getIssueId()))
                {
                    continue;
                }
                if (!ReviewIssueConstants.isActive(issue.getStatus()))
                {
                    continue;
                }
                if (fk.equals(resolveFamilyKey(issue)))
                {
                    familyActive.add(issue);
                }
            }
            if (familyActive.size() != 1)
            {
                continue;
            }
            ReviewIssue target = familyActive.get(0);
            String newFp = ReviewIssueFingerprint.of(top);
            // 避免唯一键冲突：若新指纹已被终态占用则跳过合并
            ReviewIssue collision = findByFingerprint(allIssues, newFp);
            if (collision != null && !Objects.equals(collision.getIssueId(), target.getIssueId()))
            {
                continue;
            }
            applyHit(target, task, run, top, headSha, ReviewIssueConstants.OPERATOR_SYSTEM);
            target.setFingerprint(newFp);
            target.setFamilyKey(fk);
            if (ReviewIssueConstants.STATUS_RECHECKING.equals(target.getStatus()))
            {
                String from = target.getStatus();
                target.setStatus(ReviewIssueConstants.STATUS_AWAITING_FIX);
                issueMapper.updateIssueSnapshot(target);
                insertSystemAction(target.getIssueId(), ReviewIssueConstants.ACTION_AUTO_REOPEN, from,
                    ReviewIssueConstants.STATUS_AWAITING_FIX,
                    "第 " + roundNo + " 轮审查再次命中，修复未生效");
                reopened.add(target);
            }
            else
            {
                issueMapper.updateIssueSnapshot(target);
            }
            hitIssueIds.add(target.getIssueId());
            consumedRoundIndexes.add(i);
        }

        // Pass 3 新物化
        Set<String> usedFingerprints = new HashSet<>();
        for (ReviewIssue issue : allIssues)
        {
            if (issue.getFingerprint() != null)
            {
                usedFingerprints.add(issue.getFingerprint());
            }
        }
        int batchIndex = 0;
        for (int i = 0; i < roundIssues.size(); i++)
        {
            if (consumedRoundIndexes.contains(i))
            {
                continue;
            }
            batchIndex++;
            ReviewTopIssue top = roundIssues.get(i);
            String baseFp = ReviewIssueFingerprint.of(top);
            String fp = baseFp;
            if (usedFingerprints.contains(fp))
            {
                fp = ReviewIssueFingerprint.withBatchSuffix(baseFp, batchIndex);
            }
            usedFingerprints.add(fp);
            ReviewIssue created = buildNewIssue(task, run, top, fp, headSha, ReviewIssueConstants.OPERATOR_SYSTEM);
            issueMapper.insertIssue(created);
            allIssues.add(created);
            newlyMaterialized.add(created);
            hitIssueIds.add(created.getIssueId());
        }

        // Pass 4 未命中判定
        for (ReviewIssue issue : allIssues)
        {
            if (!ReviewIssueConstants.isActive(issue.getStatus()))
            {
                continue;
            }
            if (hitIssueIds.contains(issue.getIssueId()))
            {
                continue;
            }
            if (StringUtils.isNotEmpty(headSha) && headSha.equals(issue.getLastSeenHeadSha()))
            {
                continue;
            }
            if (runId != null && runId.equals(issue.getLastMissedRunId()))
            {
                continue;
            }
            int streak = issue.getMissedStreak() == null ? 0 : issue.getMissedStreak();
            streak++;
            issue.setMissedStreak(streak);
            issue.setLastMissedRunId(runId);
            issue.setUpdateBy(ReviewIssueConstants.OPERATOR_SYSTEM);
            boolean shouldRecheck = streak >= threshold
                && !ReviewIssueConstants.STATUS_RECHECKING.equals(issue.getStatus());
            if (shouldRecheck)
            {
                String from = issue.getStatus();
                issue.setStatus(ReviewIssueConstants.STATUS_RECHECKING);
                issue.setRecheckTaskId(task.getTaskId());
                issue.setRecheckRunId(runId);
                issue.setRecheckCommitSha(headSha);
                issueMapper.updateIssueSnapshot(issue);
                String shortSha = ReviewSummaryContentFactory.shortSha(headSha);
                insertSystemAction(issue.getIssueId(), ReviewIssueConstants.ACTION_AUTO_RECHECK, from,
                    ReviewIssueConstants.STATUS_RECHECKING,
                    "第 " + roundNo + " 轮审查（commit " + (StringUtils.isEmpty(shortSha) ? "--" : shortSha)
                        + "）未再命中");
                movedToRechecking.add(issue);
            }
            else
            {
                issueMapper.updateIssueSnapshot(issue);
            }
        }

        return new ReviewRoundReconcileResult(newlyMaterialized, movedToRechecking, reopened);
    }

    private static boolean shouldReconcile(ReviewTask task, ReviewTaskRun run)
    {
        if (!ReviewPipelineConstants.TASK_SUCCESS.equals(task.getTaskStatus()))
        {
            return false;
        }
        String parseStatus = run != null && StringUtils.isNotEmpty(run.getParseStatus())
            ? run.getParseStatus() : task.getParseStatus();
        if (!ReviewScoringConstants.PARSE_SUCCESS.equals(parseStatus))
        {
            return false;
        }
        return ReviewPipelineConstants.isLlmDirectMode(task.getSnapshotReviewMode());
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
            ReviewIssueConstants.STATUS_AWAITING_FIX, null, null);
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
        String closeSource = ReviewIssueConstants.STATUS_RECHECKING.equals(issue.getStatus())
            ? ReviewIssueConstants.CLOSE_SOURCE_AUTO_RECHECK
            : ReviewIssueConstants.CLOSE_SOURCE_MANUAL;
        transition(issue, ReviewIssueConstants.ACTION_CLOSE,
            ReviewIssueConstants.STATUS_CLOSED, note, closeSource);
        return scheduleCommentRerender(issue);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReviewCommentSyncResult dismiss(Long issueId, String dismissType, String resolveNote)
    {
        ReviewIssue issue = requireIssue(issueId);
        checkIssueDataScope(issue);
        if (ReviewIssueConstants.STATUS_RECHECKING.equals(issue.getStatus()))
        {
            throw new ServiceException("待复核问题不可忽略/误报，请确认已修复或重新打开");
        }
        if (!ReviewIssueConstants.isConfirmable(issue.getStatus()))
        {
            throw new ServiceException(terminalOrIllegalMessage(issue.getStatus(), "忽略/误报"));
        }
        if (!ReviewIssueConstants.STATUS_IGNORED.equals(dismissType)
            && !ReviewIssueConstants.STATUS_FALSE_POSITIVE.equals(dismissType))
        {
            throw new ServiceException("dismissType 必须为 IGNORED 或 FALSE_POSITIVE");
        }
        String note = normalizeNote(resolveNote, true);
        transition(issue, ReviewIssueConstants.ACTION_DISMISS, dismissType, note,
            ReviewIssueConstants.CLOSE_SOURCE_MANUAL);
        return scheduleCommentRerender(issue);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReviewCommentSyncResult reopen(Long issueId)
    {
        ReviewIssue issue = requireIssue(issueId);
        checkIssueDataScope(issue);
        if (!ReviewIssueConstants.STATUS_RECHECKING.equals(issue.getStatus()))
        {
            throw new ServiceException("仅待复核问题可重新打开");
        }
        transition(issue, ReviewIssueConstants.ACTION_REOPEN,
            ReviewIssueConstants.STATUS_AWAITING_FIX, null, null);
        return scheduleCommentRerender(issue);
    }

    @Override
    public List<String> listRecheckingTitles(Long projectId, Integer prNumber)
    {
        if (projectId == null || prNumber == null)
        {
            return List.of();
        }
        List<ReviewIssue> list = issueMapper.selectByProjectAndPr(projectId, prNumber);
        if (list == null || list.isEmpty())
        {
            return List.of();
        }
        List<String> titles = new ArrayList<>();
        for (ReviewIssue issue : list)
        {
            if (!ReviewIssueConstants.STATUS_RECHECKING.equals(issue.getStatus()))
            {
                continue;
            }
            titles.add(StringUtils.isEmpty(issue.getTitle())
                ? ReviewIssueConstants.DEFAULT_TITLE : issue.getTitle());
        }
        return titles;
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

    private void transition(ReviewIssue issue, String actionType, String toStatus,
                            String resolveNote, String closeSource)
    {
        String from = issue.getStatus();
        String operator = safeOperator();
        Date now = new Date();

        issue.setStatus(toStatus);
        issue.setResolveNote(resolveNote);
        issue.setUpdateBy(operator);
        if (ReviewIssueConstants.isTerminal(toStatus))
        {
            issue.setCloseSource(StringUtils.isNotEmpty(closeSource)
                ? closeSource : ReviewIssueConstants.CLOSE_SOURCE_MANUAL);
            issue.setClosedBy(operator);
            issue.setClosedTime(now);
        }
        else
        {
            // 重开等非终态：清理关闭痕迹
            if (ReviewIssueConstants.ACTION_REOPEN.equals(actionType)
                || ReviewIssueConstants.ACTION_CONFIRM.equals(actionType))
            {
                issue.setCloseSource(null);
                issue.setClosedBy(null);
                issue.setClosedTime(null);
            }
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

    private void insertSystemAction(Long issueId, String actionType, String from, String to, String note)
    {
        ReviewIssueAction action = new ReviewIssueAction();
        action.setIssueId(issueId);
        action.setOperator(ReviewIssueConstants.OPERATOR_SYSTEM);
        action.setActionType(actionType);
        action.setFromStatus(from);
        action.setToStatus(to);
        action.setResolveNote(note);
        action.setCreateTime(new Date());
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

    private static void applyHit(ReviewIssue issue, ReviewTask task, ReviewTaskRun run,
                                 ReviewTopIssue top, String headSha, String operator)
    {
        issue.setLastTaskId(task.getTaskId());
        issue.setLastRunId(run == null ? null : run.getRunId());
        applySnapshotFields(issue, top);
        if (StringUtils.isEmpty(issue.getFamilyKey()))
        {
            issue.setFamilyKey(ReviewIssueFingerprint.familyKey(top));
        }
        issue.setMissedStreak(0);
        issue.setLastSeenHeadSha(headSha);
        issue.setUpdateBy(operator);
    }

    private static ReviewIssue buildNewIssue(ReviewTask task, ReviewTaskRun run, ReviewTopIssue top,
                                            String fingerprint, String headSha, String operator)
    {
        ReviewIssue issue = new ReviewIssue();
        issue.setProjectId(task.getProjectId());
        issue.setProvider(StringUtils.defaultIfEmpty(task.getProvider(), ReviewIssueConstants.PROVIDER_GITHUB));
        issue.setPrNumber(task.getPrNumber());
        issue.setFingerprint(fingerprint);
        issue.setFamilyKey(ReviewIssueFingerprint.familyKey(top));
        issue.setFirstTaskId(task.getTaskId());
        issue.setFirstRunId(run == null ? null : run.getRunId());
        issue.setLastTaskId(task.getTaskId());
        issue.setLastRunId(run == null ? null : run.getRunId());
        applySnapshotFields(issue, top);
        issue.setStatus(ReviewIssueConstants.STATUS_AWAITING_CONFIRM);
        issue.setMissedStreak(0);
        issue.setLastSeenHeadSha(headSha);
        issue.setCreateBy(operator);
        issue.setUpdateBy(operator);
        return issue;
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

    private void ensureFamilyKeys(List<ReviewIssue> issues)
    {
        for (ReviewIssue issue : issues)
        {
            if (StringUtils.isEmpty(issue.getFamilyKey()))
            {
                issue.setFamilyKey(ReviewIssueFingerprint.familyKey(issue.getFilePath(), issue.getCategory()));
            }
        }
    }

    private static String resolveFamilyKey(ReviewIssue issue)
    {
        if (StringUtils.isNotEmpty(issue.getFamilyKey()))
        {
            return issue.getFamilyKey();
        }
        return ReviewIssueFingerprint.familyKey(issue.getFilePath(), issue.getCategory());
    }

    private static ReviewIssue findByFingerprint(List<ReviewIssue> issues, String fingerprint)
    {
        for (ReviewIssue issue : issues)
        {
            if (fingerprint.equals(issue.getFingerprint()))
            {
                return issue;
            }
        }
        return null;
    }

    private static String resolveHeadSha(ReviewTask task, ReviewTaskRun run)
    {
        if (task != null && StringUtils.isNotEmpty(task.getHeadSha()))
        {
            return task.getHeadSha();
        }
        if (run != null && StringUtils.isNotEmpty(run.getSnapshotHeadSha()))
        {
            return run.getSnapshotHeadSha();
        }
        return null;
    }

    private static int resolveRoundNumber(List<ReviewIssue> issues, Long currentTaskId)
    {
        Set<Long> taskIds = new HashSet<>();
        if (currentTaskId != null)
        {
            taskIds.add(currentTaskId);
        }
        if (issues != null)
        {
            for (ReviewIssue issue : issues)
            {
                if (issue.getFirstTaskId() != null)
                {
                    taskIds.add(issue.getFirstTaskId());
                }
                if (issue.getLastTaskId() != null)
                {
                    taskIds.add(issue.getLastTaskId());
                }
                if (issue.getRecheckTaskId() != null)
                {
                    taskIds.add(issue.getRecheckTaskId());
                }
            }
        }
        return Math.max(1, taskIds.size());
    }

    private int resolveMissedThreshold()
    {
        if (configService == null)
        {
            return ReviewIssueConstants.DEFAULT_MISSED_ROUNDS_THRESHOLD;
        }
        try
        {
            String raw = configService.selectConfigByKey(ReviewIssueConstants.CONFIG_MISSED_ROUNDS_THRESHOLD);
            if (StringUtils.isEmpty(raw))
            {
                return ReviewIssueConstants.DEFAULT_MISSED_ROUNDS_THRESHOLD;
            }
            int value = Integer.parseInt(raw.trim());
            return value > 0 ? value : ReviewIssueConstants.DEFAULT_MISSED_ROUNDS_THRESHOLD;
        }
        catch (Exception ex)
        {
            return ReviewIssueConstants.DEFAULT_MISSED_ROUNDS_THRESHOLD;
        }
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
            return ReviewIssueConstants.OPERATOR_SYSTEM;
        }
    }
}
