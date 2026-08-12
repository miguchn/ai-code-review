package com.acr.review.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import com.acr.common.exception.ServiceException;
import com.acr.common.utils.SecurityUtils;
import com.acr.review.domain.ReviewProject;
import com.acr.review.domain.ReviewProjectMember;
import com.acr.review.domain.ReviewTask;
import com.acr.review.mapper.ReviewProjectMapper;
import com.acr.review.mapper.ReviewProjectMemberMapper;
import com.acr.system.service.ISysDeptService;

class ReviewProjectAccessServiceTest
{
    private final ReviewProjectMapper projectMapper = mock(ReviewProjectMapper.class);
    private final ReviewProjectMemberMapper memberMapper = mock(ReviewProjectMemberMapper.class);
    private final ISysDeptService deptService = mock(ISysDeptService.class);
    private final ReviewProjectAccessService service =
        new ReviewProjectAccessService(projectMapper, memberMapper, deptService);

    @Test
    void queryScopeUsesCurrentUserInsteadOfDepartmentManagerExpansion()
    {
        ReviewTask query = new ReviewTask();
        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class))
        {
            security.when(SecurityUtils::isAdmin).thenReturn(false);
            security.when(SecurityUtils::getUserId).thenReturn(21L);
            service.applyQueryScope(query);
        }
        assertEquals(21L, query.getParams().get(ReviewProjectAccessService.PROJECT_ACCESS_USER_ID));
    }

    @Test
    void sameDepartmentNonMemberIsRejected()
    {
        ReviewProject project = project(100L, 9L, 30L);
        when(projectMapper.selectReviewProjectById(100L)).thenReturn(project);
        when(memberMapper.selectByProjectAndUser(100L, 21L)).thenReturn(null);
        try (MockedStatic<SecurityUtils> security = currentUser(21L))
        {
            assertThrows(ServiceException.class, () -> service.requireView(100L));
        }
        verify(deptService).checkDeptDataScope(9L);
    }

    @Test
    void viewerCanReadButCannotOperate()
    {
        ReviewProject project = project(100L, 9L, 30L);
        ReviewProjectMember member = member(ReviewProjectMember.ROLE_VIEWER);
        when(projectMapper.selectReviewProjectById(100L)).thenReturn(project);
        when(memberMapper.selectByProjectAndUser(100L, 21L)).thenReturn(member);
        try (MockedStatic<SecurityUtils> security = currentUser(21L))
        {
            assertEquals(project, service.requireView(100L));
            assertThrows(ServiceException.class, () -> service.requireOperate(100L));
        }
    }

    @Test
    void reviewerCanOperateAndOwnerCanManage()
    {
        ReviewProject project = project(100L, 9L, 30L);
        when(projectMapper.selectReviewProjectById(100L)).thenReturn(project);
        when(memberMapper.selectByProjectAndUser(100L, 21L)).thenReturn(member(ReviewProjectMember.ROLE_REVIEWER));
        try (MockedStatic<SecurityUtils> security = currentUser(21L))
        {
            assertEquals(project, service.requireOperate(100L));
        }
        try (MockedStatic<SecurityUtils> security = currentUser(30L))
        {
            assertEquals(project, service.requireManage(100L));
        }
    }

    @Test
    void adminDoesNotReceiveProjectFilter()
    {
        ReviewTask query = new ReviewTask();
        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class))
        {
            security.when(SecurityUtils::isAdmin).thenReturn(true);
            service.applyQueryScope(query);
        }
        assertTrue(query.getParams().isEmpty());
    }

    private static ReviewProject project(Long projectId, Long deptId, Long ownerUserId)
    {
        ReviewProject project = new ReviewProject();
        project.setProjectId(projectId);
        project.setDeptId(deptId);
        project.setOwnerUserId(ownerUserId);
        return project;
    }

    private static ReviewProjectMember member(String role)
    {
        ReviewProjectMember member = new ReviewProjectMember();
        member.setProjectRole(role);
        member.setStatus("0");
        return member;
    }

    private static MockedStatic<SecurityUtils> currentUser(Long userId)
    {
        MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class);
        security.when(SecurityUtils::isAdmin).thenReturn(false);
        security.when(SecurityUtils::getUserId).thenReturn(userId);
        return security;
    }
}
