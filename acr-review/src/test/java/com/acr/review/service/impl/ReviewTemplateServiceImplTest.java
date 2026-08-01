package com.acr.review.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import com.acr.common.exception.ServiceException;
import com.acr.common.utils.SecurityUtils;
import com.acr.review.domain.ReviewPlatformRules;
import com.acr.review.domain.ReviewTemplate;
import com.acr.review.mapper.ReviewTemplateMapper;
import com.acr.review.service.ReviewScoringConstants;

class ReviewTemplateServiceImplTest
{
    @Test
    void getPlatformRulesReturnsSharedScoringSource()
    {
        ReviewTemplateServiceImpl service = new ReviewTemplateServiceImpl(mock(ReviewTemplateMapper.class));
        ReviewPlatformRules rules = service.getPlatformRules();

        assertEquals(ReviewScoringConstants.PROTOCOL_VERSION, rules.getProtocolVersion());
        assertEquals(5, rules.getDimensions().size());
        assertEquals(40, rules.getDimensions().get(0).getMaxScore());
        assertFalse(rules.getTopIssuesHint().isBlank());
    }

    @Test
    void rejectsDeletionOfBuiltinTemplate()
    {
        ReviewTemplateMapper mapper = mock(ReviewTemplateMapper.class);
        ReviewTemplate builtin = template(1L, "builtin_java", "原始内容", "1", 1);
        when(mapper.selectReviewTemplateById(1L)).thenReturn(builtin);
        ReviewTemplateServiceImpl service = new ReviewTemplateServiceImpl(mapper);

        assertThrows(ServiceException.class, () -> service.deleteReviewTemplateByIds(new Long[] { 1L }));

        verify(mapper, never()).deleteReviewTemplateByIds(any());
    }

    @Test
    void insertAlwaysCreatesCustomTemplateFromVersionOne()
    {
        ReviewTemplateMapper mapper = mock(ReviewTemplateMapper.class);
        when(mapper.selectByTemplateCode("team_java", null)).thenReturn(null);
        ReviewTemplateServiceImpl service = new ReviewTemplateServiceImpl(mapper);
        ReviewTemplate add = template(null, "team_java", "审查内容", "1", 9);

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class))
        {
            securityUtils.when(SecurityUtils::getUsername).thenReturn("tester");
            service.insertReviewTemplate(add);

            ArgumentCaptor<ReviewTemplate> captor = ArgumentCaptor.forClass(ReviewTemplate.class);
            verify(mapper).insertReviewTemplate(captor.capture());
            ReviewTemplate inserted = captor.getValue();
            // 即使调用方传入内置标记与高版本号，新增也强制为自定义模板 v1
            assertEquals("0", inserted.getBuiltinFlag());
            assertEquals(1, inserted.getVersionNo());
        }
    }

    @Test
    void incrementsVersionWhenCustomTemplateContentChanges()
    {
        ReviewTemplateMapper mapper = mock(ReviewTemplateMapper.class);
        ReviewTemplate existing = template(1L, "team_java", "原始内容", "0", 2);
        when(mapper.selectReviewTemplateById(1L)).thenReturn(existing);
        when(mapper.selectByTemplateCode("team_java", 1L)).thenReturn(null);
        ReviewTemplateServiceImpl service = new ReviewTemplateServiceImpl(mapper);
        ReviewTemplate update = template(1L, "team_java", "新内容", "0", 2);
        update.setTemplateName("团队 Java 模板");
        update.setTechStack("JAVA");
        update.setStatus("0");

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class))
        {
            securityUtils.when(SecurityUtils::getUsername).thenReturn("tester");
            service.updateReviewTemplate(update);

            assertEquals(3, update.getVersionNo());
        }
    }

    private ReviewTemplate template(Long id, String code, String content, String builtinFlag, int versionNo)
    {
        ReviewTemplate template = new ReviewTemplate();
        template.setTemplateId(id);
        template.setTemplateName("模板");
        template.setTemplateCode(code);
        template.setTechStack("FULLSTACK");
        template.setContent(content);
        template.setBuiltinFlag(builtinFlag);
        template.setVersionNo(versionNo);
        template.setStatus("0");
        return template;
    }
}
