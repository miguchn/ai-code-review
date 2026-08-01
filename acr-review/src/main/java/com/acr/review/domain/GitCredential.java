package com.acr.review.domain;

import java.util.Date;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import com.acr.common.core.domain.BaseEntity;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Git 访问凭据 review_git_credential。 */
public class GitCredential extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long credentialId;
    private String credentialName;
    private String provider;
    private String authType;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String token;

    @JsonIgnore
    private String tokenCiphertext;

    private String status;
    private String lastCheckStatus;
    private String lastCheckMessage;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastCheckTime;

    private Boolean tokenConfigured;
    private Integer referenceCount;

    public Long getCredentialId()
    {
        return credentialId;
    }

    public void setCredentialId(Long credentialId)
    {
        this.credentialId = credentialId;
    }

    @NotBlank(message = "凭据名称不能为空")
    @Size(max = 64, message = "凭据名称不能超过64个字符")
    public String getCredentialName()
    {
        return credentialName;
    }

    public void setCredentialName(String credentialName)
    {
        this.credentialName = credentialName;
    }

    public String getProvider()
    {
        return provider;
    }

    public void setProvider(String provider)
    {
        this.provider = provider;
    }

    public String getAuthType()
    {
        return authType;
    }

    public void setAuthType(String authType)
    {
        this.authType = authType;
    }

    @Size(max = 500, message = "GitHub Token 不能超过500个字符")
    public String getToken()
    {
        return token;
    }

    public void setToken(String token)
    {
        this.token = token;
    }

    public String getTokenCiphertext()
    {
        return tokenCiphertext;
    }

    public void setTokenCiphertext(String tokenCiphertext)
    {
        this.tokenCiphertext = tokenCiphertext;
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

    public Boolean getTokenConfigured()
    {
        return tokenConfigured;
    }

    public void setTokenConfigured(Boolean tokenConfigured)
    {
        this.tokenConfigured = tokenConfigured;
    }

    public Integer getReferenceCount()
    {
        return referenceCount;
    }

    public void setReferenceCount(Integer referenceCount)
    {
        this.referenceCount = referenceCount;
    }
}
