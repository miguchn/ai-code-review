package com.acr.review.service.impl;

import org.springframework.stereotype.Service;
import com.acr.common.exception.ServiceException;
import com.acr.common.utils.StringUtils;
import com.acr.review.domain.ReviewPipelineConstants;
import com.acr.review.domain.ReviewProject;
import com.acr.review.domain.ReviewTask;
import com.acr.review.domain.ReviewTemplate;
import com.acr.review.engine.config.ReviewEngineProperties;
import com.acr.review.service.IReviewTaskSnapshotService;
import com.acr.review.service.IReviewTemplateService;
import com.acr.system.domain.SysAiModelConfig;
import com.acr.system.service.ISysAiModelConfigService;

/**
 * 审查任务执行快照冻结。建单与历史任务补冻结共用同一套校验与拷贝逻辑，
 * 保证「执行只读快照」这一不变量在任何入口都一致。
 */
@Service
public class ReviewTaskSnapshotServiceImpl implements IReviewTaskSnapshotService
{
    private final IReviewTemplateService templateService;
    private final ISysAiModelConfigService aiModelConfigService;
    private final ReviewEngineProperties engineProperties;

    public ReviewTaskSnapshotServiceImpl(IReviewTemplateService templateService,
                                         ISysAiModelConfigService aiModelConfigService,
                                         ReviewEngineProperties engineProperties)
    {
        this.templateService = templateService;
        this.aiModelConfigService = aiModelConfigService;
        this.engineProperties = engineProperties;
    }

    @Override
    public void freezeExecutionSnapshot(ReviewProject project, ReviewTask task)
    {
        String reviewMode = ReviewPipelineConstants.normalizeReviewMode(project.getReviewMode());
        if (ReviewPipelineConstants.isLlmDirectMode(reviewMode))
        {
            freezeLlmSnapshot(project, task);
            return;
        }
        if (ReviewPipelineConstants.isOcrEngineMode(reviewMode))
        {
            String engineCode = StringUtils.defaultIfEmpty(project.getEngineCode(),
                ReviewPipelineConstants.ENGINE_OPEN_CODE_REVIEW);
            if (!ReviewPipelineConstants.ENGINE_OPEN_CODE_REVIEW.equals(engineCode))
            {
                throw new ServiceException("当前仅支持 open-code-review 审查引擎");
            }
            task.setSnapshotReviewMode(ReviewPipelineConstants.REVIEW_MODE_OCR_ENGINE);
            task.setSnapshotEngineCode(engineCode);
            task.setSnapshotEngineName(engineProperties.getEngineName());
            clearLlmSnapshot(task);
            return;
        }
        throw new ServiceException("项目未配置有效的审查方式，请在项目「审查执行」中完成配置");
    }

    private void freezeLlmSnapshot(ReviewProject project, ReviewTask task)
    {
        if (project.getModelId() == null)
        {
            throw new ServiceException("大模型审查未配置模型，请在项目「审查执行」中选择模型服务配置");
        }
        if (project.getTemplateId() == null)
        {
            throw new ServiceException("大模型审查未配置审查模板，请在项目「审查执行」中选择审查模板");
        }
        SysAiModelConfig model = aiModelConfigService.selectRuntimeConfigById(project.getModelId());
        if (model == null || !"1".equals(model.getEnabled()) || StringUtils.isEmpty(model.getApiKey()))
        {
            throw new ServiceException("所选模型不存在、未启用或未配置密钥");
        }
        ReviewTemplate template = templateService.selectEnabledTemplateById(project.getTemplateId());
        task.setSnapshotReviewMode(ReviewPipelineConstants.REVIEW_MODE_LLM_DIRECT);
        task.setSnapshotTemplateId(template.getTemplateId());
        task.setSnapshotTemplateName(template.getTemplateName());
        task.setSnapshotTemplateCode(template.getTemplateCode());
        task.setSnapshotTemplateVersion(template.getVersionNo());
        task.setSnapshotPromptContent(template.getContent());
        task.setSnapshotModelId(model.getModelId());
        task.setSnapshotModelName(model.getModelName());
        task.setSnapshotModelProvider(model.getProvider());
        task.setSnapshotModel(model.getModel());
        task.setSnapshotEngineCode(null);
        task.setSnapshotEngineName(null);
    }

    private void clearLlmSnapshot(ReviewTask task)
    {
        task.setSnapshotTemplateId(null);
        task.setSnapshotTemplateName(null);
        task.setSnapshotTemplateCode(null);
        task.setSnapshotTemplateVersion(null);
        task.setSnapshotPromptContent(null);
        task.setSnapshotModelId(null);
        task.setSnapshotModelName(null);
        task.setSnapshotModelProvider(null);
        task.setSnapshotModel(null);
    }
}
