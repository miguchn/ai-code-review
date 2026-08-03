package com.acr.review.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;

/** 审查问题动作流水 review_issue_action。 */
public class ReviewIssueAction
{
    private Long actionId;
    private Long issueId;
    private String operator;
    private String actionType;
    private String fromStatus;
    private String toStatus;
    private String resolveNote;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    public Long getActionId() { return actionId; }
    public void setActionId(Long actionId) { this.actionId = actionId; }
    public Long getIssueId() { return issueId; }
    public void setIssueId(Long issueId) { this.issueId = issueId; }
    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }
    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = actionType; }
    public String getFromStatus() { return fromStatus; }
    public void setFromStatus(String fromStatus) { this.fromStatus = fromStatus; }
    public String getToStatus() { return toStatus; }
    public void setToStatus(String toStatus) { this.toStatus = toStatus; }
    public String getResolveNote() { return resolveNote; }
    public void setResolveNote(String resolveNote) { this.resolveNote = resolveNote; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }
}
