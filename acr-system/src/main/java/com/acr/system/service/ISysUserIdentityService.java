package com.acr.system.service;

import java.util.List;
import com.acr.system.domain.SysUserIdentity;

public interface ISysUserIdentityService
{
    List<SysUserIdentity> listMineGit(Long userId);

    SysUserIdentity addMineGit(Long userId, String identifier, String displayName, String createBy, String origin);

    void deleteMine(Long userId, Long id);

    SysUserIdentity selectByTypeAndIdentifier(String identityType, String identifier);

    List<SysUserIdentity> listByType(String identityType);

    List<SysUserIdentity> selectScopedList(SysUserIdentity query);

    /** @return true 若发生了改派（先解除再绑定） */
    boolean bindAdmin(Long targetUserId, String identifier, String displayName, String operator);

    void unbindAdmin(Long id);

    SysUserIdentity selectById(Long id);
}
