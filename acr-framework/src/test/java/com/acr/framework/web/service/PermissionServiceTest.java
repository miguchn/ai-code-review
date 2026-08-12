package com.acr.framework.web.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import com.acr.common.core.domain.entity.SysRole;
import com.acr.common.core.domain.entity.SysUser;
import com.acr.common.core.domain.model.LoginUser;

class PermissionServiceTest
{
    private final PermissionService permissionService = new PermissionService();

    @BeforeEach
    void bindRequest()
    {
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
    }

    @AfterEach
    void clearSecurityContext()
    {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void departmentRolePermissionCannotManagePlatformAsset()
    {
        LoginUser loginUser = loginUser(role(SysRole.ROLE_SCOPE_DEPARTMENT, "review:credential:list"));
        authenticate(loginUser);
        assertFalse(permissionService.hasPlatformPermi("review:credential:list"));
    }

    @Test
    void platformPermissionMustComeFromSamePlatformRole()
    {
        SysRole platformRole = role(SysRole.ROLE_SCOPE_PLATFORM, "review:template:list");
        SysRole departmentRole = role(SysRole.ROLE_SCOPE_DEPARTMENT, "review:credential:list");
        LoginUser loginUser = loginUser(platformRole, departmentRole);
        authenticate(loginUser);
        assertTrue(permissionService.hasPlatformPermi("review:template:list"));
        assertFalse(permissionService.hasPlatformPermi("review:credential:list"));
    }

    @Test
    void superAdminKeepsPlatformPermissionBypass()
    {
        SysUser user = new SysUser();
        user.setUserId(1L);
        LoginUser loginUser = new LoginUser(user, Set.of("*:*:*"));
        authenticate(loginUser);
        assertTrue(permissionService.hasPlatformPermi("system:aimodelconfig:edit"));
    }

    private static LoginUser loginUser(SysRole... roles)
    {
        SysUser user = new SysUser();
        user.setUserId(2L);
        user.setRoles(List.of(roles));
        return new LoginUser(user, Set.of("review:credential:list", "review:template:list"));
    }

    private static SysRole role(String roleScope, String permission)
    {
        SysRole role = new SysRole(10L);
        role.setRoleScope(roleScope);
        role.setStatus("0");
        role.setPermissions(Set.of(permission));
        return role;
    }

    private static void authenticate(LoginUser loginUser)
    {
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities()));
    }
}
