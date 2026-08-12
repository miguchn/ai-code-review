package com.acr.review.service.impl;

import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.acr.common.exception.ServiceException;
import com.acr.common.utils.SecurityUtils;
import com.acr.common.utils.StringUtils;
import com.acr.review.domain.ReviewPlatformRules;
import com.acr.review.domain.ReviewTemplate;
import com.acr.review.mapper.ReviewTemplateMapper;
import com.acr.review.service.IReviewTemplateService;
import com.acr.review.service.ReviewScoringConstants;
import com.acr.system.domain.SysBusinessAudit;
import com.acr.system.service.ISysBusinessAuditService;
import com.alibaba.fastjson2.JSON;

/** 项目审查模板管理。 */
@Service
public class ReviewTemplateServiceImpl implements IReviewTemplateService
{
    private final ReviewTemplateMapper templateMapper;

    @Autowired(required = false)
    private ISysBusinessAuditService businessAuditService;

    public ReviewTemplateServiceImpl(ReviewTemplateMapper templateMapper)
    {
        this.templateMapper = templateMapper;
    }

    @Override
    public ReviewPlatformRules getPlatformRules()
    {
        return ReviewScoringConstants.platformRulesForUi();
    }

    @Override
    public ReviewTemplate selectReviewTemplateById(Long templateId)
    {
        ReviewTemplate template = templateMapper.selectReviewTemplateById(templateId);
        if (template == null)
        {
            throw new ServiceException("审查模板不存在");
        }
        return template;
    }

    @Override
    public ReviewTemplate selectEnabledTemplateById(Long templateId)
    {
        ReviewTemplate template = selectReviewTemplateById(templateId);
        if (!"0".equals(template.getStatus()))
        {
            throw new ServiceException("审查模板已停用");
        }
        if (StringUtils.isEmpty(template.getContent()))
        {
            throw new ServiceException("审查模板内容为空");
        }
        return template;
    }

    @Override
    public List<ReviewTemplate> selectReviewTemplateList(ReviewTemplate template)
    {
        return templateMapper.selectReviewTemplateList(template);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertReviewTemplate(ReviewTemplate template)
    {
        normalize(template);
        template.setBuiltinFlag("0");
        template.setVersionNo(1);
        checkCodeUnique(template);
        template.setCreateBy(currentUsername());
        int rows = templateMapper.insertReviewTemplate(template);
        recordTemplateAudit("CREATE", null, template, "审查模板创建");
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateReviewTemplate(ReviewTemplate template)
    {
        if (template.getTemplateId() == null)
        {
            throw new ServiceException("审查模板 ID 不能为空");
        }
        ReviewTemplate existing = selectReviewTemplateById(template.getTemplateId());
        if ("1".equals(existing.getBuiltinFlag()))
        {
            if (!StringUtils.equals(existing.getContent(), template.getContent())
                || !StringUtils.equals(existing.getTemplateCode(), template.getTemplateCode())
                || !StringUtils.equals(existing.getTechStack(), template.getTechStack()))
            {
                throw new ServiceException("内置审查模板不可修改正文、编码或技术栈，请复制后修改");
            }
        }
        normalize(template);
        template.setBuiltinFlag(existing.getBuiltinFlag());
        template.setVersionNo(StringUtils.equals(existing.getContent(), template.getContent())
            ? existing.getVersionNo() : (existing.getVersionNo() == null ? 2 : existing.getVersionNo() + 1));
        checkCodeUnique(template);
        template.setUpdateBy(currentUsername());
        int rows = templateMapper.updateReviewTemplate(template);
        recordTemplateAudit("UPDATE", existing, template, "审查模板策略变更");
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteReviewTemplateByIds(Long[] templateIds)
    {
        if (templateIds == null || templateIds.length == 0)
        {
            return;
        }
        List<ReviewTemplate> templates = new ArrayList<>();
        for (Long templateId : templateIds)
        {
            ReviewTemplate template = selectReviewTemplateById(templateId);
            templates.add(template);
            if ("1".equals(template.getBuiltinFlag()))
            {
                throw new ServiceException("系统内置审查模板不能删除，可复制后修改");
            }
            int references = templateMapper.countProjectsByTemplateId(templateId);
            if (references > 0)
            {
                throw new ServiceException("审查模板“" + template.getTemplateName() + "”已被 "
                    + references + " 个项目引用，不能删除");
            }
        }
        templateMapper.deleteReviewTemplateByIds(templateIds);
        for (ReviewTemplate template : templates)
        {
            recordTemplateAudit("DELETE", template, null, "审查模板删除");
        }
    }

    private void recordTemplateAudit(String action, ReviewTemplate before, ReviewTemplate after, String reason)
    {
        if (businessAuditService == null)
        {
            return;
        }
        ReviewTemplate target = after == null ? before : after;
        SysBusinessAudit audit = new SysBusinessAudit();
        audit.setEventKey("review-template-" + action + "-" + (target.getTemplateId() == null
            ? java.util.UUID.randomUUID() : target.getTemplateId()) + "-" + java.util.UUID.randomUUID());
        audit.setSource("acr-review");
        audit.setAction("REVIEW_TEMPLATE_" + action);
        audit.setObjectType("REVIEW_TEMPLATE");
        audit.setObjectId(target.getTemplateId() == null ? null : String.valueOf(target.getTemplateId()));
        audit.setObjectName(target.getTemplateName());
        audit.setBeforeValue(before == null ? null : JSON.toJSONString(before));
        audit.setAfterValue(after == null ? null : JSON.toJSONString(after));
        audit.setReason(reason);
        businessAuditService.record(audit);
    }

    private void normalize(ReviewTemplate template)
    {
        template.setTemplateName(template.getTemplateName().trim());
        template.setTemplateCode(template.getTemplateCode().trim());
        template.setTechStack(template.getTechStack().trim());
        template.setContent(template.getContent().trim());
        if (!"0".equals(template.getStatus()) && !"1".equals(template.getStatus()))
        {
            template.setStatus("0");
        }
        if (!"1".equals(template.getBuiltinFlag()))
        {
            template.setBuiltinFlag("0");
        }
        if (template.getVersionNo() == null || template.getVersionNo() < 1)
        {
            template.setVersionNo(1);
        }
    }

    private void checkCodeUnique(ReviewTemplate template)
    {
        if (templateMapper.selectByTemplateCode(template.getTemplateCode(), template.getTemplateId()) != null)
        {
            throw new ServiceException("审查模板编码已存在");
        }
    }

    private String currentUsername()
    {
        try
        {
            return SecurityUtils.getUsername();
        }
        catch (ServiceException ex)
        {
            return "system";
        }
    }
}
