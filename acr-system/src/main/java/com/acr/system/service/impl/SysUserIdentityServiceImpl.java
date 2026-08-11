package com.acr.system.service.impl;

import java.util.List;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.acr.common.exception.ServiceException;
import com.acr.common.utils.StringUtils;
import com.acr.system.domain.SysUserIdentity;
import com.acr.system.mapper.SysUserIdentityMapper;
import com.acr.system.service.ISysUserIdentityService;
import com.acr.system.service.ISysUserService;

@Service
public class SysUserIdentityServiceImpl implements ISysUserIdentityService
{
    private final SysUserIdentityMapper identityMapper;
    private final ISysUserService userService;

    public SysUserIdentityServiceImpl(SysUserIdentityMapper identityMapper, ISysUserService userService)
    {
        this.identityMapper = identityMapper;
        this.userService = userService;
    }

    @Override
    public List<SysUserIdentity> listMineGit(Long userId)
    {
        return identityMapper.selectByUserId(userId, SysUserIdentity.TYPE_GIT_COMMIT);
    }

    @Override
    public SysUserIdentity addMineGit(Long userId, String identifier, String displayName, String createBy,
                                     String origin)
    {
        String normalized = normalizeGitIdentifier(identifier);
        if (StringUtils.isEmpty(normalized))
        {
            throw new ServiceException("请填写提交邮箱或 Git 名称");
        }
        SysUserIdentity existing = identityMapper.selectByTypeAndIdentifier(
            SysUserIdentity.TYPE_GIT_COMMIT, normalized);
        if (existing != null)
        {
            return resolveExistingOwnership(userId, existing);
        }
        String resolvedOrigin = SysUserIdentity.ORIGIN_AUTO.equals(origin)
            ? SysUserIdentity.ORIGIN_AUTO : SysUserIdentity.ORIGIN_SELF;
        SysUserIdentity row = new SysUserIdentity();
        row.setUserId(userId);
        row.setIdentityType(SysUserIdentity.TYPE_GIT_COMMIT);
        row.setIdentifier(normalized);
        row.setDisplayName(StringUtils.isNotEmpty(displayName) ? displayName.trim() : null);
        row.setOrigin(resolvedOrigin);
        row.setCreateBy(createBy);
        try
        {
            identityMapper.insert(row);
            return row;
        }
        catch (DuplicateKeyException ex)
        {
            return resolveDuplicateAfterInsert(userId, normalized);
        }
    }

    @Override
    public void deleteMine(Long userId, Long id)
    {
        SysUserIdentity row = identityMapper.selectById(id);
        if (row == null)
        {
            throw new ServiceException("关联不存在或已删除");
        }
        if (!userId.equals(row.getUserId()))
        {
            throw new ServiceException("只能移除自己的提交邮箱");
        }
        identityMapper.deleteById(id);
    }

    @Override
    public SysUserIdentity selectByTypeAndIdentifier(String identityType, String identifier)
    {
        return identityMapper.selectByTypeAndIdentifier(identityType, identifier);
    }

    @Override
    public List<SysUserIdentity> listByType(String identityType)
    {
        return identityMapper.selectByType(identityType);
    }

    @Override
    public List<SysUserIdentity> selectScopedList(SysUserIdentity query)
    {
        return identityMapper.selectScopedList(query);
    }

    @Override
    @Transactional
    public boolean bindAdmin(Long targetUserId, String identifier, String displayName, String operator)
    {
        if (targetUserId == null)
        {
            throw new ServiceException("请选择要关联的用户");
        }
        userService.checkUserDataScope(targetUserId);
        String normalized = normalizeGitIdentifier(identifier);
        if (StringUtils.isEmpty(normalized))
        {
            throw new ServiceException("请填写提交邮箱或 Git 名称");
        }
        boolean reassigned = false;
        SysUserIdentity existing = identityMapper.selectByTypeAndIdentifier(
            SysUserIdentity.TYPE_GIT_COMMIT, normalized);
        if (existing != null)
        {
            if (targetUserId.equals(existing.getUserId()))
            {
                return false;
            }
            userService.checkUserDataScope(existing.getUserId());
            identityMapper.deleteById(existing.getId());
            reassigned = true;
        }
        SysUserIdentity row = new SysUserIdentity();
        row.setUserId(targetUserId);
        row.setIdentityType(SysUserIdentity.TYPE_GIT_COMMIT);
        row.setIdentifier(normalized);
        row.setDisplayName(StringUtils.isNotEmpty(displayName) ? displayName.trim() : null);
        row.setOrigin(SysUserIdentity.ORIGIN_ADMIN);
        row.setCreateBy(operator);
        try
        {
            identityMapper.insert(row);
            return reassigned;
        }
        catch (DuplicateKeyException ex)
        {
            resolveDuplicateAfterInsert(targetUserId, normalized);
            return reassigned;
        }
    }

    @Override
    public void unbindAdmin(Long id)
    {
        SysUserIdentity row = identityMapper.selectById(id);
        if (row == null)
        {
            throw new ServiceException("关联不存在或已删除");
        }
        userService.checkUserDataScope(row.getUserId());
        identityMapper.deleteById(id);
    }

    @Override
    public SysUserIdentity selectById(Long id)
    {
        return identityMapper.selectById(id);
    }

    /** GIT 标识归一：含 @ 则小写邮箱，否则 trim 名称。 */
    public static String normalizeGitIdentifier(String identifier)
    {
        if (StringUtils.isEmpty(identifier))
        {
            return null;
        }
        String trimmed = identifier.trim();
        if (trimmed.isEmpty())
        {
            return null;
        }
        if (trimmed.contains("@"))
        {
            return trimmed.toLowerCase();
        }
        return trimmed;
    }

    private SysUserIdentity resolveDuplicateAfterInsert(Long userId, String normalized)
    {
        SysUserIdentity raced = identityMapper.selectByTypeAndIdentifier(
            SysUserIdentity.TYPE_GIT_COMMIT, normalized);
        if (raced == null)
        {
            throw new ServiceException("该邮箱关联冲突，请稍后重试");
        }
        return resolveExistingOwnership(userId, raced);
    }

    private static SysUserIdentity resolveExistingOwnership(Long userId, SysUserIdentity existing)
    {
        if (userId.equals(existing.getUserId()))
        {
            return existing;
        }
        String nick = StringUtils.isNotEmpty(existing.getNickName())
            ? existing.getNickName() : existing.getUserName();
        throw new ServiceException("该邮箱已关联到用户 " + nick + "，如归属有误请联系管理员调整");
    }
}
