package com.acr.review.domain;

import java.util.Date;
import com.acr.common.core.domain.BaseEntity;
import com.fasterxml.jackson.annotation.JsonFormat;

/** 审查任务 review_task：一次由事件触发的执行实例。 */
public class ReviewTask extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long taskId;
    private Long projectId;
    private Long eventId;
    private String provider;
    private Integer prNumber;
    private String prTitle;
    private String prAuthor;
    private String sourceBranch;
    private String targetBranch;
    private String baseSha;
    private String headSha;
    private Integer additions;
    private Integer deletions;
    private Integer changedFiles;
    private String triggerType;
    private String taskStatus;
    private String reviewConclusion;
    private String currentStep;
    private String failureStep;
    private String failureType;
    private String failureMessage;
    private Integer attemptCount;
    private Long latestRunId;
    private String snapshotReviewMode;
    private Long snapshotTemplateId;
    private String snapshotTemplateName;
    private String snapshotTemplateCode;
    private Integer snapshotTemplateVersion;
    private String snapshotPromptContent;
    private Long snapshotModelId;
    private String snapshotModelName;
    private String snapshotModelProvider;
    private String snapshotModel;
    private String snapshotEngineCode;
    private String snapshotEngineName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date startedTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date finishedTime;

    private Long durationMs;
    private Integer totalScore;
    private Integer scoreCorrectness;
    private Integer scoreSecurity;
    private Integer scorePractice;
    private Integer scorePerformance;
    private Integer scoreCommitQuality;
    private String protocolVersion;
    private Integer focusIssueCount;
    private String hasCriticalSecurity;
    private String parseStatus;

    /** 列表展示用项目名称（join review_project，不持久化）。 */
    private String projectName;

    /** 列表展示用业务系统名称（join sys_business_system，不持久化）。 */
    private String businessSystemName;

    /** 列表生成 PR 链接用（join review_project，不持久化）。 */
    private String repositoryOwner;
    private String repositoryName;
    private String repositoryUrl;

    /** 列表重点问题分级统计用：最新 run 的 Top3 JSON（不持久化到 task）。 */
    private String topIssuesJson;

    /**
     * 查询专用：为 true 时仅返回执行队列状态（PENDING/RUNNING/FAILED），不持久化。
     */
    private Boolean queueOnly;

    public Long getTaskId()
    {
        return taskId;
    }

    public void setTaskId(Long taskId)
    {
        this.taskId = taskId;
    }

    public Long getProjectId()
    {
        return projectId;
    }

    public void setProjectId(Long projectId)
    {
        this.projectId = projectId;
    }

    public Long getEventId()
    {
        return eventId;
    }

    public void setEventId(Long eventId)
    {
        this.eventId = eventId;
    }

    public String getProvider()
    {
        return provider;
    }

    public void setProvider(String provider)
    {
        this.provider = provider;
    }

    public Integer getPrNumber()
    {
        return prNumber;
    }

    public void setPrNumber(Integer prNumber)
    {
        this.prNumber = prNumber;
    }

    public String getPrTitle()
    {
        return prTitle;
    }

    public void setPrTitle(String prTitle)
    {
        this.prTitle = prTitle;
    }

    public String getPrAuthor()
    {
        return prAuthor;
    }

    public void setPrAuthor(String prAuthor)
    {
        this.prAuthor = prAuthor;
    }

    public String getSourceBranch()
    {
        return sourceBranch;
    }

    public void setSourceBranch(String sourceBranch)
    {
        this.sourceBranch = sourceBranch;
    }

    public String getTargetBranch()
    {
        return targetBranch;
    }

    public void setTargetBranch(String targetBranch)
    {
        this.targetBranch = targetBranch;
    }

    public String getBaseSha()
    {
        return baseSha;
    }

    public void setBaseSha(String baseSha)
    {
        this.baseSha = baseSha;
    }

    public String getHeadSha()
    {
        return headSha;
    }

    public void setHeadSha(String headSha)
    {
        this.headSha = headSha;
    }

    public Integer getAdditions()
    {
        return additions;
    }

    public void setAdditions(Integer additions)
    {
        this.additions = additions;
    }

    public Integer getDeletions()
    {
        return deletions;
    }

    public void setDeletions(Integer deletions)
    {
        this.deletions = deletions;
    }

    public Integer getChangedFiles()
    {
        return changedFiles;
    }

    public void setChangedFiles(Integer changedFiles)
    {
        this.changedFiles = changedFiles;
    }

    public String getTriggerType()
    {
        return triggerType;
    }

    public void setTriggerType(String triggerType)
    {
        this.triggerType = triggerType;
    }

    public String getTaskStatus()
    {
        return taskStatus;
    }

    public void setTaskStatus(String taskStatus)
    {
        this.taskStatus = taskStatus;
    }

    public String getReviewConclusion()
    {
        return reviewConclusion;
    }

    public void setReviewConclusion(String reviewConclusion)
    {
        this.reviewConclusion = reviewConclusion;
    }

    public String getCurrentStep()
    {
        return currentStep;
    }

    public void setCurrentStep(String currentStep)
    {
        this.currentStep = currentStep;
    }

    public String getFailureStep()
    {
        return failureStep;
    }

    public void setFailureStep(String failureStep)
    {
        this.failureStep = failureStep;
    }

    public String getFailureType()
    {
        return failureType;
    }

    public void setFailureType(String failureType)
    {
        this.failureType = failureType;
    }

    public String getFailureMessage()
    {
        return failureMessage;
    }

    public void setFailureMessage(String failureMessage)
    {
        this.failureMessage = failureMessage;
    }

    public Integer getAttemptCount()
    {
        return attemptCount;
    }

    public void setAttemptCount(Integer attemptCount)
    {
        this.attemptCount = attemptCount;
    }

    public Long getLatestRunId()
    {
        return latestRunId;
    }

    public void setLatestRunId(Long latestRunId)
    {
        this.latestRunId = latestRunId;
    }

    public String getSnapshotReviewMode()
    {
        return snapshotReviewMode;
    }

    public void setSnapshotReviewMode(String snapshotReviewMode)
    {
        this.snapshotReviewMode = snapshotReviewMode;
    }

    public Long getSnapshotTemplateId()
    {
        return snapshotTemplateId;
    }

    public void setSnapshotTemplateId(Long snapshotTemplateId)
    {
        this.snapshotTemplateId = snapshotTemplateId;
    }

    public String getSnapshotTemplateName()
    {
        return snapshotTemplateName;
    }

    public void setSnapshotTemplateName(String snapshotTemplateName)
    {
        this.snapshotTemplateName = snapshotTemplateName;
    }

    public String getSnapshotTemplateCode()
    {
        return snapshotTemplateCode;
    }

    public void setSnapshotTemplateCode(String snapshotTemplateCode)
    {
        this.snapshotTemplateCode = snapshotTemplateCode;
    }

    public Integer getSnapshotTemplateVersion()
    {
        return snapshotTemplateVersion;
    }

    public void setSnapshotTemplateVersion(Integer snapshotTemplateVersion)
    {
        this.snapshotTemplateVersion = snapshotTemplateVersion;
    }

    public String getSnapshotPromptContent()
    {
        return snapshotPromptContent;
    }

    public void setSnapshotPromptContent(String snapshotPromptContent)
    {
        this.snapshotPromptContent = snapshotPromptContent;
    }

    public Long getSnapshotModelId()
    {
        return snapshotModelId;
    }

    public void setSnapshotModelId(Long snapshotModelId)
    {
        this.snapshotModelId = snapshotModelId;
    }

    public String getSnapshotModelName()
    {
        return snapshotModelName;
    }

    public void setSnapshotModelName(String snapshotModelName)
    {
        this.snapshotModelName = snapshotModelName;
    }

    public String getSnapshotModelProvider()
    {
        return snapshotModelProvider;
    }

    public void setSnapshotModelProvider(String snapshotModelProvider)
    {
        this.snapshotModelProvider = snapshotModelProvider;
    }

    public String getSnapshotModel()
    {
        return snapshotModel;
    }

    public void setSnapshotModel(String snapshotModel)
    {
        this.snapshotModel = snapshotModel;
    }

    public String getSnapshotEngineCode()
    {
        return snapshotEngineCode;
    }

    public void setSnapshotEngineCode(String snapshotEngineCode)
    {
        this.snapshotEngineCode = snapshotEngineCode;
    }

    public String getSnapshotEngineName()
    {
        return snapshotEngineName;
    }

    public void setSnapshotEngineName(String snapshotEngineName)
    {
        this.snapshotEngineName = snapshotEngineName;
    }

    public Date getStartedTime()
    {
        return startedTime;
    }

    public void setStartedTime(Date startedTime)
    {
        this.startedTime = startedTime;
    }

    public Date getFinishedTime()
    {
        return finishedTime;
    }

    public void setFinishedTime(Date finishedTime)
    {
        this.finishedTime = finishedTime;
    }

    public Long getDurationMs()
    {
        return durationMs;
    }

    public void setDurationMs(Long durationMs)
    {
        this.durationMs = durationMs;
    }

    public Integer getTotalScore()
    {
        return totalScore;
    }

    public void setTotalScore(Integer totalScore)
    {
        this.totalScore = totalScore;
    }

    public Integer getScoreCorrectness()
    {
        return scoreCorrectness;
    }

    public void setScoreCorrectness(Integer scoreCorrectness)
    {
        this.scoreCorrectness = scoreCorrectness;
    }

    public Integer getScoreSecurity()
    {
        return scoreSecurity;
    }

    public void setScoreSecurity(Integer scoreSecurity)
    {
        this.scoreSecurity = scoreSecurity;
    }

    public Integer getScorePractice()
    {
        return scorePractice;
    }

    public void setScorePractice(Integer scorePractice)
    {
        this.scorePractice = scorePractice;
    }

    public Integer getScorePerformance()
    {
        return scorePerformance;
    }

    public void setScorePerformance(Integer scorePerformance)
    {
        this.scorePerformance = scorePerformance;
    }

    public Integer getScoreCommitQuality()
    {
        return scoreCommitQuality;
    }

    public void setScoreCommitQuality(Integer scoreCommitQuality)
    {
        this.scoreCommitQuality = scoreCommitQuality;
    }

    public String getProtocolVersion()
    {
        return protocolVersion;
    }

    public void setProtocolVersion(String protocolVersion)
    {
        this.protocolVersion = protocolVersion;
    }

    public Integer getFocusIssueCount()
    {
        return focusIssueCount;
    }

    public void setFocusIssueCount(Integer focusIssueCount)
    {
        this.focusIssueCount = focusIssueCount;
    }

    public String getHasCriticalSecurity()
    {
        return hasCriticalSecurity;
    }

    public void setHasCriticalSecurity(String hasCriticalSecurity)
    {
        this.hasCriticalSecurity = hasCriticalSecurity;
    }

    public String getParseStatus()
    {
        return parseStatus;
    }

    public void setParseStatus(String parseStatus)
    {
        this.parseStatus = parseStatus;
    }

    public String getProjectName()
    {
        return projectName;
    }

    public void setProjectName(String projectName)
    {
        this.projectName = projectName;
    }

    public String getBusinessSystemName()
    {
        return businessSystemName;
    }

    public void setBusinessSystemName(String businessSystemName)
    {
        this.businessSystemName = businessSystemName;
    }

    public String getRepositoryOwner()
    {
        return repositoryOwner;
    }

    public void setRepositoryOwner(String repositoryOwner)
    {
        this.repositoryOwner = repositoryOwner;
    }

    public String getRepositoryName()
    {
        return repositoryName;
    }

    public void setRepositoryName(String repositoryName)
    {
        this.repositoryName = repositoryName;
    }

    public String getRepositoryUrl()
    {
        return repositoryUrl;
    }

    public void setRepositoryUrl(String repositoryUrl)
    {
        this.repositoryUrl = repositoryUrl;
    }

    public String getTopIssuesJson()
    {
        return topIssuesJson;
    }

    public void setTopIssuesJson(String topIssuesJson)
    {
        this.topIssuesJson = topIssuesJson;
    }

    public Boolean getQueueOnly()
    {
        return queueOnly;
    }

    public void setQueueOnly(Boolean queueOnly)
    {
        this.queueOnly = queueOnly;
    }
}
