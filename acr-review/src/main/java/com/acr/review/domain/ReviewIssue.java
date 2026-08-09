package com.acr.review.domain;

import java.util.Date;
import java.util.List;
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
    /** 参考分支：PR 线空串；push 线为推送分支名。 */
    private String refBranch;
    private String fingerprint;
    private String familyKey;
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

    private Integer missedStreak;
    private String lastSeenHeadSha;
    private Long lastMissedRunId;
    private Long recheckTaskId;
    private Long recheckRunId;
    private String recheckCommitSha;

    /** 列表展示 */
    private String projectName;
    private String businessSystemName;
    private String eventSource;
    private String prTitle;
    private String prAuthor;
    private String sourceBranch;
    private String targetBranch;
    private String headSha;
    private String repositoryOwner;
    private String repositoryName;
    private String repositoryUrl;

    /** 查询专用：按审查记录查看时由服务层设置精确问题 ID 集合。 */
    private Long reviewTaskId;
    private List<Long> issueIds;
    private String branchKeyword;
    private String keyword;
    private String beginTime;
    private String endTime;
    /**
     * 活跃视图标志：Y 时筛选 AWAITING_CONFIRM / AWAITING_FIX / RECHECKING；
     * 与 status 并存时 status 优先。
     */
    private String activeFlag;
    /**
     * 待人工处置筛选：Y 时筛选 AWAITING_CONFIRM / RECHECKING；
     * 与 status 并存时 status 优先。
     */
    private String pendingOnly;
    /**
     * 终态筛选：Y 时筛选 CLOSED / IGNORED / FALSE_POSITIVE；
     * 与 status 并存时 status 优先。
     */
    private String closedFlag;

    /** 当前阶段进入时间（最后一次状态变更；无则回退 create_time）。 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date stageEnteredTime;

    public Long getIssueId() { return issueId; }
    public void setIssueId(Long issueId) { this.issueId = issueId; }
    public Long getProjectId() { return projectId; }
    public void setProjectId(Long projectId) { this.projectId = projectId; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public Integer getPrNumber() { return prNumber; }
    public void setPrNumber(Integer prNumber) { this.prNumber = prNumber; }
    public String getRefBranch() { return refBranch; }
    public void setRefBranch(String refBranch) { this.refBranch = refBranch; }
    public String getFingerprint() { return fingerprint; }
    public void setFingerprint(String fingerprint) { this.fingerprint = fingerprint; }
    public String getFamilyKey() { return familyKey; }
    public void setFamilyKey(String familyKey) { this.familyKey = familyKey; }
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
    public Integer getMissedStreak() { return missedStreak; }
    public void setMissedStreak(Integer missedStreak) { this.missedStreak = missedStreak; }
    public String getLastSeenHeadSha() { return lastSeenHeadSha; }
    public void setLastSeenHeadSha(String lastSeenHeadSha) { this.lastSeenHeadSha = lastSeenHeadSha; }
    public Long getLastMissedRunId() { return lastMissedRunId; }
    public void setLastMissedRunId(Long lastMissedRunId) { this.lastMissedRunId = lastMissedRunId; }
    public Long getRecheckTaskId() { return recheckTaskId; }
    public void setRecheckTaskId(Long recheckTaskId) { this.recheckTaskId = recheckTaskId; }
    public Long getRecheckRunId() { return recheckRunId; }
    public void setRecheckRunId(Long recheckRunId) { this.recheckRunId = recheckRunId; }
    public String getRecheckCommitSha() { return recheckCommitSha; }
    public void setRecheckCommitSha(String recheckCommitSha) { this.recheckCommitSha = recheckCommitSha; }
    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }
    public String getBusinessSystemName() { return businessSystemName; }
    public void setBusinessSystemName(String businessSystemName) { this.businessSystemName = businessSystemName; }
    public String getEventSource() { return eventSource; }
    public void setEventSource(String eventSource) { this.eventSource = eventSource; }
    public String getPrTitle() { return prTitle; }
    public void setPrTitle(String prTitle) { this.prTitle = prTitle; }
    public String getPrAuthor() { return prAuthor; }
    public void setPrAuthor(String prAuthor) { this.prAuthor = prAuthor; }
    public String getSourceBranch() { return sourceBranch; }
    public void setSourceBranch(String sourceBranch) { this.sourceBranch = sourceBranch; }
    public String getTargetBranch() { return targetBranch; }
    public void setTargetBranch(String targetBranch) { this.targetBranch = targetBranch; }
    public String getHeadSha() { return headSha; }
    public void setHeadSha(String headSha) { this.headSha = headSha; }
    public String getRepositoryOwner() { return repositoryOwner; }
    public void setRepositoryOwner(String repositoryOwner) { this.repositoryOwner = repositoryOwner; }
    public String getRepositoryName() { return repositoryName; }
    public void setRepositoryName(String repositoryName) { this.repositoryName = repositoryName; }
    public String getRepositoryUrl() { return repositoryUrl; }
    public void setRepositoryUrl(String repositoryUrl) { this.repositoryUrl = repositoryUrl; }
    public Long getReviewTaskId() { return reviewTaskId; }
    public void setReviewTaskId(Long reviewTaskId) { this.reviewTaskId = reviewTaskId; }
    public List<Long> getIssueIds() { return issueIds; }
    public void setIssueIds(List<Long> issueIds) { this.issueIds = issueIds; }
    public String getBranchKeyword() { return branchKeyword; }
    public void setBranchKeyword(String branchKeyword) { this.branchKeyword = branchKeyword; }
    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
    public String getBeginTime() { return beginTime; }
    public void setBeginTime(String beginTime) { this.beginTime = beginTime; }
    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }
    public String getActiveFlag() { return activeFlag; }
    public void setActiveFlag(String activeFlag) { this.activeFlag = activeFlag; }
    public String getPendingOnly() { return pendingOnly; }
    public void setPendingOnly(String pendingOnly) { this.pendingOnly = pendingOnly; }
    public String getClosedFlag() { return closedFlag; }
    public void setClosedFlag(String closedFlag) { this.closedFlag = closedFlag; }
    public Date getStageEnteredTime() { return stageEnteredTime; }
    public void setStageEnteredTime(Date stageEnteredTime) { this.stageEnteredTime = stageEnteredTime; }
}
