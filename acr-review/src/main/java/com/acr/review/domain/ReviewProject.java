package com.acr.review.domain;

import java.util.Date;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import com.acr.common.core.domain.BaseEntity;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/** 代码审查项目 review_project。 */
public class ReviewProject extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long projectId;
    private String projectName;
    private String provider;
    private String repositoryUrl;
    private String repositoryOwner;
    private String repositoryName;
    private String repositoryFullPath;
    private String defaultBranch;
    private String prReviewEnabled;
    private String prTargetBranches;
    private Long businessSystemId;
    private String businessSystemName;
    private Long deptId;
    private String deptName;
    private Long ownerUserId;
    private String ownerName;
    private Long credentialId;
    private String credentialName;
    /** 大模型审查时绑定的模型配置 ID。 */
    private Long modelId;
    private String modelName;
    /** 项目主要语言/技术栈。 */
    private String primaryStack;
    /** 大模型审查时绑定的审查模板 ID。 */
    private Long templateId;
    private String templateName;
    private String reviewMode;
    private String engineCode;

    /** 审查范围：项目排除路径 glob（换行分隔，平台默认排除之外追加）。 */
    private String scopeExcludePatterns;
    /** 审查范围：是否审查测试文件（Y/N，默认 N）。 */
    private String scopeIncludeTests;
    /** 审查范围：是否上报历史存量问题（Y/N，默认 N；归属打标能力生效后消费）。 */
    private String scopeReportExisting;
    /** 审查范围：高影响变更自动扩展整文件（Y/N，默认 Y）。 */
    private String scopeExpandEnabled;

    /** 是否启用 IM 通知（Y/N，默认 N）。 */
    private String notifyEnabled;
    /** 通知渠道 ID。 */
    private Long notifyChannelId;
    private String notifyChannelName;
    /** FAILED 时是否发送简讯（Y/N，默认 Y）。 */
    private String notifyOnFailure;

    private String status;
    private String lastCheckStatus;
    private String lastCheckMessage;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastCheckTime;

    private String lastBranchSyncStatus;
    private String lastBranchSyncMessage;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastBranchSyncTime;

    /** Webhook Secret 明文（只写，编辑留空保留原值）。 */
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String webhookSecret;

    @JsonIgnore
    private String webhookSecretCiphertext;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastWebhookTime;

    private String lastWebhookResult;

    /** 响应只读：是否已配置 Webhook Secret。 */
    private Boolean webhookSecretConfigured;

    /** 响应只读：Webhook 回调地址（配置项拼接，不持久化）。 */
    private String webhookCallbackUrl;

    @JsonIgnore
    private Long accessUserId;

    public Long getProjectId()
    {
        return projectId;
    }

    public void setProjectId(Long projectId)
    {
        this.projectId = projectId;
    }

    @NotBlank(message = "项目名称不能为空")
    @Size(max = 100, message = "项目名称不能超过100个字符")
    public String getProjectName()
    {
        return projectName;
    }

    public void setProjectName(String projectName)
    {
        this.projectName = projectName;
    }

    public String getProvider()
    {
        return provider;
    }

    public void setProvider(String provider)
    {
        this.provider = provider;
    }

    @NotBlank(message = "GitHub 仓库地址不能为空")
    @Size(max = 500, message = "GitHub 仓库地址不能超过500个字符")
    public String getRepositoryUrl()
    {
        return repositoryUrl;
    }

    public void setRepositoryUrl(String repositoryUrl)
    {
        this.repositoryUrl = repositoryUrl;
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

    @Size(max = 255, message = "仓库全路径不能超过255个字符")
    public String getRepositoryFullPath()
    {
        return repositoryFullPath;
    }

    public void setRepositoryFullPath(String repositoryFullPath)
    {
        this.repositoryFullPath = repositoryFullPath;
    }

    @Size(max = 255, message = "默认分支不能超过255个字符")
    public String getDefaultBranch()
    {
        return defaultBranch;
    }

    public void setDefaultBranch(String defaultBranch)
    {
        this.defaultBranch = defaultBranch;
    }

    public String getPrReviewEnabled()
    {
        return prReviewEnabled;
    }

    public void setPrReviewEnabled(String prReviewEnabled)
    {
        this.prReviewEnabled = prReviewEnabled;
    }

    @Size(max = 1000, message = "PR 目标分支不能超过1000个字符")
    public String getPrTargetBranches()
    {
        return prTargetBranches;
    }

    public void setPrTargetBranches(String prTargetBranches)
    {
        this.prTargetBranches = prTargetBranches;
    }

    @NotNull(message = "所属业务系统不能为空")
    public Long getBusinessSystemId()
    {
        return businessSystemId;
    }

    public void setBusinessSystemId(Long businessSystemId)
    {
        this.businessSystemId = businessSystemId;
    }

    public String getBusinessSystemName()
    {
        return businessSystemName;
    }

    public void setBusinessSystemName(String businessSystemName)
    {
        this.businessSystemName = businessSystemName;
    }

    @NotNull(message = "所属部门不能为空")
    public Long getDeptId()
    {
        return deptId;
    }

    public void setDeptId(Long deptId)
    {
        this.deptId = deptId;
    }

    public String getDeptName()
    {
        return deptName;
    }

    public void setDeptName(String deptName)
    {
        this.deptName = deptName;
    }

    @NotNull(message = "项目负责人不能为空")
    public Long getOwnerUserId()
    {
        return ownerUserId;
    }

    public void setOwnerUserId(Long ownerUserId)
    {
        this.ownerUserId = ownerUserId;
    }

    public String getOwnerName()
    {
        return ownerName;
    }

    public void setOwnerName(String ownerName)
    {
        this.ownerName = ownerName;
    }

    @NotNull(message = "GitHub 访问凭据不能为空")
    public Long getCredentialId()
    {
        return credentialId;
    }

    public void setCredentialId(Long credentialId)
    {
        this.credentialId = credentialId;
    }

    public String getCredentialName()
    {
        return credentialName;
    }

    public void setCredentialName(String credentialName)
    {
        this.credentialName = credentialName;
    }

    public Long getModelId()
    {
        return modelId;
    }

    public void setModelId(Long modelId)
    {
        this.modelId = modelId;
    }

    public String getModelName()
    {
        return modelName;
    }

    public void setModelName(String modelName)
    {
        this.modelName = modelName;
    }

    public String getPrimaryStack()
    {
        return primaryStack;
    }

    public void setPrimaryStack(String primaryStack)
    {
        this.primaryStack = primaryStack;
    }

    public Long getTemplateId()
    {
        return templateId;
    }

    public void setTemplateId(Long templateId)
    {
        this.templateId = templateId;
    }

    public String getTemplateName()
    {
        return templateName;
    }

    public void setTemplateName(String templateName)
    {
        this.templateName = templateName;
    }

    public String getReviewMode()
    {
        return reviewMode;
    }

    public void setReviewMode(String reviewMode)
    {
        this.reviewMode = reviewMode;
    }

    public String getEngineCode()
    {
        return engineCode;
    }

    public void setEngineCode(String engineCode)
    {
        this.engineCode = engineCode;
    }

    @Size(max = 2000, message = "审查范围排除路径不能超过2000个字符")
    public String getScopeExcludePatterns()
    {
        return scopeExcludePatterns;
    }

    public void setScopeExcludePatterns(String scopeExcludePatterns)
    {
        this.scopeExcludePatterns = scopeExcludePatterns;
    }

    public String getScopeIncludeTests()
    {
        return scopeIncludeTests;
    }

    public void setScopeIncludeTests(String scopeIncludeTests)
    {
        this.scopeIncludeTests = scopeIncludeTests;
    }

    public String getScopeReportExisting()
    {
        return scopeReportExisting;
    }

    public void setScopeReportExisting(String scopeReportExisting)
    {
        this.scopeReportExisting = scopeReportExisting;
    }

    public String getScopeExpandEnabled()
    {
        return scopeExpandEnabled;
    }

    public void setScopeExpandEnabled(String scopeExpandEnabled)
    {
        this.scopeExpandEnabled = scopeExpandEnabled;
    }

    public String getNotifyEnabled()
    {
        return notifyEnabled;
    }

    public void setNotifyEnabled(String notifyEnabled)
    {
        this.notifyEnabled = notifyEnabled;
    }

    public Long getNotifyChannelId()
    {
        return notifyChannelId;
    }

    public void setNotifyChannelId(Long notifyChannelId)
    {
        this.notifyChannelId = notifyChannelId;
    }

    public String getNotifyChannelName()
    {
        return notifyChannelName;
    }

    public void setNotifyChannelName(String notifyChannelName)
    {
        this.notifyChannelName = notifyChannelName;
    }

    public String getNotifyOnFailure()
    {
        return notifyOnFailure;
    }

    public void setNotifyOnFailure(String notifyOnFailure)
    {
        this.notifyOnFailure = notifyOnFailure;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public String getLastCheckStatus()
    {
        return lastCheckStatus;
    }

    public void setLastCheckStatus(String lastCheckStatus)
    {
        this.lastCheckStatus = lastCheckStatus;
    }

    public String getLastCheckMessage()
    {
        return lastCheckMessage;
    }

    public void setLastCheckMessage(String lastCheckMessage)
    {
        this.lastCheckMessage = lastCheckMessage;
    }

    public Date getLastCheckTime()
    {
        return lastCheckTime;
    }

    public void setLastCheckTime(Date lastCheckTime)
    {
        this.lastCheckTime = lastCheckTime;
    }

    public String getLastBranchSyncStatus()
    {
        return lastBranchSyncStatus;
    }

    public void setLastBranchSyncStatus(String lastBranchSyncStatus)
    {
        this.lastBranchSyncStatus = lastBranchSyncStatus;
    }

    public String getLastBranchSyncMessage()
    {
        return lastBranchSyncMessage;
    }

    public void setLastBranchSyncMessage(String lastBranchSyncMessage)
    {
        this.lastBranchSyncMessage = lastBranchSyncMessage;
    }

    public Date getLastBranchSyncTime()
    {
        return lastBranchSyncTime;
    }

    public void setLastBranchSyncTime(Date lastBranchSyncTime)
    {
        this.lastBranchSyncTime = lastBranchSyncTime;
    }

    public String getWebhookSecret()
    {
        return webhookSecret;
    }

    public void setWebhookSecret(String webhookSecret)
    {
        this.webhookSecret = webhookSecret;
    }

    public String getWebhookSecretCiphertext()
    {
        return webhookSecretCiphertext;
    }

    public void setWebhookSecretCiphertext(String webhookSecretCiphertext)
    {
        this.webhookSecretCiphertext = webhookSecretCiphertext;
    }

    public Date getLastWebhookTime()
    {
        return lastWebhookTime;
    }

    public void setLastWebhookTime(Date lastWebhookTime)
    {
        this.lastWebhookTime = lastWebhookTime;
    }

    public String getLastWebhookResult()
    {
        return lastWebhookResult;
    }

    public void setLastWebhookResult(String lastWebhookResult)
    {
        this.lastWebhookResult = lastWebhookResult;
    }

    public Boolean getWebhookSecretConfigured()
    {
        return webhookSecretConfigured;
    }

    public void setWebhookSecretConfigured(Boolean webhookSecretConfigured)
    {
        this.webhookSecretConfigured = webhookSecretConfigured;
    }

    public String getWebhookCallbackUrl()
    {
        return webhookCallbackUrl;
    }

    public void setWebhookCallbackUrl(String webhookCallbackUrl)
    {
        this.webhookCallbackUrl = webhookCallbackUrl;
    }

    public Long getAccessUserId()
    {
        return accessUserId;
    }

    public void setAccessUserId(Long accessUserId)
    {
        this.accessUserId = accessUserId;
    }
}
