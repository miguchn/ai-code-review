package com.acr.review.service.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.acr.common.annotation.DataScope;
import com.acr.common.core.domain.entity.SysDept;
import com.acr.common.core.domain.entity.SysUser;
import com.acr.common.exception.ServiceException;
import com.acr.common.utils.SecurityUtils;
import com.acr.common.utils.StringUtils;
import com.acr.review.delivery.ReviewDeliveryConstants;
import com.acr.review.domain.GitCredential;
import com.acr.review.domain.GitRepositoryReadRequest;
import com.acr.review.domain.ReviewNotifyChannel;
import com.acr.review.domain.ReviewPipelineConstants;
import com.acr.review.domain.ReviewProject;
import com.acr.review.domain.ReviewProjectOptions;
import com.acr.review.domain.ReviewRepositoryInfo;
import com.acr.review.domain.ReviewTemplate;
import com.acr.review.git.GitAccessContext;
import com.acr.review.git.GitAdapterRegistry;
import com.acr.review.git.GitConnectionFailure;
import com.acr.review.git.GitConnectionResult;
import com.acr.review.git.GitProvider;
import com.acr.review.git.GitProviderCodes;
import com.acr.review.git.GitRepositoryCoordinates;
import com.acr.review.git.GitRepositoryInfoResult;
import com.acr.review.mapper.GitCredentialMapper;
import com.acr.review.mapper.ReviewNotifyChannelMapper;
import com.acr.review.mapper.ReviewProjectMapper;
import com.acr.review.security.CredentialCryptoService;
import com.acr.review.service.IGitCredentialService;
import com.acr.review.service.IReviewProjectService;
import com.acr.review.service.IReviewTemplateService;
import com.acr.review.service.ReviewScoringConstants;
import com.acr.system.domain.SysAiModelConfig;
import com.acr.system.domain.SysBusinessSystem;
import com.acr.system.service.ISysAiModelConfigService;
import com.acr.system.service.ISysBusinessSystemService;
import com.acr.system.service.ISysConfigService;
import com.acr.system.service.ISysDeptService;
import com.acr.system.service.ISysUserService;

/** 代码项目管理（多平台）。 */
@Service
public class ReviewProjectServiceImpl implements IReviewProjectService
{
    private static final String LONG_LIVED_BRANCHES_KEY = "review.github.longLivedBranches";
    private static final String ROBOT_BRANCH_PREFIXES_KEY = "review.github.robotBranchPrefixes";
    private static final String PR_EVENTS_KEY = "review.github.prEvents";
    private static final String DEFAULT_LONG_LIVED_BRANCHES = "dev,develop,main,int,uat";
    private static final String DEFAULT_ROBOT_BRANCH_PREFIXES = "dependabot/,renovate/,github-actions/";
    private static final String DEFAULT_PR_EVENTS = "opened,reopened,synchronize";

    private final ReviewProjectMapper projectMapper;
    private final GitCredentialMapper credentialMapper;
    private final ReviewNotifyChannelMapper notifyChannelMapper;
    private final IGitCredentialService credentialService;
    private final GitAdapterRegistry adapterRegistry;
    private final ISysBusinessSystemService businessSystemService;
    private final ISysConfigService configService;
    private final ISysDeptService deptService;
    private final ISysUserService userService;
    private final ISysAiModelConfigService aiModelConfigService;
    private final IReviewTemplateService templateService;
    private final CredentialCryptoService cryptoService;
    private final String webhookCallbackBaseUrl;

    public ReviewProjectServiceImpl(ReviewProjectMapper projectMapper,
                                    GitCredentialMapper credentialMapper,
                                    ReviewNotifyChannelMapper notifyChannelMapper,
                                    IGitCredentialService credentialService,
                                    GitAdapterRegistry adapterRegistry,
                                    ISysBusinessSystemService businessSystemService,
                                    ISysConfigService configService,
                                    ISysDeptService deptService,
                                    ISysUserService userService,
                                    ISysAiModelConfigService aiModelConfigService,
                                    IReviewTemplateService templateService,
                                    CredentialCryptoService cryptoService,
                                    @Value("${review.webhook.callback-base-url:http://localhost:8080}") String webhookCallbackBaseUrl)
    {
        this.projectMapper = projectMapper;
        this.credentialMapper = credentialMapper;
        this.notifyChannelMapper = notifyChannelMapper;
        this.credentialService = credentialService;
        this.adapterRegistry = adapterRegistry;
        this.businessSystemService = businessSystemService;
        this.configService = configService;
        this.deptService = deptService;
        this.userService = userService;
        this.aiModelConfigService = aiModelConfigService;
        this.templateService = templateService;
        this.cryptoService = cryptoService;
        this.webhookCallbackBaseUrl = webhookCallbackBaseUrl;
    }

    @Override
    public ReviewProject selectReviewProjectById(Long projectId)
    {
        ReviewProject project = projectMapper.selectReviewProjectById(projectId);
        checkProjectAccess(project);
        fillWebhookView(project);
        return project;
    }

    @Override
    @DataScope(deptAlias = "d", userAlias = "owner", permission = "review:project:list")
    public List<ReviewProject> selectReviewProjectList(ReviewProject project)
    {
        if (!SecurityUtils.isAdmin())
        {
            project.setAccessUserId(SecurityUtils.getUserId());
        }
        return projectMapper.selectReviewProjectList(project);
    }

    @Override
    @DataScope(deptAlias = "d", userAlias = "owner", permission = "review:project:list")
    public int countReviewProjectList(ReviewProject project)
    {
        if (!SecurityUtils.isAdmin())
        {
            project.setAccessUserId(SecurityUtils.getUserId());
        }
        return projectMapper.countReviewProjectList(project);
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

        fillDepartmentAndOwnerOptions(options);

        GitCredential credentialQuery = new GitCredential();
        credentialQuery.setStatus("0");
        options.setCredentials(credentialMapper.selectGitCredentialList(credentialQuery).stream()
            .map(credential -> new ReviewProjectOptions.Option(credential.getCredentialId(), credential.getCredentialName(), null, null, credential.getStatus()))
            .toList());

        SysAiModelConfig modelQuery = new SysAiModelConfig();
        modelQuery.setEnabled("1");
        options.setModels(aiModelConfigService.selectSysAiModelConfigList(modelQuery).stream()
            .map(model -> {
                String label = model.getModelName();
                if ("1".equals(model.getIsDefault()))
                {
                    label = label + "（平台默认）";
                }
                return new ReviewProjectOptions.Option(model.getModelId(), label, null, null, model.getEnabled());
            })
            .toList());

        ReviewTemplate templateQuery = new ReviewTemplate();
        templateQuery.setStatus("0");
        options.setTemplates(templateService.selectReviewTemplateList(templateQuery).stream()
            .map(template -> {
                ReviewProjectOptions.Option option = new ReviewProjectOptions.Option(
                    template.getTemplateId(), template.getTemplateName(), null, null, template.getStatus());
                option.setTechStack(template.getTechStack());
                option.setVersionNo(template.getVersionNo());
                return option;
            })
            .toList());

        options.setLongLivedBranches(configValues(LONG_LIVED_BRANCHES_KEY, DEFAULT_LONG_LIVED_BRANCHES));
        options.setRobotBranchPrefixes(configValues(ROBOT_BRANCH_PREFIXES_KEY, DEFAULT_ROBOT_BRANCH_PREFIXES));
        options.setPrEvents(configValues(PR_EVENTS_KEY, DEFAULT_PR_EVENTS));
        options.setWebhookCallbackUrl(buildWebhookCallbackUrl(null));
        return options;
    }

    /**
     * 管理员返回全量部门/负责人；非管理员仅本部门及子部门与对应用户。
     * 当前用户无所属部门时 fail-close：departments / owners 返回空列表。
     */
    private void fillDepartmentAndOwnerOptions(ReviewProjectOptions options)
    {
        if (SecurityUtils.isAdmin())
        {
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
            return;
        }

        Long deptId = SecurityUtils.getDeptId();
        if (deptId == null)
        {
            options.setDepartments(List.of());
            options.setOwners(List.of());
            return;
        }

        options.setDepartments(listScopedDepartments(deptId).stream()
            .map(dept -> new ReviewProjectOptions.Option(dept.getDeptId(), dept.getDeptName(), dept.getDeptId(), dept.getParentId(), dept.getStatus()))
            .toList());

        SysUser userQuery = new SysUser();
        userQuery.setStatus("0");
        userQuery.setDeptId(deptId);
        options.setOwners(userService.selectUserList(userQuery).stream()
            .map(user -> new ReviewProjectOptions.Option(user.getUserId(), user.getNickName(), user.getDeptId(), null, user.getStatus()))
            .toList());
    }

    /** 当前部门 + ancestors 子树中 status=0 的部门。 */
    private List<SysDept> listScopedDepartments(Long deptId)
    {
        List<SysDept> departments = new ArrayList<>();
        SysDept self = deptService.selectDeptById(deptId);
        if (self != null && "0".equals(self.getStatus()) && !"2".equals(self.getDelFlag()))
        {
            departments.add(self);
        }
        List<SysDept> children = deptService.selectChildrenDeptById(deptId);
        if (children != null)
        {
            for (SysDept child : children)
            {
                if (child != null && "0".equals(child.getStatus()) && !"2".equals(child.getDelFlag()))
                {
                    departments.add(child);
                }
            }
        }
        return departments;
    }

    /**
     * Git 凭据为平台级共享资源，由管理员统一维护凭据，
     * 访问控制依赖 review:project:test 权限串与操作日志审计。
     */
    @Override
    public ReviewRepositoryInfo readRepositoryInfo(GitRepositoryReadRequest request)
    {
        GitCredential credential = credentialMapper.selectGitCredentialById(request.getCredentialId());
        if (credential == null || !"0".equals(credential.getStatus()))
        {
            return new ReviewRepositoryInfo(false, GitConnectionFailure.INVALID_REPOSITORY_URL, "Git 凭据不存在或已停用",
                null, null, null, null, null, List.of(), List.of(), new Date());
        }
        String provider = credential.getProvider();
        GitProvider gitProvider = adapterRegistry.requireProvider(provider);
        GitAccessContext parseAccess = GitAccessContext.forParse(
            GitCredentialServiceImpl.resolveServerUrl(provider, credential.getServerUrl()));
        GitRepositoryCoordinates repository;
        try
        {
            repository = gitProvider.parseRepository(request.getRepositoryUrl(), parseAccess);
        }
        catch (IllegalArgumentException e)
        {
            return new ReviewRepositoryInfo(false, GitConnectionFailure.INVALID_REPOSITORY_URL, e.getMessage(),
                null, null, null, null, null, List.of(), List.of(), new Date());
        }

        ReviewProject existing = null;
        if (request.getProjectId() != null)
        {
            existing = selectReviewProjectById(request.getProjectId());
        }

        String token = credentialService.getPlainToken(request.getCredentialId(), true);
        GitAccessContext access = GitAccessContext.of(token,
            GitCredentialServiceImpl.resolveServerUrl(provider, credential.getServerUrl()));
        GitRepositoryInfoResult result = gitProvider.readRepository(repository, access);
        List<String> recommended = result.success()
            ? recommendTargetBranches(result.branches(), result.defaultBranch()) : List.of();

        if (existing != null
            && Objects.equals(existing.getRepositoryUrl(), repository.canonicalUrl())
            && Objects.equals(existing.getCredentialId(), request.getCredentialId()))
        {
            persistRepositorySync(existing.getProjectId(), result);
        }

        return new ReviewRepositoryInfo(result.success(), result.failure(), result.message(), result.repositoryUrl(),
            result.repositoryOwner(), result.repositoryName(), repository.fullPath(), result.defaultBranch(),
            result.branches(), recommended, result.syncedAt());
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
        applyWebhookSecret(project);
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
        applyWebhookSecret(project);
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
            if (!"0".equals(project.getPrReviewEnabled()) && !"0".equals(project.getPushReviewEnabled()))
            {
                throw new ServiceException("请至少选择一种审查类型");
            }
            if ("0".equals(project.getPrReviewEnabled())
                && (project.getPrTargetBranches() == null || project.getPrTargetBranches().isBlank()))
            {
                throw new ServiceException("启用 PR 审查的项目必须配置目标分支");
            }
            if ("0".equals(project.getPushReviewEnabled())
                && (project.getPushTriggerBranches() == null || project.getPushTriggerBranches().isBlank()))
            {
                throw new ServiceException("启用推送审查的项目必须配置触发分支");
            }
        }
        return projectMapper.updateProjectStatus(projectId, status, SecurityUtils.getUsername());
    }

    @Override
    public GitConnectionResult testConnection(Long projectId)
    {
        ReviewProject project = selectReviewProjectById(projectId);
        GitCredential credential = credentialMapper.selectGitCredentialById(project.getCredentialId());
        String provider = project.getProvider();
        String token = credentialService.getPlainToken(project.getCredentialId(), true);
        GitAccessContext access = GitAccessContext.of(token,
            GitCredentialServiceImpl.resolveServerUrl(provider, credential.getServerUrl()));
        GitProvider gitProvider = adapterRegistry.requireProvider(provider);
        GitRepositoryCoordinates repository = gitProvider.parseRepository(project.getRepositoryUrl(), access);
        GitConnectionResult result = gitProvider.testRepository(repository, access);

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
        if (StringUtils.isEmpty(project.getPrimaryStack()))
        {
            throw new ServiceException("项目主要语言/技术栈不能为空");
        }
        project.setPrimaryStack(project.getPrimaryStack().trim());
        if (StringUtils.isEmpty(project.getProvider()))
        {
            throw new ServiceException("Git Provider 不能为空");
        }
        String provider = project.getProvider().trim().toUpperCase(java.util.Locale.ROOT);
        if (!GitProviderCodes.isSupported(provider))
        {
            throw new ServiceException("暂不支持的 Git Provider：" + provider);
        }
        project.setProvider(provider);

        GitCredential credential = credentialMapper.selectGitCredentialById(project.getCredentialId());
        if (credential == null || !"0".equals(credential.getStatus()))
        {
            throw new ServiceException("Git 凭据不存在或已停用");
        }
        if (!provider.equalsIgnoreCase(credential.getProvider()))
        {
            throw new ServiceException("项目平台与凭据平台不一致");
        }

        GitProvider gitProvider = adapterRegistry.requireProvider(provider);
        GitAccessContext parseAccess = GitAccessContext.forParse(
            GitCredentialServiceImpl.resolveServerUrl(provider, credential.getServerUrl()));
        GitRepositoryCoordinates repository;
        try
        {
            repository = gitProvider.parseRepository(project.getRepositoryUrl(), parseAccess);
        }
        catch (IllegalArgumentException e)
        {
            throw new ServiceException(e.getMessage());
        }
        project.setRepositoryUrl(repository.canonicalUrl());
        project.setRepositoryOwner(repository.owner());
        project.setRepositoryName(repository.repository());
        project.setRepositoryFullPath(repository.fullPath());
        if (!"0".equals(project.getPrReviewEnabled()) && !"1".equals(project.getPrReviewEnabled()))
        {
            project.setPrReviewEnabled("0");
        }
        if (!"0".equals(project.getPushReviewEnabled()) && !"1".equals(project.getPushReviewEnabled()))
        {
            project.setPushReviewEnabled("1");
        }
        normalizeInlineCommentConfig(project);
        if (!"0".equals(project.getPrReviewEnabled()) && !"0".equals(project.getPushReviewEnabled()))
        {
            throw new ServiceException("请至少选择一种审查类型");
        }
        project.setPrTargetBranches(normalizeTargetBranches(project.getPrTargetBranches()));
        project.setPushTriggerBranches(normalizePushTriggerBranches(project.getPushTriggerBranches()));
        if ("0".equals(project.getPushReviewEnabled())
            && (project.getPushTriggerBranches() == null || project.getPushTriggerBranches().isBlank()))
        {
            throw new ServiceException("启用推送审查时必须配置触发分支");
        }
        if ("0".equals(project.getPrReviewEnabled())
            && (project.getPrTargetBranches() == null || project.getPrTargetBranches().isBlank()))
        {
            throw new ServiceException("启用合并请求审查时必须配置目标分支");
        }

        if (projectMapper.selectByFullPath(provider, repository.fullPath(), excludeProjectId) != null)
        {
            throw new ServiceException("该仓库已接入");
        }
        if (projectMapper.selectByRepository(provider, repository.owner(), repository.repository(), excludeProjectId) != null)
        {
            throw new ServiceException("该仓库已接入");
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

        validateReviewExecutionConfig(project);
        normalizeScopeConfig(project);
        normalizeNotifyConfig(project);

        if (!"0".equals(project.getStatus()) && !"1".equals(project.getStatus()))
        {
            project.setStatus("1");
        }
    }

    /**
     * 行内评论配置归一：开关非法/空回落停用（'1'）；
     * 严重度白名单空或全非法回落 CRITICAL,HIGH，仅保留 CRITICAL/HIGH/MEDIUM/LOW。
     */
    private void normalizeInlineCommentConfig(ReviewProject project)
    {
        if (!"0".equals(project.getInlineCommentEnabled()) && !"1".equals(project.getInlineCommentEnabled()))
        {
            project.setInlineCommentEnabled("1");
        }
        project.setInlineSeverities(normalizeInlineSeverities(project.getInlineSeverities()));
    }

    static String normalizeInlineSeverities(String csv)
    {
        if (StringUtils.isEmpty(csv))
        {
            return ReviewDeliveryConstants.DEFAULT_INLINE_SEVERITIES;
        }
        java.util.LinkedHashSet<String> allowed = new java.util.LinkedHashSet<>();
        for (String part : csv.split("[,;\\s]+"))
        {
            if (part == null || part.isBlank())
            {
                continue;
            }
            String severity = part.trim().toUpperCase(java.util.Locale.ROOT);
            if (ReviewScoringConstants.SEVERITY_CRITICAL.equals(severity)
                || ReviewScoringConstants.SEVERITY_HIGH.equals(severity)
                || ReviewScoringConstants.SEVERITY_MEDIUM.equals(severity)
                || ReviewScoringConstants.SEVERITY_LOW.equals(severity))
            {
                allowed.add(severity);
            }
        }
        if (allowed.isEmpty())
        {
            return ReviewDeliveryConstants.DEFAULT_INLINE_SEVERITIES;
        }
        return String.join(",", allowed);
    }

    /** 审查范围配置归一：排除 glob 逐行 trim 去空行去重，三开关非法值回落默认（N/N/Y）。 */
    private void normalizeScopeConfig(ReviewProject project)
    {
        String patterns = ReviewTaskSnapshotServiceImpl.normalizeScopePatterns(project.getScopeExcludePatterns());
        if (patterns != null && patterns.length() > 2000)
        {
            throw new ServiceException("审查范围排除路径过长，请控制在 2000 字符以内");
        }
        project.setScopeExcludePatterns(patterns);
        project.setScopeIncludeTests(normalizeScopeFlag(project.getScopeIncludeTests(), "N"));
        project.setScopeReportExisting(normalizeScopeFlag(project.getScopeReportExisting(), "N"));
        project.setScopeExpandEnabled(normalizeScopeFlag(project.getScopeExpandEnabled(), "Y"));
    }

    private static String normalizeScopeFlag(String value, String defaultValue)
    {
        return ("Y".equals(value) || "N".equals(value)) ? value : defaultValue;
    }

    /** 通知绑定归一：总开关默认 N；失败简讯默认 Y；启用时渠道必须存在且可用。 */
    private void normalizeNotifyConfig(ReviewProject project)
    {
        project.setNotifyEnabled(normalizeScopeFlag(project.getNotifyEnabled(), "N"));
        project.setNotifyOnFailure(normalizeScopeFlag(project.getNotifyOnFailure(), "Y"));
        if (!"Y".equals(project.getNotifyEnabled()))
        {
            return;
        }
        if (project.getNotifyChannelId() == null)
        {
            throw new ServiceException("启用通知时必须选择通知渠道");
        }
        ReviewNotifyChannel channel = notifyChannelMapper.selectReviewNotifyChannelById(project.getNotifyChannelId());
        if (channel == null || !"0".equals(channel.getStatus()))
        {
            throw new ServiceException("通知渠道不存在或已停用");
        }
        if (!ReviewDeliveryConstants.isSupportedNotifyChannelType(channel.getChannelType()))
        {
            throw new ServiceException("通知渠道类型无效");
        }
    }

    /** 审查方式二选一：大模型审查绑定模型+模板；审查引擎绑定引擎，不绑定项目级模型/模板。 */
    private void validateReviewExecutionConfig(ReviewProject project)
    {
        if (StringUtils.isEmpty(project.getPrimaryStack()))
        {
            project.setPrimaryStack("FULLSTACK");
        }
        String reviewMode = ReviewPipelineConstants.normalizeReviewMode(
            StringUtils.defaultIfEmpty(project.getReviewMode(), ReviewPipelineConstants.REVIEW_MODE_OCR_ENGINE));
        project.setReviewMode(reviewMode);

        if (ReviewPipelineConstants.isLlmDirectMode(reviewMode))
        {
            if (project.getModelId() == null)
            {
                throw new ServiceException("大模型审查必须选择模型服务中的模型配置");
            }
            if (project.getTemplateId() == null)
            {
                throw new ServiceException("大模型审查必须选择审查模板");
            }
            SysAiModelConfig model = aiModelConfigService.selectSysAiModelConfigById(project.getModelId());
            if (model == null || !"1".equals(model.getEnabled()))
            {
                throw new ServiceException("所选模型不存在或未启用，请先在「模型服务」启用");
            }
            templateService.selectEnabledTemplateById(project.getTemplateId());
            project.setEngineCode(null);
            return;
        }

        if (!ReviewPipelineConstants.isOcrEngineMode(reviewMode))
        {
            throw new ServiceException("审查方式仅支持「大模型审查」或「审查引擎」二选一");
        }
        if (StringUtils.isEmpty(project.getEngineCode()))
        {
            project.setEngineCode(ReviewPipelineConstants.ENGINE_OPEN_CODE_REVIEW);
        }
        if (!ReviewPipelineConstants.ENGINE_OPEN_CODE_REVIEW.equals(project.getEngineCode()))
        {
            throw new ServiceException("当前仅支持 open-code-review 审查引擎");
        }
        // 引擎路径禁止混用项目级模型/模板，运行时使用平台默认模型注入 OCR
        project.setModelId(null);
        project.setTemplateId(null);
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
        GitCredential credential = credentialMapper.selectGitCredentialById(project.getCredentialId());
        String provider = project.getProvider();
        String token = credentialService.getPlainToken(project.getCredentialId(), true);
        GitAccessContext access = GitAccessContext.of(token,
            GitCredentialServiceImpl.resolveServerUrl(provider, credential.getServerUrl()));
        GitProvider gitProvider = adapterRegistry.requireProvider(provider);
        GitRepositoryCoordinates repository = gitProvider.parseRepository(project.getRepositoryUrl(), access);
        GitRepositoryInfoResult result = gitProvider.readRepository(repository, access);
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
            throw new ServiceException("未识别到可用的合并请求目标分支，请在读取仓库信息后选择");
        }
        if (!result.branches().containsAll(selected))
        {
            throw new ServiceException("合并请求目标分支必须从实际分支中选择");
        }

        project.setRepositoryUrl(result.repositoryUrl());
        project.setRepositoryOwner(result.repositoryOwner());
        project.setRepositoryName(result.repositoryName());
        project.setRepositoryFullPath(repository.fullPath());
        project.setDefaultBranch(result.defaultBranch());
        project.setPrTargetBranches(selected.isEmpty() ? null : String.join(",", selected));
        project.setLastCheckStatus("SUCCESS");
        project.setLastCheckMessage("连接成功，可访问仓库");
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
        update.setLastCheckMessage(result.success() ? "连接成功，可访问仓库" : result.message());
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

    /** Webhook Secret 只写：提交非空时加密更新，留空保留原值。 */
    private void applyWebhookSecret(ReviewProject project)
    {
        if (StringUtils.isNotEmpty(project.getWebhookSecret()))
        {
            project.setWebhookSecretCiphertext(cryptoService.encryptWebhookSecret(project.getWebhookSecret().trim()));
        }
    }

    /** 详情视图组装：回调地址；Secret 是否配置由列表 SQL 布尔列给出，密文不出服务端。 */
    private void fillWebhookView(ReviewProject project)
    {
        project.setWebhookCallbackUrl(buildWebhookCallbackUrl(project.getProvider()));
    }

    private String buildWebhookCallbackUrl(String provider)
    {
        String base = webhookCallbackBaseUrl.endsWith("/")
            ? webhookCallbackBaseUrl.substring(0, webhookCallbackBaseUrl.length() - 1)
            : webhookCallbackBaseUrl;
        String code = StringUtils.isNotEmpty(provider) ? provider.toLowerCase(java.util.Locale.ROOT) : "github";
        return base + "/webhook/" + code;
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

    /** 推送触发分支：允许 glob 通配，逗号/中文逗号/换行分隔。 */
    private String normalizePushTriggerBranches(String branches)
    {
        if (branches == null || branches.isBlank())
        {
            return null;
        }
        List<String> values = Arrays.stream(branches.replace('，', ',').replace('\n', ',').replace('\r', ',').split(","))
            .map(String::trim)
            .filter(value -> !value.isEmpty())
            .distinct()
            .collect(Collectors.toList());
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
