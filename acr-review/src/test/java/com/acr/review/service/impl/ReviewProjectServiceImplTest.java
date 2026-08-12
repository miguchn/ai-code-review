package com.acr.review.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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
import com.acr.common.exception.ServiceException;
import com.acr.common.utils.SecurityUtils;
import com.acr.review.domain.GitCredential;
import com.acr.review.domain.GitRepositoryReadRequest;
import com.acr.review.domain.ReviewPipelineConstants;
import com.acr.review.domain.ReviewProject;
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
import com.acr.review.service.ReviewProjectAccessService;
import com.acr.system.domain.SysBusinessSystem;
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
            mock(IReviewTemplateService.class), mock(CredentialCryptoService.class),
            mock(ReviewProjectAccessService.class), "http://localhost:8080");
        GitRepositoryReadRequest request = new GitRepositoryReadRequest();
        request.setRepositoryUrl(repository.canonicalUrl());
        request.setCredentialId(1L);

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class))
        {
            security.when(() -> SecurityUtils.hasPlatformPermi("review:credential:query")).thenReturn(true);
            ReviewRepositoryInfo result = service.readRepositoryInfo(request);

            assertTrue(result.success());
            assertEquals(List.of("dev"), result.recommendedTargetBranches());
        }
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

    @Test
    void updateProjectStatusRejectsWhenNoReviewTypeEnabled()
    {
        ReviewProjectMapper projectMapper = mock(ReviewProjectMapper.class);
        IGitCredentialService credentialService = mock(IGitCredentialService.class);
        ReviewProject project = new ReviewProject();
        project.setProjectId(1L);
        project.setLastCheckStatus("SUCCESS");
        project.setCredentialId(5L);
        project.setPrReviewEnabled("1");
        project.setPushReviewEnabled("1");
        when(projectMapper.selectReviewProjectById(1L)).thenReturn(project);
        when(credentialService.getPlainToken(5L, true)).thenReturn("token");
        ReviewProjectServiceImpl service = newStatusService(projectMapper, credentialService);

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class))
        {
            security.when(SecurityUtils::isAdmin).thenReturn(true);
            ServiceException ex = assertThrows(ServiceException.class,
                () -> service.updateProjectStatus(1L, "0"));
            assertTrue(ex.getMessage().contains("请至少选择一种审查类型"));
        }
    }

    @Test
    void updateProjectStatusRejectsPushEnabledWithoutTriggerBranches()
    {
        ReviewProjectMapper projectMapper = mock(ReviewProjectMapper.class);
        IGitCredentialService credentialService = mock(IGitCredentialService.class);
        ReviewProject project = new ReviewProject();
        project.setProjectId(2L);
        project.setLastCheckStatus("SUCCESS");
        project.setCredentialId(5L);
        project.setPrReviewEnabled("1");
        project.setPushReviewEnabled("0");
        project.setPushTriggerBranches(null);
        when(projectMapper.selectReviewProjectById(2L)).thenReturn(project);
        when(credentialService.getPlainToken(5L, true)).thenReturn("token");
        ReviewProjectServiceImpl service = newStatusService(projectMapper, credentialService);

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class))
        {
            security.when(SecurityUtils::isAdmin).thenReturn(true);
            ServiceException ex = assertThrows(ServiceException.class,
                () -> service.updateProjectStatus(2L, "0"));
            assertTrue(ex.getMessage().contains("触发分支"));
        }
    }

    @Test
    void insertNormalizesIllegalReviewTypeFlagsAndRejectsNoneSelected()
    {
        ReviewProjectMapper projectMapper = mock(ReviewProjectMapper.class);
        GitCredentialMapper credentialMapper = mock(GitCredentialMapper.class);
        IGitCredentialService credentialService = mock(IGitCredentialService.class);
        GitProvider gitProvider = mock(GitProvider.class);
        GitAdapterRegistry adapterRegistry = mock(GitAdapterRegistry.class);
        ISysBusinessSystemService businessSystemService = mock(ISysBusinessSystemService.class);
        ISysDeptService deptService = mock(ISysDeptService.class);
        ISysUserService userService = mock(ISysUserService.class);
        GitRepositoryCoordinates repository = new GitRepositoryCoordinates(
            "owner", "repo", "owner/repo", "https://github.com/owner/repo");

        GitCredential credential = new GitCredential();
        credential.setCredentialId(1L);
        credential.setProvider("GITHUB");
        credential.setStatus("0");
        when(credentialMapper.selectGitCredentialById(1L)).thenReturn(credential);
        when(adapterRegistry.requireProvider("GITHUB")).thenReturn(gitProvider);
        when(gitProvider.parseRepository(eq("https://github.com/owner/repo"), any())).thenReturn(repository);
        when(projectMapper.selectByFullPath(any(), any(), isNull())).thenReturn(null);
        when(projectMapper.selectByRepository(any(), any(), any(), isNull())).thenReturn(null);

        ReviewProjectServiceImpl service = new ReviewProjectServiceImpl(projectMapper, credentialMapper,
            mock(ReviewNotifyChannelMapper.class), credentialService, adapterRegistry, businessSystemService,
            mock(ISysConfigService.class), deptService, userService, mock(ISysAiModelConfigService.class),
            mock(IReviewTemplateService.class), mock(CredentialCryptoService.class),
            mock(ReviewProjectAccessService.class), "http://localhost:8080");

        ReviewProject noneSelected = baseInsertProject();
        noneSelected.setPrReviewEnabled("1");
        noneSelected.setPushReviewEnabled("1");

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class))
        {
            security.when(SecurityUtils::getUsername).thenReturn("admin");
            security.when(() -> SecurityUtils.hasPlatformPermi("review:credential:query")).thenReturn(true);
            ServiceException ex = assertThrows(ServiceException.class,
                () -> service.insertReviewProject(noneSelected));
            assertTrue(ex.getMessage().contains("请至少选择一种审查类型"));
        }

        // 非法开关值：PR 归一为启用、推送归一为停用
        ReviewProject illegal = baseInsertProject();
        illegal.setPrReviewEnabled("x");
        illegal.setPushReviewEnabled("y");
        illegal.setPrTargetBranches("main");
        SysBusinessSystem system = new SysBusinessSystem();
        system.setSystemId(9L);
        system.setDeptId(100L);
        system.setStatus("0");
        when(businessSystemService.selectSysBusinessSystemById(9L)).thenReturn(system);
        SysUser owner = user(11L, 100L, "张三");
        when(userService.selectUserById(11L)).thenReturn(owner);
        when(credentialService.getPlainToken(1L, true)).thenReturn("token");
        when(gitProvider.readRepository(any(), any())).thenReturn(
            GitRepositoryInfoResult.success(repository, repository.canonicalUrl(), "main", List.of("main", "dev")));
        when(projectMapper.insertReviewProject(any())).thenReturn(1);

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class))
        {
            security.when(SecurityUtils::getUsername).thenReturn("admin");
            security.when(SecurityUtils::isAdmin).thenReturn(true);
            security.when(() -> SecurityUtils.hasPlatformPermi("review:credential:query")).thenReturn(true);
            service.insertReviewProject(illegal);
            assertEquals("0", illegal.getPrReviewEnabled());
            assertEquals("1", illegal.getPushReviewEnabled());
        }
    }

    @Test
    void insertNormalizesMissingInlineCommentFields()
    {
        ReviewProjectMapper projectMapper = mock(ReviewProjectMapper.class);
        GitCredentialMapper credentialMapper = mock(GitCredentialMapper.class);
        IGitCredentialService credentialService = mock(IGitCredentialService.class);
        ISysBusinessSystemService businessSystemService = mock(ISysBusinessSystemService.class);
        ISysDeptService deptService = mock(ISysDeptService.class);
        ISysUserService userService = mock(ISysUserService.class);
        GitProvider gitProvider = mock(GitProvider.class);
        GitAdapterRegistry adapterRegistry = mock(GitAdapterRegistry.class);
        GitRepositoryCoordinates repository = new GitRepositoryCoordinates(
            "owner", "repo", "owner/repo", "https://github.com/owner/repo");
        GitCredential credential = new GitCredential();
        credential.setCredentialId(1L);
        credential.setProvider("GITHUB");
        credential.setStatus("0");
        when(credentialMapper.selectGitCredentialById(1L)).thenReturn(credential);
        when(adapterRegistry.requireProvider("GITHUB")).thenReturn(gitProvider);
        when(gitProvider.parseRepository(eq("https://github.com/owner/repo"), any())).thenReturn(repository);
        when(projectMapper.selectByFullPath(any(), any(), isNull())).thenReturn(null);
        when(projectMapper.selectByRepository(any(), any(), any(), isNull())).thenReturn(null);
        SysBusinessSystem system = new SysBusinessSystem();
        system.setSystemId(9L);
        system.setDeptId(100L);
        system.setStatus("0");
        when(businessSystemService.selectSysBusinessSystemById(9L)).thenReturn(system);
        when(userService.selectUserById(11L)).thenReturn(user(11L, 100L, "张三"));
        when(credentialService.getPlainToken(1L, true)).thenReturn("token");
        when(gitProvider.readRepository(any(), any())).thenReturn(
            GitRepositoryInfoResult.success(repository, repository.canonicalUrl(), "main", List.of("main", "dev")));
        when(projectMapper.insertReviewProject(any())).thenReturn(1);

        ReviewProjectServiceImpl service = new ReviewProjectServiceImpl(projectMapper, credentialMapper,
            mock(ReviewNotifyChannelMapper.class), credentialService, adapterRegistry, businessSystemService,
            mock(ISysConfigService.class), deptService, userService, mock(ISysAiModelConfigService.class),
            mock(IReviewTemplateService.class), mock(CredentialCryptoService.class),
            mock(ReviewProjectAccessService.class), "http://localhost:8080");

        ReviewProject project = baseInsertProject();
        project.setPrReviewEnabled("0");
        project.setPushReviewEnabled("1");
        project.setPrTargetBranches("main");
        project.setInlineCommentEnabled(null);
        project.setInlineSeverities(null);

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class))
        {
            security.when(SecurityUtils::getUsername).thenReturn("admin");
            security.when(SecurityUtils::isAdmin).thenReturn(true);
            security.when(() -> SecurityUtils.hasPlatformPermi("review:credential:query")).thenReturn(true);
            service.insertReviewProject(project);
            assertEquals("1", project.getInlineCommentEnabled());
            assertEquals("CRITICAL,HIGH", project.getInlineSeverities());
        }

        assertEquals("CRITICAL,HIGH",
            ReviewProjectServiceImpl.normalizeInlineSeverities(""));
        assertEquals("CRITICAL,HIGH",
            ReviewProjectServiceImpl.normalizeInlineSeverities("FOO,BAR"));
        assertEquals("HIGH,MEDIUM",
            ReviewProjectServiceImpl.normalizeInlineSeverities(" high , MEDIUM , junk "));
    }

    @Test
    void insertRejectsPushEnabledWithoutTriggerBranches()
    {
        ReviewProjectMapper projectMapper = mock(ReviewProjectMapper.class);
        GitCredentialMapper credentialMapper = mock(GitCredentialMapper.class);
        GitProvider gitProvider = mock(GitProvider.class);
        GitAdapterRegistry adapterRegistry = mock(GitAdapterRegistry.class);
        GitRepositoryCoordinates repository = new GitRepositoryCoordinates(
            "owner", "repo", "owner/repo", "https://github.com/owner/repo");
        GitCredential credential = new GitCredential();
        credential.setCredentialId(1L);
        credential.setProvider("GITHUB");
        credential.setStatus("0");
        when(credentialMapper.selectGitCredentialById(1L)).thenReturn(credential);
        when(adapterRegistry.requireProvider("GITHUB")).thenReturn(gitProvider);
        when(gitProvider.parseRepository(eq("https://github.com/owner/repo"), any())).thenReturn(repository);

        ReviewProjectServiceImpl service = new ReviewProjectServiceImpl(projectMapper, credentialMapper,
            mock(ReviewNotifyChannelMapper.class), mock(IGitCredentialService.class), adapterRegistry,
            mock(ISysBusinessSystemService.class), mock(ISysConfigService.class), mock(ISysDeptService.class),
            mock(ISysUserService.class), mock(ISysAiModelConfigService.class),
            mock(IReviewTemplateService.class), mock(CredentialCryptoService.class),
            mock(ReviewProjectAccessService.class), "http://localhost:8080");

        ReviewProject project = baseInsertProject();
        project.setPrReviewEnabled("1");
        project.setPushReviewEnabled("0");
        project.setPushTriggerBranches("");

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class))
        {
            security.when(() -> SecurityUtils.hasPlatformPermi("review:credential:query")).thenReturn(true);
            ServiceException ex = assertThrows(ServiceException.class, () -> service.insertReviewProject(project));
            assertTrue(ex.getMessage().contains("触发分支"));
        }
    }

    @Test
    void insertRejectsCredentialBindingWithoutPlatformRole()
    {
        ReviewProjectServiceImpl service = new ReviewProjectServiceImpl(mock(ReviewProjectMapper.class),
            mock(GitCredentialMapper.class), mock(ReviewNotifyChannelMapper.class),
            mock(IGitCredentialService.class), mock(GitAdapterRegistry.class),
            mock(ISysBusinessSystemService.class), mock(ISysConfigService.class), mock(ISysDeptService.class),
            mock(ISysUserService.class), mock(ISysAiModelConfigService.class),
            mock(IReviewTemplateService.class), mock(CredentialCryptoService.class),
            mock(ReviewProjectAccessService.class), "http://localhost:8080");

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class))
        {
            security.when(() -> SecurityUtils.hasPlatformPermi("review:credential:query")).thenReturn(false);
            ServiceException ex = assertThrows(ServiceException.class,
                () -> service.insertReviewProject(baseInsertProject()));
            assertTrue(ex.getMessage().contains("平台级角色"));
        }
    }

    private static ReviewProject baseInsertProject()
    {
        ReviewProject project = new ReviewProject();
        project.setProjectName("demo");
        project.setPrimaryStack("JAVA");
        project.setProvider("GITHUB");
        project.setRepositoryUrl("https://github.com/owner/repo");
        project.setCredentialId(1L);
        project.setBusinessSystemId(9L);
        project.setDeptId(100L);
        project.setOwnerUserId(11L);
        project.setReviewMode(ReviewPipelineConstants.REVIEW_MODE_OCR_ENGINE);
        project.setStatus("1");
        return project;
    }

    private static ReviewProjectServiceImpl newStatusService(ReviewProjectMapper projectMapper,
                                                            IGitCredentialService credentialService)
    {
        ReviewProjectAccessService accessService = mock(ReviewProjectAccessService.class);
        when(accessService.requireManage(any())).thenAnswer(invocation ->
            projectMapper.selectReviewProjectById(invocation.getArgument(0)));
        return new ReviewProjectServiceImpl(projectMapper, mock(GitCredentialMapper.class),
            mock(ReviewNotifyChannelMapper.class), credentialService, mock(GitAdapterRegistry.class),
            mock(ISysBusinessSystemService.class), mock(ISysConfigService.class), mock(ISysDeptService.class),
            mock(ISysUserService.class), mock(ISysAiModelConfigService.class),
            mock(IReviewTemplateService.class), mock(CredentialCryptoService.class),
            accessService, "http://localhost:8080");
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
            mock(CredentialCryptoService.class), mock(ReviewProjectAccessService.class),
            "http://localhost:8080");
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
