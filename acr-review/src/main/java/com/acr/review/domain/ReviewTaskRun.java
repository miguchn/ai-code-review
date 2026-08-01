package com.acr.review.domain;

import java.util.Date;
import com.acr.common.core.domain.BaseEntity;
import com.fasterxml.jackson.annotation.JsonFormat;

/** 审查任务执行记录 review_task_run。 */
public class ReviewTaskRun extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long runId;
    private Long taskId;
    private Integer attemptNo;
    private String runStatus;
    private String currentStep;
    private String failureStep;
    private String failureType;
    private String failureMessage;
    private String reviewConclusion;
    private String snapshotReviewMode;
    private String snapshotEngineCode;
    private String snapshotEngineName;
    private String snapshotEngineVersion;
    private Long snapshotModelId;
    private String snapshotModelName;
    private String snapshotModelProvider;
    private String snapshotModel;
    private Long snapshotTemplateId;
    private String snapshotTemplateName;
    private String snapshotTemplateCode;
    private Integer snapshotTemplateVersion;
    private String snapshotPromptContent;
    private Integer snapshotTimeoutSeconds;
    private String snapshotBaseSha;
    private String snapshotHeadSha;
    private String resultSummary;
    private String resultJson;
    private Integer totalScore;
    private Integer scoreCorrectness;
    private Integer scoreSecurity;
    private Integer scorePractice;
    private Integer scorePerformance;
    private Integer scoreCommitQuality;
    private String protocolVersion;
    private String scoreWeightsJson;
    private Integer scoreThreshold;
    private Integer focusIssueCount;
    private String hasCriticalSecurity;
    private String topIssuesJson;
    private String parseStatus;
    private String parseError;
    private String rawResponseExcerpt;
    private String renderedPrompt;
    private String prDescription;
    private String commitMessages;
    private Long durationMs;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date startedTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date finishedTime;

    public Long getRunId()
    {
        return runId;
    }

    public void setRunId(Long runId)
    {
        this.runId = runId;
    }

    public Long getTaskId()
    {
        return taskId;
    }

    public void setTaskId(Long taskId)
    {
        this.taskId = taskId;
    }

    public Integer getAttemptNo()
    {
        return attemptNo;
    }

    public void setAttemptNo(Integer attemptNo)
    {
        this.attemptNo = attemptNo;
    }

    public String getRunStatus()
    {
        return runStatus;
    }

    public void setRunStatus(String runStatus)
    {
        this.runStatus = runStatus;
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

    public String getReviewConclusion()
    {
        return reviewConclusion;
    }

    public void setReviewConclusion(String reviewConclusion)
    {
        this.reviewConclusion = reviewConclusion;
    }

    public String getSnapshotReviewMode()
    {
        return snapshotReviewMode;
    }

    public void setSnapshotReviewMode(String snapshotReviewMode)
    {
        this.snapshotReviewMode = snapshotReviewMode;
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

    public String getSnapshotEngineVersion()
    {
        return snapshotEngineVersion;
    }

    public void setSnapshotEngineVersion(String snapshotEngineVersion)
    {
        this.snapshotEngineVersion = snapshotEngineVersion;
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

    public Integer getSnapshotTimeoutSeconds()
    {
        return snapshotTimeoutSeconds;
    }

    public void setSnapshotTimeoutSeconds(Integer snapshotTimeoutSeconds)
    {
        this.snapshotTimeoutSeconds = snapshotTimeoutSeconds;
    }

    public String getSnapshotBaseSha()
    {
        return snapshotBaseSha;
    }

    public void setSnapshotBaseSha(String snapshotBaseSha)
    {
        this.snapshotBaseSha = snapshotBaseSha;
    }

    public String getSnapshotHeadSha()
    {
        return snapshotHeadSha;
    }

    public void setSnapshotHeadSha(String snapshotHeadSha)
    {
        this.snapshotHeadSha = snapshotHeadSha;
    }

    public String getResultSummary()
    {
        return resultSummary;
    }

    public void setResultSummary(String resultSummary)
    {
        this.resultSummary = resultSummary;
    }

    public String getResultJson()
    {
        return resultJson;
    }

    public void setResultJson(String resultJson)
    {
        this.resultJson = resultJson;
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

    public String getScoreWeightsJson()
    {
        return scoreWeightsJson;
    }

    public void setScoreWeightsJson(String scoreWeightsJson)
    {
        this.scoreWeightsJson = scoreWeightsJson;
    }

    public Integer getScoreThreshold()
    {
        return scoreThreshold;
    }

    public void setScoreThreshold(Integer scoreThreshold)
    {
        this.scoreThreshold = scoreThreshold;
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

    public String getTopIssuesJson()
    {
        return topIssuesJson;
    }

    public void setTopIssuesJson(String topIssuesJson)
    {
        this.topIssuesJson = topIssuesJson;
    }

    public String getParseStatus()
    {
        return parseStatus;
    }

    public void setParseStatus(String parseStatus)
    {
        this.parseStatus = parseStatus;
    }

    public String getParseError()
    {
        return parseError;
    }

    public void setParseError(String parseError)
    {
        this.parseError = parseError;
    }

    public String getRawResponseExcerpt()
    {
        return rawResponseExcerpt;
    }

    public void setRawResponseExcerpt(String rawResponseExcerpt)
    {
        this.rawResponseExcerpt = rawResponseExcerpt;
    }

    public String getRenderedPrompt()
    {
        return renderedPrompt;
    }

    public void setRenderedPrompt(String renderedPrompt)
    {
        this.renderedPrompt = renderedPrompt;
    }

    public String getPrDescription()
    {
        return prDescription;
    }

    public void setPrDescription(String prDescription)
    {
        this.prDescription = prDescription;
    }

    public String getCommitMessages()
    {
        return commitMessages;
    }

    public void setCommitMessages(String commitMessages)
    {
        this.commitMessages = commitMessages;
    }

    public Long getDurationMs()
    {
        return durationMs;
    }

    public void setDurationMs(Long durationMs)
    {
        this.durationMs = durationMs;
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
}
