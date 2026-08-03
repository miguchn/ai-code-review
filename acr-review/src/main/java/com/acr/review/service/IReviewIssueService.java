package com.acr.review.service;

import java.util.List;
import java.util.Map;
import com.acr.review.domain.ReviewCommentSyncResult;
import com.acr.review.domain.ReviewIssue;
import com.acr.review.domain.ReviewIssueDetail;
import com.acr.review.domain.ReviewTask;
import com.acr.review.domain.ReviewTaskRun;
import com.acr.review.domain.result.ReviewTopIssue;

/** 问题台账：物化、查询与处置。 */
public interface IReviewIssueService
{
    /** SUCCESS 后物化 Top3；失败不影响任务状态。 */
    void materializeAfterSuccess(ReviewTask task, ReviewTaskRun run);

    List<ReviewIssue> selectIssueList(ReviewIssue query);

    ReviewIssueDetail selectIssueDetail(Long issueId);

    /** 确认：待确认 → 待修复。返回评论同步结果。 */
    ReviewCommentSyncResult confirm(Long issueId);

    /** 关闭 → CLOSED；resolveNote 选填。返回评论同步结果。 */
    ReviewCommentSyncResult close(Long issueId, String resolveNote);

    /** 忽略/误报；dismissType=IGNORED|FALSE_POSITIVE；resolveNote 必填。 */
    ReviewCommentSyncResult dismiss(Long issueId, String dismissType, String resolveNote);

    /** 按 PR 加载问题集合（指纹 → issue）。 */
    Map<String, ReviewIssue> mapByFingerprint(Long projectId, Integer prNumber);

    /** 给 Top3 附加 issueId / 处置态。 */
    void enrichTopIssues(List<ReviewTopIssue> topIssues, Long projectId, Integer prNumber);

    /**
     * 给 runs 的 topIssuesJson 附加台账处置态（仅写回响应对象，不落库）。
     */
    void enrichRuns(List<ReviewTaskRun> runs, Long projectId, Integer prNumber);

    /** 未关闭新增问题数（排除 EXISTING）。 */
    int countOpenNewByProject(Long projectId);
}
