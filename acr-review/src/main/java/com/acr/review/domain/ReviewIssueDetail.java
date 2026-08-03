package com.acr.review.domain;

import java.util.ArrayList;
import java.util.List;

/** 问题详情：快照 + 来源任务摘要 + 动作时间线。 */
public class ReviewIssueDetail
{
    private ReviewIssue issue;
    private ReviewTask sourceTask;
    private List<ReviewIssueAction> actions = new ArrayList<>();
    /** 评论同步结果：SUCCESS / FAILED / SKIPPED，可选。 */
    private String commentSyncStatus;

    public ReviewIssue getIssue() { return issue; }
    public void setIssue(ReviewIssue issue) { this.issue = issue; }
    public ReviewTask getSourceTask() { return sourceTask; }
    public void setSourceTask(ReviewTask sourceTask) { this.sourceTask = sourceTask; }
    public List<ReviewIssueAction> getActions() { return actions; }
    public void setActions(List<ReviewIssueAction> actions) { this.actions = actions; }
    public String getCommentSyncStatus() { return commentSyncStatus; }
    public void setCommentSyncStatus(String commentSyncStatus) { this.commentSyncStatus = commentSyncStatus; }
}
