package com.acr.review.service.impl;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;
import com.acr.review.domain.ReviewProject;
import com.acr.review.domain.ReviewWebhookEvent;
import com.acr.review.domain.WebhookHandleResult;
import com.acr.review.git.GitAdapterRegistry;
import com.acr.review.git.GitPullRequestEvent;
import com.acr.review.git.GitPushEvent;
import com.acr.review.git.GitRepositoryCoordinates;
import com.acr.review.git.GitWebhookAdapter;
import com.acr.review.git.WebhookRequestHeaders;
import com.acr.review.git.github.GitHubWebhookAdapter;
import com.acr.review.insight.ReviewCommitFactIngestService;
import com.acr.review.mapper.ReviewProjectMapper;
import com.acr.review.mapper.ReviewWebhookEventMapper;
import com.acr.review.security.CredentialCryptoService;
import com.acr.review.service.IReviewIssueService;
import com.acr.review.service.IReviewTaskCreateService;
import com.acr.system.service.ISysConfigService;

class ReviewWebhookServiceImplTest
{
    private static final byte[] PAYLOAD = "{}".getBytes(StandardCharsets.UTF_8);
    private static final GitRepositoryCoordinates REPO =
        new GitRepositoryCoordinates("miguchn", "demo", "miguchn/demo", "https://github.com/miguchn/demo");
    private static final GitPullRequestEvent PR_EVENT = new GitPullRequestEvent(
        "d-1", "opened", "miguchn", "demo", 12, "feat: login",
        "feature/login", "dev", "aaaabbbbccccddddeeeeffff0000111122223333", "ffffeeeeddddccccbbbbaaaa3333222211110000");
    private static final String ZERO_SHA = "0000000000000000000000000000000000000000";
    private static final GitPushEvent PUSH_EVENT = new GitPushEvent(
        "d-1", "miguchn", "demo", "miguchn/demo", "dev",
        "aaaabbbbccccddddeeeeffff0000111122223333", "ffffeeeeddddccccbbbbaaaa3333222211110000",
        "alice", 2, "feat: push login", false, false, java.util.List.of());

    private ReviewWebhookEventMapper eventMapper;
    private ReviewProjectMapper projectMapper;
    private GitWebhookAdapter webhookAdapter;
    private GitAdapterRegistry adapterRegistry;
    private CredentialCryptoService cryptoService;
    private ISysConfigService configService;
    private IReviewTaskCreateService taskCreateService;
    private IReviewIssueService issueService;
    private ReviewCommitFactIngestService commitFactIngestService;
    private ReviewWebhookServiceImpl service;

    @BeforeEach
    void setUp()
    {
        eventMapper = mock(ReviewWebhookEventMapper.class);
        projectMapper = mock(ReviewProjectMapper.class);
        webhookAdapter = mock(GitWebhookAdapter.class);
        adapterRegistry = mock(GitAdapterRegistry.class);
        cryptoService = mock(CredentialCryptoService.class);
        configService = mock(ISysConfigService.class);
        taskCreateService = mock(IReviewTaskCreateService.class);
        issueService = mock(IReviewIssueService.class);
        commitFactIngestService = mock(ReviewCommitFactIngestService.class);
        service = new ReviewWebhookServiceImpl(eventMapper, projectMapper, adapterRegistry,
            cryptoService, configService, taskCreateService, issueService, commitFactIngestService, 262144);

        when(adapterRegistry.requireWebhookAdapter("GITHUB")).thenReturn(webhookAdapter);
        when(webhookAdapter.resolveDeliveryId(any(), eq(PAYLOAD))).thenReturn("d-1");
        when(webhookAdapter.resolveEventType(any())).thenAnswer(inv -> {
            WebhookRequestHeaders headers = inv.getArgument(0);
            return headers == null ? null : headers.get("X-GitHub-Event");
        });
        when(webhookAdapter.isPullRequestEventType("pull_request")).thenReturn(true);
        when(webhookAdapter.isPullRequestEventType("ping")).thenReturn(false);
        when(webhookAdapter.isPullRequestEventType("push")).thenReturn(false);
        when(webhookAdapter.isPushEventType("push")).thenReturn(true);
        when(webhookAdapter.isPushEventType("ping")).thenReturn(false);
        when(webhookAdapter.isPushEventType("pull_request")).thenReturn(false);
        // mock 不走接口 default；默认恒等，与其它平台 JSON 体行为一致。
        when(webhookAdapter.unwrapPayload(any())).thenAnswer(inv -> inv.getArgument(0));
        when(webhookAdapter.parseRepository(PAYLOAD)).thenReturn(REPO);
        when(configService.selectConfigByKey("review.github.prEvents")).thenReturn("opened,reopened,synchronize");
        when(configService.selectConfigByKey("review.github.pushEvents")).thenReturn("push");
    }

    @Test
    void acceptsValidPrEventAndCreatesTask()
    {
        ReviewProject project = enabledProject();
        when(projectMapper.selectByFullPath("GITHUB", "miguchn/demo", null)).thenReturn(project);
        when(cryptoService.decryptWebhookSecret("cipher")).thenReturn("secret");
        when(webhookAdapter.verify(eq("secret"), eq(PAYLOAD), any())).thenReturn(true);
        when(webhookAdapter.parsePullRequestEvent("pull_request", "d-1", PAYLOAD)).thenReturn(PR_EVENT);
        when(taskCreateService.createTaskFromEvent(eq(project), any(), eq(PR_EVENT))).thenReturn(100L);

        WebhookHandleResult result = service.handleGitHubWebhook("pull_request", "d-1", "sig", PAYLOAD);

        assertEquals(200, result.httpStatus());
        assertTrue(result.message().contains("100"));
        verify(taskCreateService).createTaskFromEvent(eq(project), any(), eq(PR_EVENT));
        verify(projectMapper).updateLastWebhook(eq(1L), any());
    }

    @Test
    void ignoresDuplicateDeliveryWithoutNewTask()
    {
        when(eventMapper.insertEvent(any())).thenThrow(new DuplicateKeyException("dup"));

        WebhookHandleResult result = service.handleGitHubWebhook("pull_request", "d-1", "sig", PAYLOAD);

        assertEquals(200, result.httpStatus());
        verify(eventMapper).incrementDuplicate("GITHUB", "d-1");
        verify(taskCreateService, never()).createTaskFromEvent(any(), any(), any());
    }

    @Test
    void ignoresEventWhenProjectNotMatched()
    {
        when(projectMapper.selectByFullPath("GITHUB", "miguchn/demo", null)).thenReturn(null);

        WebhookHandleResult result = service.handleGitHubWebhook("pull_request", "d-1", "sig", PAYLOAD);

        assertEquals(200, result.httpStatus());
        verify(eventMapper).updateProcessResult(argMatchesStatus("IGNORED"));
        verify(taskCreateService, never()).createTaskFromEvent(any(), any(), any());
    }

    @Test
    void ignoresEventWhenProjectDisabled()
    {
        ReviewProject project = enabledProject();
        project.setStatus("1");
        when(projectMapper.selectByFullPath("GITHUB", "miguchn/demo", null)).thenReturn(project);

        WebhookHandleResult result = service.handleGitHubWebhook("pull_request", "d-1", "sig", PAYLOAD);

        assertEquals(200, result.httpStatus());
        verify(eventMapper).updateProcessResult(argMatchesStatus("IGNORED"));
        verify(webhookAdapter, never()).verify(any(), any(), any());
    }

    @Test
    void failsWhenSecretNotConfigured()
    {
        ReviewProject project = enabledProject();
        project.setWebhookSecretCiphertext(null);
        when(projectMapper.selectByFullPath("GITHUB", "miguchn/demo", null)).thenReturn(project);

        WebhookHandleResult result = service.handleGitHubWebhook("pull_request", "d-1", "sig", PAYLOAD);

        assertEquals(401, result.httpStatus());
        verify(eventMapper).updateProcessResult(argMatchesStatus("FAILED"));
    }

    @Test
    void failsWhenSignatureInvalid()
    {
        when(projectMapper.selectByFullPath("GITHUB", "miguchn/demo", null)).thenReturn(enabledProject());
        when(cryptoService.decryptWebhookSecret("cipher")).thenReturn("secret");
        when(webhookAdapter.verify(eq("secret"), eq(PAYLOAD), any())).thenReturn(false);

        WebhookHandleResult result = service.handleGitHubWebhook("pull_request", "d-1", "bad-sig", PAYLOAD);

        assertEquals(401, result.httpStatus());
        verify(eventMapper).updateProcessResult(argMatchesStatus("FAILED"));
        verify(taskCreateService, never()).createTaskFromEvent(any(), any(), any());
    }

    @Test
    void ignoresNonPullRequestEvent()
    {
        when(projectMapper.selectByFullPath("GITHUB", "miguchn/demo", null)).thenReturn(enabledProject());
        when(cryptoService.decryptWebhookSecret("cipher")).thenReturn("secret");
        when(webhookAdapter.verify(eq("secret"), eq(PAYLOAD), any())).thenReturn(true);

        WebhookHandleResult result = service.handleGitHubWebhook("ping", "d-1", "sig", PAYLOAD);

        assertEquals(200, result.httpStatus());
        verify(eventMapper).updateProcessResult(argMatchesStatus("IGNORED"));
    }

    @Test
    void ignoresActionOutsideWhitelist()
    {
        when(projectMapper.selectByFullPath("GITHUB", "miguchn/demo", null)).thenReturn(enabledProject());
        when(cryptoService.decryptWebhookSecret("cipher")).thenReturn("secret");
        when(webhookAdapter.verify(eq("secret"), eq(PAYLOAD), any())).thenReturn(true);
        when(webhookAdapter.parsePullRequestEvent("pull_request", "d-1", PAYLOAD))
            .thenReturn(new GitPullRequestEvent("d-1", "edited", "miguchn", "demo", 12, "t",
                "feature/login", "dev", PR_EVENT.baseSha(), PR_EVENT.headSha()));

        WebhookHandleResult result = service.handleGitHubWebhook("pull_request", "d-1", "sig", PAYLOAD);

        assertEquals(200, result.httpStatus());
        verify(eventMapper).updateProcessResult(argMatchesStatus("IGNORED"));
        verify(taskCreateService, never()).createTaskFromEvent(any(), any(), any());
        verify(issueService, never()).closeActiveIssuesForPr(any(), any(), anyBoolean());
    }

    @Test
    void closesActiveIssuesOnPrClosedWithoutCreatingTask()
    {
        when(projectMapper.selectByFullPath("GITHUB", "miguchn/demo", null)).thenReturn(enabledProject());
        when(cryptoService.decryptWebhookSecret("cipher")).thenReturn("secret");
        when(webhookAdapter.verify(eq("secret"), eq(PAYLOAD), any())).thenReturn(true);
        when(webhookAdapter.parsePullRequestEvent("pull_request", "d-1", PAYLOAD))
            .thenReturn(new GitPullRequestEvent("d-1", "closed", "miguchn", "demo", "miguchn/demo", 12, "t",
                "feature/login", "dev", PR_EVENT.baseSha(), PR_EVENT.headSha(), null, null, null, null, false));
        when(issueService.closeActiveIssuesForPr(1L, 12, false)).thenReturn(3);

        WebhookHandleResult result = service.handleGitHubWebhook("pull_request", "d-1", "sig", PAYLOAD);

        assertEquals(200, result.httpStatus());
        assertTrue(result.message().contains("联动关闭 3 条问题"));
        verify(issueService).closeActiveIssuesForPr(1L, 12, false);
        verify(taskCreateService, never()).createTaskFromEvent(any(), any(), any());
        verify(eventMapper).updateProcessResult(argMatchesStatus("ACCEPTED"));
        verify(configService, never()).selectConfigByKey(any());
    }

    @Test
    void closesActiveIssuesOnPrMergedIndependentOfReviewEnabled()
    {
        ReviewProject project = enabledProject();
        project.setPrReviewEnabled("1");
        when(projectMapper.selectByFullPath("GITHUB", "miguchn/demo", null)).thenReturn(project);
        when(cryptoService.decryptWebhookSecret("cipher")).thenReturn("secret");
        when(webhookAdapter.verify(eq("secret"), eq(PAYLOAD), any())).thenReturn(true);
        when(webhookAdapter.parsePullRequestEvent("pull_request", "d-1", PAYLOAD))
            .thenReturn(new GitPullRequestEvent("d-1", "closed", "miguchn", "demo", "miguchn/demo", 12, "t",
                "feature/login", "main", PR_EVENT.baseSha(), PR_EVENT.headSha(), null, null, null, null, true));
        when(issueService.closeActiveIssuesForPr(1L, 12, true)).thenReturn(0);

        WebhookHandleResult result = service.handleGitHubWebhook("pull_request", "d-1", "sig", PAYLOAD);

        assertEquals(200, result.httpStatus());
        assertTrue(result.message().contains("已合并"));
        verify(issueService).closeActiveIssuesForPr(1L, 12, true);
        verify(taskCreateService, never()).createTaskFromEvent(any(), any(), any());
    }

    @Test
    void ignoresWhenTargetBranchNotConfigured()
    {
        when(projectMapper.selectByFullPath("GITHUB", "miguchn/demo", null)).thenReturn(enabledProject());
        when(cryptoService.decryptWebhookSecret("cipher")).thenReturn("secret");
        when(webhookAdapter.verify(eq("secret"), eq(PAYLOAD), any())).thenReturn(true);
        when(webhookAdapter.parsePullRequestEvent("pull_request", "d-1", PAYLOAD))
            .thenReturn(new GitPullRequestEvent("d-1", "opened", "miguchn", "demo", 12, "t",
                "feature/login", "main", PR_EVENT.baseSha(), PR_EVENT.headSha()));

        WebhookHandleResult result = service.handleGitHubWebhook("pull_request", "d-1", "sig", PAYLOAD);

        assertEquals(200, result.httpStatus());
        verify(eventMapper).updateProcessResult(argMatchesStatus("IGNORED"));
        verify(taskCreateService, never()).createTaskFromEvent(any(), any(), any());
    }

    @Test
    void ignoresWhenPrReviewDisabled()
    {
        ReviewProject project = enabledProject();
        project.setPrReviewEnabled("1");
        when(projectMapper.selectByFullPath("GITHUB", "miguchn/demo", null)).thenReturn(project);
        when(cryptoService.decryptWebhookSecret("cipher")).thenReturn("secret");
        when(webhookAdapter.verify(eq("secret"), eq(PAYLOAD), any())).thenReturn(true);
        when(webhookAdapter.parsePullRequestEvent("pull_request", "d-1", PAYLOAD)).thenReturn(PR_EVENT);

        WebhookHandleResult result = service.handleGitHubWebhook("pull_request", "d-1", "sig", PAYLOAD);

        assertEquals(200, result.httpStatus());
        verify(eventMapper).updateProcessResult(argMatchesStatus("IGNORED"));
        verify(taskCreateService, never()).createTaskFromEvent(any(), any(), any());
    }

    @Test
    void rejectsOversizedPayload()
    {
        byte[] huge = new byte[262145];

        WebhookHandleResult result = service.handleGitHubWebhook("pull_request", "d-1", "sig", huge);

        assertEquals(413, result.httpStatus());
        verify(taskCreateService, never()).createTaskFromEvent(any(), any(), any());
    }

    @Test
    void rejectsMissingDeliveryId()
    {
        when(webhookAdapter.resolveDeliveryId(any(), eq(PAYLOAD))).thenReturn(null);

        WebhookHandleResult result = service.handleGitHubWebhook("pull_request", null, "sig", PAYLOAD);

        assertEquals(400, result.httpStatus());
    }

    @Test
    void handleWebhookUsesProviderSpecificConfigKey()
    {
        when(adapterRegistry.requireWebhookAdapter("GITLAB")).thenReturn(webhookAdapter);
        when(webhookAdapter.resolveDeliveryId(any(), eq(PAYLOAD))).thenReturn("d-2");
        when(webhookAdapter.resolveEventType(any())).thenReturn("Merge Request Hook");
        when(webhookAdapter.isPullRequestEventType("Merge Request Hook")).thenReturn(true);
        when(projectMapper.selectByFullPath("GITLAB", "miguchn/demo", null)).thenReturn(enabledProject());
        when(cryptoService.decryptWebhookSecret("cipher")).thenReturn("secret");
        when(webhookAdapter.verify(eq("secret"), eq(PAYLOAD), any())).thenReturn(true);
        when(webhookAdapter.parsePullRequestEvent("Merge Request Hook", "d-2", PAYLOAD)).thenReturn(PR_EVENT);
        when(configService.selectConfigByKey("review.gitlab.mrEvents")).thenReturn("opened,reopened,synchronize");
        when(taskCreateService.createTaskFromEvent(any(), any(), eq(PR_EVENT))).thenReturn(200L);

        WebhookHandleResult result = service.handleWebhook("GITLAB",
            WebhookRequestHeaders.of(Map.of("X-Gitlab-Event", "Merge Request Hook")), PAYLOAD);

        assertEquals(200, result.httpStatus());
        verify(configService).selectConfigByKey("review.gitlab.mrEvents");
    }

    @Test
    void acceptsValidPushEventAndCreatesTask()
    {
        ReviewProject project = pushEnabledProject();
        when(projectMapper.selectByFullPath("GITHUB", "miguchn/demo", null)).thenReturn(project);
        when(cryptoService.decryptWebhookSecret("cipher")).thenReturn("secret");
        when(webhookAdapter.verify(eq("secret"), eq(PAYLOAD), any())).thenReturn(true);
        when(webhookAdapter.parsePushEvent("push", "d-1", PAYLOAD)).thenReturn(PUSH_EVENT);
        when(taskCreateService.createTaskFromPushEvent(eq(project), any(), eq(PUSH_EVENT))).thenReturn(300L);
        doAnswer(inv -> {
            ReviewWebhookEvent event = inv.getArgument(0);
            event.setEventId(55L);
            return 1;
        }).when(eventMapper).insertEvent(any());

        WebhookHandleResult result = service.handleGitHubWebhook("push", "d-1", "sig", PAYLOAD);

        assertEquals(200, result.httpStatus());
        assertTrue(result.message().contains("300"));
        verify(taskCreateService).createTaskFromPushEvent(eq(project), any(), eq(PUSH_EVENT));
        verify(taskCreateService, never()).createTaskFromEvent(any(), any(), any());
        verify(configService).selectConfigByKey("review.github.pushEvents");
        verify(commitFactIngestService).ingestFromPush(eq(1L), eq(55L), eq(PUSH_EVENT));
    }

    @Test
    void pushAcceptSucceedsEvenWhenCommitFactIngestThrows()
    {
        ReviewProject project = pushEnabledProject();
        when(projectMapper.selectByFullPath("GITHUB", "miguchn/demo", null)).thenReturn(project);
        when(cryptoService.decryptWebhookSecret("cipher")).thenReturn("secret");
        when(webhookAdapter.verify(eq("secret"), eq(PAYLOAD), any())).thenReturn(true);
        when(webhookAdapter.parsePushEvent("push", "d-1", PAYLOAD)).thenReturn(PUSH_EVENT);
        when(taskCreateService.createTaskFromPushEvent(eq(project), any(), eq(PUSH_EVENT))).thenReturn(301L);
        doAnswer(inv -> {
            ReviewWebhookEvent event = inv.getArgument(0);
            event.setEventId(56L);
            return 1;
        }).when(eventMapper).insertEvent(any());
        doThrow(new RuntimeException("ingest boom")).when(commitFactIngestService)
            .ingestFromPush(any(), any(), any());

        WebhookHandleResult result = service.handleGitHubWebhook("push", "d-1", "sig", PAYLOAD);

        assertEquals(200, result.httpStatus());
        assertTrue(result.message().contains("301"));
        verify(taskCreateService).createTaskFromPushEvent(eq(project), any(), eq(PUSH_EVENT));
        verify(projectMapper).updateLastWebhook(eq(1L), any());
    }

    @Test
    void ignoresPushWhenPushReviewDisabled()
    {
        ReviewProject project = pushEnabledProject();
        project.setPushReviewEnabled("1");
        when(projectMapper.selectByFullPath("GITHUB", "miguchn/demo", null)).thenReturn(project);
        when(cryptoService.decryptWebhookSecret("cipher")).thenReturn("secret");
        when(webhookAdapter.verify(eq("secret"), eq(PAYLOAD), any())).thenReturn(true);
        when(webhookAdapter.parsePushEvent("push", "d-1", PAYLOAD)).thenReturn(PUSH_EVENT);

        WebhookHandleResult result = service.handleGitHubWebhook("push", "d-1", "sig", PAYLOAD);

        assertEquals(200, result.httpStatus());
        verify(eventMapper).updateProcessResult(argMatchesStatus("IGNORED"));
        verify(taskCreateService, never()).createTaskFromPushEvent(any(), any(), any());
    }

    @Test
    void ignoresDeletedBranchPush()
    {
        GitPushEvent deleted = new GitPushEvent(
            "d-1", "miguchn", "demo", "miguchn/demo", "dev",
            PUSH_EVENT.beforeSha(), ZERO_SHA, "alice", 0, null, false, true, java.util.List.of());
        when(projectMapper.selectByFullPath("GITHUB", "miguchn/demo", null)).thenReturn(pushEnabledProject());
        when(cryptoService.decryptWebhookSecret("cipher")).thenReturn("secret");
        when(webhookAdapter.verify(eq("secret"), eq(PAYLOAD), any())).thenReturn(true);
        when(webhookAdapter.parsePushEvent("push", "d-1", PAYLOAD)).thenReturn(deleted);

        WebhookHandleResult result = service.handleGitHubWebhook("push", "d-1", "sig", PAYLOAD);

        assertEquals(200, result.httpStatus());
        verify(eventMapper).updateProcessResult(argMatchesStatus("IGNORED"));
        verify(taskCreateService, never()).createTaskFromPushEvent(any(), any(), any());
    }

    @Test
    void ignoresCreatedBranchPush()
    {
        GitPushEvent created = new GitPushEvent(
            "d-1", "miguchn", "demo", "miguchn/demo", "dev",
            ZERO_SHA, PUSH_EVENT.afterSha(), "alice", 1, "init", true, false, java.util.List.of());
        when(projectMapper.selectByFullPath("GITHUB", "miguchn/demo", null)).thenReturn(pushEnabledProject());
        when(cryptoService.decryptWebhookSecret("cipher")).thenReturn("secret");
        when(webhookAdapter.verify(eq("secret"), eq(PAYLOAD), any())).thenReturn(true);
        when(webhookAdapter.parsePushEvent("push", "d-1", PAYLOAD)).thenReturn(created);

        WebhookHandleResult result = service.handleGitHubWebhook("push", "d-1", "sig", PAYLOAD);

        assertEquals(200, result.httpStatus());
        verify(eventMapper).updateProcessResult(argMatchesStatus("IGNORED"));
        verify(taskCreateService, never()).createTaskFromPushEvent(any(), any(), any());
    }

    @Test
    void ignoresPushWhenBranchNotInTriggerList()
    {
        GitPushEvent other = new GitPushEvent(
            "d-1", "miguchn", "demo", "miguchn/demo", "feature/x",
            PUSH_EVENT.beforeSha(), PUSH_EVENT.afterSha(), "alice", 1, "wip", false, false, java.util.List.of());
        when(projectMapper.selectByFullPath("GITHUB", "miguchn/demo", null)).thenReturn(pushEnabledProject());
        when(cryptoService.decryptWebhookSecret("cipher")).thenReturn("secret");
        when(webhookAdapter.verify(eq("secret"), eq(PAYLOAD), any())).thenReturn(true);
        when(webhookAdapter.parsePushEvent("push", "d-1", PAYLOAD)).thenReturn(other);

        WebhookHandleResult result = service.handleGitHubWebhook("push", "d-1", "sig", PAYLOAD);

        assertEquals(200, result.httpStatus());
        verify(eventMapper).updateProcessResult(argMatchesStatus("IGNORED"));
        verify(taskCreateService, never()).createTaskFromPushEvent(any(), any(), any());
    }

    @Test
    void acceptsPushWhenBranchMatchesGlob()
    {
        ReviewProject project = pushEnabledProject();
        project.setPushTriggerBranches("release/*");
        GitPushEvent releasePush = new GitPushEvent(
            "d-1", "miguchn", "demo", "miguchn/demo", "release/1.0",
            PUSH_EVENT.beforeSha(), PUSH_EVENT.afterSha(), "alice", 1, "cut", false, false, java.util.List.of());
        when(projectMapper.selectByFullPath("GITHUB", "miguchn/demo", null)).thenReturn(project);
        when(cryptoService.decryptWebhookSecret("cipher")).thenReturn("secret");
        when(webhookAdapter.verify(eq("secret"), eq(PAYLOAD), any())).thenReturn(true);
        when(webhookAdapter.parsePushEvent("push", "d-1", PAYLOAD)).thenReturn(releasePush);
        when(taskCreateService.createTaskFromPushEvent(eq(project), any(), eq(releasePush))).thenReturn(301L);

        WebhookHandleResult result = service.handleGitHubWebhook("push", "d-1", "sig", PAYLOAD);

        assertEquals(200, result.httpStatus());
        verify(taskCreateService).createTaskFromPushEvent(eq(project), any(), eq(releasePush));
    }

    @Test
    void failsWhenPushPayloadInvalid()
    {
        when(projectMapper.selectByFullPath("GITHUB", "miguchn/demo", null)).thenReturn(pushEnabledProject());
        when(cryptoService.decryptWebhookSecret("cipher")).thenReturn("secret");
        when(webhookAdapter.verify(eq("secret"), eq(PAYLOAD), any())).thenReturn(true);
        when(webhookAdapter.parsePushEvent("push", "d-1", PAYLOAD)).thenReturn(null);

        WebhookHandleResult result = service.handleGitHubWebhook("push", "d-1", "sig", PAYLOAD);

        assertEquals(200, result.httpStatus());
        verify(eventMapper).updateProcessResult(argMatchesStatus("FAILED"));
        verify(taskCreateService, never()).createTaskFromPushEvent(any(), any(), any());
    }

    @Test
    void ignoresTagPushAsIgnoredNotFailed()
    {
        byte[] tagPayload = """
            {
              "ref": "refs/tags/v1.0.0",
              "before": "aaaabbbbccccddddeeeeffff0000111122223333",
              "after": "ffffeeeeddddccccbbbbaaaa3333222211110000",
              "repository": { "name": "demo", "owner": { "login": "miguchn" } }
            }
            """.getBytes(StandardCharsets.UTF_8);
        when(webhookAdapter.resolveDeliveryId(any(), eq(tagPayload))).thenReturn("d-tag");
        when(webhookAdapter.parseRepository(tagPayload)).thenReturn(REPO);
        when(projectMapper.selectByFullPath("GITHUB", "miguchn/demo", null)).thenReturn(pushEnabledProject());
        when(cryptoService.decryptWebhookSecret("cipher")).thenReturn("secret");
        when(webhookAdapter.verify(eq("secret"), eq(tagPayload), any())).thenReturn(true);
        when(webhookAdapter.parsePushEvent("push", "d-tag", tagPayload)).thenReturn(null);

        WebhookHandleResult result = service.handleGitHubWebhook("push", "d-tag", "sig", tagPayload);

        assertEquals(200, result.httpStatus());
        verify(eventMapper).updateProcessResult(org.mockito.ArgumentMatchers.argThat(event ->
            event != null && "IGNORED".equals(event.getProcessStatus())
                && event.getProcessMessage() != null && event.getProcessMessage().contains("tag")));
        verify(taskCreateService, never()).createTaskFromPushEvent(any(), any(), any());
    }

    @Test
    void ignoresPushWhenEventTypeOutsideWhitelist()
    {
        when(projectMapper.selectByFullPath("GITHUB", "miguchn/demo", null)).thenReturn(pushEnabledProject());
        when(cryptoService.decryptWebhookSecret("cipher")).thenReturn("secret");
        when(webhookAdapter.verify(eq("secret"), eq(PAYLOAD), any())).thenReturn(true);
        when(webhookAdapter.parsePushEvent("push", "d-1", PAYLOAD)).thenReturn(PUSH_EVENT);
        when(configService.selectConfigByKey("review.github.pushEvents")).thenReturn("push_request");

        WebhookHandleResult result = service.handleGitHubWebhook("push", "d-1", "sig", PAYLOAD);

        assertEquals(200, result.httpStatus());
        verify(eventMapper).updateProcessResult(argMatchesStatus("IGNORED"));
        verify(taskCreateService, never()).createTaskFromPushEvent(any(), any(), any());
    }

    @Test
    void acceptsFormEncodedPushAndVerifiesRawBody()
    {
        GitHubWebhookAdapter realAdapter = new GitHubWebhookAdapter();
        String pushJson = """
            {
              "ref": "refs/heads/dev",
              "before": "aaaabbbbccccddddeeeeffff0000111122223333",
              "after": "ffffeeeeddddccccbbbbaaaa3333222211110000",
              "repository": { "name": "demo", "owner": { "login": "miguchn" } }
            }
            """;
        byte[] formPayload = ("payload=" + URLEncoder.encode(pushJson, StandardCharsets.UTF_8))
            .getBytes(StandardCharsets.UTF_8);
        byte[] parsePayload = realAdapter.unwrapPayload(formPayload);
        ReviewProject project = pushEnabledProject();
        when(webhookAdapter.unwrapPayload(any())).thenAnswer(inv -> realAdapter.unwrapPayload(inv.getArgument(0)));
        when(webhookAdapter.resolveDeliveryId(any(), eq(formPayload))).thenReturn("d-1");
        when(webhookAdapter.parseRepository(eq(parsePayload))).thenReturn(REPO);
        when(projectMapper.selectByFullPath("GITHUB", "miguchn/demo", null)).thenReturn(project);
        when(cryptoService.decryptWebhookSecret("cipher")).thenReturn("secret");
        when(webhookAdapter.verify(eq("secret"), eq(formPayload), any())).thenReturn(true);
        when(webhookAdapter.parsePushEvent("push", "d-1", parsePayload)).thenReturn(PUSH_EVENT);
        when(taskCreateService.createTaskFromPushEvent(eq(project), any(), eq(PUSH_EVENT))).thenReturn(310L);

        WebhookHandleResult result = service.handleGitHubWebhook("push", "d-1", "sig", formPayload);

        assertEquals(200, result.httpStatus());
        assertTrue(result.message().contains("310"));
        ArgumentCaptor<byte[]> verifyPayload = ArgumentCaptor.forClass(byte[].class);
        verify(webhookAdapter).verify(eq("secret"), verifyPayload.capture(), any());
        assertArrayEquals(formPayload, verifyPayload.getValue());
        verify(webhookAdapter).parseRepository(parsePayload);
        verify(webhookAdapter).parsePushEvent("push", "d-1", parsePayload);
        verify(taskCreateService).createTaskFromPushEvent(eq(project), any(), eq(PUSH_EVENT));
    }

    @Test
    void acceptsFormEncodedPullRequestAndVerifiesRawBody()
    {
        GitHubWebhookAdapter realAdapter = new GitHubWebhookAdapter();
        String prJson = """
            {
              "action": "opened",
              "number": 12,
              "pull_request": {
                "number": 12,
                "title": "feat: login",
                "base": { "ref": "dev", "sha": "aaaabbbbccccddddeeeeffff0000111122223333" },
                "head": { "ref": "feature/login", "sha": "ffffeeeeddddccccbbbbaaaa3333222211110000" }
              },
              "repository": { "name": "demo", "owner": { "login": "miguchn" } }
            }
            """;
        byte[] formPayload = ("payload=" + URLEncoder.encode(prJson, StandardCharsets.UTF_8))
            .getBytes(StandardCharsets.UTF_8);
        byte[] parsePayload = realAdapter.unwrapPayload(formPayload);
        ReviewProject project = enabledProject();
        when(webhookAdapter.unwrapPayload(any())).thenAnswer(inv -> realAdapter.unwrapPayload(inv.getArgument(0)));
        when(webhookAdapter.resolveDeliveryId(any(), eq(formPayload))).thenReturn("d-1");
        when(webhookAdapter.parseRepository(eq(parsePayload))).thenReturn(REPO);
        when(projectMapper.selectByFullPath("GITHUB", "miguchn/demo", null)).thenReturn(project);
        when(cryptoService.decryptWebhookSecret("cipher")).thenReturn("secret");
        when(webhookAdapter.verify(eq("secret"), eq(formPayload), any())).thenReturn(true);
        when(webhookAdapter.parsePullRequestEvent("pull_request", "d-1", parsePayload)).thenReturn(PR_EVENT);
        when(taskCreateService.createTaskFromEvent(eq(project), any(), eq(PR_EVENT))).thenReturn(311L);

        WebhookHandleResult result = service.handleGitHubWebhook("pull_request", "d-1", "sig", formPayload);

        assertEquals(200, result.httpStatus());
        assertTrue(result.message().contains("311"));
        ArgumentCaptor<byte[]> verifyPayload = ArgumentCaptor.forClass(byte[].class);
        verify(webhookAdapter).verify(eq("secret"), verifyPayload.capture(), any());
        assertArrayEquals(formPayload, verifyPayload.getValue());
        verify(webhookAdapter).parseRepository(parsePayload);
        verify(webhookAdapter).parsePullRequestEvent("pull_request", "d-1", parsePayload);
        verify(taskCreateService).createTaskFromEvent(eq(project), any(), eq(PR_EVENT));
    }

    @Test
    void matchesTriggerBranchSupportsExactAndGlob()
    {
        assertTrue(ReviewWebhookServiceImpl.matchesTriggerBranch("dev,main", "dev"));
        assertTrue(ReviewWebhookServiceImpl.matchesTriggerBranch("release/*", "release/1.2"));
        assertFalse(ReviewWebhookServiceImpl.matchesTriggerBranch("dev", "main"));
        assertFalse(ReviewWebhookServiceImpl.matchesTriggerBranch("", "dev"));
        assertFalse(ReviewWebhookServiceImpl.matchesTriggerBranch(null, "dev"));
    }

    private ReviewProject enabledProject()
    {
        ReviewProject project = new ReviewProject();
        project.setProjectId(1L);
        project.setStatus("0");
        project.setPrReviewEnabled("0");
        project.setPrTargetBranches("dev,develop");
        project.setPushReviewEnabled("1");
        project.setWebhookSecretCiphertext("cipher");
        return project;
    }

    private ReviewProject pushEnabledProject()
    {
        ReviewProject project = enabledProject();
        project.setPushReviewEnabled("0");
        project.setPushTriggerBranches("dev,main");
        return project;
    }

    private static ReviewWebhookEvent argMatchesStatus(String status)
    {
        return org.mockito.ArgumentMatchers.argThat(event ->
            event != null && status.equals(event.getProcessStatus()));
    }
}
