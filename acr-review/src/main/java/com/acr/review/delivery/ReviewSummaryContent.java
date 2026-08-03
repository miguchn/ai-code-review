package com.acr.review.delivery;

import java.util.List;
import com.acr.review.domain.result.ReviewScopeStats;
import com.acr.review.domain.result.ReviewTopIssue;

/** 审查结论摘要共享内容模型（GitHub 评论与 IM 同源）。 */
public final class ReviewSummaryContent
{
    private final String taskStatus;
    private final Long taskId;
    private final String conclusion;
    private final String conclusionLabel;
    private final Integer totalScore;
    private final String headShaShort;
    private final Integer prNumber;
    private final String prTitle;
    private final String prAuthor;
    private final String repositoryOwner;
    private final String repositoryName;
    private final String sourceBranch;
    private final String targetBranch;
    private final Integer changedFiles;
    private final Integer additions;
    private final Integer deletions;
    private final List<ReviewTopIssue> topIssues;
    private final ReviewScopeStats scopeStats;
    private final String prUrl;
    private final String detailUrl;
    private final String failureType;
    private final String failureTypeLabel;

    private ReviewSummaryContent(Builder builder)
    {
        this.taskStatus = builder.taskStatus;
        this.taskId = builder.taskId;
        this.conclusion = builder.conclusion;
        this.conclusionLabel = builder.conclusionLabel;
        this.totalScore = builder.totalScore;
        this.headShaShort = builder.headShaShort;
        this.prNumber = builder.prNumber;
        this.prTitle = builder.prTitle;
        this.prAuthor = builder.prAuthor;
        this.repositoryOwner = builder.repositoryOwner;
        this.repositoryName = builder.repositoryName;
        this.sourceBranch = builder.sourceBranch;
        this.targetBranch = builder.targetBranch;
        this.changedFiles = builder.changedFiles;
        this.additions = builder.additions;
        this.deletions = builder.deletions;
        this.topIssues = builder.topIssues == null ? List.of() : List.copyOf(builder.topIssues);
        this.scopeStats = builder.scopeStats;
        this.prUrl = builder.prUrl;
        this.detailUrl = builder.detailUrl;
        this.failureType = builder.failureType;
        this.failureTypeLabel = builder.failureTypeLabel;
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public String getTaskStatus() { return taskStatus; }
    public Long getTaskId() { return taskId; }
    public String getConclusion() { return conclusion; }
    public String getConclusionLabel() { return conclusionLabel; }
    public Integer getTotalScore() { return totalScore; }
    public String getHeadShaShort() { return headShaShort; }
    public Integer getPrNumber() { return prNumber; }
    public String getPrTitle() { return prTitle; }
    public String getPrAuthor() { return prAuthor; }
    public String getRepositoryOwner() { return repositoryOwner; }
    public String getRepositoryName() { return repositoryName; }
    public String getSourceBranch() { return sourceBranch; }
    public String getTargetBranch() { return targetBranch; }
    public Integer getChangedFiles() { return changedFiles; }
    public Integer getAdditions() { return additions; }
    public Integer getDeletions() { return deletions; }
    public List<ReviewTopIssue> getTopIssues() { return topIssues; }
    public ReviewScopeStats getScopeStats() { return scopeStats; }
    public String getPrUrl() { return prUrl; }
    public String getDetailUrl() { return detailUrl; }
    public String getFailureType() { return failureType; }
    public String getFailureTypeLabel() { return failureTypeLabel; }

    public String repositoryFullName()
    {
        if (repositoryOwner == null || repositoryName == null)
        {
            return "--";
        }
        return repositoryOwner + "/" + repositoryName;
    }

    public static final class Builder
    {
        private String taskStatus;
        private Long taskId;
        private String conclusion;
        private String conclusionLabel;
        private Integer totalScore;
        private String headShaShort;
        private Integer prNumber;
        private String prTitle;
        private String prAuthor;
        private String repositoryOwner;
        private String repositoryName;
        private String sourceBranch;
        private String targetBranch;
        private Integer changedFiles;
        private Integer additions;
        private Integer deletions;
        private List<ReviewTopIssue> topIssues;
        private ReviewScopeStats scopeStats;
        private String prUrl;
        private String detailUrl;
        private String failureType;
        private String failureTypeLabel;

        public Builder taskStatus(String taskStatus) { this.taskStatus = taskStatus; return this; }
        public Builder taskId(Long taskId) { this.taskId = taskId; return this; }
        public Builder conclusion(String conclusion) { this.conclusion = conclusion; return this; }
        public Builder conclusionLabel(String conclusionLabel) { this.conclusionLabel = conclusionLabel; return this; }
        public Builder totalScore(Integer totalScore) { this.totalScore = totalScore; return this; }
        public Builder headShaShort(String headShaShort) { this.headShaShort = headShaShort; return this; }
        public Builder prNumber(Integer prNumber) { this.prNumber = prNumber; return this; }
        public Builder prTitle(String prTitle) { this.prTitle = prTitle; return this; }
        public Builder prAuthor(String prAuthor) { this.prAuthor = prAuthor; return this; }
        public Builder repositoryOwner(String repositoryOwner) { this.repositoryOwner = repositoryOwner; return this; }
        public Builder repositoryName(String repositoryName) { this.repositoryName = repositoryName; return this; }
        public Builder sourceBranch(String sourceBranch) { this.sourceBranch = sourceBranch; return this; }
        public Builder targetBranch(String targetBranch) { this.targetBranch = targetBranch; return this; }
        public Builder changedFiles(Integer changedFiles) { this.changedFiles = changedFiles; return this; }
        public Builder additions(Integer additions) { this.additions = additions; return this; }
        public Builder deletions(Integer deletions) { this.deletions = deletions; return this; }
        public Builder topIssues(List<ReviewTopIssue> topIssues) { this.topIssues = topIssues; return this; }
        public Builder scopeStats(ReviewScopeStats scopeStats) { this.scopeStats = scopeStats; return this; }
        public Builder prUrl(String prUrl) { this.prUrl = prUrl; return this; }
        public Builder detailUrl(String detailUrl) { this.detailUrl = detailUrl; return this; }
        public Builder failureType(String failureType) { this.failureType = failureType; return this; }
        public Builder failureTypeLabel(String failureTypeLabel) { this.failureTypeLabel = failureTypeLabel; return this; }

        public ReviewSummaryContent build()
        {
            return new ReviewSummaryContent(this);
        }
    }
}
