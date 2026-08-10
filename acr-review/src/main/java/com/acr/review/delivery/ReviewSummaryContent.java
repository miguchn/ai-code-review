package com.acr.review.delivery;

import java.util.Date;
import java.util.List;
import com.acr.review.domain.ReviewPipelineConstants;
import com.acr.review.domain.result.ReviewScopeStats;
import com.acr.review.domain.result.ReviewTopIssue;
import com.acr.review.service.ReviewScoringConstants;

/** 审查结论摘要共享内容模型（GitHub 评论与 IM 同源）。 */
public final class ReviewSummaryContent
{
    private final String taskStatus;
    private final Long taskId;
    /** 执行代次，嵌入总结评论 marker 供外部回溯。 */
    private final Long runId;
    private final String conclusion;
    private final String conclusionLabel;
    private final Integer totalScore;
    private final String headShaShort;
    private final Integer prNumber;
    private final String prTitle;
    private final String prAuthor;
    private final String repositoryOwner;
    private final String repositoryName;
    /** 企业内部项目名称（归属行优先展示；空则回退 owner/repo）。 */
    private final String projectName;
    /** 业务系统名称（归属行首段；空则整段省略）。 */
    private final String businessSystemName;
    private final String sourceBranch;
    private final String targetBranch;
    private final Integer changedFiles;
    private final Integer additions;
    private final Integer deletions;
    private final List<ReviewTopIssue> topIssues;
    /** 本轮转待复核问题标题（疑似已修复段）。 */
    private final List<String> recheckingTitles;
    private final ReviewScopeStats scopeStats;
    private final String prUrl;
    private final String detailUrl;
    private final String failureType;
    private final String failureTypeLabel;
    /** Commit 信息（由 run.commitMessages 装配）。 */
    private final String commitMessage;
    /** 审查总结文本（由 run.resultSummary 装配）。 */
    private final String summaryText;
    /** 审查完成时间（由 run.finishedTime 装配）。 */
    private final Date reviewTime;
    /** 事件来源：PR / PUSH。 */
    private final String eventSource;
    /** 行内评论预告：门槛过滤后的总条数（总结评论范围段用）。 */
    private final Integer inlineCommentCount;
    private final Integer inlineCriticalCount;
    private final Integer inlineHighCount;

    private ReviewSummaryContent(Builder builder)
    {
        this.taskStatus = builder.taskStatus;
        this.taskId = builder.taskId;
        this.runId = builder.runId;
        this.conclusion = builder.conclusion;
        this.conclusionLabel = builder.conclusionLabel;
        this.totalScore = builder.totalScore;
        this.headShaShort = builder.headShaShort;
        this.prNumber = builder.prNumber;
        this.prTitle = builder.prTitle;
        this.prAuthor = builder.prAuthor;
        this.repositoryOwner = builder.repositoryOwner;
        this.repositoryName = builder.repositoryName;
        this.projectName = builder.projectName;
        this.businessSystemName = builder.businessSystemName;
        this.sourceBranch = builder.sourceBranch;
        this.targetBranch = builder.targetBranch;
        this.changedFiles = builder.changedFiles;
        this.additions = builder.additions;
        this.deletions = builder.deletions;
        this.topIssues = builder.topIssues == null ? List.of() : List.copyOf(builder.topIssues);
        this.recheckingTitles = builder.recheckingTitles == null ? List.of() : List.copyOf(builder.recheckingTitles);
        this.scopeStats = builder.scopeStats;
        this.prUrl = builder.prUrl;
        this.detailUrl = builder.detailUrl;
        this.failureType = builder.failureType;
        this.failureTypeLabel = builder.failureTypeLabel;
        this.commitMessage = builder.commitMessage;
        this.summaryText = builder.summaryText;
        this.reviewTime = builder.reviewTime;
        this.eventSource = builder.eventSource;
        this.inlineCommentCount = builder.inlineCommentCount;
        this.inlineCriticalCount = builder.inlineCriticalCount;
        this.inlineHighCount = builder.inlineHighCount;
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public String getTaskStatus() { return taskStatus; }
    public Long getTaskId() { return taskId; }
    public Long getRunId() { return runId; }
    public String getConclusion() { return conclusion; }
    public String getConclusionLabel() { return conclusionLabel; }
    public Integer getTotalScore() { return totalScore; }
    public String getHeadShaShort() { return headShaShort; }
    public Integer getPrNumber() { return prNumber; }
    public String getPrTitle() { return prTitle; }
    public String getPrAuthor() { return prAuthor; }
    public String getRepositoryOwner() { return repositoryOwner; }
    public String getRepositoryName() { return repositoryName; }
    public String getProjectName() { return projectName; }
    public String getBusinessSystemName() { return businessSystemName; }
    public String getSourceBranch() { return sourceBranch; }
    public String getTargetBranch() { return targetBranch; }
    public Integer getChangedFiles() { return changedFiles; }
    public Integer getAdditions() { return additions; }
    public Integer getDeletions() { return deletions; }
    public List<ReviewTopIssue> getTopIssues() { return topIssues; }

    /** 展示层 Top N（总结评论 / IM）。 */
    public List<ReviewTopIssue> displayTopIssues()
    {
        if (topIssues.isEmpty())
        {
            return topIssues;
        }
        int limit = ReviewScoringConstants.MAX_TOP_ISSUES;
        return topIssues.size() <= limit ? topIssues : topIssues.subList(0, limit);
    }

    public List<String> getRecheckingTitles() { return recheckingTitles; }
    public ReviewScopeStats getScopeStats() { return scopeStats; }
    public String getPrUrl() { return prUrl; }
    public String getDetailUrl() { return detailUrl; }
    public String getFailureType() { return failureType; }
    public String getFailureTypeLabel() { return failureTypeLabel; }
    public String getCommitMessage() { return commitMessage; }
    public String getSummaryText() { return summaryText; }
    public Date getReviewTime() { return reviewTime; }
    public String getEventSource() { return eventSource; }
    public Integer getInlineCommentCount() { return inlineCommentCount; }
    public Integer getInlineCriticalCount() { return inlineCriticalCount; }
    public Integer getInlineHighCount() { return inlineHighCount; }

    public boolean isPushReview()
    {
        return ReviewPipelineConstants.EVENT_SOURCE_PUSH.equals(eventSource);
    }

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
        private Long runId;
        private String conclusion;
        private String conclusionLabel;
        private Integer totalScore;
        private String headShaShort;
        private Integer prNumber;
        private String prTitle;
        private String prAuthor;
        private String repositoryOwner;
        private String repositoryName;
        private String projectName;
        private String businessSystemName;
        private String sourceBranch;
        private String targetBranch;
        private Integer changedFiles;
        private Integer additions;
        private Integer deletions;
        private List<ReviewTopIssue> topIssues;
        private List<String> recheckingTitles;
        private ReviewScopeStats scopeStats;
        private String prUrl;
        private String detailUrl;
        private String failureType;
        private String failureTypeLabel;
        private String commitMessage;
        private String summaryText;
        private Date reviewTime;
        private String eventSource;
        private Integer inlineCommentCount;
        private Integer inlineCriticalCount;
        private Integer inlineHighCount;

        public Builder taskStatus(String taskStatus) { this.taskStatus = taskStatus; return this; }
        public Builder taskId(Long taskId) { this.taskId = taskId; return this; }
        public Builder runId(Long runId) { this.runId = runId; return this; }
        public Builder conclusion(String conclusion) { this.conclusion = conclusion; return this; }
        public Builder conclusionLabel(String conclusionLabel) { this.conclusionLabel = conclusionLabel; return this; }
        public Builder totalScore(Integer totalScore) { this.totalScore = totalScore; return this; }
        public Builder headShaShort(String headShaShort) { this.headShaShort = headShaShort; return this; }
        public Builder prNumber(Integer prNumber) { this.prNumber = prNumber; return this; }
        public Builder prTitle(String prTitle) { this.prTitle = prTitle; return this; }
        public Builder prAuthor(String prAuthor) { this.prAuthor = prAuthor; return this; }
        public Builder repositoryOwner(String repositoryOwner) { this.repositoryOwner = repositoryOwner; return this; }
        public Builder repositoryName(String repositoryName) { this.repositoryName = repositoryName; return this; }
        public Builder projectName(String projectName) { this.projectName = projectName; return this; }
        public Builder businessSystemName(String businessSystemName) { this.businessSystemName = businessSystemName; return this; }
        public Builder sourceBranch(String sourceBranch) { this.sourceBranch = sourceBranch; return this; }
        public Builder targetBranch(String targetBranch) { this.targetBranch = targetBranch; return this; }
        public Builder changedFiles(Integer changedFiles) { this.changedFiles = changedFiles; return this; }
        public Builder additions(Integer additions) { this.additions = additions; return this; }
        public Builder deletions(Integer deletions) { this.deletions = deletions; return this; }
        public Builder topIssues(List<ReviewTopIssue> topIssues) { this.topIssues = topIssues; return this; }
        public Builder recheckingTitles(List<String> recheckingTitles) { this.recheckingTitles = recheckingTitles; return this; }
        public Builder scopeStats(ReviewScopeStats scopeStats) { this.scopeStats = scopeStats; return this; }
        public Builder prUrl(String prUrl) { this.prUrl = prUrl; return this; }
        public Builder detailUrl(String detailUrl) { this.detailUrl = detailUrl; return this; }
        public Builder failureType(String failureType) { this.failureType = failureType; return this; }
        public Builder failureTypeLabel(String failureTypeLabel) { this.failureTypeLabel = failureTypeLabel; return this; }
        public Builder commitMessage(String commitMessage) { this.commitMessage = commitMessage; return this; }
        public Builder summaryText(String summaryText) { this.summaryText = summaryText; return this; }
        public Builder reviewTime(Date reviewTime) { this.reviewTime = reviewTime; return this; }
        public Builder eventSource(String eventSource) { this.eventSource = eventSource; return this; }
        public Builder inlineCommentCount(Integer inlineCommentCount) { this.inlineCommentCount = inlineCommentCount; return this; }
        public Builder inlineCriticalCount(Integer inlineCriticalCount) { this.inlineCriticalCount = inlineCriticalCount; return this; }
        public Builder inlineHighCount(Integer inlineHighCount) { this.inlineHighCount = inlineHighCount; return this; }

        public ReviewSummaryContent build()
        {
            return new ReviewSummaryContent(this);
        }
    }
}
