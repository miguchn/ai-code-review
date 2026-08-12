package com.acr.system.domain;

import java.util.Date;
import com.acr.common.core.domain.BaseEntity;

/**
 * 业务审计事实。该对象只支持新增和查询，不提供更新、删除能力。
 */
public class SysBusinessAudit extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long auditId;
    private String eventKey;
    private String source;
    private String action;
    private String objectType;
    private String objectId;
    private String objectName;
    private String beforeValue;
    private String afterValue;
    private String reason;
    private String relatedObject;
    private String operator;
    private Date auditTime;

    public Long getAuditId() { return auditId; }
    public void setAuditId(Long auditId) { this.auditId = auditId; }
    public String getEventKey() { return eventKey; }
    public void setEventKey(String eventKey) { this.eventKey = eventKey; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getObjectType() { return objectType; }
    public void setObjectType(String objectType) { this.objectType = objectType; }
    public String getObjectId() { return objectId; }
    public void setObjectId(String objectId) { this.objectId = objectId; }
    public String getObjectName() { return objectName; }
    public void setObjectName(String objectName) { this.objectName = objectName; }
    public String getBeforeValue() { return beforeValue; }
    public void setBeforeValue(String beforeValue) { this.beforeValue = beforeValue; }
    public String getAfterValue() { return afterValue; }
    public void setAfterValue(String afterValue) { this.afterValue = afterValue; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getRelatedObject() { return relatedObject; }
    public void setRelatedObject(String relatedObject) { this.relatedObject = relatedObject; }
    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }
    public Date getAuditTime() { return auditTime; }
    public void setAuditTime(Date auditTime) { this.auditTime = auditTime; }
}
