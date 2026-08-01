package com.acr.review.service.impl;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import com.acr.review.domain.ReviewProject;
import com.acr.review.domain.ReviewWebhookEvent;
import com.acr.review.domain.WebhookHandleResult;
import com.acr.review.git.GitPullRequestEvent;
import com.acr.review.git.GitRepositoryCoordinates;
import com.acr.review.git.GitWebhookAdapter;
import com.acr.review.mapper.ReviewProjectMapper;
import com.acr.review.mapper.ReviewWebhookEventMapper;
import com.acr.review.security.CredentialCryptoService;
import com.acr.review.service.IReviewTaskCreateService;
import com.acr.review.service.IReviewWebhookService;
import com.acr.system.service.ISysConfigService;

/** GitHub Webhook 事件接入：验签、项目匹配、分支判断、幂等、建单。 */
@Service
public class ReviewWebhookServiceImpl implements IReviewWebhookService
{
    private static final Logger log = LoggerFactory.getLogger(ReviewWebhookServiceImpl.class);
    private static final String PROVIDER = "GITHUB";
    private static final String PULL_REQUEST_EVENT = "pull_request";
    private static final String PR_EVENTS_KEY = "review.github.prEvents";
    private static final String DEFAULT_PR_EVENTS = "opened,reopened,synchronize";

    private final ReviewWebhookEventMapper eventMapper;
    private final ReviewProjectMapper projectMapper;
    private final GitWebhookAdapter webhookAdapter;
    private final CredentialCryptoService cryptoService;
    private final ISysConfigService configService;
    private final IReviewTaskCreateService taskCreateService;
    private final int maxPayloadBytes;

    public ReviewWebhookServiceImpl(ReviewWebhookEventMapper eventMapper,
                                    ReviewProjectMapper projectMapper,
                                    GitWebhookAdapter webhookAdapter,
                                    CredentialCryptoService cryptoService,
                                    ISysConfigService configService,
                                    IReviewTaskCreateService taskCreateService,
                                    @Value("${review.webhook.max-payload-bytes:262144}") int maxPayloadBytes)
    {
        this.eventMapper = eventMapper;
        this.projectMapper = projectMapper;
        this.webhookAdapter = webhookAdapter;
        this.cryptoService = cryptoService;
        this.configService = configService;
        this.taskCreateService = taskCreateService;
        this.maxPayloadBytes = maxPayloadBytes;
    }

    @Override
    public WebhookHandleResult handleGitHubWebhook(String eventType, String deliveryId, String signatureHeader, byte[] payload)
    {
        int payloadSize = payload == null ? 0 : payload.length;
        if (payloadSize > maxPayloadBytes)
        {
            recordPayloadTooLarge(eventType, deliveryId, payloadSize);
            return WebhookHandleResult.payloadTooLarge("Webhook 载荷超过大小限制");
        }
        if (deliveryId == null || deliveryId.isBlank())
        {
            return WebhookHandleResult.badRequest("缺少 X-GitHub-Delivery 头");
        }
        if (eventType == null || eventType.isBlank())
        {
            return WebhookHandleResult.badRequest("缺少 X-GitHub-Event 头");
        }

        ReviewWebhookEvent event = buildReceivedEvent(eventType, deliveryId, payloadSize);
        try
        {
            eventMapper.insertEvent(event);
        }
        catch (DuplicateKeyException e)
        {
            eventMapper.incrementDuplicate(PROVIDER, deliveryId);
            return WebhookHandleResult.ok("重复投递，已忽略");
        }

        try
        {
            return process(event, signatureHeader, payload);
        }
        catch (RuntimeException e)
        {
            log.error("Webhook 事件处理异常, deliveryId={}", deliveryId, e);
            finishEvent(event, "FAILED", "事件处理内部异常");
            return WebhookHandleResult.ok("事件接收成功，处理结果请查看平台记录");
        }
    }

    private WebhookHandleResult process(ReviewWebhookEvent event, String signatureHeader, byte[] payload)
    {
        GitRepositoryCoordinates repository = webhookAdapter.parseRepository(payload);
        if (repository == null)
        {
            finishEvent(event, "FAILED", "Webhook 载荷非法，无法解析仓库信息");
            return WebhookHandleResult.ok("载荷非法，已记录");
        }
        event.setRepositoryOwner(repository.owner());
        event.setRepositoryName(repository.repository());

        ReviewProject project = projectMapper.selectByRepository(PROVIDER, repository.owner(), repository.repository(), null);
        if (project == null)
        {
            finishEvent(event, "IGNORED", "未匹配到已接入的代码项目");
            return WebhookHandleResult.ok("未匹配到已接入项目，已忽略");
        }
        event.setProjectId(project.getProjectId());
        if (!"0".equals(project.getStatus()))
        {
            finishEventWithProject(event, project, "IGNORED", "项目已停用，事件忽略");
            return WebhookHandleResult.ok("项目已停用，事件忽略");
        }

        if (project.getWebhookSecretCiphertext() == null || project.getWebhookSecretCiphertext().isBlank())
        {
            finishEventWithProject(event, project, "FAILED", "项目未配置 Webhook Secret");
            return WebhookHandleResult.unauthorized("Webhook 签名校验失败");
        }
        String secret = cryptoService.decryptWebhookSecret(project.getWebhookSecretCiphertext());
        if (!webhookAdapter.verifySignature(secret, payload, signatureHeader))
        {
            finishEventWithProject(event, project, "FAILED", "Webhook 签名校验失败");
            return WebhookHandleResult.unauthorized("Webhook 签名校验失败");
        }

        if (!PULL_REQUEST_EVENT.equals(event.getEventType()))
        {
            finishEventWithProject(event, project, "IGNORED", "非 PR 事件（" + event.getEventType() + "），已忽略");
            return WebhookHandleResult.ok("非 PR 事件，已忽略");
        }

        GitPullRequestEvent prEvent = webhookAdapter.parsePullRequestEvent(event.getEventType(), event.getDeliveryId(), payload);
        if (prEvent == null)
        {
            finishEventWithProject(event, project, "FAILED", "PR 载荷解析失败");
            return WebhookHandleResult.ok("PR 载荷解析失败，已记录");
        }
        fillPrFields(event, prEvent);

        List<String> enabledActions = configValues(PR_EVENTS_KEY, DEFAULT_PR_EVENTS);
        if (prEvent.action() == null || !enabledActions.contains(prEvent.action()))
        {
            finishEventWithProject(event, project, "IGNORED", "PR 动作 " + prEvent.action() + " 不在启用范围");
            return WebhookHandleResult.ok("PR 动作未启用，已忽略");
        }
        if (!"0".equals(project.getPrReviewEnabled()))
        {
            finishEventWithProject(event, project, "IGNORED", "项目未启用 PR 审查");
            return WebhookHandleResult.ok("项目未启用 PR 审查，已忽略");
        }
        if (!splitValues(project.getPrTargetBranches()).contains(prEvent.targetBranch()))
        {
            finishEventWithProject(event, project, "IGNORED", "目标分支 " + prEvent.targetBranch() + " 不在审查范围");
            return WebhookHandleResult.ok("目标分支不在审查范围，已忽略");
        }

        Long taskId = taskCreateService.createTaskFromEvent(project, event, prEvent);
        String message = "已受理 PR #" + prEvent.prNumber() + "，生成审查任务 #" + taskId;
        projectMapper.updateLastWebhook(project.getProjectId(), message);
        return WebhookHandleResult.ok(message);
    }

    private void recordPayloadTooLarge(String eventType, String deliveryId, int payloadSize)
    {
        if (deliveryId == null || deliveryId.isBlank() || eventType == null || eventType.isBlank())
        {
            return;
        }
        ReviewWebhookEvent event = buildReceivedEvent(eventType, deliveryId, payloadSize);
        event.setProcessStatus("FAILED");
        event.setProcessMessage("Webhook 载荷超过大小限制");
        event.setProcessTime(new Date());
        try
        {
            eventMapper.insertEvent(event);
        }
        catch (DuplicateKeyException e)
        {
            eventMapper.incrementDuplicate(PROVIDER, deliveryId);
        }
    }

    private ReviewWebhookEvent buildReceivedEvent(String eventType, String deliveryId, int payloadSize)
    {
        ReviewWebhookEvent event = new ReviewWebhookEvent();
        event.setProvider(PROVIDER);
        event.setDeliveryId(deliveryId);
        event.setEventType(eventType);
        event.setProcessStatus("RECEIVED");
        event.setDuplicateCount(0);
        event.setPayloadSize(payloadSize);
        event.setReceiveTime(new Date());
        return event;
    }

    private void fillPrFields(ReviewWebhookEvent event, GitPullRequestEvent prEvent)
    {
        event.setAction(prEvent.action());
        event.setPrNumber(prEvent.prNumber());
        event.setPrTitle(prEvent.prTitle());
        event.setSourceBranch(prEvent.sourceBranch());
        event.setTargetBranch(prEvent.targetBranch());
        event.setBaseSha(prEvent.baseSha());
        event.setHeadSha(prEvent.headSha());
    }

    private void finishEvent(ReviewWebhookEvent event, String status, String message)
    {
        event.setProcessStatus(status);
        event.setProcessMessage(message);
        event.setProcessTime(new Date());
        eventMapper.updateProcessResult(event);
    }

    private void finishEventWithProject(ReviewWebhookEvent event, ReviewProject project, String status, String message)
    {
        finishEvent(event, status, message);
        projectMapper.updateLastWebhook(project.getProjectId(), message);
    }

    private List<String> configValues(String key, String fallback)
    {
        String value = configService.selectConfigByKey(key);
        return splitValues(value == null || value.isBlank() ? fallback : value);
    }

    private List<String> splitValues(String values)
    {
        if (values == null || values.isBlank())
        {
            return List.of();
        }
        return Arrays.stream(values.replace('，', ',').split(","))
            .map(String::trim)
            .filter(value -> !value.isEmpty())
            .collect(Collectors.toList());
    }
}
