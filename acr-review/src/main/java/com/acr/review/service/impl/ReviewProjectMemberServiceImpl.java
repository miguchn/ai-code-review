package com.acr.review.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import com.acr.common.core.domain.entity.SysUser;
import com.acr.common.exception.ServiceException;
import com.acr.common.utils.SecurityUtils;
import com.acr.review.domain.ReviewProject;
import com.acr.review.domain.ReviewProjectMember;
import com.acr.review.mapper.ReviewProjectMemberMapper;
import com.acr.review.service.IReviewProjectMemberService;
import com.acr.review.service.ReviewProjectAccessService;
import com.acr.system.service.ISysUserService;

/** 项目成员管理。 */
@Service
public class ReviewProjectMemberServiceImpl implements IReviewProjectMemberService
{
    private static final Set<String> ASSIGNABLE_ROLES = Set.of(
        ReviewProjectMember.ROLE_ADMIN,
        ReviewProjectMember.ROLE_REVIEWER,
        ReviewProjectMember.ROLE_VIEWER);

    private final ReviewProjectMemberMapper memberMapper;
    private final ReviewProjectAccessService accessService;
    private final ISysUserService userService;

    public ReviewProjectMemberServiceImpl(ReviewProjectMemberMapper memberMapper,
                                          ReviewProjectAccessService accessService,
                                          ISysUserService userService)
    {
        this.memberMapper = memberMapper;
        this.accessService = accessService;
        this.userService = userService;
    }

    @Override
    public List<ReviewProjectMember> selectProjectMembers(Long projectId)
    {
        ReviewProject project = accessService.requireView(projectId);
        List<ReviewProjectMember> result = new ArrayList<>();
        ReviewProjectMember owner = new ReviewProjectMember();
        owner.setProjectId(projectId);
        owner.setUserId(project.getOwnerUserId());
        owner.setUserName(project.getOwnerName());
        owner.setDeptId(project.getDeptId());
        owner.setDeptName(project.getDeptName());
        owner.setProjectRole(ReviewProjectMember.ROLE_OWNER);
        owner.setStatus("0");
        owner.setProjectOwner(true);
        result.add(owner);
        result.addAll(memberMapper.selectByProjectId(projectId));
        return result;
    }

    @Override
    public int saveProjectMember(Long projectId, ReviewProjectMember member)
    {
        ReviewProject project = accessService.requireManage(projectId);
        if (member == null || member.getUserId() == null)
        {
            throw new ServiceException("项目成员不能为空");
        }
        String role = member.getProjectRole() == null ? null : member.getProjectRole().trim().toUpperCase();
        if (!ASSIGNABLE_ROLES.contains(role))
        {
            throw new ServiceException("项目角色无效");
        }
        if (member.getUserId().equals(project.getOwnerUserId()))
        {
            throw new ServiceException("项目负责人已拥有 OWNER 权限，无需重复添加");
        }
        SysUser user = userService.selectUserById(member.getUserId());
        if (user == null || !"0".equals(user.getStatus()) || "2".equals(user.getDelFlag()))
        {
            throw new ServiceException("项目成员不存在或已停用");
        }
        userService.checkUserDataScope(member.getUserId());

        ReviewProjectMember existing = memberMapper.selectByProjectAndUser(projectId, member.getUserId());
        if (existing == null)
        {
            member.setProjectId(projectId);
            member.setProjectRole(role);
            member.setStatus("0");
            member.setCreateBy(SecurityUtils.getUsername());
            return memberMapper.insertReviewProjectMember(member);
        }
        existing.setProjectRole(role);
        existing.setStatus("0");
        existing.setUpdateBy(SecurityUtils.getUsername());
        return memberMapper.updateReviewProjectMember(existing);
    }

    @Override
    public int deleteProjectMember(Long projectId, Long memberId)
    {
        accessService.requireManage(projectId);
        ReviewProjectMember member = memberMapper.selectByMemberId(memberId);
        if (member == null || !projectId.equals(member.getProjectId()))
        {
            throw new ServiceException("项目成员不存在");
        }
        return memberMapper.deleteReviewProjectMemberById(memberId);
    }
}
