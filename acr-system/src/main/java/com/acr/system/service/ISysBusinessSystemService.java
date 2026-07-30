package com.acr.system.service;

import java.util.List;
import com.acr.system.domain.SysBusinessSystem;

/**
 * 业务系统 服务层
 */
public interface ISysBusinessSystemService
{
    /**
     * 查询业务系统
     *
     * @param systemId 业务系统ID
     * @return 业务系统信息
     */
    public SysBusinessSystem selectSysBusinessSystemById(Long systemId);

    /**
     * 查询业务系统列表（自动根据当前用户权限过滤）
     *
     * @param sysBusinessSystem 业务系统信息
     * @return 业务系统集合
     */
    public List<SysBusinessSystem> selectSysBusinessSystemList(SysBusinessSystem sysBusinessSystem);

    /**
     * 新增业务系统
     *
     * @param sysBusinessSystem 业务系统信息
     * @return 结果
     */
    public int insertSysBusinessSystem(SysBusinessSystem sysBusinessSystem);

    /**
     * 修改业务系统
     *
     * @param sysBusinessSystem 业务系统信息
     * @return 结果
     */
    public int updateSysBusinessSystem(SysBusinessSystem sysBusinessSystem);

    /**
     * 批量删除业务系统信息
     *
     * @param systemIds 需要删除的业务系统ID
     */
    public void deleteSysBusinessSystemByIds(Long[] systemIds);

    /**
     * 校验业务系统编码是否唯一
     *
     * @param sysBusinessSystem 业务系统信息
     * @return 结果
     */
    public boolean checkSystemCodeUnique(SysBusinessSystem sysBusinessSystem);
}
