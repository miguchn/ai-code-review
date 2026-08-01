package com.acr.review.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 读取 GitHub 仓库信息所需的最小表单参数。 */
public class GitRepositoryReadRequest
{
    private Long projectId;
    private String repositoryUrl;
    private Long credentialId;

    public Long getProjectId()
    {
        return projectId;
    }

    public void setProjectId(Long projectId)
    {
        this.projectId = projectId;
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

    @NotNull(message = "GitHub 访问凭据不能为空")
    public Long getCredentialId()
    {
        return credentialId;
    }

    public void setCredentialId(Long credentialId)
    {
        this.credentialId = credentialId;
    }
}
