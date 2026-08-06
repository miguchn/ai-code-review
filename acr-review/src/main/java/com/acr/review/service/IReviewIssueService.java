package com.acr.review.service;

import java.util.List;
import java.util.Map;
import com.acr.review.domain.ReviewCommentSyncResult;
import com.acr.review.domain.ReviewIssue;
import com.acr.review.domain.ReviewIssueBatchRequest;
import com.acr.review.domain.ReviewIssueBatchResult;
import com.acr.review.domain.ReviewIssueDetail;
import com.acr.review.domain.ReviewIssueStats;
import com.acr.review.domain.ReviewRoundReconcileResult;
import com.acr.review.domain.ReviewTask;
import com.acr.review.domain.ReviewTaskRun;
import com.acr.review.domain.result.ReviewTopIssue;

/** 问题台账：对账物化、查询与处置。 */
public interface IReviewIssueService
{
    /**
     * SUCCESS 后轮次对账（命中/未命中/新物化/转复核/复活）。
     * 仅 LLM_DIRECT 且 parseStatus=SUCCESS 执行；失败由调用方隔离，不影响任务状态。
     */
    ReviewRoundReconcileResult reconcileAfterSuccess(ReviewTask task, ReviewTaskRun run);

    List<ReviewIssue> selectIssueList(ReviewIssue query);

    ReviewIssueDetail selectIssueDetail(Long issueId);

    /** 确认：待确认 → 待修复。返回评论同步结果。 */
    ReviewCommentSyncResult confirm(Long issueId);

    /** 关闭 → CLOSED；RECHECKING 关闭时 close_source=auto_recheck。 */
    ReviewCommentSyncResult close(Long issueId, String resolveNote);

    /**
     * PR 关闭/合并时批量关闭名下活跃问题（AWAITING_CONFIRM / AWAITING_FIX / RECHECKING）。
     * 终态行不碰；operator=system；不触发评论重渲染。
     *
     * @param merged true 表示已合并（closeSource=pr_merged），false 为关闭未合并（pr_closed）
     * @return 实际关闭条数
     */
    int closeActiveIssuesForPr(Long projectId, Integer prNumber, boolean merged);

    /** 忽略/误报；dismissType=IGNORED|FALSE_POSITIVE；resolveNote 必填；RECHECKING 拒绝。 */
    ReviewCommentSyncResult dismiss(Long issueId, String dismissType, String resolveNote);

    /** 人工重开：RECHECKING → AWAITING_FIX。 */
    ReviewCommentSyncResult reopen(Long issueId);

    /**
     * 批量处置：事务内全有或全无预校验，通过后写状态与 action；
     * 评论重渲染按 projectId+prNumber 去重。
     */
    ReviewIssueBatchResult batchDispose(ReviewIssueBatchRequest request);

    /**
     * 当前 PR 下 RECHECKING 问题标题列表（处置重渲染 / 人工重试派生「疑似已修复」段）。
     * 顺序与 selectByProjectAndPr 一致。
     */
    List<String> listRecheckingTitles(Long projectId, Integer prNumber);

    /** 按 PR 加载问题集合（指纹 → issue）。 */
    Map<String, ReviewIssue> mapByFingerprint(Long projectId, Integer prNumber);

    /** 给问题清单附加 issueId / 处置态。 */
    void enrichTopIssues(List<ReviewTopIssue> topIssues, Long projectId, Integer prNumber);

    /**
     * 给 runs 的 topIssuesJson 附加台账处置态（仅写回响应对象，不落库）。
     */
    void enrichRuns(List<ReviewTaskRun> runs, Long projectId, Integer prNumber);

    /** 未关闭新增问题数（含待复核，排除 EXISTING）。 */
    int countOpenNewByProject(Long projectId);

    /** 与台账列表同口径计数（含 DataScope）。 */
    int countIssueList(ReviewIssue query);

    /** 今日关闭问题数（CLOSED + CURDATE）；query 仅承载 DataScope。 */
    int countClosedToday(ReviewIssue query);

    /** 状态总览五计数；query 仅承载 DataScope。 */
    ReviewIssueStats selectIssueStats(ReviewIssue query);
}
