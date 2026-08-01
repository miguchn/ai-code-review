package com.acr.review.domain;

import java.util.Date;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import com.acr.common.core.domain.BaseEntity;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;

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
    private String status;
    private String lastCheckStatus;
    private String lastCheckMessage;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastCheckTime;

    private String lastBranchSyncStatus;
    private String lastBranchSyncMessage;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastBranchSyncTime;

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

    public Long getAccessUserId()
    {
        return accessUserId;
    }

    public void setAccessUserId(Long accessUserId)
    {
        this.accessUserId = accessUserId;
    }
}
