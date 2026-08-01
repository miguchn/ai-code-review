package com.acr.review.service.impl;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.Semaphore;
import org.springframework.stereotype.Service;
import com.acr.common.exception.ServiceException;
import com.acr.common.utils.StringUtils;
import com.acr.review.domain.ReviewEngineTestRequest;
import com.acr.review.engine.OpenCodeReviewCliAdapter;
import com.acr.review.engine.OcrModelConfigMapper;
import com.acr.review.engine.ReviewEngine;
import com.acr.review.engine.ReviewEngineFailureType;
import com.acr.review.engine.ReviewEngineInfo;
import com.acr.review.engine.ReviewEngineInvocationType;
import com.acr.review.engine.ReviewEngineRequest;
import com.acr.review.engine.ReviewEngineResult;
import com.acr.review.engine.ReviewEngineSampleWorkspace;
import com.acr.review.engine.ReviewEngineWorkspaceManager;
import com.acr.review.engine.config.ReviewEngineProperties;
import com.acr.review.service.IReviewEngineService;
import com.acr.system.domain.SysAiModelConfig;
import com.acr.system.service.ISysAiModelConfigService;

/** 内置 open-code-review 引擎管理实现。 */
@Service
public class ReviewEngineServiceImpl implements IReviewEngineService
{
    private final ReviewEngineProperties properties;
    private final ReviewEngine reviewEngine;
    private final ReviewEngineWorkspaceManager workspaceManager;
    private final ReviewEngineSampleWorkspace sampleWorkspace;
    private final OcrModelConfigMapper modelConfigMapper;
    private final ISysAiModelConfigService aiModelConfigService;
    private final Semaphore concurrencyLimiter;
    private final ReviewEngineRuntimeState runtimeState = new ReviewEngineRuntimeState();

    public ReviewEngineServiceImpl(ReviewEngineProperties properties, OpenCodeReviewCliAdapter reviewEngine,
        ReviewEngineWorkspaceManager workspaceManager, ReviewEngineSampleWorkspace sampleWorkspace,
        OcrModelConfigMapper modelConfigMapper, ISysAiModelConfigService aiModelConfigService)
    {
        this.properties = properties;
        this.reviewEngine = reviewEngine;
        this.workspaceManager = workspaceManager;
        this.sampleWorkspace = sampleWorkspace;
        this.modelConfigMapper = modelConfigMapper;
        this.aiModelConfigService = aiModelConfigService;
        this.concurrencyLimiter = new Semaphore(Math.max(1, properties.getMaxConcurrency()), true);
    }

    @Override
    public ReviewEngineInfo getEngineInfo()
    {
        ReviewEngineInfo info = new ReviewEngineInfo();
        info.setEngineName(properties.getEngineName());
        info.setEngineType(properties.getEngineType());
        info.setExecutablePath(properties.getExecutablePath());
        info.setConfigSource("review.engine.* / ACR_OCR_*");
        info.setVersion(runtimeState.getVersion());
        info.setAvailabilityStatus(resolveAvailability());
        info.setLastDetectTime(runtimeState.getLastDetectTime());
        info.setLastDetectMessage(runtimeState.getLastDetectMessage());
        info.setLastDetectSuccess(runtimeState.isLastDetectSuccess());
        info.setLastTestTime(runtimeState.getLastTestTime());
        info.setLastTestMessage(runtimeState.getLastTestMessage());
        info.setLastTestSuccess(runtimeState.isLastTestSuccess());
        info.setDefaultTimeoutSeconds(properties.getDefaultTimeoutSeconds());
        info.setMaxConcurrency(properties.getMaxConcurrency());
        info.setMaxOutputBytes(properties.getMaxOutputBytes());
        info.setWorkspaceRoot(workspaceManager.getWorkspaceRootText());
        return info;
    }

    @Override
    public ReviewEngineResult detectEnvironment()
    {
        Path workspace = null;
        boolean acquired = false;
        try
        {
            acquired = acquirePermit();
            if (!acquired)
            {
                return concurrencyFailure();
            }
            workspace = workspaceManager.createIsolatedWorkspace();
            ReviewEngineRequest request = baseRequest(workspace);
            request.setInvocationType(ReviewEngineInvocationType.VERSION);
            ReviewEngineResult result = reviewEngine.execute(request);
            runtimeState.recordDetect(result);
            return result;
        }
        catch (IOException ex)
        {
            ReviewEngineResult result = ReviewEngineResult.failure(properties.getEngineName(), runtimeState.getVersion(),
                0, "", ex.getMessage(), null, ReviewEngineFailureType.WORKSPACE_ERROR, "工作目录初始化失败");
            runtimeState.recordDetect(result);
            return result;
        }
        finally
        {
            workspaceManager.cleanup(workspace);
            releasePermit(acquired);
        }
    }

    @Override
    public ReviewEngineResult testInvoke(ReviewEngineTestRequest testRequest)
    {
        Path workspace = null;
        boolean acquired = false;
        try
        {
            acquired = acquirePermit();
            if (!acquired)
            {
                return concurrencyFailure();
            }

            SysAiModelConfig modelConfig = resolveModelConfig(testRequest != null ? testRequest.getModelId() : null);
            Map<String, String> modelEnvironment = modelConfigMapper.toEnvironment(modelConfig);

            workspace = workspaceManager.createIsolatedWorkspace();
            sampleWorkspace.prepare(workspace);

            ReviewEngineRequest llmRequest = baseRequest(workspace);
            llmRequest.setInvocationType(ReviewEngineInvocationType.LLM_TEST);
            llmRequest.setModelEnvironment(modelEnvironment);
            ReviewEngineResult llmResult = reviewEngine.execute(llmRequest);
            if (!llmResult.isSuccess())
            {
                runtimeState.recordTest(llmResult);
                return llmResult;
            }

            ReviewEngineRequest previewRequest = baseRequest(workspace);
            previewRequest.setInvocationType(ReviewEngineInvocationType.REVIEW_PREVIEW);
            previewRequest.setProjectKey("sample");
            previewRequest.setRepositoryKey("sample-repo");
            previewRequest.setBaseSha("HEAD~1");
            previewRequest.setHeadSha("HEAD");
            previewRequest.setModelEnvironment(modelEnvironment);
            ReviewEngineResult previewResult = reviewEngine.execute(previewRequest);
            runtimeState.recordTest(previewResult);
            return previewResult;
        }
        catch (IOException | InterruptedException ex)
        {
            if (ex instanceof InterruptedException)
            {
                Thread.currentThread().interrupt();
            }
            ReviewEngineResult result = ReviewEngineResult.failure(properties.getEngineName(), runtimeState.getVersion(),
                0, "", ex.getMessage(), null, ReviewEngineFailureType.WORKSPACE_ERROR, "测试样例准备失败");
            runtimeState.recordTest(result);
            return result;
        }
        catch (ServiceException | IllegalArgumentException ex)
        {
            ReviewEngineResult result = ReviewEngineResult.failure(properties.getEngineName(), runtimeState.getVersion(),
                0, "", "", null, ReviewEngineFailureType.MODEL_CALL_FAILED, ex.getMessage());
            runtimeState.recordTest(result);
            return result;
        }
        finally
        {
            workspaceManager.cleanup(workspace);
            releasePermit(acquired);
        }
    }

    private ReviewEngineRequest baseRequest(Path workspace)
    {
        ReviewEngineRequest request = new ReviewEngineRequest();
        request.setWorkingDirectory(workspace.toString());
        request.setTimeoutSeconds(properties.getDefaultTimeoutSeconds());
        return request;
    }

    private SysAiModelConfig resolveModelConfig(Long modelId)
    {
        SysAiModelConfig config = modelId != null
            ? aiModelConfigService.selectRuntimeConfigById(modelId)
            : aiModelConfigService.selectDefaultRuntimeConfig();
        if (config == null)
        {
            throw new ServiceException("未找到可用的模型配置，请先配置并启用默认模型");
        }
        if (!"1".equals(config.getEnabled()))
        {
            throw new ServiceException("所选模型未启用");
        }
        if (StringUtils.isEmpty(config.getApiKey()))
        {
            throw new ServiceException("模型密钥未配置");
        }
        return config;
    }

    private boolean acquirePermit()
    {
        return concurrencyLimiter.tryAcquire();
    }

    private void releasePermit(boolean acquired)
    {
        if (acquired)
        {
            concurrencyLimiter.release();
        }
    }

    private ReviewEngineResult concurrencyFailure()
    {
        return ReviewEngineResult.failure(properties.getEngineName(), runtimeState.getVersion(), 0,
            "", "", null, ReviewEngineFailureType.CONCURRENCY_LIMIT, "审查引擎并发已达上限，请稍后重试");
    }

    private String resolveAvailability()
    {
        if (runtimeState.isLastDetectSuccess())
        {
            return "AVAILABLE";
        }
        if (runtimeState.getLastDetectTime() != null)
        {
            return "UNAVAILABLE";
        }
        return "UNKNOWN";
    }

    private static final class ReviewEngineRuntimeState
    {
        private String version;
        private LocalDateTime lastDetectTime;
        private String lastDetectMessage;
        private boolean lastDetectSuccess;
        private LocalDateTime lastTestTime;
        private String lastTestMessage;
        private boolean lastTestSuccess;

        void recordDetect(ReviewEngineResult result)
        {
            if (result.getEngineVersion() != null)
            {
                version = result.getEngineVersion();
            }
            lastDetectTime = LocalDateTime.now();
            lastDetectSuccess = result.isSuccess();
            lastDetectMessage = result.isSuccess()
                ? "CLI 可用，版本 " + StringUtils.defaultString(result.getEngineVersion(), "未知")
                : StringUtils.defaultString(result.getFailureReason(), "检测失败");
        }

        void recordTest(ReviewEngineResult result)
        {
            if (result.getEngineVersion() != null)
            {
                version = result.getEngineVersion();
            }
            lastTestTime = LocalDateTime.now();
            lastTestSuccess = result.isSuccess();
            lastTestMessage = result.isSuccess()
                ? "测试调用成功，耗时 " + result.getDurationMs() + " ms"
                : StringUtils.defaultString(result.getFailureReason(), "测试调用失败");
        }

        String getVersion()
        {
            return version;
        }

        LocalDateTime getLastDetectTime()
        {
            return lastDetectTime;
        }

        String getLastDetectMessage()
        {
            return lastDetectMessage;
        }

        boolean isLastDetectSuccess()
        {
            return lastDetectSuccess;
        }

        LocalDateTime getLastTestTime()
        {
            return lastTestTime;
        }

        String getLastTestMessage()
        {
            return lastTestMessage;
        }

        boolean isLastTestSuccess()
        {
            return lastTestSuccess;
        }
    }
}
