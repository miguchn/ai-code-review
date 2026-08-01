package com.acr.review.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import com.acr.common.core.domain.BaseEntity;

/** 项目审查模板 review_template。 */
public class ReviewTemplate extends BaseEntity
{
    private static final long serialVersionUID = 1L;
    private Long templateId;
    private String templateName;
    private String templateCode;
    private String techStack;
    private String content;
    private Integer versionNo;
    private String builtinFlag;
    private String status;

    public Long getTemplateId() { return templateId; }
    public void setTemplateId(Long templateId) { this.templateId = templateId; }

    @NotBlank(message = "模板名称不能为空")
    @Size(max = 100, message = "模板名称不能超过100个字符")
    public String getTemplateName() { return templateName; }
    public void setTemplateName(String templateName) { this.templateName = templateName; }

    @NotBlank(message = "模板编码不能为空")
    @Size(max = 64, message = "模板编码不能超过64个字符")
    public String getTemplateCode() { return templateCode; }
    public void setTemplateCode(String templateCode) { this.templateCode = templateCode; }

    @NotBlank(message = "适用技术栈不能为空")
    @Size(max = 40, message = "适用技术栈不能超过40个字符")
    public String getTechStack() { return techStack; }
    public void setTechStack(String techStack) { this.techStack = techStack; }

    @NotBlank(message = "模板内容不能为空")
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Integer getVersionNo() { return versionNo; }
    public void setVersionNo(Integer versionNo) { this.versionNo = versionNo; }
    public String getBuiltinFlag() { return builtinFlag; }
    public void setBuiltinFlag(String builtinFlag) { this.builtinFlag = builtinFlag; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
