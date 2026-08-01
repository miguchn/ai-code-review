package com.acr.review.service.impl;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.acr.common.annotation.DataScope;
import com.acr.common.core.domain.entity.SysDept;
import com.acr.common.core.domain.entity.SysUser;
import com.acr.common.exception.ServiceException;
import com.acr.common.utils.SecurityUtils;
import com.acr.review.domain.GitCredential;
import com.acr.review.domain.GitRepositoryReadRequest;
import com.acr.review.domain.ReviewProject;
import com.acr.review.domain.ReviewProjectOptions;
import com.acr.review.domain.ReviewRepositoryInfo;
import com.acr.review.git.GitConnectionFailure;
import com.acr.review.git.GitConnectionResult;
import com.acr.review.git.GitProvider;
import com.acr.review.git.GitRepositoryCoordinates;
import com.acr.review.git.GitRepositoryInfoResult;
import com.acr.review.mapper.GitCredentialMapper;
import com.acr.review.mapper.ReviewProjectMapper;
import com.acr.review.service.IGitCredentialService;
import com.acr.review.service.IReviewProjectService;
import com.acr.system.domain.SysBusinessSystem;
import com.acr.system.service.ISysBusinessSystemService;
import com.acr.system.service.ISysConfigService;
import com.acr.system.service.ISysDeptService;
import com.acr.system.service.ISysUserService;

/** GitHub 代码项目管理。 */
@Service
public class ReviewProjectServiceImpl implements IReviewProjectService
{
    private static final String PROVIDER = "GITHUB";
    private static final String LONG_LIVED_BRANCHES_KEY = "review.github.longLivedBranches";
    private static final String ROBOT_BRANCH_PREFIXES_KEY = "review.github.robotBranchPrefixes";
    private static final String PR_EVENTS_KEY = "review.github.prEvents";
    private static final String DEFAULT_LONG_LIVED_BRANCHES = "dev,develop,main,int,uat";
    private static final String DEFAULT_ROBOT_BRANCH_PREFIXES = "dependabot/,renovate/,github-actions/";
    private static final String DEFAULT_PR_EVENTS = "opened,reopened,synchronize";

    private final ReviewProjectMapper projectMapper;
    private final GitCredentialMapper credentialMapper;
    private final IGitCredentialService credentialService;
    private final GitProvider gitProvider;
    private final ISysBusinessSystemService businessSystemService;
    private final ISysConfigService configService;
    private final ISysDeptService deptService;
    private final ISysUserService userService;

    public ReviewProjectServiceImpl(ReviewProjectMapper projectMapper,
                                    GitCredentialMapper credentialMapper,
                                    IGitCredentialService credentialService,
                                    GitProvider gitProvider,
                                    ISysBusinessSystemService businessSystemService,
                                    ISysConfigService configService,
                                    ISysDeptService deptService,
                                    ISysUserService userService)
    {
        this.projectMapper = projectMapper;
        this.credentialMapper = credentialMapper;
        this.credentialService = credentialService;
        this.gitProvider = gitProvider;
        this.businessSystemService = businessSystemService;
        this.configService = configService;
        this.deptService = deptService;
        this.userService = userService;
    }

    @Override
    public ReviewProject selectReviewProjectById(Long projectId)
    {
        ReviewProject project = projectMapper.selectReviewProjectById(projectId);
        checkProjectAccess(project);
        return project;
    }

    @Override
    @DataScope(deptAlias = "d", userAlias = "owner", permission = "review:project:list")
    public List<ReviewProject> selectReviewProjectList(ReviewProject project)
    {
        project.setProvider(PROVIDER);
        if (!SecurityUtils.isAdmin())
        {
            project.setAccessUserId(SecurityUtils.getUserId());
        }
        return projectMapper.selectReviewProjectList(project);
    }

    @Override
    public ReviewProjectOptions getFormOptions()
    {
        ReviewProjectOptions options = new ReviewProjectOptions();

        SysBusinessSystem systemQuery = new SysBusinessSystem();
        systemQuery.setStatus("0");
        options.setBusinessSystems(businessSystemService.selectSysBusinessSystemList(systemQuery).stream()
            .map(system -> new ReviewProjectOptions.Option(system.getSystemId(), system.getSystemName(), system.getDeptId(), null, system.getStatus()))
            .toList());

        SysDept deptQuery = new SysDept();
        deptQuery.setStatus("0");
        options.setDepartments(deptService.selectDeptList(deptQuery).stream()
            .map(dept -> new ReviewProjectOptions.Option(dept.getDeptId(), dept.getDeptName(), dept.getDeptId(), dept.getParentId(), dept.getStatus()))
            .toList());

        SysUser userQuery = new SysUser();
        userQuery.setStatus("0");
        options.setOwners(userService.selectUserList(userQuery).stream()
            .map(user -> new ReviewProjectOptions.Option(user.getUserId(), user.getNickName(), user.getDeptId(), null, user.getStatus()))
            .toList());

        GitCredential credentialQuery = new GitCredential();
        credentialQuery.setProvider(PROVIDER);
        credentialQuery.setStatus("0");
        options.setCredentials(credentialMapper.selectGitCredentialList(credentialQuery).stream()
            .map(credential -> new ReviewProjectOptions.Option(credential.getCredentialId(), credential.getCredentialName(), null, null, credential.getStatus()))
            .toList());
        options.setLongLivedBranches(configValues(LONG_LIVED_BRANCHES_KEY, DEFAULT_LONG_LIVED_BRANCHES));
        options.setRobotBranchPrefixes(configValues(ROBOT_BRANCH_PREFIXES_KEY, DEFAULT_ROBOT_BRANCH_PREFIXES));
        options.setPrEvents(configValues(PR_EVENTS_KEY, DEFAULT_PR_EVENTS));
        return options;
    }

    @Override
    public ReviewRepositoryInfo readRepositoryInfo(GitRepositoryReadRequest request)
    {
        GitRepositoryCoordinates repository;
        try
        {
            repository = gitProvider.parseRepository(request.getRepositoryUrl());
        }
        catch (IllegalArgumentException e)
        {
            return new ReviewRepositoryInfo(false, GitConnectionFailure.INVALID_REPOSITORY_URL, e.getMessage(),
                null, null, null, null, List.of(), List.of(), new Date());
        }

        ReviewProject existing = null;
        if (request.getProjectId() != null)
        {
            existing = selectReviewProjectById(request.getProjectId());
        }

        String token = credentialService.getPlainToken(request.getCredentialId(), true);
        GitRepositoryInfoResult result = gitProvider.readRepository(repository, token);
        List<String> recommended = result.success()
            ? recommendTargetBranches(result.branches(), result.defaultBranch()) : List.of();

        if (existing != null
            && Objects.equals(existing.getRepositoryUrl(), repository.canonicalUrl())
            && Objects.equals(existing.getCredentialId(), request.getCredentialId()))
        {
            persistRepositorySync(existing.getProjectId(), result);
        }

        return new ReviewRepositoryInfo(result.success(), result.failure(), result.message(), result.repositoryUrl(),
            result.repositoryOwner(), result.repositoryName(), result.defaultBranch(), result.branches(), recommended,
            result.syncedAt());
    }

    @Override
    public int insertReviewProject(ReviewProject project)
    {
        normalizeAndValidate(project, null);
        readAndApplyRepositoryInfo(project);
        if ("0".equals(project.getStatus()))
        {
            throw new ServiceException("新项目需先保存并通过连接测试后再启用");
        }
        project.setCreateBy(SecurityUtils.getUsername());
        return projectMapper.insertReviewProject(project);
    }

    @Override
    @Transactional
    public int updateReviewProject(ReviewProject project)
    {
        if (project.getProjectId() == null)
        {
            throw new ServiceException("项目 ID 不能为空");
        }
        ReviewProject existing = selectReviewProjectById(project.getProjectId());
        normalizeAndValidate(project, project.getProjectId());
        boolean connectionChanged = !Objects.equals(existing.getRepositoryUrl(), project.getRepositoryUrl())
            || !Objects.equals(existing.getCredentialId(), project.getCredentialId());
        boolean enablingPrReview = !"0".equals(existing.getPrReviewEnabled())
            && "0".equals(project.getPrReviewEnabled());
        boolean targetsChanged = !Objects.equals(existing.getPrTargetBranches(), project.getPrTargetBranches());
        if (connectionChanged || enablingPrReview || targetsChanged)
        {
            readAndApplyRepositoryInfo(project);
        }
        else
        {
            copyRepositoryState(existing, project);
        }
        project.setUpdateBy(SecurityUtils.getUsername());
        int rows = projectMapper.updateReviewProject(project);
        if (connectionChanged && "0".equals(existing.getStatus()))
        {
            projectMapper.updateProjectStatus(project.getProjectId(), "1", SecurityUtils.getUsername());
        }
        return rows;
    }

    @Override
    @Transactional
    public void deleteReviewProjectByIds(Long[] projectIds)
    {
        for (Long projectId : projectIds)
        {
            selectReviewProjectById(projectId);
        }
        projectMapper.deleteReviewProjectByIds(projectIds);
    }

    @Override
    public int updateProjectStatus(Long projectId, String status)
    {
        if (!"0".equals(status) && !"1".equals(status))
        {
            throw new ServiceException("项目状态无效");
        }
        ReviewProject project = selectReviewProjectById(projectId);
        if ("0".equals(status))
        {
            if (!"SUCCESS".equals(project.getLastCheckStatus()))
            {
                throw new ServiceException("项目连接测试成功后才能启用");
            }
            credentialService.getPlainToken(project.getCredentialId(), true);
            if ("0".equals(project.getPrReviewEnabled())
                && (project.getPrTargetBranches() == null || project.getPrTargetBranches().isBlank()))
            {
                throw new ServiceException("启用 PR 审查的项目必须配置目标分支");
            }
        }
        return projectMapper.updateProjectStatus(projectId, status, SecurityUtils.getUsername());
    }

    @Override
    public GitConnectionResult testConnection(Long projectId)
    {
        ReviewProject project = selectReviewProjectById(projectId);
        String token = credentialService.getPlainToken(project.getCredentialId(), true);
        GitRepositoryCoordinates repository = gitProvider.parseRepository(project.getRepositoryUrl());
        GitConnectionResult result = gitProvider.testRepository(repository, token);

        ReviewProject update = new ReviewProject();
        update.setProjectId(projectId);
        update.setLastCheckStatus(result.isSuccess() ? "SUCCESS" : "FAILED");
        update.setLastCheckMessage(result.getMessage());
        update.setLastCheckTime(result.getCheckedAt());
        update.setDefaultBranch(result.getDefaultBranch());
        update.setUpdateBy(SecurityUtils.getUsername());
        projectMapper.updateConnectionCheck(update);
        return result;
    }

    private void normalizeAndValidate(ReviewProject project, Long excludeProjectId)
    {
        project.setProjectName(project.getProjectName().trim());
        project.setProvider(PROVIDER);
        GitRepositoryCoordinates repository;
        try
        {
            repository = gitProvider.parseRepository(project.getRepositoryUrl());
        }
        catch (IllegalArgumentException e)
        {
            throw new ServiceException(e.getMessage());
        }
        project.setRepositoryUrl(repository.canonicalUrl());
        project.setRepositoryOwner(repository.owner());
        project.setRepositoryName(repository.repository());
        if (!"0".equals(project.getPrReviewEnabled()) && !"1".equals(project.getPrReviewEnabled()))
        {
            project.setPrReviewEnabled("0");
        }
        project.setPrTargetBranches(normalizeTargetBranches(project.getPrTargetBranches()));

        if (projectMapper.selectByRepository(PROVIDER, repository.owner(), repository.repository(), excludeProjectId) != null)
        {
            throw new ServiceException("该 GitHub 仓库已接入");
        }

        SysBusinessSystem system = businessSystemService.selectSysBusinessSystemById(project.getBusinessSystemId());
        if (system == null || !"0".equals(system.getStatus()))
        {
            throw new ServiceException("所属业务系统不存在、已停用或当前用户无权访问");
        }
        if (!Objects.equals(system.getDeptId(), project.getDeptId()))
        {
            throw new ServiceException("项目所属部门必须与业务系统所属部门一致");
        }
        deptService.checkDeptDataScope(project.getDeptId());

        SysUser owner = userService.selectUserById(project.getOwnerUserId());
        if (owner == null || !"0".equals(owner.getStatus()))
        {
            throw new ServiceException("项目负责人不存在或已停用");
        }
        userService.checkUserDataScope(project.getOwnerUserId());

        GitCredential credential = credentialMapper.selectGitCredentialById(project.getCredentialId());
        if (credential == null || !"0".equals(credential.getStatus()))
        {
            throw new ServiceException("GitHub 凭据不存在或已停用");
        }
        if (!"0".equals(project.getStatus()) && !"1".equals(project.getStatus()))
        {
            project.setStatus("1");
        }
    }

    private void checkProjectAccess(ReviewProject project)
    {
        if (project == null)
        {
            throw new ServiceException("代码项目不存在");
        }
        if (SecurityUtils.isAdmin())
        {
            return;
        }
        deptService.checkDeptDataScope(project.getDeptId());
        boolean isOwner = Objects.equals(project.getOwnerUserId(), SecurityUtils.getUserId());
        boolean isSystemManager = businessSystemService.selectSysBusinessSystemById(project.getBusinessSystemId()) != null;
        if (!isOwner && !isSystemManager)
        {
            throw new ServiceException("没有权限访问该代码项目");
        }
    }

    private void readAndApplyRepositoryInfo(ReviewProject project)
    {
        String token = credentialService.getPlainToken(project.getCredentialId(), true);
        GitRepositoryCoordinates repository = gitProvider.parseRepository(project.getRepositoryUrl());
        GitRepositoryInfoResult result = gitProvider.readRepository(repository, token);
        if (!result.success())
        {
            throw new ServiceException(result.message());
        }

        List<String> selected = splitValues(project.getPrTargetBranches());
        if (selected.isEmpty() && "0".equals(project.getPrReviewEnabled()))
        {
            selected = recommendTargetBranches(result.branches(), result.defaultBranch());
        }
        if ("0".equals(project.getPrReviewEnabled()) && selected.isEmpty())
        {
            throw new ServiceException("未识别到可用的 PR 目标分支，请在读取仓库信息后选择");
        }
        if (!result.branches().containsAll(selected))
        {
            throw new ServiceException("PR 目标分支必须从 GitHub 实际分支中选择");
        }

        project.setRepositoryUrl(result.repositoryUrl());
        project.setRepositoryOwner(result.repositoryOwner());
        project.setRepositoryName(result.repositoryName());
        project.setDefaultBranch(result.defaultBranch());
        project.setPrTargetBranches(selected.isEmpty() ? null : String.join(",", selected));
        project.setLastCheckStatus("SUCCESS");
        project.setLastCheckMessage("连接成功，可访问 GitHub 仓库");
        project.setLastCheckTime(result.syncedAt());
        project.setLastBranchSyncStatus("SUCCESS");
        project.setLastBranchSyncMessage(result.message() + "，共 " + result.branches().size() + " 个分支");
        project.setLastBranchSyncTime(result.syncedAt());
    }

    private void persistRepositorySync(Long projectId, GitRepositoryInfoResult result)
    {
        ReviewProject update = new ReviewProject();
        update.setProjectId(projectId);
        update.setLastCheckStatus(result.success() ? "SUCCESS" : "FAILED");
        update.setLastCheckMessage(result.success() ? "连接成功，可访问 GitHub 仓库" : result.message());
        update.setLastCheckTime(result.syncedAt());
        update.setLastBranchSyncStatus(result.success() ? "SUCCESS" : "FAILED");
        update.setLastBranchSyncMessage(result.success()
            ? result.message() + "，共 " + result.branches().size() + " 个分支" : result.message());
        update.setLastBranchSyncTime(result.syncedAt());
        if (result.success())
        {
            update.setRepositoryUrl(result.repositoryUrl());
            update.setRepositoryOwner(result.repositoryOwner());
            update.setRepositoryName(result.repositoryName());
            update.setDefaultBranch(result.defaultBranch());
        }
        update.setUpdateBy(SecurityUtils.getUsername());
        projectMapper.updateRepositorySync(update);
    }

    private void copyRepositoryState(ReviewProject existing, ReviewProject project)
    {
        project.setDefaultBranch(existing.getDefaultBranch());
        project.setLastCheckStatus(existing.getLastCheckStatus());
        project.setLastCheckMessage(existing.getLastCheckMessage());
        project.setLastCheckTime(existing.getLastCheckTime());
        project.setLastBranchSyncStatus(existing.getLastBranchSyncStatus());
        project.setLastBranchSyncMessage(existing.getLastBranchSyncMessage());
        project.setLastBranchSyncTime(existing.getLastBranchSyncTime());
    }

    private List<String> recommendTargetBranches(List<String> branches, String defaultBranch)
    {
        for (String recommended : configValues(LONG_LIVED_BRANCHES_KEY, DEFAULT_LONG_LIVED_BRANCHES))
        {
            if (branches.contains(recommended))
            {
                return List.of(recommended);
            }
        }
        return defaultBranch != null && branches.contains(defaultBranch) ? List.of(defaultBranch) : List.of();
    }

    private List<String> configValues(String key, String fallback)
    {
        String value = configService.selectConfigByKey(key);
        return splitValues(value == null || value.isBlank() ? fallback : value);
    }

    private String normalizeTargetBranches(String branches)
    {
        List<String> values = splitValues(branches);
        if (values.stream().anyMatch(value -> value.contains("*") || value.contains("?")))
        {
            throw new ServiceException("PR 目标分支不支持通配规则，请从实际分支中选择");
        }
        return values.isEmpty() ? null : String.join(",", values);
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
            .distinct()
            .collect(Collectors.toList());
    }
}
