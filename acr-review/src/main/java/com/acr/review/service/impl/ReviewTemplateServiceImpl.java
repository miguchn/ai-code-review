package com.acr.review.service.impl;

import java.util.List;
import org.springframework.stereotype.Service;
import com.acr.common.exception.ServiceException;
import com.acr.common.utils.SecurityUtils;
import com.acr.common.utils.StringUtils;
import com.acr.review.domain.ReviewPlatformRules;
import com.acr.review.domain.ReviewTemplate;
import com.acr.review.mapper.ReviewTemplateMapper;
import com.acr.review.service.IReviewTemplateService;
import com.acr.review.service.ReviewScoringConstants;

/** 项目审查模板管理。 */
@Service
public class ReviewTemplateServiceImpl implements IReviewTemplateService
{
    private final ReviewTemplateMapper templateMapper;

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
    public int insertReviewTemplate(ReviewTemplate template)
    {
        normalize(template);
        template.setBuiltinFlag("0");
        template.setVersionNo(1);
        checkCodeUnique(template);
        template.setCreateBy(currentUsername());
        return templateMapper.insertReviewTemplate(template);
    }

    @Override
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
        return templateMapper.updateReviewTemplate(template);
    }

    @Override
    public void deleteReviewTemplateByIds(Long[] templateIds)
    {
        if (templateIds == null || templateIds.length == 0)
        {
            return;
        }
        for (Long templateId : templateIds)
        {
            ReviewTemplate template = selectReviewTemplateById(templateId);
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
