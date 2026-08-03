package com.acr.review.domain;

import java.util.Date;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import com.acr.common.core.domain.BaseEntity;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/** 审查通知渠道 review_notify_channel（群机器人）。 */
public class ReviewNotifyChannel extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long channelId;
    private String channelName;
    private String channelType;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String webhookUrl;

    @JsonIgnore
    private String webhookUrlCiphertext;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String secret;

    @JsonIgnore
    private String secretCiphertext;

    private String status;
    private String lastCheckStatus;
    private String lastCheckMessage;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastCheckTime;

    private Boolean webhookUrlConfigured;
    private Boolean secretConfigured;
    private Integer referenceCount;

    public Long getChannelId()
    {
        return channelId;
    }

    public void setChannelId(Long channelId)
    {
        this.channelId = channelId;
    }

    @NotBlank(message = "渠道名称不能为空")
    @Size(max = 64, message = "渠道名称不能超过64个字符")
    public String getChannelName()
    {
        return channelName;
    }

    public void setChannelName(String channelName)
    {
        this.channelName = channelName;
    }

    @NotBlank(message = "渠道类型不能为空")
    public String getChannelType()
    {
        return channelType;
    }

    public void setChannelType(String channelType)
    {
        this.channelType = channelType;
    }

    @Size(max = 1000, message = "Webhook URL 不能超过1000个字符")
    public String getWebhookUrl()
    {
        return webhookUrl;
    }

    public void setWebhookUrl(String webhookUrl)
    {
        this.webhookUrl = webhookUrl;
    }

    public String getWebhookUrlCiphertext()
    {
        return webhookUrlCiphertext;
    }

    public void setWebhookUrlCiphertext(String webhookUrlCiphertext)
    {
        this.webhookUrlCiphertext = webhookUrlCiphertext;
    }

    @Size(max = 500, message = "加签 Secret 不能超过500个字符")
    public String getSecret()
    {
        return secret;
    }

    public void setSecret(String secret)
    {
        this.secret = secret;
    }

    public String getSecretCiphertext()
    {
        return secretCiphertext;
    }

    public void setSecretCiphertext(String secretCiphertext)
    {
        this.secretCiphertext = secretCiphertext;
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

    public Boolean getWebhookUrlConfigured()
    {
        return webhookUrlConfigured;
    }

    public void setWebhookUrlConfigured(Boolean webhookUrlConfigured)
    {
        this.webhookUrlConfigured = webhookUrlConfigured;
    }

    public Boolean getSecretConfigured()
    {
        return secretConfigured;
    }

    public void setSecretConfigured(Boolean secretConfigured)
    {
        this.secretConfigured = secretConfigured;
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
