package com.acr.review.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
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
import com.acr.system.service.ISysConfigService;

class ReviewWebhookServiceImplTest
{
    private static final byte[] PAYLOAD = "{}".getBytes(StandardCharsets.UTF_8);
    private static final GitRepositoryCoordinates REPO =
        new GitRepositoryCoordinates("miguchn", "demo", "https://github.com/miguchn/demo");
    private static final GitPullRequestEvent PR_EVENT = new GitPullRequestEvent(
        "d-1", "opened", "miguchn", "demo", 12, "feat: login",
        "feature/login", "dev", "aaaabbbbccccddddeeeeffff0000111122223333", "ffffeeeeddddccccbbbbaaaa3333222211110000");

    private ReviewWebhookEventMapper eventMapper;
    private ReviewProjectMapper projectMapper;
    private GitWebhookAdapter webhookAdapter;
    private CredentialCryptoService cryptoService;
    private ISysConfigService configService;
    private IReviewTaskCreateService taskCreateService;
    private ReviewWebhookServiceImpl service;

    @BeforeEach
    void setUp()
    {
        eventMapper = mock(ReviewWebhookEventMapper.class);
        projectMapper = mock(ReviewProjectMapper.class);
        webhookAdapter = mock(GitWebhookAdapter.class);
        cryptoService = mock(CredentialCryptoService.class);
        configService = mock(ISysConfigService.class);
        taskCreateService = mock(IReviewTaskCreateService.class);
        service = new ReviewWebhookServiceImpl(eventMapper, projectMapper, webhookAdapter,
            cryptoService, configService, taskCreateService, 262144);

        when(webhookAdapter.parseRepository(PAYLOAD)).thenReturn(REPO);
        when(configService.selectConfigByKey("review.github.prEvents")).thenReturn("opened,reopened,synchronize");
    }

    @Test
    void acceptsValidPrEventAndCreatesTask()
    {
        ReviewProject project = enabledProject();
        when(projectMapper.selectByRepository("GITHUB", "miguchn", "demo", null)).thenReturn(project);
        when(cryptoService.decryptWebhookSecret("cipher")).thenReturn("secret");
        when(webhookAdapter.verifySignature("secret", PAYLOAD, "sig")).thenReturn(true);
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
        when(projectMapper.selectByRepository("GITHUB", "miguchn", "demo", null)).thenReturn(null);

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
        when(projectMapper.selectByRepository("GITHUB", "miguchn", "demo", null)).thenReturn(project);

        WebhookHandleResult result = service.handleGitHubWebhook("pull_request", "d-1", "sig", PAYLOAD);

        assertEquals(200, result.httpStatus());
        verify(eventMapper).updateProcessResult(argMatchesStatus("IGNORED"));
        verify(webhookAdapter, never()).verifySignature(any(), any(), any());
    }

    @Test
    void failsWhenSecretNotConfigured()
    {
        ReviewProject project = enabledProject();
        project.setWebhookSecretCiphertext(null);
        when(projectMapper.selectByRepository("GITHUB", "miguchn", "demo", null)).thenReturn(project);

        WebhookHandleResult result = service.handleGitHubWebhook("pull_request", "d-1", "sig", PAYLOAD);

        assertEquals(401, result.httpStatus());
        verify(eventMapper).updateProcessResult(argMatchesStatus("FAILED"));
    }

    @Test
    void failsWhenSignatureInvalid()
    {
        when(projectMapper.selectByRepository("GITHUB", "miguchn", "demo", null)).thenReturn(enabledProject());
        when(cryptoService.decryptWebhookSecret("cipher")).thenReturn("secret");
        when(webhookAdapter.verifySignature("secret", PAYLOAD, "bad-sig")).thenReturn(false);

        WebhookHandleResult result = service.handleGitHubWebhook("pull_request", "d-1", "bad-sig", PAYLOAD);

        assertEquals(401, result.httpStatus());
        verify(eventMapper).updateProcessResult(argMatchesStatus("FAILED"));
        verify(taskCreateService, never()).createTaskFromEvent(any(), any(), any());
    }

    @Test
    void ignoresNonPullRequestEvent()
    {
        when(projectMapper.selectByRepository("GITHUB", "miguchn", "demo", null)).thenReturn(enabledProject());
        when(cryptoService.decryptWebhookSecret("cipher")).thenReturn("secret");
        when(webhookAdapter.verifySignature("secret", PAYLOAD, "sig")).thenReturn(true);

        WebhookHandleResult result = service.handleGitHubWebhook("ping", "d-1", "sig", PAYLOAD);

        assertEquals(200, result.httpStatus());
        verify(eventMapper).updateProcessResult(argMatchesStatus("IGNORED"));
    }

    @Test
    void ignoresActionOutsideWhitelist()
    {
        when(projectMapper.selectByRepository("GITHUB", "miguchn", "demo", null)).thenReturn(enabledProject());
        when(cryptoService.decryptWebhookSecret("cipher")).thenReturn("secret");
        when(webhookAdapter.verifySignature("secret", PAYLOAD, "sig")).thenReturn(true);
        when(webhookAdapter.parsePullRequestEvent("pull_request", "d-1", PAYLOAD))
            .thenReturn(new GitPullRequestEvent("d-1", "closed", "miguchn", "demo", 12, "t",
                "feature/login", "dev", PR_EVENT.baseSha(), PR_EVENT.headSha()));

        WebhookHandleResult result = service.handleGitHubWebhook("pull_request", "d-1", "sig", PAYLOAD);

        assertEquals(200, result.httpStatus());
        verify(eventMapper).updateProcessResult(argMatchesStatus("IGNORED"));
        verify(taskCreateService, never()).createTaskFromEvent(any(), any(), any());
    }

    @Test
    void ignoresWhenTargetBranchNotConfigured()
    {
        when(projectMapper.selectByRepository("GITHUB", "miguchn", "demo", null)).thenReturn(enabledProject());
        when(cryptoService.decryptWebhookSecret("cipher")).thenReturn("secret");
        when(webhookAdapter.verifySignature("secret", PAYLOAD, "sig")).thenReturn(true);
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
        when(projectMapper.selectByRepository("GITHUB", "miguchn", "demo", null)).thenReturn(project);
        when(cryptoService.decryptWebhookSecret("cipher")).thenReturn("secret");
        when(webhookAdapter.verifySignature("secret", PAYLOAD, "sig")).thenReturn(true);
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
        WebhookHandleResult result = service.handleGitHubWebhook("pull_request", null, "sig", PAYLOAD);

        assertEquals(400, result.httpStatus());
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
