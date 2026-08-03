package com.acr.review.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.util.List;
import org.junit.jupiter.api.Test;
import com.acr.review.domain.GitRepositoryReadRequest;
import com.acr.review.domain.ReviewRepositoryInfo;
import com.acr.review.domain.GitCredential;
import com.acr.review.git.GitAdapterRegistry;
import com.acr.review.git.GitProvider;
import com.acr.review.git.GitRepositoryCoordinates;
import com.acr.review.git.GitRepositoryInfoResult;
import com.acr.review.mapper.GitCredentialMapper;
import com.acr.review.mapper.ReviewNotifyChannelMapper;
import com.acr.review.mapper.ReviewProjectMapper;
import com.acr.review.security.CredentialCryptoService;
import com.acr.review.service.IGitCredentialService;
import com.acr.review.service.IReviewTemplateService;
import com.acr.system.service.ISysAiModelConfigService;
import com.acr.system.service.ISysBusinessSystemService;
import com.acr.system.service.ISysConfigService;
import com.acr.system.service.ISysDeptService;
import com.acr.system.service.ISysUserService;

class ReviewProjectServiceImplTest
{
    @Test
    void recommendsDevFromActualRepositoryBranches()
    {
        ReviewProjectMapper projectMapper = mock(ReviewProjectMapper.class);
        GitCredentialMapper credentialMapper = mock(GitCredentialMapper.class);
        IGitCredentialService credentialService = mock(IGitCredentialService.class);
        GitProvider gitProvider = mock(GitProvider.class);
        GitAdapterRegistry adapterRegistry = mock(GitAdapterRegistry.class);
        ISysConfigService configService = mock(ISysConfigService.class);
        GitRepositoryCoordinates repository = new GitRepositoryCoordinates("owner", "repo", "owner/repo", "https://github.com/owner/repo");
        GitCredential credential = new GitCredential();
        credential.setCredentialId(1L);
        credential.setProvider("GITHUB");
        credential.setStatus("0");

        when(credentialMapper.selectGitCredentialById(1L)).thenReturn(credential);
        when(adapterRegistry.requireProvider("GITHUB")).thenReturn(gitProvider);
        when(credentialService.getPlainToken(1L, true)).thenReturn("test-token");
        when(gitProvider.parseRepository(eq("https://github.com/owner/repo"), any())).thenReturn(repository);
        when(gitProvider.readRepository(any(), any())).thenReturn(
            GitRepositoryInfoResult.success(repository, repository.canonicalUrl(), "main", List.of("main", "develop", "dev")));
        when(configService.selectConfigByKey(any())).thenReturn("");

        ReviewProjectServiceImpl service = new ReviewProjectServiceImpl(projectMapper, credentialMapper,
            mock(ReviewNotifyChannelMapper.class),
            credentialService, adapterRegistry, mock(ISysBusinessSystemService.class), configService,
            mock(ISysDeptService.class), mock(ISysUserService.class), mock(ISysAiModelConfigService.class),
            mock(IReviewTemplateService.class), mock(CredentialCryptoService.class), "http://localhost:8080");
        GitRepositoryReadRequest request = new GitRepositoryReadRequest();
        request.setRepositoryUrl(repository.canonicalUrl());
        request.setCredentialId(1L);

        ReviewRepositoryInfo result = service.readRepositoryInfo(request);

        assertTrue(result.success());
        assertEquals(List.of("dev"), result.recommendedTargetBranches());
    }
}
