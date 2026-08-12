package com.acr.system.service.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.acr.common.annotation.DataScope;
import com.acr.common.constant.UserConstants;
import com.acr.common.core.domain.entity.SysRole;
import com.acr.common.exception.ServiceException;
import com.acr.common.utils.SecurityUtils;
import com.acr.common.utils.StringUtils;
import com.acr.common.utils.spring.SpringUtils;
import com.acr.system.domain.SysRoleDept;
import com.acr.system.domain.SysRoleMenu;
import com.acr.system.domain.SysUserRole;
import com.acr.system.mapper.SysRoleDeptMapper;
import com.acr.system.mapper.SysRoleMapper;
import com.acr.system.mapper.SysRoleMenuMapper;
import com.acr.system.mapper.SysUserRoleMapper;
import com.acr.system.domain.SysBusinessAudit;
import com.acr.system.service.ISysBusinessAuditService;
import com.acr.system.service.ISysRoleService;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;

/**
 * 角色 业务层处理
 * 
 * @author ruoyi
 */
@Service
public class SysRoleServiceImpl implements ISysRoleService
{
    @Autowired
    private SysRoleMapper roleMapper;

    @Autowired
    private SysRoleMenuMapper roleMenuMapper;

    @Autowired
    private SysUserRoleMapper userRoleMapper;

    @Autowired
    private SysRoleDeptMapper roleDeptMapper;

    @Autowired
    private ISysBusinessAuditService businessAuditService;

    /**
     * 根据条件分页查询角色数据
     * 
     * @param role 角色信息
     * @return 角色数据集合信息
     */
    @Override
    @DataScope(deptAlias = "d")
    public List<SysRole> selectRoleList(SysRole role)
    {
        if (!SecurityUtils.isAdmin())
        {
            role.getParams().put("hidePlatformRoles", true);
        }
        return roleMapper.selectRoleList(role);
    }

    /**
     * 根据用户ID查询角色
     * 
     * @param userId 用户ID
     * @return 角色列表
     */
    @Override
    public List<SysRole> selectRolesByUserId(Long userId)
    {
        List<SysRole> userRoles = roleMapper.selectRolePermissionByUserId(userId);
        List<SysRole> roles = selectRoleAll();
        for (SysRole role : roles)
        {
            for (SysRole userRole : userRoles)
            {
                if (role.getRoleId().longValue() == userRole.getRoleId().longValue())
                {
                    role.setFlag(true);
                    break;
                }
            }
        }
        return roles;
    }

    /**
     * 根据用户ID查询权限
     * 
     * @param userId 用户ID
     * @return 权限列表
     */
    @Override
    public Set<String> selectRolePermissionByUserId(Long userId)
    {
        List<SysRole> perms = roleMapper.selectRolePermissionByUserId(userId);
        Set<String> permsSet = new HashSet<>();
        for (SysRole perm : perms)
        {
            if (StringUtils.isNotNull(perm))
            {
                permsSet.addAll(Arrays.asList(perm.getRoleKey().trim().split(",")));
            }
        }
        return permsSet;
    }

    /**
     * 查询所有角色
     * 
     * @return 角色列表
     */
    @Override
    public List<SysRole> selectRoleAll()
    {
        return SpringUtils.getAopProxy(this).selectRoleList(new SysRole());
    }

    /**
     * 根据用户ID获取角色选择框列表
     * 
     * @param userId 用户ID
     * @return 选中角色ID列表
     */
    @Override
    public List<Long> selectRoleListByUserId(Long userId)
    {
        return roleMapper.selectRoleListByUserId(userId);
    }

    /**
     * 通过角色ID查询角色
     * 
     * @param roleId 角色ID
     * @return 角色对象信息
     */
    @Override
    public SysRole selectRoleById(Long roleId)
    {
        return roleMapper.selectRoleById(roleId);
    }

    /**
     * 校验角色名称是否唯一
     * 
     * @param role 角色信息
     * @return 结果
     */
    @Override
    public boolean checkRoleNameUnique(SysRole role)
    {
        Long roleId = StringUtils.isNull(role.getRoleId()) ? -1L : role.getRoleId();
        SysRole info = roleMapper.checkRoleNameUnique(role.getRoleName());
        if (StringUtils.isNotNull(info) && info.getRoleId().longValue() != roleId.longValue())
        {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 校验角色权限是否唯一
     * 
     * @param role 角色信息
     * @return 结果
     */
    @Override
    public boolean checkRoleKeyUnique(SysRole role)
    {
        Long roleId = StringUtils.isNull(role.getRoleId()) ? -1L : role.getRoleId();
        SysRole info = roleMapper.checkRoleKeyUnique(role.getRoleKey());
        if (StringUtils.isNotNull(info) && info.getRoleId().longValue() != roleId.longValue())
        {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 校验角色是否允许操作
     * 
     * @param role 角色信息
     */
    @Override
    public void checkRoleAllowed(SysRole role)
    {
        if (StringUtils.isNotNull(role.getRoleId()) && role.isAdmin())
        {
            throw new ServiceException("不允许操作超级管理员角色");
        }
        if (!SecurityUtils.isAdmin())
        {
            SysRole existing = role.getRoleId() == null ? null : roleMapper.selectRoleById(role.getRoleId());
            if (SysRole.ROLE_SCOPE_PLATFORM.equals(role.getRoleScope())
                || (existing != null && SysRole.ROLE_SCOPE_PLATFORM.equals(existing.getRoleScope())))
            {
                throw new ServiceException("平台级角色仅允许超级管理员维护");
            }
        }
    }

    /**
     * 校验角色是否有数据权限
     * 
     * @param roleIds 角色id
     */
    @Override
    public void checkRoleDataScope(Long... roleIds)
    {
        if (!SecurityUtils.isAdmin())
        {
            for (Long roleId : roleIds)
            {
                SysRole role = new SysRole();
                role.setRoleId(roleId);
                List<SysRole> roles = SpringUtils.getAopProxy(this).selectRoleList(role);
                if (StringUtils.isEmpty(roles))
                {
                    throw new ServiceException("没有权限访问角色数据！");
                }
            }
        }
    }

    /**
     * 通过角色ID查询角色使用数量
     * 
     * @param roleId 角色ID
     * @return 结果
     */
    @Override
    public int countUserRoleByRoleId(Long roleId)
    {
        return userRoleMapper.countUserRoleByRoleId(roleId);
    }

    /**
     * 新增保存角色信息
     * 
     * @param role 角色信息
     * @return 结果
     */
    @Override
    @Transactional
    public int insertRole(SysRole role)
    {
        normalizeAndCheckRoleScope(role);
        // 新增角色信息
        roleMapper.insertRole(role);
        int rows = insertRoleMenu(role);
        recordRoleAudit("CREATE", null, role, "角色创建");
        return rows;
    }

    /**
     * 修改保存角色信息
     * 
     * @param role 角色信息
     * @return 结果
     */
    @Override
    @Transactional
    public int updateRole(SysRole role)
    {
        SysRole before = selectRoleById(role.getRoleId());
        normalizeAndCheckRoleScope(role);
        // 修改角色信息
        roleMapper.updateRole(role);
        // 删除角色与菜单关联
        roleMenuMapper.deleteRoleMenuByRoleId(role.getRoleId());
        int rows = insertRoleMenu(role);
        recordRoleAudit("UPDATE", before, role, "角色及菜单权限变更");
        return rows;
    }

    /**
     * 修改角色状态
     * 
     * @param role 角色信息
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateRoleStatus(SysRole role)
    {
        SysRole before = selectRoleById(role.getRoleId());
        int rows = roleMapper.updateRole(role);
        SysRole after = selectRoleById(role.getRoleId());
        recordRoleAudit("STATUS", before, after, "角色状态变更");
        return rows;
    }

    /**
     * 修改数据权限信息
     * 
     * @param role 角色信息
     * @return 结果
     */
    @Override
    @Transactional
    public int authDataScope(SysRole role)
    {
        SysRole before = selectRoleById(role.getRoleId());
        // 修改角色信息
        roleMapper.updateRole(role);
        // 删除角色与部门关联
        roleDeptMapper.deleteRoleDeptByRoleId(role.getRoleId());
        // 新增角色和部门信息（数据权限）
        int rows = insertRoleDept(role);
        recordRoleAudit("DATA_SCOPE", before, role, "角色数据范围变更");
        return rows;
    }

    /**
     * 新增角色菜单信息
     * 
     * @param role 角色对象
     */
    public int insertRoleMenu(SysRole role)
    {
        int rows = 1;
        // 新增用户与角色管理
        List<SysRoleMenu> list = new ArrayList<SysRoleMenu>();
        for (Long menuId : role.getMenuIds())
        {
            SysRoleMenu rm = new SysRoleMenu();
            rm.setRoleId(role.getRoleId());
            rm.setMenuId(menuId);
            list.add(rm);
        }
        if (list.size() > 0)
        {
            rows = roleMenuMapper.batchRoleMenu(list);
        }
        return rows;
    }

    /**
     * 新增角色部门信息(数据权限)
     *
     * @param role 角色对象
     */
    public int insertRoleDept(SysRole role)
    {
        int rows = 1;
        // 新增角色与部门（数据权限）管理
        List<SysRoleDept> list = new ArrayList<SysRoleDept>();
        for (Long deptId : role.getDeptIds())
        {
            SysRoleDept rd = new SysRoleDept();
            rd.setRoleId(role.getRoleId());
            rd.setDeptId(deptId);
            list.add(rd);
        }
        if (list.size() > 0)
        {
            rows = roleDeptMapper.batchRoleDept(list);
        }
        return rows;
    }

    /**
     * 通过角色ID删除角色
     * 
     * @param roleId 角色ID
     * @return 结果
     */
    @Override
    @Transactional
    public int deleteRoleById(Long roleId)
    {
        SysRole before = selectRoleById(roleId);
        // 删除角色与菜单关联
        roleMenuMapper.deleteRoleMenuByRoleId(roleId);
        // 删除角色与部门关联
        roleDeptMapper.deleteRoleDeptByRoleId(roleId);
        int rows = roleMapper.deleteRoleById(roleId);
        recordRoleAudit("DELETE", before, null, "角色删除");
        return rows;
    }

    /**
     * 批量删除角色信息
     * 
     * @param roleIds 需要删除的角色ID
     * @return 结果
     */
    @Override
    @Transactional
    public int deleteRoleByIds(Long[] roleIds)
    {
        List<SysRole> beforeRoles = new ArrayList<>();
        for (Long roleId : roleIds)
        {
            checkRoleAllowed(new SysRole(roleId));
            checkRoleDataScope(roleId);
            SysRole role = selectRoleById(roleId);
            beforeRoles.add(role);
            if (countUserRoleByRoleId(roleId) > 0)
            {
                throw new ServiceException(String.format("%1$s已分配,不能删除", role.getRoleName()));
            }
        }
        // 删除角色与菜单关联
        roleMenuMapper.deleteRoleMenu(roleIds);
        // 删除角色与部门关联
        roleDeptMapper.deleteRoleDept(roleIds);
        int rows = roleMapper.deleteRoleByIds(roleIds);
        for (SysRole role : beforeRoles)
        {
            recordRoleAudit("DELETE", role, null, "角色删除");
        }
        return rows;
    }

    /**
     * 取消授权用户角色
     * 
     * @param userRole 用户和角色关联信息
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteAuthUser(SysUserRole userRole)
    {
        int rows = userRoleMapper.deleteUserRoleInfo(userRole);
        recordRoleRelationAudit("REVOKE", userRole.getRoleId(),
            new Long[] {userRole.getUserId()}, "角色授权撤销");
        return rows;
    }

    /**
     * 批量取消授权用户角色
     * 
     * @param roleId 角色ID
     * @param userIds 需要取消授权的用户数据ID
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteAuthUsers(Long roleId, Long[] userIds)
    {
        int rows = userRoleMapper.deleteUserRoleInfos(roleId, userIds);
        recordRoleRelationAudit("REVOKE", roleId, userIds, "角色授权撤销");
        return rows;
    }

    /**
     * 批量选择授权用户角色
     * 
     * @param roleId 角色ID
     * @param userIds 需要授权的用户数据ID
     * @return 结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertAuthUsers(Long roleId, Long[] userIds)
    {
        checkRoleAllowed(new SysRole(roleId));
        // 新增用户与角色管理
        List<SysUserRole> list = new ArrayList<SysUserRole>();
        for (Long userId : userIds)
        {
            SysUserRole ur = new SysUserRole();
            ur.setUserId(userId);
            ur.setRoleId(roleId);
            list.add(ur);
        }
        int rows = userRoleMapper.batchUserRole(list);
        recordRoleRelationAudit("GRANT", roleId, userIds, "角色授权变更");
        return rows;
    }

    private void recordRoleAudit(String action, SysRole before, SysRole after, String reason)
    {
        SysRole target = after == null ? before : after;
        if (target == null)
        {
            return;
        }
        SysBusinessAudit audit = new SysBusinessAudit();
        audit.setEventKey("system-role-" + action + "-" + target.getRoleId() + "-" + java.util.UUID.randomUUID());
        audit.setSource("acr-system");
        audit.setAction("SYSTEM_ROLE_" + action);
        audit.setObjectType("SYS_ROLE");
        audit.setObjectId(String.valueOf(target.getRoleId()));
        audit.setObjectName(target.getRoleName());
        audit.setBeforeValue(before == null ? null : JSON.toJSONString(before));
        audit.setAfterValue(after == null ? null : JSON.toJSONString(after));
        audit.setReason(reason);
        businessAuditService.record(audit);
    }

    private void recordRoleRelationAudit(String action, Long roleId, Long[] userIds, String reason)
    {
        JSONObject relation = new JSONObject();
        relation.put("roleId", roleId);
        relation.put("userIds", userIds);
        SysBusinessAudit audit = new SysBusinessAudit();
        audit.setEventKey("system-role-user-" + action + "-" + roleId + "-" + java.util.UUID.randomUUID());
        audit.setSource("acr-system");
        audit.setAction("SYSTEM_ROLE_USER_" + action);
        audit.setObjectType("SYS_ROLE_USER");
        audit.setObjectId(String.valueOf(roleId));
        audit.setBeforeValue("REVOKE".equals(action) ? relation.toJSONString() : null);
        audit.setAfterValue("GRANT".equals(action) ? relation.toJSONString() : null);
        audit.setRelatedObject(relation.toJSONString());
        audit.setReason(reason);
        businessAuditService.record(audit);
    }

    private void normalizeAndCheckRoleScope(SysRole role)
    {
        if (StringUtils.isEmpty(role.getRoleScope()))
        {
            role.setRoleScope(SysRole.ROLE_SCOPE_DEPARTMENT);
        }
        if (!SysRole.ROLE_SCOPE_PLATFORM.equals(role.getRoleScope())
            && !SysRole.ROLE_SCOPE_DEPARTMENT.equals(role.getRoleScope()))
        {
            throw new ServiceException("角色层级无效");
        }
        checkRoleAllowed(role);
    }
}
