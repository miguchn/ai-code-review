package com.acr.review.domain;

import java.util.ArrayList;
import java.util.List;
import com.acr.review.domain.result.ReviewTopIssue;

/** 按一次审查记录查看的问题台账上下文。 */
public class ReviewIssueRecordContext
{
    private ReviewTask record;
    private ReviewTaskRun run;
    private List<ReviewIssue> issues = new ArrayList<>();
    private List<ReviewTopIssue> untrackedIssues = new ArrayList<>();
    private int resultIssueCount;

    public ReviewTask getRecord() { return record; }
    public void setRecord(ReviewTask record) { this.record = record; }
    public ReviewTaskRun getRun() { return run; }
    public void setRun(ReviewTaskRun run) { this.run = run; }
    public List<ReviewIssue> getIssues() { return issues; }
    public void setIssues(List<ReviewIssue> issues) { this.issues = issues; }
    public List<ReviewTopIssue> getUntrackedIssues() { return untrackedIssues; }
    public void setUntrackedIssues(List<ReviewTopIssue> untrackedIssues) { this.untrackedIssues = untrackedIssues; }
    public int getResultIssueCount() { return resultIssueCount; }
    public void setResultIssueCount(int resultIssueCount) { this.resultIssueCount = resultIssueCount; }
}
