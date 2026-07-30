package com.acr.system.mapper;

import java.util.List;
import com.acr.system.domain.SysBusinessSystem;
import org.apache.ibatis.annotations.Param;

/**
 * 业务系统 数据层
 */
public interface SysBusinessSystemMapper
{
    /**
     * 查询业务系统
     *
     * @param systemId 业务系统ID
     * @return 业务系统信息
     */
    public SysBusinessSystem selectSysBusinessSystemById(Long systemId);

    /**
     * 查询业务系统列表（管理员查看全部）
     *
     * @param sysBusinessSystem 业务系统信息
     * @return 业务系统集合
     */
    public List<SysBusinessSystem> selectSysBusinessSystemList(SysBusinessSystem sysBusinessSystem);

    /**
     * 查询业务系统列表（普通用户仅查看有管理权限的）
     *
     * @param userId 用户ID
     * @return 业务系统集合
     */
    public List<SysBusinessSystem> selectSysBusinessSystemByUserId(@Param("userId") Long userId);

    /**
     * 校验业务系统编码是否唯一
     *
     * @param systemCode 业务系统编码
     * @return 业务系统信息
     */
    public SysBusinessSystem checkSystemCodeUnique(String systemCode);

    /**
     * 校验业务系统编码是否唯一（排除指定ID）
     *
     * @param systemCode 业务系统编码
     * @param systemId 业务系统ID
     * @return 业务系统信息
     */
    public SysBusinessSystem checkSystemCodeUniqueExcludeId(@Param("systemCode") String systemCode, @Param("systemId") Long systemId);

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
     * 删除业务系统
     *
     * @param systemId 业务系统ID
     * @return 结果
     */
    public int deleteSysBusinessSystemById(Long systemId);

    /**
     * 批量删除业务系统
     *
     * @param systemIds 需要删除的业务系统ID
     * @return 结果
     */
    public int deleteSysBusinessSystemByIds(Long[] systemIds);
}
