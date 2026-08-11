package com.acr.system.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import com.acr.common.exception.ServiceException;
import com.acr.system.domain.SysUserIdentity;
import com.acr.system.mapper.SysUserIdentityMapper;
import com.acr.system.service.ISysUserService;

@ExtendWith(MockitoExtension.class)
class SysUserIdentityServiceImplTest
{
    @Mock
    private SysUserIdentityMapper identityMapper;
    @Mock
    private ISysUserService userService;

    private SysUserIdentityServiceImpl service;

    @BeforeEach
    void setUp()
    {
        service = new SysUserIdentityServiceImpl(identityMapper, userService);
    }

    @Test
    void addMineGit_conflictMessage_isHumanReadable()
    {
        SysUserIdentity existing = new SysUserIdentity();
        existing.setUserId(2L);
        existing.setNickName("李姐");
        when(identityMapper.selectByTypeAndIdentifier(eq(SysUserIdentity.TYPE_GIT_COMMIT), eq("a@x.com")))
            .thenReturn(existing);

        ServiceException ex = assertThrows(ServiceException.class,
            () -> service.addMineGit(1L, "A@X.com", null, "u1", SysUserIdentity.ORIGIN_SELF));
        assertEquals("该邮箱已关联到用户 李姐，如归属有误请联系管理员调整", ex.getMessage());
        verify(identityMapper, never()).insert(any());
    }

    @Test
    void bindAdmin_reassigns_deletesThenInserts()
    {
        SysUserIdentity existing = new SysUserIdentity();
        existing.setId(9L);
        existing.setUserId(2L);
        when(identityMapper.selectByTypeAndIdentifier(eq(SysUserIdentity.TYPE_GIT_COMMIT), eq("b@x.com")))
            .thenReturn(existing);

        boolean reassigned = service.bindAdmin(3L, "b@x.com", "Bob", "admin");
        assertTrue(reassigned);
        verify(userService).checkUserDataScope(3L);
        verify(userService).checkUserDataScope(2L);
        verify(identityMapper).deleteById(9L);
        ArgumentCaptor<SysUserIdentity> captor = ArgumentCaptor.forClass(SysUserIdentity.class);
        verify(identityMapper).insert(captor.capture());
        assertEquals(3L, captor.getValue().getUserId());
        assertEquals(SysUserIdentity.ORIGIN_ADMIN, captor.getValue().getOrigin());
    }

    @Test
    void bindAdmin_sameUser_noOp()
    {
        SysUserIdentity existing = new SysUserIdentity();
        existing.setId(9L);
        existing.setUserId(3L);
        when(identityMapper.selectByTypeAndIdentifier(eq(SysUserIdentity.TYPE_GIT_COMMIT), eq("b@x.com")))
            .thenReturn(existing);

        assertFalse(service.bindAdmin(3L, "b@x.com", null, "admin"));
        verify(identityMapper, never()).deleteById(any());
        verify(identityMapper, never()).insert(any());
    }

    @Test
    void normalizeGitIdentifier_lowercasesEmail()
    {
        assertEquals("a@x.com", SysUserIdentityServiceImpl.normalizeGitIdentifier(" A@X.COM "));
        assertEquals("ZhangWei", SysUserIdentityServiceImpl.normalizeGitIdentifier(" ZhangWei "));
    }

    @Test
    void addMineGit_duplicateKey_otherUser_throwsHumanMessage()
    {
        when(identityMapper.selectByTypeAndIdentifier(eq(SysUserIdentity.TYPE_GIT_COMMIT), eq("c@x.com")))
            .thenReturn(null)
            .thenReturn(ownedBy(2L, "李姐"));
        when(identityMapper.insert(any())).thenThrow(new DuplicateKeyException("uk_identity"));

        ServiceException ex = assertThrows(ServiceException.class,
            () -> service.addMineGit(1L, "c@x.com", null, "u1", SysUserIdentity.ORIGIN_SELF));
        assertEquals("该邮箱已关联到用户 李姐，如归属有误请联系管理员调整", ex.getMessage());
        verify(identityMapper, times(2)).selectByTypeAndIdentifier(eq(SysUserIdentity.TYPE_GIT_COMMIT), eq("c@x.com"));
    }

    @Test
    void addMineGit_duplicateKey_sameUser_returnsExisting()
    {
        SysUserIdentity mine = ownedBy(1L, "老王");
        when(identityMapper.selectByTypeAndIdentifier(eq(SysUserIdentity.TYPE_GIT_COMMIT), eq("c@x.com")))
            .thenReturn(null)
            .thenReturn(mine);
        when(identityMapper.insert(any())).thenThrow(new DuplicateKeyException("uk_identity"));

        SysUserIdentity result = service.addMineGit(1L, "c@x.com", null, "u1", SysUserIdentity.ORIGIN_SELF);
        assertEquals(mine, result);
    }

    private static SysUserIdentity ownedBy(Long userId, String nick)
    {
        SysUserIdentity row = new SysUserIdentity();
        row.setId(99L);
        row.setUserId(userId);
        row.setIdentifier("c@x.com");
        row.setNickName(nick);
        return row;
    }
}
