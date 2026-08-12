package com.acr.review.service;

import java.util.Set;
import org.springframework.stereotype.Service;
import com.acr.common.core.domain.BaseEntity;
import com.acr.common.exception.ServiceException;
import com.acr.common.utils.SecurityUtils;
import com.acr.review.domain.ReviewProject;
import com.acr.review.domain.ReviewProjectMember;
import com.acr.review.mapper.ReviewProjectMapper;
import com.acr.review.mapper.ReviewProjectMemberMapper;
import com.acr.system.service.ISysDeptService;

/** 项目级数据与动作授权边界。功能权限仍由 Controller 的 RBAC 校验负责。 */
@Service
public class ReviewProjectAccessService
{
    public static final String PROJECT_ACCESS_USER_ID = "projectAccessUserId";

    private final ReviewProjectMapper projectMapper;
    private final ReviewProjectMemberMapper memberMapper;
    private final ISysDeptService deptService;

    public ReviewProjectAccessService(ReviewProjectMapper projectMapper,
                                      ReviewProjectMemberMapper memberMapper,
                                      ISysDeptService deptService)
    {
        this.projectMapper = projectMapper;
        this.memberMapper = memberMapper;
        this.deptService = deptService;
    }

    /** 列表查询叠加当前用户的项目成员范围，超级管理员保持全量视图。 */
    public void applyQueryScope(BaseEntity query)
    {
        if (query != null && !SecurityUtils.isAdmin())
        {
            query.getParams().put(PROJECT_ACCESS_USER_ID, SecurityUtils.getUserId());
        }
    }

    public ReviewProject requireView(Long projectId)
    {
        return requireRole(projectId, Set.of(
            ReviewProjectMember.ROLE_OWNER,
            ReviewProjectMember.ROLE_ADMIN,
            ReviewProjectMember.ROLE_REVIEWER,
            ReviewProjectMember.ROLE_VIEWER));
    }

    public ReviewProject requireOperate(Long projectId)
    {
        return requireRole(projectId, Set.of(
            ReviewProjectMember.ROLE_OWNER,
            ReviewProjectMember.ROLE_ADMIN,
            ReviewProjectMember.ROLE_REVIEWER));
    }

    public ReviewProject requireManage(Long projectId)
    {
        return requireRole(projectId, Set.of(
            ReviewProjectMember.ROLE_OWNER,
            ReviewProjectMember.ROLE_ADMIN));
    }

    public ReviewProject requireOwner(Long projectId)
    {
        return requireRole(projectId, Set.of(ReviewProjectMember.ROLE_OWNER));
    }

    private ReviewProject requireRole(Long projectId, Set<String> allowedRoles)
    {
        ReviewProject project = projectMapper.selectReviewProjectById(projectId);
        if (project == null)
        {
            throw new ServiceException("代码项目不存在");
        }
        if (SecurityUtils.isAdmin())
        {
            return project;
        }

        deptService.checkDeptDataScope(project.getDeptId());
        Long userId = SecurityUtils.getUserId();
        String projectRole;
        if (userId.equals(project.getOwnerUserId()))
        {
            projectRole = ReviewProjectMember.ROLE_OWNER;
        }
        else
        {
            ReviewProjectMember member = memberMapper.selectByProjectAndUser(projectId, userId);
            projectRole = member != null && "0".equals(member.getStatus()) ? member.getProjectRole() : null;
        }
        if (projectRole == null || !allowedRoles.contains(projectRole))
        {
            throw new ServiceException("没有权限访问或操作该代码项目");
        }
        return project;
    }
}
