package com.acr.review.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import com.acr.common.exception.ServiceException;
import com.acr.common.utils.SecurityUtils;
import com.acr.review.delivery.ReviewDeliveryConstants;
import com.acr.review.domain.ReviewCommentSyncResult;
import com.acr.review.domain.ReviewDeliveryRecord;
import com.acr.review.domain.ReviewIssue;
import com.acr.review.domain.ReviewIssueAction;
import com.acr.review.domain.ReviewIssueBatchRequest;
import com.acr.review.domain.ReviewIssueBatchResult;
import com.acr.review.domain.ReviewIssueConstants;
import com.acr.review.domain.ReviewIssueDetail;
import com.acr.review.domain.ReviewIssueRecordContext;
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
import com.acr.review.mapper.ReviewTaskRunMapper;
import com.acr.review.service.IReviewDeliveryService;
import com.acr.review.service.ReviewIssueFingerprint;
import com.acr.review.service.ReviewScoringConstants;
import com.acr.system.service.ISysConfigService;
import com.acr.system.service.ISysDeptService;
import com.alibaba.fastjson2.JSON;

@ExtendWith(MockitoExtension.class)
class ReviewIssueServiceImplTest
{
    @Mock private ReviewIssueMapper issueMapper;
    @Mock private ReviewIssueActionMapper actionMapper;
    @Mock private ReviewProjectMapper projectMapper;
    @Mock private ReviewTaskMapper taskMapper;
    @Mock private ReviewTaskRunMapper runMapper;
    @Mock private ISysDeptService deptService;
    @Mock private IReviewDeliveryService deliveryService;
    @Mock private ISysConfigService configService;

    private ReviewIssueServiceImpl service;

    @BeforeEach
    void setUp()
    {
        service = new ReviewIssueServiceImpl(issueMapper, actionMapper, projectMapper, taskMapper, runMapper,
            deptService, deliveryService, configService);
        lenient().when(configService.selectConfigByKey(ReviewIssueConstants.CONFIG_MISSED_ROUNDS_THRESHOLD))
            .thenReturn("1");
    }

    @Test
    void reconcileInsertsNewIssues()
    {
        ReviewTask task = successLlmTask(1L, 10L, 8, "aaa1111");
        ReviewTaskRun run = runWithIssues(100L, top("SEC", "a.java", "leak", 1));
        when(issueMapper.selectByProjectAndPr(10L, 8, "")).thenReturn(new ArrayList<>());
        when(issueMapper.insertIssue(any())).thenAnswer(inv -> {
            ReviewIssue created = inv.getArgument(0);
            created.setIssueId(501L);
            return 1;
        });

        ReviewRoundReconcileResult result = service.reconcileAfterSuccess(task, run);

        ArgumentCaptor<ReviewIssue> captor = ArgumentCaptor.forClass(ReviewIssue.class);
        verify(issueMapper).insertIssue(captor.capture());
        ReviewIssue created = captor.getValue();
        assertEquals(ReviewIssueConstants.STATUS_AWAITING_CONFIRM, created.getStatus());
        assertEquals(ReviewIssueFingerprint.of("SEC", "a.java", "leak"), created.getFingerprint());
        assertEquals(ReviewIssueFingerprint.familyKey("a.java", "SEC"), created.getFamilyKey());
        assertEquals("aaa1111", created.getLastSeenHeadSha());
        assertEquals(1, result.getNewlyMaterialized().size());
        ArgumentCaptor<ReviewIssueAction> actionCaptor = ArgumentCaptor.forClass(ReviewIssueAction.class);
        verify(actionMapper).insertAction(actionCaptor.capture());
        ReviewIssueAction detected = actionCaptor.getValue();
        assertEquals(ReviewIssueConstants.ACTION_DETECTED, detected.getActionType());
        assertEquals(501L, detected.getIssueId());
        assertEquals(ReviewIssueConstants.OPERATOR_SYSTEM, detected.getOperator());
        assertEquals(ReviewIssueConstants.STATUS_AWAITING_CONFIRM, detected.getFromStatus());
        assertEquals(ReviewIssueConstants.STATUS_AWAITING_CONFIRM, detected.getToStatus());
        assertTrue(detected.getResolveNote().contains("发现"));
        assertTrue(detected.getResolveNote().contains("aaa1111"));
        ArgumentCaptor<String> snapshotCaptor = ArgumentCaptor.forClass(String.class);
        verify(runMapper).updateTopIssuesJson(eq(100L), snapshotCaptor.capture());
        assertTrue(snapshotCaptor.getValue().contains("\"issueId\":501"));
    }

    @Test
    void reconcileExactHitClearsMissedStreak()
    {
        ReviewTask task = successLlmTask(2L, 10L, 8, "bbb2222");
        ReviewTaskRun run = runWithIssues(101L, top("SEC", "a.java", "leak", 1));
        ReviewIssue existing = openIssue(99L, "SEC", "a.java", "leak");
        existing.setMissedStreak(2);
        existing.setLastSeenHeadSha("oldsha");
        when(issueMapper.selectByProjectAndPr(10L, 8, "")).thenReturn(new ArrayList<>(List.of(existing)));

        service.reconcileAfterSuccess(task, run);

        verify(issueMapper, never()).insertIssue(any());
        verify(issueMapper).updateIssueSnapshot(existing);
        assertEquals(0, existing.getMissedStreak());
        assertEquals("bbb2222", existing.getLastSeenHeadSha());
        assertEquals(2L, existing.getLastTaskId());
        ArgumentCaptor<ReviewIssueAction> actionCaptor = ArgumentCaptor.forClass(ReviewIssueAction.class);
        verify(actionMapper).insertAction(actionCaptor.capture());
        ReviewIssueAction hit = actionCaptor.getValue();
        assertEquals(ReviewIssueConstants.ACTION_ROUND_HIT, hit.getActionType());
        assertEquals(99L, hit.getIssueId());
        assertEquals(ReviewIssueConstants.STATUS_AWAITING_CONFIRM, hit.getFromStatus());
        assertEquals(ReviewIssueConstants.STATUS_AWAITING_CONFIRM, hit.getToStatus());
        assertTrue(hit.getResolveNote().contains("再次命中"));
        assertTrue(hit.getResolveNote().contains("bbb2222"));
    }

    @Test
    void reconcileFamilyMergeUpdatesFingerprint()
    {
        ReviewTask task = successLlmTask(3L, 10L, 8, "ccc3333");
        // 同文件同类别、不同 title → 不同指纹、相同 family
        ReviewTopIssue drifted = top("SEC", "a.java", "password leak v2", 1);
        ReviewTaskRun run = runWithIssues(102L, drifted);
        ReviewIssue existing = openIssue(99L, "SEC", "a.java", "password leak");
        existing.setFamilyKey(ReviewIssueFingerprint.familyKey("a.java", "SEC"));
        when(issueMapper.selectByProjectAndPr(10L, 8, "")).thenReturn(new ArrayList<>(List.of(existing)));

        service.reconcileAfterSuccess(task, run);

        verify(issueMapper, never()).insertIssue(any());
        assertEquals(ReviewIssueFingerprint.of(drifted), existing.getFingerprint());
        assertEquals(0, existing.getMissedStreak());
        assertEquals("password leak v2", existing.getTitle());
    }

    @Test
    void reconcileAmbiguousFamilyDoesNotMerge()
    {
        ReviewTask task = successLlmTask(4L, 10L, 8, "ddd4444");
        ReviewTopIssue drifted = top("SEC", "a.java", "new wording", 1);
        ReviewTaskRun run = runWithIssues(103L, drifted);
        ReviewIssue a = openIssue(1L, "SEC", "a.java", "one");
        ReviewIssue b = openIssue(2L, "SEC", "a.java", "two");
        a.setFamilyKey(ReviewIssueFingerprint.familyKey("a.java", "SEC"));
        b.setFamilyKey(ReviewIssueFingerprint.familyKey("a.java", "SEC"));
        when(issueMapper.selectByProjectAndPr(10L, 8, "")).thenReturn(new ArrayList<>(List.of(a, b)));

        ReviewRoundReconcileResult result = service.reconcileAfterSuccess(task, run);

        verify(issueMapper).insertIssue(any());
        assertEquals(1, result.getNewlyMaterialized().size());
        // 两条旧问题未命中 → 转待复核
        assertEquals(2, result.getMovedToRechecking().size());
    }

    @Test
    void reconcileMissBelowThresholdWritesRoundMiss()
    {
        when(configService.selectConfigByKey(ReviewIssueConstants.CONFIG_MISSED_ROUNDS_THRESHOLD))
            .thenReturn("2");
        ReviewTask task = successLlmTask(51L, 10L, 8, "miss1111");
        ReviewTaskRun run = runWithIssues(151L); // 空清单
        ReviewIssue existing = openIssue(99L, "SEC", "a.java", "leak");
        existing.setStatus(ReviewIssueConstants.STATUS_AWAITING_FIX);
        existing.setLastSeenHeadSha("oldsha");
        existing.setMissedStreak(0);
        when(issueMapper.selectByProjectAndPr(10L, 8, "")).thenReturn(new ArrayList<>(List.of(existing)));

        ReviewRoundReconcileResult result = service.reconcileAfterSuccess(task, run);

        assertTrue(result.getMovedToRechecking().isEmpty());
        assertEquals(ReviewIssueConstants.STATUS_AWAITING_FIX, existing.getStatus());
        assertEquals(1, existing.getMissedStreak());
        ArgumentCaptor<ReviewIssueAction> actionCaptor = ArgumentCaptor.forClass(ReviewIssueAction.class);
        verify(actionMapper).insertAction(actionCaptor.capture());
        ReviewIssueAction miss = actionCaptor.getValue();
        assertEquals(ReviewIssueConstants.ACTION_ROUND_MISS, miss.getActionType());
        assertEquals(ReviewIssueConstants.STATUS_AWAITING_FIX, miss.getFromStatus());
        assertEquals(ReviewIssueConstants.STATUS_AWAITING_FIX, miss.getToStatus());
        assertTrue(miss.getResolveNote().contains("未命中"));
        assertTrue(miss.getResolveNote().contains("连续未命中 1 次"));
    }

    @Test
    void reconcileMissMovesToRecheckingWithEvidence()
    {
        ReviewTask task = successLlmTask(5L, 10L, 8, "eee5555");
        ReviewTaskRun run = runWithIssues(104L); // 空清单
        ReviewIssue existing = openIssue(99L, "SEC", "a.java", "leak");
        existing.setStatus(ReviewIssueConstants.STATUS_AWAITING_FIX);
        existing.setLastSeenHeadSha("oldsha");
        when(issueMapper.selectByProjectAndPr(10L, 8, "")).thenReturn(new ArrayList<>(List.of(existing)));

        ReviewRoundReconcileResult result = service.reconcileAfterSuccess(task, run);

        assertEquals(1, result.getMovedToRechecking().size());
        assertEquals(ReviewIssueConstants.STATUS_RECHECKING, existing.getStatus());
        assertEquals(1, existing.getMissedStreak());
        assertEquals(104L, existing.getLastMissedRunId());
        assertEquals(5L, existing.getRecheckTaskId());
        assertEquals(104L, existing.getRecheckRunId());
        assertEquals("eee5555", existing.getRecheckCommitSha());
        ArgumentCaptor<ReviewIssueAction> actionCaptor = ArgumentCaptor.forClass(ReviewIssueAction.class);
        verify(actionMapper).insertAction(actionCaptor.capture());
        assertEquals(ReviewIssueConstants.ACTION_AUTO_RECHECK, actionCaptor.getValue().getActionType());
        assertEquals(ReviewIssueConstants.OPERATOR_SYSTEM, actionCaptor.getValue().getOperator());
        // 达阈值时只写 AUTO_RECHECK，不重复写 ROUND_MISS
        verify(actionMapper, times(1)).insertAction(any());
    }

    @Test
    void reconcileSameCommitRerunDoesNotCountMiss()
    {
        ReviewTask task = successLlmTask(6L, 10L, 8, "sameSHA");
        ReviewTaskRun run = runWithIssues(105L);
        ReviewIssue existing = openIssue(99L, "SEC", "a.java", "leak");
        existing.setLastSeenHeadSha("sameSHA");
        existing.setMissedStreak(0);
        when(issueMapper.selectByProjectAndPr(10L, 8, "")).thenReturn(new ArrayList<>(List.of(existing)));

        ReviewRoundReconcileResult result = service.reconcileAfterSuccess(task, run);

        assertTrue(result.getMovedToRechecking().isEmpty());
        assertEquals(0, existing.getMissedStreak());
        verify(issueMapper, never()).updateIssueSnapshot(any());
    }

    @Test
    void reconcileSameRunIdempotentDoesNotDoubleCount()
    {
        ReviewTask task = successLlmTask(7L, 10L, 8, "fff6666");
        ReviewTaskRun run = runWithIssues(106L);
        ReviewIssue existing = openIssue(99L, "SEC", "a.java", "leak");
        existing.setLastSeenHeadSha("old");
        existing.setMissedStreak(1);
        existing.setLastMissedRunId(106L);
        existing.setStatus(ReviewIssueConstants.STATUS_RECHECKING);
        when(issueMapper.selectByProjectAndPr(10L, 8, "")).thenReturn(new ArrayList<>(List.of(existing)));

        service.reconcileAfterSuccess(task, run);

        assertEquals(1, existing.getMissedStreak());
        verify(issueMapper, never()).updateIssueSnapshot(any());
        verify(actionMapper, never()).insertAction(any());
    }

    @Test
    void reconcileSkipsTerminalOnExactHit()
    {
        ReviewTask task = successLlmTask(8L, 10L, 8, "ggg7777");
        ReviewTaskRun run = runWithIssues(107L, top("SEC", "a.java", "leak", 1));
        ReviewIssue existing = openIssue(99L, "SEC", "a.java", "leak");
        existing.setStatus(ReviewIssueConstants.STATUS_CLOSED);
        when(issueMapper.selectByProjectAndPr(10L, 8, "")).thenReturn(new ArrayList<>(List.of(existing)));

        ReviewRoundReconcileResult result = service.reconcileAfterSuccess(task, run);

        verify(issueMapper, never()).insertIssue(any());
        verify(issueMapper, never()).updateIssueSnapshot(any());
        assertTrue(result.getReopened().isEmpty());
    }

    @Test
    void reconcileAutoReopensRecheckingOnHit()
    {
        ReviewTask task = successLlmTask(9L, 10L, 8, "hhh8888");
        ReviewTaskRun run = runWithIssues(108L, top("SEC", "a.java", "leak", 1));
        ReviewIssue existing = openIssue(99L, "SEC", "a.java", "leak");
        existing.setStatus(ReviewIssueConstants.STATUS_RECHECKING);
        when(issueMapper.selectByProjectAndPr(10L, 8, "")).thenReturn(new ArrayList<>(List.of(existing)));

        ReviewRoundReconcileResult result = service.reconcileAfterSuccess(task, run);

        assertEquals(1, result.getReopened().size());
        assertEquals(ReviewIssueConstants.STATUS_AWAITING_FIX, existing.getStatus());
        ArgumentCaptor<ReviewIssueAction> actionCaptor = ArgumentCaptor.forClass(ReviewIssueAction.class);
        verify(actionMapper).insertAction(actionCaptor.capture());
        assertEquals(ReviewIssueConstants.ACTION_AUTO_REOPEN, actionCaptor.getValue().getActionType());
        // RECHECKING 被命中只写 AUTO_REOPEN，不重复写 ROUND_HIT
        verify(actionMapper, times(1)).insertAction(any());
    }

    @Test
    void reconcileHandlesBatchFingerprintCollision()
    {
        ReviewTask task = successLlmTask(10L, 10L, 8, "iii9999");
        ReviewTopIssue a = top("SEC", "a.java", "same", 1);
        ReviewTopIssue b = top("SEC", "a.java", "same", 2);
        ReviewTaskRun run = runWithIssues(109L, a, b);
        when(issueMapper.selectByProjectAndPr(10L, 8, "")).thenReturn(new ArrayList<>());

        service.reconcileAfterSuccess(task, run);

        ArgumentCaptor<ReviewIssue> captor = ArgumentCaptor.forClass(ReviewIssue.class);
        verify(issueMapper, times(2)).insertIssue(captor.capture());
        List<ReviewIssue> inserted = captor.getAllValues();
        assertTrue(!inserted.get(0).getFingerprint().equals(inserted.get(1).getFingerprint())
            || inserted.get(1).getFingerprint().contains(":"));
    }

    @Test
    void reconcileMaterializesOcrEngineIssues()
    {
        ReviewTask task = successLlmTask(11L, 10L, 8, "jjj");
        task.setSnapshotReviewMode(ReviewPipelineConstants.REVIEW_MODE_OCR_ENGINE);
        ReviewTaskRun run = runWithIssues(110L, top("SECURITY", "a.java", "leak", 1));
        when(issueMapper.selectByProjectAndPr(10L, 8, "")).thenReturn(new ArrayList<>());
        when(issueMapper.insertIssue(any())).thenAnswer(inv -> {
            ReviewIssue created = inv.getArgument(0);
            created.setIssueId(601L);
            return 1;
        });

        ReviewRoundReconcileResult result = service.reconcileAfterSuccess(task, run);

        assertEquals(1, result.getNewlyMaterialized().size());
        ArgumentCaptor<ReviewIssue> captor = ArgumentCaptor.forClass(ReviewIssue.class);
        verify(issueMapper).insertIssue(captor.capture());
        assertEquals("leak", captor.getValue().getTitle());
        assertEquals("SECURITY", captor.getValue().getCategory());
    }

    @Test
    void reconcileDoesNotFailWhenIssueLinkSnapshotCannotBePersisted()
    {
        ReviewTask task = successLlmTask(12L, 10L, 8, "snapshotsha");
        ReviewTaskRun run = runWithIssues(112L);
        when(issueMapper.selectByProjectAndPr(10L, 8, "")).thenReturn(new ArrayList<>());
        doThrow(new RuntimeException("db down")).when(runMapper).updateTopIssuesJson(eq(112L), any());

        ReviewRoundReconcileResult result = service.reconcileAfterSuccess(task, run);

        assertTrue(result.getNewlyMaterialized().isEmpty());
        assertEquals("[]", run.getTopIssuesJson());
    }

    @Test
    void listRecheckingTitlesFiltersActiveRechecking()
    {
        ReviewIssue a = openIssue(1L, "SEC", "a.java", "sql-injection");
        a.setStatus(ReviewIssueConstants.STATUS_RECHECKING);
        ReviewIssue b = openIssue(2L, "SEC", "b.java", "open-issue");
        b.setStatus(ReviewIssueConstants.STATUS_AWAITING_FIX);
        ReviewIssue c = openIssue(3L, "SEC", "c.java", "cmd-injection");
        c.setStatus(ReviewIssueConstants.STATUS_RECHECKING);
        when(issueMapper.selectByProjectAndPr(10L, 8, "")).thenReturn(List.of(a, b, c));

        assertEquals(List.of("sql-injection", "cmd-injection"), service.listRecheckingTitles(10L, 8, ""));
        assertEquals(List.of(), service.listRecheckingTitles(null, 8, ""));
    }

    @Test
    void closeFromRecheckingUsesAutoRecheckSource()
    {
        ReviewIssue issue = openIssue(7L, "SEC", "a.java", "x");
        issue.setStatus(ReviewIssueConstants.STATUS_RECHECKING);
        stubProjectScope(issue);
        when(issueMapper.selectIssueById(7L)).thenReturn(issue);
        when(deliveryService.rerenderSummaryComment(10L, 8))
            .thenReturn(ReviewCommentSyncResult.of(ReviewDeliveryConstants.STATUS_SUCCESS, null, 1L));

        service.close(7L, "确认已修复");

        assertEquals(ReviewIssueConstants.STATUS_CLOSED, issue.getStatus());
        assertEquals(ReviewIssueConstants.CLOSE_SOURCE_AUTO_RECHECK, issue.getCloseSource());
    }

    @Test
    void reopenFromRechecking()
    {
        ReviewIssue issue = openIssue(8L, "SEC", "a.java", "x");
        issue.setStatus(ReviewIssueConstants.STATUS_RECHECKING);
        stubProjectScope(issue);
        when(issueMapper.selectIssueById(8L)).thenReturn(issue);
        when(deliveryService.rerenderSummaryComment(10L, 8))
            .thenReturn(ReviewCommentSyncResult.of(ReviewDeliveryConstants.STATUS_SUCCESS, null, 1L));

        service.reopen(8L);

        assertEquals(ReviewIssueConstants.STATUS_AWAITING_FIX, issue.getStatus());
        ArgumentCaptor<ReviewIssueAction> actionCaptor = ArgumentCaptor.forClass(ReviewIssueAction.class);
        verify(actionMapper).insertAction(actionCaptor.capture());
        assertEquals(ReviewIssueConstants.ACTION_REOPEN, actionCaptor.getValue().getActionType());
    }

    @Test
    void dismissRejectsRechecking()
    {
        ReviewIssue issue = openIssue(9L, "SEC", "a.java", "x");
        issue.setStatus(ReviewIssueConstants.STATUS_RECHECKING);
        stubProjectScope(issue);
        when(issueMapper.selectIssueById(9L)).thenReturn(issue);

        assertThrows(ServiceException.class, () -> service.dismiss(9L, "IGNORED", "nope"));
    }

    @Test
    void confirmThenCloseAndDismissTerminal()
    {
        ReviewIssue issue = openIssue(7L, "SEC", "a.java", "x");
        issue.setStatus(ReviewIssueConstants.STATUS_AWAITING_CONFIRM);
        stubProjectScope(issue);
        when(issueMapper.selectIssueById(7L)).thenReturn(issue);
        when(deliveryService.rerenderSummaryComment(10L, 8))
            .thenReturn(ReviewCommentSyncResult.of(ReviewDeliveryConstants.STATUS_SUCCESS, null, 100L));

        assertEquals(ReviewDeliveryConstants.STATUS_SUCCESS, service.confirm(7L).getStatus());
        assertEquals(ReviewIssueConstants.STATUS_AWAITING_FIX, issue.getStatus());

        when(issueMapper.selectIssueById(7L)).thenReturn(issue);
        service.close(7L, "fixed");
        assertEquals(ReviewIssueConstants.STATUS_CLOSED, issue.getStatus());
        assertEquals(ReviewIssueConstants.CLOSE_SOURCE_MANUAL, issue.getCloseSource());

        assertThrows(ServiceException.class, () -> service.dismiss(7L, "IGNORED", "nope"));
    }

    @Test
    void dismissRequiresNote()
    {
        ReviewIssue issue = openIssue(8L, "SEC", "a.java", "x");
        stubProjectScope(issue);
        when(issueMapper.selectIssueById(8L)).thenReturn(issue);

        assertThrows(ServiceException.class, () -> service.dismiss(8L, "FALSE_POSITIVE", " "));
    }

    @Test
    void dismissFalsePositiveWritesAction()
    {
        ReviewIssue issue = openIssue(9L, "SEC", "a.java", "x");
        stubProjectScope(issue);
        when(issueMapper.selectIssueById(9L)).thenReturn(issue);
        when(deliveryService.rerenderSummaryComment(10L, 8))
            .thenReturn(ReviewCommentSyncResult.of(ReviewDeliveryConstants.STATUS_FAILED, "GitHub API 超时", 88L));

        ReviewCommentSyncResult sync = service.dismiss(9L, "FALSE_POSITIVE", "历史噪声");
        assertEquals(ReviewDeliveryConstants.STATUS_FAILED, sync.getStatus());
        assertEquals(ReviewIssueConstants.STATUS_FALSE_POSITIVE, issue.getStatus());
        ArgumentCaptor<ReviewIssueAction> actionCaptor = ArgumentCaptor.forClass(ReviewIssueAction.class);
        verify(actionMapper).insertAction(actionCaptor.capture());
        assertEquals(ReviewIssueConstants.ACTION_DISMISS, actionCaptor.getValue().getActionType());
        assertNotNull(issue.getClosedTime());
    }

    @Test
    void selectIssueDetailIncludesSummaryDelivery()
    {
        ReviewIssue issue = openIssue(11L, "SEC", "a.java", "x");
        stubProjectScope(issue);
        when(issueMapper.selectIssueById(11L)).thenReturn(issue);
        when(actionMapper.selectByIssueId(11L)).thenReturn(List.of());
        ReviewDeliveryRecord delivery = new ReviewDeliveryRecord();
        delivery.setDeliveryId(55L);
        delivery.setDeliveryStatus(ReviewDeliveryConstants.STATUS_FAILED);
        delivery.setTriggerSource(ReviewDeliveryConstants.TRIGGER_ISSUE_DISPOSITION);
        when(deliveryService.selectSummaryDelivery(10L, 8)).thenReturn(delivery);

        ReviewIssueDetail detail = service.selectIssueDetail(11L);

        assertNotNull(detail.getSummaryDelivery());
        assertEquals(55L, detail.getSummaryDelivery().getDeliveryId());
    }

    @Test
    void selectIssueDetailIncludesFirstAndLastReviewRecords()
    {
        ReviewIssue issue = openIssue(12L, "SEC", "a.java", "x");
        issue.setFirstTaskId(101L);
        issue.setLastTaskId(103L);
        stubProjectScope(issue);
        when(issueMapper.selectIssueById(12L)).thenReturn(issue);
        when(actionMapper.selectByIssueId(12L)).thenReturn(List.of());
        ReviewTask first = successLlmTask(101L, 10L, 8, "sha1");
        ReviewTask last = successLlmTask(103L, 10L, 8, "sha3");
        when(taskMapper.selectReviewTaskById(101L)).thenReturn(first);
        when(taskMapper.selectReviewTaskById(103L)).thenReturn(last);

        ReviewIssueDetail detail = service.selectIssueDetail(12L);

        assertEquals(101L, detail.getFirstTask().getTaskId());
        assertEquals(103L, detail.getLastTask().getTaskId());
        assertEquals(103L, detail.getSourceTask().getTaskId());
    }

    @Test
    void selectRecordContextUsesLatestSuccessfulRunAndKeepsResultOrder()
    {
        ReviewTask task = successLlmTask(201L, 10L, 8, "recordsha");
        when(taskMapper.selectReviewTaskById(201L)).thenReturn(task);
        ReviewIssue first = openIssue(501L, "SEC", "a.java", "first");
        ReviewIssue second = openIssue(502L, "QUALITY", "b.java", "second");
        stubProjectScope(first);

        ReviewTopIssue firstTop = top("SEC", "a.java", "first", 1);
        firstTop.setIssueId(501L);
        ReviewTopIssue secondTop = top("QUALITY", "b.java", "second", 2);
        secondTop.setIssueId(502L);
        ReviewTaskRun failedLatest = runWithIssues(302L, top("SEC", "x.java", "failed", 1));
        failedLatest.setAttemptNo(2);
        failedLatest.setRunStatus(ReviewPipelineConstants.RUN_FAILED);
        ReviewTaskRun success = runWithIssues(301L, firstTop, secondTop);
        success.setAttemptNo(1);
        success.setRunStatus(ReviewPipelineConstants.RUN_SUCCESS);
        when(runMapper.selectRunsByTaskId(201L)).thenReturn(List.of(failedLatest, success));
        when(issueMapper.selectByProjectAndPr(10L, 8, "")).thenReturn(List.of(first, second));
        when(issueMapper.selectIssueList(any())).thenReturn(List.of(second, first));

        ReviewIssue query = new ReviewIssue();
        query.setReviewTaskId(201L);
        ReviewIssueRecordContext context = service.selectRecordContext(query);

        assertEquals(301L, context.getRun().getRunId());
        assertEquals(2, context.getResultIssueCount());
        assertEquals(List.of(501L, 502L), context.getIssues().stream()
            .map(ReviewIssue::getIssueId).collect(Collectors.toList()));
        assertTrue(context.getUntrackedIssues().isEmpty());
        assertEquals(List.of(501L, 502L), query.getIssueIds());
    }

    @Test
    void selectRecordContextReportsHistoricalUntrackedIssues()
    {
        ReviewTask task = successLlmTask(202L, 10L, 8, "recordsha");
        when(taskMapper.selectReviewTaskById(202L)).thenReturn(task);
        ReviewProject project = new ReviewProject();
        project.setProjectId(10L);
        project.setDeptId(1L);
        when(projectMapper.selectReviewProjectById(10L)).thenReturn(project);
        ReviewTaskRun success = runWithIssues(303L, top("SEC", "old.java", "not materialized", 1));
        success.setRunStatus(ReviewPipelineConstants.RUN_SUCCESS);
        when(runMapper.selectRunsByTaskId(202L)).thenReturn(List.of(success));
        when(issueMapper.selectByProjectAndPr(10L, 8, "")).thenReturn(List.of());

        ReviewIssue query = new ReviewIssue();
        query.setReviewTaskId(202L);
        ReviewIssueRecordContext context = service.selectRecordContext(query);

        assertEquals(1, context.getResultIssueCount());
        assertEquals(1, context.getUntrackedIssues().size());
        assertTrue(context.getIssues().isEmpty());
        verify(issueMapper, never()).selectIssueList(any());
    }

    @Test
    void selectRecordContextRejectsRunningTask()
    {
        ReviewTask task = successLlmTask(203L, 10L, 8, "recordsha");
        task.setTaskStatus(ReviewPipelineConstants.TASK_RUNNING);
        when(taskMapper.selectReviewTaskById(203L)).thenReturn(task);
        ReviewIssue query = new ReviewIssue();
        query.setReviewTaskId(203L);

        assertThrows(ServiceException.class, () -> service.selectRecordContext(query));
        verify(runMapper, never()).selectRunsByTaskId(anyLong());
    }

    @Test
    void selectRecordContextKeepsFailedRecordWithEmptyIssueSet()
    {
        ReviewTask task = successLlmTask(204L, 10L, 8, "recordsha");
        task.setTaskStatus(ReviewPipelineConstants.TASK_FAILED);
        when(taskMapper.selectReviewTaskById(204L)).thenReturn(task);
        ReviewProject project = new ReviewProject();
        project.setProjectId(10L);
        project.setDeptId(1L);
        when(projectMapper.selectReviewProjectById(10L)).thenReturn(project);
        ReviewTaskRun failed = runWithIssues(304L);
        failed.setRunStatus(ReviewPipelineConstants.RUN_FAILED);
        when(runMapper.selectRunsByTaskId(204L)).thenReturn(List.of(failed));
        ReviewIssue query = new ReviewIssue();
        query.setReviewTaskId(204L);

        ReviewIssueRecordContext context = service.selectRecordContext(query);

        assertEquals(204L, context.getRecord().getTaskId());
        assertNull(context.getRun());
        assertTrue(context.getIssues().isEmpty());
        verify(issueMapper, never()).selectIssueList(any());
    }

    @Test
    void selectRecordContextRejectsProjectOutsideDataScope()
    {
        ReviewTask task = successLlmTask(205L, 99L, 8, "recordsha");
        when(taskMapper.selectReviewTaskById(205L)).thenReturn(task);
        ReviewProject project = new ReviewProject();
        project.setProjectId(99L);
        project.setDeptId(9L);
        when(projectMapper.selectReviewProjectById(99L)).thenReturn(project);
        doThrow(new ServiceException("没有权限访问部门数据！")).when(deptService).checkDeptDataScope(9L);
        ReviewIssue query = new ReviewIssue();
        query.setReviewTaskId(205L);

        assertThrows(ServiceException.class, () -> service.selectRecordContext(query));
        verify(runMapper, never()).selectRunsByTaskId(anyLong());
    }

    @Test
    void batchConfirmAllAwaitingConfirmTransitionsAndRerendersOnce()
    {
        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class))
        {
            security.when(() -> SecurityUtils.hasPermi(any())).thenReturn(true);
            ReviewIssue a = openIssue(21L, "SEC", "a.java", "one");
            ReviewIssue b = openIssue(22L, "SEC", "b.java", "two");
            stubProjectScope(a);
            stubProjectScope(b);
            when(issueMapper.selectIssueById(21L)).thenReturn(a);
            when(issueMapper.selectIssueById(22L)).thenReturn(b);
            when(deliveryService.rerenderSummaryComment(10L, 8))
                .thenReturn(ReviewCommentSyncResult.of(ReviewDeliveryConstants.STATUS_SUCCESS, null, 1L));

            ReviewIssueBatchRequest req = batchRequest(ReviewIssueConstants.ACTION_CONFIRM, List.of(21L, 22L));
            ReviewIssueBatchResult result = service.batchDispose(req);

            assertFalse(result.hasFailures());
            assertEquals(2, result.getSuccessCount());
            assertEquals(ReviewIssueConstants.STATUS_AWAITING_FIX, a.getStatus());
            assertEquals(ReviewIssueConstants.STATUS_AWAITING_FIX, b.getStatus());
            verify(issueMapper, times(2)).updateIssueDisposition(any());
            verify(deliveryService, times(1)).rerenderSummaryComment(10L, 8);
            assertEquals(ReviewDeliveryConstants.STATUS_SUCCESS, result.getCommentSync().getStatus());
        }
    }

    @Test
    void batchRejectsWhenPreconditionFailsWithoutUpdating()
    {
        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class))
        {
            security.when(() -> SecurityUtils.hasPermi(any())).thenReturn(true);
            ReviewIssue ok = openIssue(31L, "SEC", "a.java", "ok");
            ReviewIssue closed = openIssue(32L, "SEC", "b.java", "done");
            closed.setStatus(ReviewIssueConstants.STATUS_CLOSED);
            stubProjectScope(ok);
            stubProjectScope(closed);
            when(issueMapper.selectIssueById(31L)).thenReturn(ok);
            when(issueMapper.selectIssueById(32L)).thenReturn(closed);

            ReviewIssueBatchResult result = service.batchDispose(
                batchRequest(ReviewIssueConstants.ACTION_CONFIRM, List.of(31L, 32L)));

            assertTrue(result.hasFailures());
            assertEquals(1, result.getFailures().size());
            assertEquals(32L, result.getFailures().get(0).get("issueId"));
            verify(issueMapper, never()).updateIssueDisposition(any());
            verify(deliveryService, never()).rerenderSummaryComment(anyLong(), any());
        }
    }

    @Test
    void batchRejectsWhenDeptOutOfScope()
    {
        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class))
        {
            security.when(() -> SecurityUtils.hasPermi(any())).thenReturn(true);
            ReviewIssue a = openIssue(41L, "SEC", "a.java", "in-scope");
            ReviewIssue b = openIssue(42L, "SEC", "b.java", "out-of-scope");
            b.setProjectId(99L);
            stubProjectScope(a);
            ReviewProject out = new ReviewProject();
            out.setProjectId(99L);
            out.setDeptId(9L);
            when(projectMapper.selectReviewProjectById(99L)).thenReturn(out);
            when(issueMapper.selectIssueById(41L)).thenReturn(a);
            when(issueMapper.selectIssueById(42L)).thenReturn(b);
            lenient().doNothing().when(deptService).checkDeptDataScope(1L);
            doThrow(new ServiceException("没有权限访问部门数据！")).when(deptService).checkDeptDataScope(9L);

            ReviewIssueBatchResult result = service.batchDispose(
                batchRequest(ReviewIssueConstants.ACTION_CONFIRM, List.of(41L, 42L)));

            assertTrue(result.hasFailures());
            assertEquals(42L, result.getFailures().get(0).get("issueId"));
            verify(deptService).checkDeptDataScope(9L);
            verify(issueMapper, never()).updateIssueDisposition(any());
        }
    }

    @Test
    void batchCloseMixedStatusesUsesCorrectCloseSource()
    {
        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class))
        {
            security.when(() -> SecurityUtils.hasPermi(any())).thenReturn(true);
            ReviewIssue fix = openIssue(51L, "SEC", "a.java", "fix");
            fix.setStatus(ReviewIssueConstants.STATUS_AWAITING_FIX);
            ReviewIssue recheck = openIssue(52L, "SEC", "b.java", "recheck");
            recheck.setStatus(ReviewIssueConstants.STATUS_RECHECKING);
            stubProjectScope(fix);
            stubProjectScope(recheck);
            when(issueMapper.selectIssueById(51L)).thenReturn(fix);
            when(issueMapper.selectIssueById(52L)).thenReturn(recheck);
            when(deliveryService.rerenderSummaryComment(10L, 8))
                .thenReturn(ReviewCommentSyncResult.of(ReviewDeliveryConstants.STATUS_SUCCESS, null, 1L));

            ReviewIssueBatchResult result = service.batchDispose(
                batchRequest(ReviewIssueConstants.ACTION_CLOSE, List.of(51L, 52L)));

            assertFalse(result.hasFailures());
            assertEquals(2, result.getSuccessCount());
            ArgumentCaptor<ReviewIssue> captor = ArgumentCaptor.forClass(ReviewIssue.class);
            verify(issueMapper, times(2)).updateIssueDisposition(captor.capture());
            List<ReviewIssue> updated = captor.getAllValues();
            assertEquals(ReviewIssueConstants.CLOSE_SOURCE_MANUAL, updated.get(0).getCloseSource());
            assertEquals(ReviewIssueConstants.CLOSE_SOURCE_AUTO_RECHECK, updated.get(1).getCloseSource());
        }
    }

    @Test
    void batchDismissRequiresNoteAndRejectsRechecking()
    {
        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class))
        {
            security.when(() -> SecurityUtils.hasPermi(any())).thenReturn(true);
            ReviewIssueBatchRequest missingNote = batchRequest(ReviewIssueConstants.ACTION_DISMISS, List.of(61L));
            missingNote.setDismissType(ReviewIssueConstants.STATUS_IGNORED);
            assertThrows(ServiceException.class, () -> service.batchDispose(missingNote));

            ReviewIssue recheck = openIssue(62L, "SEC", "b.java", "recheck");
            recheck.setStatus(ReviewIssueConstants.STATUS_RECHECKING);
            stubProjectScope(recheck);
            when(issueMapper.selectIssueById(62L)).thenReturn(recheck);
            ReviewIssueBatchRequest withRecheck = batchRequest(ReviewIssueConstants.ACTION_DISMISS, List.of(62L));
            withRecheck.setDismissType(ReviewIssueConstants.STATUS_IGNORED);
            withRecheck.setResolveNote("批量忽略");
            ReviewIssueBatchResult result = service.batchDispose(withRecheck);
            assertTrue(result.hasFailures());
            assertEquals(62L, result.getFailures().get(0).get("issueId"));
            verify(issueMapper, never()).updateIssueDisposition(any());
        }
    }

    @Test
    void batchRejectsEmptyOrOverLimitIssueIds()
    {
        assertThrows(ServiceException.class, () ->
            service.batchDispose(batchRequest(ReviewIssueConstants.ACTION_CONFIRM, List.of())));
        assertThrows(ServiceException.class, () ->
            service.batchDispose(batchRequest(ReviewIssueConstants.ACTION_CONFIRM, null)));

        List<Long> tooMany = LongStream.rangeClosed(1, 201).boxed().collect(Collectors.toList());
        assertThrows(ServiceException.class, () ->
            service.batchDispose(batchRequest(ReviewIssueConstants.ACTION_CONFIRM, tooMany)));
    }

    @Test
    void batchCloseRejectedWithoutClosePermission()
    {
        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class))
        {
            security.when(() -> SecurityUtils.hasPermi("review:issue:close")).thenReturn(false);

            assertThrows(ServiceException.class, () ->
                service.batchDispose(batchRequest(ReviewIssueConstants.ACTION_CLOSE, List.of(71L))));
            verify(issueMapper, never()).updateIssueDisposition(any());
        }
    }

    @Test
    void closeActiveIssuesForPrClosesAllActiveWithMergedSourceAndActions()
    {
        ReviewIssue confirm = openIssue(81L, "SEC", "a.java", "confirm");
        ReviewIssue fix = openIssue(82L, "SEC", "b.java", "fix");
        fix.setStatus(ReviewIssueConstants.STATUS_AWAITING_FIX);
        ReviewIssue recheck = openIssue(83L, "SEC", "c.java", "recheck");
        recheck.setStatus(ReviewIssueConstants.STATUS_RECHECKING);
        when(issueMapper.selectByProjectAndPr(10L, 8, null)).thenReturn(List.of(confirm, fix, recheck));

        int closed = service.closeActiveIssuesForPr(10L, 8, true);

        assertEquals(3, closed);
        assertEquals(ReviewIssueConstants.STATUS_CLOSED, confirm.getStatus());
        assertEquals(ReviewIssueConstants.STATUS_CLOSED, fix.getStatus());
        assertEquals(ReviewIssueConstants.STATUS_CLOSED, recheck.getStatus());
        assertEquals(ReviewIssueConstants.CLOSE_SOURCE_PR_MERGED, confirm.getCloseSource());
        assertEquals(ReviewIssueConstants.CLOSE_SOURCE_PR_MERGED, fix.getCloseSource());
        assertEquals(ReviewIssueConstants.CLOSE_SOURCE_PR_MERGED, recheck.getCloseSource());
        assertEquals(ReviewIssueConstants.OPERATOR_SYSTEM, confirm.getClosedBy());
        assertEquals("合并请求已合并，问题随之关闭", confirm.getResolveNote());
        verify(issueMapper, times(3)).updateIssueDisposition(any());
        ArgumentCaptor<ReviewIssueAction> actionCaptor = ArgumentCaptor.forClass(ReviewIssueAction.class);
        verify(actionMapper, times(3)).insertAction(actionCaptor.capture());
        for (ReviewIssueAction action : actionCaptor.getAllValues())
        {
            assertEquals(ReviewIssueConstants.OPERATOR_SYSTEM, action.getOperator());
            assertEquals(ReviewIssueConstants.ACTION_CLOSE, action.getActionType());
            assertEquals(ReviewIssueConstants.STATUS_CLOSED, action.getToStatus());
        }
        assertEquals(ReviewIssueConstants.STATUS_AWAITING_CONFIRM, actionCaptor.getAllValues().get(0).getFromStatus());
        assertEquals(ReviewIssueConstants.STATUS_AWAITING_FIX, actionCaptor.getAllValues().get(1).getFromStatus());
        assertEquals(ReviewIssueConstants.STATUS_RECHECKING, actionCaptor.getAllValues().get(2).getFromStatus());
        verify(deliveryService, never()).rerenderSummaryComment(anyLong(), any());
    }

    @Test
    void closeActiveIssuesForPrUsesPrClosedSourceWhenNotMerged()
    {
        ReviewIssue issue = openIssue(84L, "SEC", "a.java", "open");
        when(issueMapper.selectByProjectAndPr(10L, 8, null)).thenReturn(List.of(issue));

        int closed = service.closeActiveIssuesForPr(10L, 8, false);

        assertEquals(1, closed);
        assertEquals(ReviewIssueConstants.CLOSE_SOURCE_PR_CLOSED, issue.getCloseSource());
        assertEquals("合并请求已关闭，问题随之关闭", issue.getResolveNote());
    }

    @Test
    void closeActiveIssuesForPrSkipsTerminalAndIsIdempotent()
    {
        ReviewIssue closed = openIssue(85L, "SEC", "a.java", "done");
        closed.setStatus(ReviewIssueConstants.STATUS_CLOSED);
        ReviewIssue ignored = openIssue(86L, "SEC", "b.java", "ignored");
        ignored.setStatus(ReviewIssueConstants.STATUS_IGNORED);
        when(issueMapper.selectByProjectAndPr(10L, 8, null)).thenReturn(List.of(closed, ignored));

        assertEquals(0, service.closeActiveIssuesForPr(10L, 8, true));
        verify(issueMapper, never()).updateIssueDisposition(any());
        verify(actionMapper, never()).insertAction(any());
    }

    @Test
    void closeActiveIssuesForPrReturnsZeroWhenNoIssues()
    {
        when(issueMapper.selectByProjectAndPr(10L, 8, null)).thenReturn(List.of());
        assertEquals(0, service.closeActiveIssuesForPr(10L, 8, false));
        assertEquals(0, service.closeActiveIssuesForPr(null, 8, false));
        assertEquals(0, service.closeActiveIssuesForPr(10L, null, true));
        verify(issueMapper, never()).updateIssueDisposition(any());
    }

    @Test
    void pr3RegressionPrototypeThreeRounds()
    {
        // round1: 物化 3 条
        ReviewTask t1 = successLlmTask(1L, 10L, 3, "sha1");
        ReviewTaskRun r1 = runWithIssues(1L,
            top("SEC", "a.java", "sql-injection", 1),
            top("SEC", "b.java", "cmd-injection", 2),
            top("SEC", "c.java", "hardcoded-password", 3));
        List<ReviewIssue> ledger = new ArrayList<>();
        when(issueMapper.selectByProjectAndPr(10L, 3, "")).thenReturn(ledger);
        when(issueMapper.insertIssue(any())).thenAnswer(inv -> {
            ReviewIssue created = inv.getArgument(0);
            // 仅回填 ID；入账列表由 reconcile Pass 3 的 allIssues.add 维护，避免双写
            created.setIssueId((long) (ledger.size() + 1));
            return 1;
        });

        ReviewRoundReconcileResult round1 = service.reconcileAfterSuccess(t1, r1);
        assertEquals(3, round1.getNewlyMaterialized().size());
        assertTrue(ledger.stream().allMatch(i -> ReviewIssueConstants.STATUS_AWAITING_CONFIRM.equals(i.getStatus())));

        // 人工确认
        for (ReviewIssue issue : ledger)
        {
            issue.setStatus(ReviewIssueConstants.STATUS_AWAITING_FIX);
        }

        // round2: 全未命中 + 新发现 3 条
        ReviewTask t2 = successLlmTask(2L, 10L, 3, "sha2");
        ReviewTaskRun r2 = runWithIssues(2L,
            top("QUALITY", "d.java", "naming", 1),
            top("QUALITY", "e.java", "complexity", 2),
            top("QUALITY", "f.java", "dup", 3));
        ReviewRoundReconcileResult round2 = service.reconcileAfterSuccess(t2, r2);
        assertEquals(3, round2.getMovedToRechecking().size());
        assertEquals(3, round2.getNewlyMaterialized().size());
        assertEquals(3, ledger.stream().filter(i -> ReviewIssueConstants.STATUS_RECHECKING.equals(i.getStatus())).count());
        assertEquals(3, ledger.stream().filter(i -> ReviewIssueConstants.STATUS_AWAITING_CONFIRM.equals(i.getStatus())).count());
        for (ReviewIssue issue : round2.getMovedToRechecking())
        {
            assertNotNull(issue.getRecheckTaskId());
            assertNotNull(issue.getRecheckRunId());
            assertEquals("sha2", issue.getRecheckCommitSha());
        }

        // round3: 命中原问题 1 条 + 命中 round2 新问题，其余 2 条保持 RECHECKING
        ReviewTask t3 = successLlmTask(3L, 10L, 3, "sha3");
        ReviewTaskRun r3 = runWithIssues(3L,
            top("SEC", "a.java", "sql-injection", 1),
            top("QUALITY", "d.java", "naming", 2),
            top("QUALITY", "e.java", "complexity", 3),
            top("QUALITY", "f.java", "dup", 4));
        ReviewRoundReconcileResult round3 = service.reconcileAfterSuccess(t3, r3);
        assertEquals(1, round3.getReopened().size());
        assertEquals(ReviewIssueConstants.STATUS_AWAITING_FIX, round3.getReopened().get(0).getStatus());
        assertEquals("sql-injection", round3.getReopened().get(0).getTitle());
        assertEquals(2, ledger.stream().filter(i -> ReviewIssueConstants.STATUS_RECHECKING.equals(i.getStatus())).count());
    }

    @Test
    void reconcilePushTaskGroupsByRefBranchAndMaterializesRefBranch()
    {
        ReviewTask task = successLlmTask(80L, 10L, 0, "pushsha1");
        task.setEventSource(ReviewPipelineConstants.EVENT_SOURCE_PUSH);
        task.setTargetBranch("main");
        ReviewTaskRun run = runWithIssues(180L, top("SEC", "a.java", "leak", 1));
        when(issueMapper.selectByProjectAndPr(10L, 0, "main")).thenReturn(new ArrayList<>());
        when(issueMapper.insertIssue(any())).thenAnswer(inv -> {
            ReviewIssue created = inv.getArgument(0);
            created.setIssueId(801L);
            return 1;
        });

        ReviewRoundReconcileResult result = service.reconcileAfterSuccess(task, run);

        verify(issueMapper).selectByProjectAndPr(10L, 0, "main");
        ArgumentCaptor<ReviewIssue> captor = ArgumentCaptor.forClass(ReviewIssue.class);
        verify(issueMapper).insertIssue(captor.capture());
        assertEquals("main", captor.getValue().getRefBranch());
        assertEquals(0, captor.getValue().getPrNumber());
        assertEquals(1, result.getNewlyMaterialized().size());
    }

    @Test
    void closeActiveIssuesForPrIgnoresSentinelZero()
    {
        assertEquals(0, service.closeActiveIssuesForPr(10L, 0, true));
        assertEquals(0, service.closeActiveIssuesForPr(10L, 0, false));
        verify(issueMapper, never()).selectByProjectAndPr(any(), any(), any());
        verify(issueMapper, never()).updateIssueDisposition(any());
    }

    private static ReviewIssueBatchRequest batchRequest(String action, List<Long> issueIds)
    {
        ReviewIssueBatchRequest req = new ReviewIssueBatchRequest();
        req.setAction(action);
        req.setIssueIds(issueIds);
        return req;
    }

    private void stubProjectScope(ReviewIssue issue)
    {
        ReviewProject project = new ReviewProject();
        project.setProjectId(issue.getProjectId());
        project.setDeptId(1L);
        when(projectMapper.selectReviewProjectById(issue.getProjectId())).thenReturn(project);
    }

    private static ReviewIssue openIssue(Long id, String category, String path, String title)
    {
        ReviewIssue issue = new ReviewIssue();
        issue.setIssueId(id);
        issue.setProjectId(10L);
        issue.setPrNumber(8);
        issue.setFingerprint(ReviewIssueFingerprint.of(category, path, title));
        issue.setFamilyKey(ReviewIssueFingerprint.familyKey(path, category));
        issue.setCategory(category);
        issue.setFilePath(path);
        issue.setStatus(ReviewIssueConstants.STATUS_AWAITING_CONFIRM);
        issue.setTitle(title);
        issue.setOrigin(ReviewIssueConstants.ORIGIN_NEW);
        issue.setMissedStreak(0);
        issue.setFirstTaskId(1L);
        issue.setLastTaskId(1L);
        return issue;
    }

    private static ReviewTask successLlmTask(Long taskId, Long projectId, int pr, String headSha)
    {
        ReviewTask task = new ReviewTask();
        task.setTaskId(taskId);
        task.setProjectId(projectId);
        task.setPrNumber(pr);
        task.setTaskStatus(ReviewPipelineConstants.TASK_SUCCESS);
        task.setParseStatus(ReviewScoringConstants.PARSE_SUCCESS);
        task.setSnapshotReviewMode(ReviewPipelineConstants.REVIEW_MODE_LLM_DIRECT);
        task.setProvider("GITHUB");
        task.setHeadSha(headSha);
        return task;
    }

    private static ReviewTaskRun runWithIssues(Long runId, ReviewTopIssue... issues)
    {
        ReviewTaskRun run = new ReviewTaskRun();
        run.setRunId(runId);
        run.setRunStatus(ReviewPipelineConstants.RUN_SUCCESS);
        run.setParseStatus(ReviewScoringConstants.PARSE_SUCCESS);
        run.setTopIssuesJson(JSON.toJSONString(List.of(issues)));
        return run;
    }

    private static ReviewTopIssue top(String category, String path, String title, int rank)
    {
        ReviewTopIssue issue = new ReviewTopIssue();
        issue.setCategory(category);
        issue.setFilePath(path);
        issue.setTitle(title);
        issue.setRank(rank);
        issue.setSeverity("HIGH");
        issue.setOrigin("NEW");
        return issue;
    }
}
