package com.acr.system.service;

import java.util.List;
import com.acr.system.domain.SysUserIdentity;

public interface ISysUserIdentityService
{
    List<SysUserIdentity> listMineGit(Long userId);

    SysUserIdentity addMineGit(Long userId, String identifier, String displayName, String createBy, String origin);

    List<SysUserIdentity> listMineIm(Long userId);

    SysUserIdentity addMineIm(Long userId, String identityType, String identifier, String createBy);

    void deleteMineIm(Long userId, Long id);

    List<SysUserIdentity> listByUserId(Long userId);

    void deleteMine(Long userId, Long id);

    SysUserIdentity selectByTypeAndIdentifier(String identityType, String identifier);

    /** 自动发现 Git 平台用户身份：若 (type, identifier) 不存在则插入，user_id 为空、origin=AUTO。幂等。 */
    void recordAutoDiscovered(String identityType, String identifier, String displayName);

    /** 管理员将自动发现的身份绑定到系统用户（origin 改为 ADMIN）。 */
    void mapIdentity(Long id, Long userId);

    List<SysUserIdentity> listByType(String identityType);

    List<SysUserIdentity> selectScopedList(SysUserIdentity query);

    /** @return true 若发生了改派（先解除再绑定） */
    boolean bindAdmin(Long targetUserId, String identifier, String displayName, String operator);

    void unbindAdmin(Long id);

    SysUserIdentity selectById(Long id);
}
