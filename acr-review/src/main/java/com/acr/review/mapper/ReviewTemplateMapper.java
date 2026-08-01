package com.acr.review.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.acr.review.domain.ReviewTemplate;

/** 项目审查模板数据访问。 */
public interface ReviewTemplateMapper
{
    ReviewTemplate selectReviewTemplateById(Long templateId);
    List<ReviewTemplate> selectReviewTemplateList(ReviewTemplate template);
    ReviewTemplate selectByTemplateCode(@Param("templateCode") String templateCode,
                                        @Param("excludeTemplateId") Long excludeTemplateId);
    int countProjectsByTemplateId(@Param("templateId") Long templateId);
    int insertReviewTemplate(ReviewTemplate template);
    int updateReviewTemplate(ReviewTemplate template);
    int deleteReviewTemplateByIds(Long[] templateIds);
}
