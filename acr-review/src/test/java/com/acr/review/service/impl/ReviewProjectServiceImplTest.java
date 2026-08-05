package com.acr.review.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import com.acr.common.core.domain.entity.SysDept;
import com.acr.common.core.domain.entity.SysUser;
import com.acr.common.utils.SecurityUtils;
import com.acr.review.domain.GitCredential;
import com.acr.review.domain.GitRepositoryReadRequest;
import com.acr.review.domain.ReviewProjectOptions;
import com.acr.review.domain.ReviewRepositoryInfo;
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

    @Test
    void getFormOptions_adminReturnsFullDepartmentsAndOwners()
    {
        ISysDeptService deptService = mock(ISysDeptService.class);
        ISysUserService userService = mock(ISysUserService.class);
        ReviewProjectServiceImpl service = newFormOptionsService(deptService, userService);

        SysDept root = dept(100L, 0L, "总部", "0");
        SysDept child = dept(101L, 100L, "研发", "0");
        SysUser owner = user(11L, 100L, "张三");
        when(deptService.selectDeptList(any(SysDept.class))).thenReturn(List.of(root, child));
        when(userService.selectUserList(any(SysUser.class))).thenReturn(List.of(owner));

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class))
        {
            security.when(SecurityUtils::isAdmin).thenReturn(true);

            ReviewProjectOptions options = service.getFormOptions();

            assertEquals(List.of(100L, 101L), options.getDepartments().stream().map(ReviewProjectOptions.Option::getId).toList());
            assertEquals(List.of(11L), options.getOwners().stream().map(ReviewProjectOptions.Option::getId).toList());
            verify(deptService).selectDeptList(any(SysDept.class));
            verify(deptService, never()).selectDeptById(any());
            verify(deptService, never()).selectChildrenDeptById(any());
            ArgumentCaptor<SysUser> userCaptor = ArgumentCaptor.forClass(SysUser.class);
            verify(userService).selectUserList(userCaptor.capture());
            assertEquals("0", userCaptor.getValue().getStatus());
            assertEquals(null, userCaptor.getValue().getDeptId());
        }
    }

    @Test
    void getFormOptions_nonAdminReturnsDeptSubtreeAndScopedOwners()
    {
        ISysDeptService deptService = mock(ISysDeptService.class);
        ISysUserService userService = mock(ISysUserService.class);
        ReviewProjectServiceImpl service = newFormOptionsService(deptService, userService);

        Long deptId = 200L;
        SysDept self = dept(deptId, 100L, "业务一部", "0");
        SysDept child = dept(201L, deptId, "业务一组", "0");
        SysDept disabledChild = dept(202L, deptId, "停用组", "1");
        SysUser owner = user(21L, deptId, "李四");
        when(deptService.selectDeptById(deptId)).thenReturn(self);
        when(deptService.selectChildrenDeptById(deptId)).thenReturn(List.of(child, disabledChild));
        when(userService.selectUserList(any(SysUser.class))).thenReturn(List.of(owner));

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class))
        {
            security.when(SecurityUtils::isAdmin).thenReturn(false);
            security.when(SecurityUtils::getDeptId).thenReturn(deptId);

            ReviewProjectOptions options = service.getFormOptions();

            assertEquals(List.of(200L, 201L), options.getDepartments().stream().map(ReviewProjectOptions.Option::getId).toList());
            assertEquals(List.of(21L), options.getOwners().stream().map(ReviewProjectOptions.Option::getId).toList());
            verify(deptService, never()).selectDeptList(any(SysDept.class));
            ArgumentCaptor<SysUser> userCaptor = ArgumentCaptor.forClass(SysUser.class);
            verify(userService).selectUserList(userCaptor.capture());
            assertEquals("0", userCaptor.getValue().getStatus());
            assertEquals(deptId, userCaptor.getValue().getDeptId());
        }
    }

    @Test
    void getFormOptions_nonAdminWithoutDeptReturnsEmptyDepartmentsAndOwners()
    {
        ISysDeptService deptService = mock(ISysDeptService.class);
        ISysUserService userService = mock(ISysUserService.class);
        ReviewProjectServiceImpl service = newFormOptionsService(deptService, userService);

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class))
        {
            security.when(SecurityUtils::isAdmin).thenReturn(false);
            security.when(SecurityUtils::getDeptId).thenReturn(null);

            ReviewProjectOptions options = service.getFormOptions();

            assertTrue(options.getDepartments().isEmpty());
            assertTrue(options.getOwners().isEmpty());
            verify(deptService, never()).selectDeptList(any(SysDept.class));
            verify(deptService, never()).selectDeptById(any());
            verify(deptService, never()).selectChildrenDeptById(any());
            verify(userService, never()).selectUserList(any(SysUser.class));
        }
    }

    private static ReviewProjectServiceImpl newFormOptionsService(ISysDeptService deptService, ISysUserService userService)
    {
        GitCredentialMapper credentialMapper = mock(GitCredentialMapper.class);
        ISysBusinessSystemService businessSystemService = mock(ISysBusinessSystemService.class);
        ISysConfigService configService = mock(ISysConfigService.class);
        ISysAiModelConfigService aiModelConfigService = mock(ISysAiModelConfigService.class);
        IReviewTemplateService templateService = mock(IReviewTemplateService.class);

        when(businessSystemService.selectSysBusinessSystemList(any())).thenReturn(List.of());
        when(credentialMapper.selectGitCredentialList(any())).thenReturn(List.of());
        when(aiModelConfigService.selectSysAiModelConfigList(any())).thenReturn(List.of());
        when(templateService.selectReviewTemplateList(any())).thenReturn(List.of());
        when(configService.selectConfigByKey(any())).thenReturn("");

        return new ReviewProjectServiceImpl(mock(ReviewProjectMapper.class), credentialMapper,
            mock(ReviewNotifyChannelMapper.class),
            mock(IGitCredentialService.class), mock(GitAdapterRegistry.class), businessSystemService, configService,
            deptService, userService, aiModelConfigService, templateService,
            mock(CredentialCryptoService.class), "http://localhost:8080");
    }

    private static SysDept dept(Long deptId, Long parentId, String name, String status)
    {
        SysDept dept = new SysDept();
        dept.setDeptId(deptId);
        dept.setParentId(parentId);
        dept.setDeptName(name);
        dept.setStatus(status);
        dept.setDelFlag("0");
        return dept;
    }

    private static SysUser user(Long userId, Long deptId, String nickName)
    {
        SysUser user = new SysUser();
        user.setUserId(userId);
        user.setDeptId(deptId);
        user.setNickName(nickName);
        user.setStatus("0");
        return user;
    }
}
