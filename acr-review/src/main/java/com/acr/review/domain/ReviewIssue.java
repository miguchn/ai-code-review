package com.acr.review.domain;

import java.util.Date;
import com.acr.common.core.domain.BaseEntity;
import com.fasterxml.jackson.annotation.JsonFormat;

/** 审查问题台账 review_issue。 */
public class ReviewIssue extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long issueId;
    private Long projectId;
    private String provider;
    private Integer prNumber;
    private String fingerprint;
    private Long firstTaskId;
    private Long firstRunId;
    private Long lastTaskId;
    private Long lastRunId;
    private Integer issueRank;
    private String severity;
    private String category;
    private String title;
    private String description;
    private String filePath;
    private Integer startLine;
    private Integer endLine;
    private String evidence;
    private String suggestion;
    private String origin;
    private String status;
    private String resolveNote;
    private String closeSource;
    private String closedBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date closedTime;

    /** 列表展示 */
    private String projectName;
    private String keyword;
    private String beginTime;
    private String endTime;

    public Long getIssueId() { return issueId; }
    public void setIssueId(Long issueId) { this.issueId = issueId; }
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public Integer getPrNumber() { return prNumber; }
    public void setPrNumber(Integer prNumber) { this.prNumber = prNumber; }
    public String getFingerprint() { return fingerprint; }
    public void setFingerprint(String fingerprint) { this.fingerprint = fingerprint; }
    public Long getFirstTaskId() { return firstTaskId; }
    public void setFirstTaskId(Long firstTaskId) { this.firstTaskId = firstTaskId; }
    public Long getFirstRunId() { return firstRunId; }
    public void setFirstRunId(Long firstRunId) { this.firstRunId = firstRunId; }
    public Long getLastTaskId() { return lastTaskId; }
    public void setLastTaskId(Long lastTaskId) { this.lastTaskId = lastTaskId; }
    public Long getLastRunId() { return lastRunId; }
    public void setLastRunId(Long lastRunId) { this.lastRunId = lastRunId; }
    public Integer getIssueRank() { return issueRank; }
    public void setIssueRank(Integer issueRank) { this.issueRank = issueRank; }
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public Integer getStartLine() { return startLine; }
    public void setStartLine(Integer startLine) { this.startLine = startLine; }
    public Integer getEndLine() { return endLine; }
    public void setEndLine(Integer endLine) { this.endLine = endLine; }
    public String getEvidence() { return evidence; }
    public void setEvidence(String evidence) { this.evidence = evidence; }
    public String getSuggestion() { return suggestion; }
    public void setSuggestion(String suggestion) { this.suggestion = suggestion; }
    public String getOrigin() { return origin; }
    public void setOrigin(String origin) { this.origin = origin; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getResolveNote() { return resolveNote; }
    public void setResolveNote(String resolveNote) { this.resolveNote = resolveNote; }
    public String getCloseSource() { return closeSource; }
    public void setCloseSource(String closeSource) { this.closeSource = closeSource; }
    public String getClosedBy() { return closedBy; }
    public void setClosedBy(String closedBy) { this.closedBy = closedBy; }
    public Date getClosedTime() { return closedTime; }
    public void setClosedTime(Date closedTime) { this.closedTime = closedTime; }
    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }
    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
    public String getBeginTime() { return beginTime; }
    public void setBeginTime(String beginTime) { this.beginTime = beginTime; }
    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }
}
