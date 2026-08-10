package com.acr.review.runtime;

import java.util.Date;

/** 内存告警条目：含可执行中文文案与跳转目标。 */
public class ReviewRuntimeAlert
{
    private String code;
    private String severity;
    private String title;
    private String message;
    private String action;
    private String targetType;
    private Long taskId;
    private Long deliveryId;
    private Date detectedAt;

    public ReviewRuntimeAlert()
    {
    }

    public ReviewRuntimeAlert(String code, String severity, String title, String message, String action,
                              String targetType, Long taskId, Long deliveryId, Date detectedAt)
    {
        this.code = code;
        this.severity = severity;
        this.title = title;
        this.message = message;
        this.action = action;
        this.targetType = targetType;
        this.taskId = taskId;
        this.deliveryId = deliveryId;
        this.detectedAt = detectedAt;
    }

    public String getCode()
    {
        return code;
    }

    public void setCode(String code)
    {
        this.code = code;
    }

    public String getSeverity()
    {
        return severity;
    }

    public void setSeverity(String severity)
    {
        this.severity = severity;
    }

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public String getMessage()
    {
        return message;
    }

    public void setMessage(String message)
    {
        this.message = message;
    }

    public String getAction()
    {
        return action;
    }

    public void setAction(String action)
    {
        this.action = action;
    }

    public String getTargetType()
    {
        return targetType;
    }

    public void setTargetType(String targetType)
    {
        this.targetType = targetType;
    }

    public Long getTaskId()
    {
        return taskId;
    }

    public void setTaskId(Long taskId)
    {
        this.taskId = taskId;
    }

    public Long getDeliveryId()
    {
        return deliveryId;
    }

    public void setDeliveryId(Long deliveryId)
    {
        this.deliveryId = deliveryId;
    }

    public Date getDetectedAt()
    {
        return detectedAt;
    }

    public void setDetectedAt(Date detectedAt)
    {
        this.detectedAt = detectedAt;
    }
}
