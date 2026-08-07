package com.acr.review.service.impl;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import com.acr.review.domain.ReviewProject;
import com.acr.review.domain.ReviewWebhookEvent;
import com.acr.review.domain.WebhookHandleResult;
import com.acr.review.git.GitAdapterRegistry;
import com.acr.review.git.GitPullRequestEvent;
import com.acr.review.git.GitProviderCodes;
import com.acr.review.git.GitPushEvent;
import com.acr.review.git.GitRepositoryCoordinates;
import com.acr.review.git.GitWebhookAdapter;
import com.acr.review.git.WebhookRequestHeaders;
import com.acr.review.mapper.ReviewProjectMapper;
import com.acr.review.mapper.ReviewWebhookEventMapper;
import com.acr.review.scope.GlobPattern;
import com.acr.review.security.CredentialCryptoService;
import com.acr.review.service.IReviewIssueService;
import com.acr.review.service.IReviewTaskCreateService;
import com.acr.review.service.IReviewWebhookService;
import com.acr.system.service.ISysConfigService;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;

/** Git Webhook 事件接入：验签、项目匹配、分支判断、幂等、建单；PR 关闭联动问题关闭。 */
@Service
public class ReviewWebhookServiceImpl implements IReviewWebhookService
{
    private static final Logger log = LoggerFactory.getLogger(ReviewWebhookServiceImpl.class);
    private static final String DEFAULT_PR_EVENTS = "opened,reopened,synchronize";
    private static final String DEFAULT_PUSH_EVENTS_GITHUB = "push";
    private static final String DEFAULT_PUSH_EVENTS_HOOK = "Push Hook";

    private final ReviewWebhookEventMapper eventMapper;
    private final ReviewProjectMapper projectMapper;
    private final GitAdapterRegistry adapterRegistry;
    private final CredentialCryptoService cryptoService;
    private final ISysConfigService configService;
    private final IReviewTaskCreateService taskCreateService;
    private final IReviewIssueService issueService;
    private final int maxPayloadBytes;

    public ReviewWebhookServiceImpl(ReviewWebhookEventMapper eventMapper,
                                    ReviewProjectMapper projectMapper,
                                    GitAdapterRegistry adapterRegistry,
                                    CredentialCryptoService cryptoService,
                                    ISysConfigService configService,
                                    IReviewTaskCreateService taskCreateService,
                                    IReviewIssueService issueService,
                                    @Value("${review.webhook.max-payload-bytes:262144}") int maxPayloadBytes)
    {
        this.eventMapper = eventMapper;
        this.projectMapper = projectMapper;
        this.adapterRegistry = adapterRegistry;
        this.cryptoService = cryptoService;
        this.configService = configService;
        this.taskCreateService = taskCreateService;
        this.issueService = issueService;
        this.maxPayloadBytes = maxPayloadBytes;
    }

    @Override
    public WebhookHandleResult handleWebhook(String providerCode, WebhookRequestHeaders headers, byte[] payload)
    {
        String provider = normalizeProvider(providerCode);
        GitWebhookAdapter webhookAdapter = adapterRegistry.requireWebhookAdapter(provider);
        WebhookRequestHeaders safeHeaders = headers == null ? WebhookRequestHeaders.empty() : headers;

        int payloadSize = payload == null ? 0 : payload.length;
        if (payloadSize > maxPayloadBytes)
        {
            String eventType = webhookAdapter.resolveEventType(safeHeaders);
            String deliveryId = webhookAdapter.resolveDeliveryId(safeHeaders, payload);
            recordPayloadTooLarge(provider, eventType, deliveryId, payloadSize);
            return WebhookHandleResult.payloadTooLarge("Webhook 载荷超过大小限制");
        }

        String deliveryId = webhookAdapter.resolveDeliveryId(safeHeaders, payload);
        if (deliveryId == null || deliveryId.isBlank())
        {
            return WebhookHandleResult.badRequest("缺少 Webhook 投递 ID");
        }
        String eventType = webhookAdapter.resolveEventType(safeHeaders);
        if (eventType == null || eventType.isBlank())
        {
            return WebhookHandleResult.badRequest("缺少 Webhook 事件类型头");
        }

        ReviewWebhookEvent event = buildReceivedEvent(provider, eventType, deliveryId, payloadSize);
        try
        {
            eventMapper.insertEvent(event);
        }
        catch (DuplicateKeyException e)
        {
            eventMapper.incrementDuplicate(provider, deliveryId);
            return WebhookHandleResult.ok("重复投递，已忽略");
        }

        try
        {
            return process(event, webhookAdapter, safeHeaders, payload);
        }
        catch (RuntimeException e)
        {
            log.error("Webhook 事件处理异常, provider={}, deliveryId={}", provider, deliveryId, e);
            finishEvent(event, "FAILED", "事件处理内部异常");
            return WebhookHandleResult.ok("事件接收成功，处理结果请查看平台记录");
        }
    }

    @Override
    @Deprecated
    public WebhookHandleResult handleGitHubWebhook(String eventType, String deliveryId, String signatureHeader, byte[] payload)
    {
        Map<String, String> headers = new HashMap<>();
        if (eventType != null)
        {
            headers.put("X-GitHub-Event", eventType);
        }
        if (deliveryId != null)
        {
            headers.put("X-GitHub-Delivery", deliveryId);
        }
        if (signatureHeader != null)
        {
            headers.put("X-Hub-Signature-256", signatureHeader);
        }
        return handleWebhook(GitProviderCodes.GITHUB, WebhookRequestHeaders.of(headers), payload);
    }

    private WebhookHandleResult process(ReviewWebhookEvent event, GitWebhookAdapter webhookAdapter,
                                        WebhookRequestHeaders headers, byte[] payload)
    {
        GitRepositoryCoordinates repository = webhookAdapter.parseRepository(payload);
        if (repository == null)
        {
            finishEvent(event, "FAILED", "Webhook 载荷非法，无法解析仓库信息");
            return WebhookHandleResult.ok("载荷非法，已记录");
        }
        event.setRepositoryOwner(repository.owner());
        event.setRepositoryName(repository.repository());
        event.setRepositoryFullPath(repository.fullPath());

        // fullPath 是唯一匹配键（owner/name 仅为展示字段）：不做模糊兜底，避免把事件绑到错误项目。
        // 未命中时事件记录已含载荷 fullPath，可在平台事件列表直接对照项目配置排障。
        ReviewProject project = projectMapper.selectByFullPath(
            event.getProvider(), repository.fullPath(), null);
        if (project == null)
        {
            finishEvent(event, "IGNORED", "未匹配到已接入的代码项目（仓库 " + repository.fullPath() + " 未接入）");
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
        if (!webhookAdapter.verify(secret, payload, headers))
        {
            finishEventWithProject(event, project, "FAILED", "Webhook 签名校验失败");
            return WebhookHandleResult.unauthorized("Webhook 签名校验失败");
        }

        if (webhookAdapter.isPullRequestEventType(event.getEventType()))
        {
            return processPullRequest(event, project, webhookAdapter, payload);
        }
        if (webhookAdapter.isPushEventType(event.getEventType()))
        {
            return processPush(event, project, webhookAdapter, payload);
        }
        finishEventWithProject(event, project, "IGNORED", "非合并请求事件（" + event.getEventType() + "），已忽略");
        return WebhookHandleResult.ok("非合并请求事件，已忽略");
    }

    private WebhookHandleResult processPullRequest(ReviewWebhookEvent event, ReviewProject project,
                                                   GitWebhookAdapter webhookAdapter, byte[] payload)
    {
        GitPullRequestEvent prEvent = webhookAdapter.parsePullRequestEvent(
            event.getEventType(), event.getDeliveryId(), payload);
        if (prEvent == null)
        {
            finishEventWithProject(event, project, "FAILED", "合并请求载荷解析失败");
            return WebhookHandleResult.ok("合并请求载荷解析失败，已记录");
        }
        fillPrFields(event, prEvent);

        // PR 关闭/合并联动：独立于 enabled-actions / 审查启用 / 目标分支，不创建评审任务。
        if (prEvent.isCloseLifecycle())
        {
            boolean merged = prEvent.merged()
                || "merge".equals(prEvent.action())
                || "merged".equals(prEvent.action());
            int closedCount = issueService.closeActiveIssuesForPr(
                project.getProjectId(), prEvent.prNumber(), merged);
            log.info("PR 关闭联动问题关闭, provider={}, projectId={}, prNumber={}, merged={}, closedCount={}",
                event.getProvider(), project.getProjectId(), prEvent.prNumber(), merged, closedCount);
            String message = "合并请求 #" + prEvent.prNumber()
                + (merged ? " 已合并" : " 已关闭")
                + "，联动关闭 " + closedCount + " 条问题";
            finishEventWithProject(event, project, "ACCEPTED", message);
            return WebhookHandleResult.ok(message);
        }

        List<String> enabledActions = configValues(prEventsConfigKey(event.getProvider()), DEFAULT_PR_EVENTS);
        if (prEvent.action() == null || !enabledActions.contains(prEvent.action()))
        {
            finishEventWithProject(event, project, "IGNORED", "合并请求动作 " + prEvent.action() + " 不在启用范围");
            return WebhookHandleResult.ok("合并请求动作未启用，已忽略");
        }
        if (!"0".equals(project.getPrReviewEnabled()))
        {
            finishEventWithProject(event, project, "IGNORED", "项目未启用合并请求审查");
            return WebhookHandleResult.ok("项目未启用合并请求审查，已忽略");
        }
        if (!splitValues(project.getPrTargetBranches()).contains(prEvent.targetBranch()))
        {
            finishEventWithProject(event, project, "IGNORED", "目标分支 " + prEvent.targetBranch() + " 不在审查范围");
            return WebhookHandleResult.ok("目标分支不在审查范围，已忽略");
        }

        Long taskId = taskCreateService.createTaskFromEvent(project, event, prEvent);
        String message = "已受理合并请求 #" + prEvent.prNumber() + "，生成审查任务 #" + taskId;
        projectMapper.updateLastWebhook(project.getProjectId(), message);
        return WebhookHandleResult.ok(message);
    }

    private WebhookHandleResult processPush(ReviewWebhookEvent event, ReviewProject project,
                                            GitWebhookAdapter webhookAdapter, byte[] payload)
    {
        GitPushEvent pushEvent = webhookAdapter.parsePushEvent(
            event.getEventType(), event.getDeliveryId(), payload);
        if (pushEvent == null)
        {
            // GitHub/Gitea 的 tag push 事件类型仍为 push，适配器因非 heads ref 返回 null；记 IGNORED 而非 FAILED。
            if (isTagPushRef(payload))
            {
                finishEventWithProject(event, project, "IGNORED", "tag 推送，忽略");
                return WebhookHandleResult.ok("tag 推送，已忽略");
            }
            finishEventWithProject(event, project, "FAILED", "推送载荷解析失败");
            return WebhookHandleResult.ok("推送载荷解析失败，已记录");
        }
        fillPushFields(event, pushEvent);

        if (pushEvent.deleted())
        {
            finishEventWithProject(event, project, "IGNORED", "分支删除推送，忽略");
            return WebhookHandleResult.ok("分支删除推送，已忽略");
        }
        if (pushEvent.created())
        {
            finishEventWithProject(event, project, "IGNORED", "新分支首次推送，暂不审查");
            return WebhookHandleResult.ok("新分支首次推送，暂不审查");
        }

        List<String> enabledPushEvents = configValues(
            pushEventsConfigKey(event.getProvider()), defaultPushEvents(event.getProvider()));
        if (event.getEventType() == null || !enabledPushEvents.contains(event.getEventType()))
        {
            finishEventWithProject(event, project, "IGNORED",
                "推送事件类型 " + event.getEventType() + " 不在启用范围");
            return WebhookHandleResult.ok("推送事件类型未启用，已忽略");
        }
        if (!"0".equals(project.getPushReviewEnabled()))
        {
            finishEventWithProject(event, project, "IGNORED", "项目未启用推送审查");
            return WebhookHandleResult.ok("项目未启用推送审查，已忽略");
        }
        if (!matchesTriggerBranch(project.getPushTriggerBranches(), pushEvent.branch()))
        {
            finishEventWithProject(event, project, "IGNORED",
                "推送分支 " + pushEvent.branch() + " 不在触发范围");
            return WebhookHandleResult.ok("推送分支不在触发范围，已忽略");
        }

        Long taskId = taskCreateService.createTaskFromPushEvent(project, event, pushEvent);
        String message = "已受理推送 " + pushEvent.branch() + "，生成审查任务 #" + taskId;
        projectMapper.updateLastWebhook(project.getProjectId(), message);
        return WebhookHandleResult.ok(message);
    }

    private void recordPayloadTooLarge(String provider, String eventType, String deliveryId, int payloadSize)
    {
        if (deliveryId == null || deliveryId.isBlank() || eventType == null || eventType.isBlank())
        {
            return;
        }
        ReviewWebhookEvent event = buildReceivedEvent(provider, eventType, deliveryId, payloadSize);
        event.setProcessStatus("FAILED");
        event.setProcessMessage("Webhook 载荷超过大小限制");
        event.setProcessTime(new Date());
        try
        {
            eventMapper.insertEvent(event);
        }
        catch (DuplicateKeyException e)
        {
            eventMapper.incrementDuplicate(provider, deliveryId);
        }
    }

    private ReviewWebhookEvent buildReceivedEvent(String provider, String eventType, String deliveryId, int payloadSize)
    {
        ReviewWebhookEvent event = new ReviewWebhookEvent();
        event.setProvider(provider);
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

    private void fillPushFields(ReviewWebhookEvent event, GitPushEvent pushEvent)
    {
        event.setAction(null);
        event.setPrNumber(null);
        event.setPrTitle(null);
        event.setSourceBranch(pushEvent.branch());
        event.setTargetBranch(pushEvent.branch());
        event.setBaseSha(pushEvent.beforeSha());
        event.setHeadSha(pushEvent.afterSha());
    }

    /** 轻量解析载荷 ref：仅用于区分 tag push 与非法载荷。 */
    static boolean isTagPushRef(byte[] payload)
    {
        if (payload == null || payload.length == 0)
        {
            return false;
        }
        try
        {
            JSONObject root = JSON.parseObject(new String(payload, StandardCharsets.UTF_8));
            if (root == null)
            {
                return false;
            }
            String ref = root.getString("ref");
            return ref != null && ref.startsWith("refs/tags/");
        }
        catch (RuntimeException ex)
        {
            return false;
        }
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

    private static String prEventsConfigKey(String provider)
    {
        return switch (normalizeProvider(provider))
        {
            case GitProviderCodes.GITLAB -> "review.gitlab.mrEvents";
            case GitProviderCodes.GITEE -> "review.gitee.prEvents";
            case GitProviderCodes.GITEA -> "review.gitea.prEvents";
            default -> "review.github.prEvents";
        };
    }

    private static String pushEventsConfigKey(String provider)
    {
        return switch (normalizeProvider(provider))
        {
            case GitProviderCodes.GITLAB -> "review.gitlab.pushEvents";
            case GitProviderCodes.GITEE -> "review.gitee.pushEvents";
            case GitProviderCodes.GITEA -> "review.gitea.pushEvents";
            default -> "review.github.pushEvents";
        };
    }

    private static String defaultPushEvents(String provider)
    {
        return switch (normalizeProvider(provider))
        {
            case GitProviderCodes.GITLAB, GitProviderCodes.GITEE -> DEFAULT_PUSH_EVENTS_HOOK;
            default -> DEFAULT_PUSH_EVENTS_GITHUB;
        };
    }

    /** 推送触发分支匹配：精确名或 glob 通配；空配置视为不匹配。 */
    static boolean matchesTriggerBranch(String configuredBranches, String branch)
    {
        if (branch == null || branch.isBlank())
        {
            return false;
        }
        List<String> patterns = splitBranchValues(configuredBranches);
        if (patterns.isEmpty())
        {
            return false;
        }
        for (String pattern : patterns)
        {
            if (pattern.equals(branch) || GlobPattern.matches(pattern, branch))
            {
                return true;
            }
        }
        return false;
    }

    private static List<String> splitBranchValues(String values)
    {
        if (values == null || values.isBlank())
        {
            return List.of();
        }
        return Arrays.stream(values.replace('，', ',').replace('\n', ',').replace('\r', ',').split(","))
            .map(String::trim)
            .filter(value -> !value.isEmpty())
            .collect(Collectors.toList());
    }

    private static String normalizeProvider(String providerCode)
    {
        if (providerCode == null || providerCode.isBlank())
        {
            throw new IllegalArgumentException("Git Provider 不能为空");
        }
        return providerCode.trim().toUpperCase(Locale.ROOT);
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
