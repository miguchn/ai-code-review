package com.acr.review.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import com.acr.review.domain.ReviewProject;
import com.acr.review.domain.ReviewWebhookEvent;
import com.acr.review.domain.WebhookHandleResult;
import com.acr.review.git.GitAdapterRegistry;
import com.acr.review.git.GitPullRequestEvent;
import com.acr.review.git.GitRepositoryCoordinates;
import com.acr.review.git.GitWebhookAdapter;
import com.acr.review.git.WebhookRequestHeaders;
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

    private ReviewWebhookEventMapper eventMapper;
    private ReviewProjectMapper projectMapper;
    private GitWebhookAdapter webhookAdapter;
    private GitAdapterRegistry adapterRegistry;
    private CredentialCryptoService cryptoService;
    private ISysConfigService configService;
    private IReviewTaskCreateService taskCreateService;
    private IReviewIssueService issueService;
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
        service = new ReviewWebhookServiceImpl(eventMapper, projectMapper, adapterRegistry,
            cryptoService, configService, taskCreateService, issueService, 262144);

        when(adapterRegistry.requireWebhookAdapter("GITHUB")).thenReturn(webhookAdapter);
        when(webhookAdapter.resolveDeliveryId(any(), eq(PAYLOAD))).thenReturn("d-1");
        when(webhookAdapter.resolveEventType(any())).thenAnswer(inv -> {
            WebhookRequestHeaders headers = inv.getArgument(0);
            return headers == null ? null : headers.get("X-GitHub-Event");
        });
        when(webhookAdapter.isPullRequestEventType("pull_request")).thenReturn(true);
        when(webhookAdapter.isPullRequestEventType("ping")).thenReturn(false);
        when(webhookAdapter.parseRepository(PAYLOAD)).thenReturn(REPO);
        when(configService.selectConfigByKey("review.github.prEvents")).thenReturn("opened,reopened,synchronize");
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

    private ReviewProject enabledProject()
    {
        ReviewProject project = new ReviewProject();
        project.setProjectId(1L);
        project.setStatus("0");
        project.setPrReviewEnabled("0");
        project.setPrTargetBranches("dev,develop");
        project.setWebhookSecretCiphertext("cipher");
        return project;
    }

    private static ReviewWebhookEvent argMatchesStatus(String status)
    {
        return org.mockito.ArgumentMatchers.argThat(event ->
            event != null && status.equals(event.getProcessStatus()));
    }
}
