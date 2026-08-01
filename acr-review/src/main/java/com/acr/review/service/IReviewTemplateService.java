package com.acr.review.service;

import java.util.List;
import com.acr.review.domain.ReviewPlatformRules;
import com.acr.review.domain.ReviewTemplate;

/** 项目审查模板用例。 */
public interface IReviewTemplateService
{
    ReviewTemplate selectReviewTemplateById(Long templateId);
    ReviewTemplate selectEnabledTemplateById(Long templateId);
    List<ReviewTemplate> selectReviewTemplateList(ReviewTemplate template);
    int insertReviewTemplate(ReviewTemplate template);
    int updateReviewTemplate(ReviewTemplate template);
    void deleteReviewTemplateByIds(Long[] templateIds);

    /** 模板页只读展示用的平台统一审查规则（与执行口径同源）。 */
    ReviewPlatformRules getPlatformRules();
}
