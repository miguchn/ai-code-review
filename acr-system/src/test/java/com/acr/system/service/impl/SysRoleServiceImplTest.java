package com.acr.system.service.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.test.util.ReflectionTestUtils;
import com.acr.common.core.domain.entity.SysRole;
import com.acr.common.exception.ServiceException;
import com.acr.common.utils.SecurityUtils;
import com.acr.system.mapper.SysRoleMapper;

class SysRoleServiceImplTest
{
    @Test
    void departmentAdministratorCannotCreatePlatformRole()
    {
        SysRoleServiceImpl service = new SysRoleServiceImpl();
        SysRole role = new SysRole();
        role.setRoleScope(SysRole.ROLE_SCOPE_PLATFORM);

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class))
        {
            security.when(SecurityUtils::isAdmin).thenReturn(false);
            assertThrows(ServiceException.class, () -> service.checkRoleAllowed(role));
        }
    }

    @Test
    void departmentAdministratorCannotDowngradeExistingPlatformRole()
    {
        SysRoleMapper roleMapper = mock(SysRoleMapper.class);
        SysRoleServiceImpl service = new SysRoleServiceImpl();
        ReflectionTestUtils.setField(service, "roleMapper", roleMapper);

        SysRole existing = new SysRole(8L);
        existing.setRoleScope(SysRole.ROLE_SCOPE_PLATFORM);
        when(roleMapper.selectRoleById(8L)).thenReturn(existing);
        SysRole update = new SysRole(8L);
        update.setRoleScope(SysRole.ROLE_SCOPE_DEPARTMENT);

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class))
        {
            security.when(SecurityUtils::isAdmin).thenReturn(false);
            assertThrows(ServiceException.class, () -> service.checkRoleAllowed(update));
        }
    }

    @Test
    void superAdministratorCanMaintainPlatformRole()
    {
        SysRoleServiceImpl service = new SysRoleServiceImpl();
        SysRole role = new SysRole();
        role.setRoleScope(SysRole.ROLE_SCOPE_PLATFORM);

        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class))
        {
            security.when(SecurityUtils::isAdmin).thenReturn(true);
            assertDoesNotThrow(() -> service.checkRoleAllowed(role));
        }
    }
}
