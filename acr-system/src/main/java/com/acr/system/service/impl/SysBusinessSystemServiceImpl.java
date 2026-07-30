package com.acr.system.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.acr.common.exception.ServiceException;
import com.acr.common.utils.StringUtils;
import com.acr.common.utils.SecurityUtils;
import com.acr.system.domain.SysBusinessSystem;
import com.acr.common.core.domain.entity.SysUser;
import com.acr.system.mapper.SysBusinessSystemMapper;
import com.acr.system.mapper.SysUserMapper;
import com.acr.system.service.ISysBusinessSystemService;

/**
 * 业务系统 服务层实现
 *
 * 数据权限规则：
 * - 系统管理员（admin 角色）可查看全部业务系统
 * - 普通用户仅能查看 manager_ids 中包含自己 user_id 的业务系统
 */
@Service
public class SysBusinessSystemServiceImpl implements ISysBusinessSystemService
{
    @Autowired
    private SysBusinessSystemMapper businessSystemMapper;

    @Autowired
    private SysUserMapper userMapper;

    /**
     * 查询业务系统
     */
    @Override
    public SysBusinessSystem selectSysBusinessSystemById(Long systemId)
    {
        Long userId = SecurityUtils.getUserId();
        boolean isAdmin = SecurityUtils.isAdmin(userId);

        SysBusinessSystem system = businessSystemMapper.selectSysBusinessSystemById(systemId);
        if (!isAdmin && system != null)
        {
            // 非管理员无权查看该业务系统
            if (!hasManagerPermission(system, userId))
            {
                return null;
            }
        }
        if (system != null)
        {
            system.setManagerNames(resolveManagerNames(system.getManagerIds()));
        }
        return system;
    }

    /**
     * 查询业务系统列表
     */
    @Override
    public List<SysBusinessSystem> selectSysBusinessSystemList(SysBusinessSystem sysBusinessSystem)
    {
        Long userId = SecurityUtils.getUserId();
        boolean isAdmin = SecurityUtils.isAdmin(userId);

        List<SysBusinessSystem> list;
        if (isAdmin)
        {
            list = businessSystemMapper.selectSysBusinessSystemList(sysBusinessSystem);
        }
        else
        {
            list = businessSystemMapper.selectSysBusinessSystemByUserId(userId);
        }

        for (SysBusinessSystem system : list)
        {
            system.setManagerNames(resolveManagerNames(system.getManagerIds()));
        }
        return list;
    }

    /**
     * 新增业务系统
     */
    @Override
    public int insertSysBusinessSystem(SysBusinessSystem sysBusinessSystem)
    {
        if (!checkSystemCodeUnique(sysBusinessSystem))
        {
            throw new ServiceException("业务系统编码已存在");
        }
        sysBusinessSystem.setCreateBy(SecurityUtils.getUsername());
        return businessSystemMapper.insertSysBusinessSystem(sysBusinessSystem);
    }

    /**
     * 修改业务系统
     */
    @Override
    public int updateSysBusinessSystem(SysBusinessSystem sysBusinessSystem)
    {
        SysBusinessSystem existing = businessSystemMapper.checkSystemCodeUniqueExcludeId(
            sysBusinessSystem.getSystemCode(), sysBusinessSystem.getSystemId());
        if (existing != null)
        {
            throw new ServiceException("业务系统编码已存在");
        }
        sysBusinessSystem.setUpdateBy(SecurityUtils.getUsername());
        return businessSystemMapper.updateSysBusinessSystem(sysBusinessSystem);
    }

    /**
     * 批量删除业务系统信息
     */
    @Override
    public void deleteSysBusinessSystemByIds(Long[] systemIds)
    {
        businessSystemMapper.deleteSysBusinessSystemByIds(systemIds);
    }

    /**
     * 校验业务系统编码是否唯一
     */
    @Override
    public boolean checkSystemCodeUnique(SysBusinessSystem sysBusinessSystem)
    {
        Long systemId = StringUtils.isNull(sysBusinessSystem.getSystemId()) ? -1L : sysBusinessSystem.getSystemId();
        SysBusinessSystem info;
        if (systemId > 0)
        {
            info = businessSystemMapper.checkSystemCodeUniqueExcludeId(sysBusinessSystem.getSystemCode(), systemId);
        }
        else
        {
            info = businessSystemMapper.checkSystemCodeUnique(sysBusinessSystem.getSystemCode());
        }
        return info == null;
    }

    /**
     * 检查当前用户是否有该业务系统的管理权限
     */
    private boolean hasManagerPermission(SysBusinessSystem system, Long userId)
    {
        if (StringUtils.isEmpty(system.getManagerIds()))
        {
            return false;
        }
        String[] ids = system.getManagerIds().split(",");
        for (String id : ids)
        {
            if (id.trim().equals(String.valueOf(userId)))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * 将管理用户 ID 列表解析为用户名称列表
     */
    private String resolveManagerNames(String managerIds)
    {
        if (StringUtils.isEmpty(managerIds))
        {
            return "";
        }
        String[] ids = managerIds.split(",");
        StringBuilder sb = new StringBuilder();
        for (String id : ids)
        {
            try
            {
                Long uid = Long.parseLong(id.trim());
                SysUser user = userMapper.selectUserById(uid);
                if (user != null)
                {
                    if (sb.length() > 0)
                    {
                        sb.append(", ");
                    }
                    sb.append(user.getNickName());
                }
            }
            catch (NumberFormatException e)
            {
                // skip invalid
            }
        }
        return sb.toString();
    }
}
