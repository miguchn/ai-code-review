package com.acr.system.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import com.acr.common.constant.CacheConstants;
import com.acr.common.core.domain.entity.SysUser;
import com.acr.common.core.redis.RedisCache;
import com.acr.system.mapper.*;
import com.acr.system.service.ISysUserService;

class SysUserSessionRevocationTest
{
    private final SysUserMapper mapper = mock(SysUserMapper.class);
    private final RedisCache redis = mock(RedisCache.class);
    private final SysUserServiceImpl service = spy(new SysUserServiceImpl());

    @BeforeEach
    void setUp()
    {
        ReflectionTestUtils.setField(service, "userMapper", mapper);
        ReflectionTestUtils.setField(service, "redisCache", redis);
        ReflectionTestUtils.setField(service, "userRoleMapper", mock(SysUserRoleMapper.class));
        ReflectionTestUtils.setField(service, "userPostMapper", mock(SysUserPostMapper.class));
        ReflectionTestUtils.setField(service, "roleMapper", mock(SysRoleMapper.class));
        doNothing().when(service).checkUserAllowed(any());
        doNothing().when(service).checkUserDataScope(anyLong());
    }

    @Test
    void disableAndEnableBothReplaceSessionVersion()
    {
        when(mapper.updateUserStatus(7L, "1")).thenReturn(1);
        when(mapper.updateUserStatus(7L, "0")).thenReturn(1);
        SysUser user = new SysUser(7L);
        user.setStatus("1");
        assertEquals(1, service.updateUserStatus(user));
        user.setStatus("0");
        assertEquals(1, service.updateUserStatus(user));
        verify(redis, times(2)).setCacheObject(eq(versionKey(7L)), anyString());
        verify(redis, never()).keys(anyString());
    }

    @Test
    void bothPasswordResetPathsReplaceSessionVersion()
    {
        when(mapper.resetUserPwd(7L, "new-hash")).thenReturn(1);
        SysUser user = new SysUser(7L);
        user.setPassword("new-hash");
        service.resetPwd(user);
        service.resetUserPwd(7L, "new-hash");
        verify(redis, times(2)).setCacheObject(eq(versionKey(7L)), anyString());
    }

    @Test
    void singleAndBatchDeletionInvalidateEveryRequestedAccount()
    {
        when(mapper.deleteUserById(7L)).thenReturn(1);
        Long[] ids = {8L, 9L};
        when(mapper.deleteUserByIds(ids)).thenReturn(2);
        service.deleteUserById(7L);
        service.deleteUserByIds(ids);
        for (Long id : List.of(7L, 8L, 9L))
        {
            verify(redis).setCacheObject(eq(versionKey(id)), anyString());
        }
    }

    @Test
    void failedDatabaseUpdateDoesNotRevokeSessions()
    {
        SysUser user = new SysUser(7L);
        user.setStatus("1");
        assertEquals(0, service.updateUserStatus(user));
        when(mapper.resetUserPwd(7L, "new-hash")).thenThrow(new IllegalStateException("database failed"));
        assertThrows(IllegalStateException.class, () -> service.resetUserPwd(7L, "new-hash"));
        verifyNoInteractions(redis);
    }

    @Test
    void failedBatchPermissionCheckDoesNotChangeDataOrSessionVersions()
    {
        doThrow(new IllegalArgumentException("outside scope")).when(service).checkUserDataScope(9L);
        assertThrows(IllegalArgumentException.class, () -> service.deleteUserByIds(new Long[] {8L, 9L}));
        verify(mapper, never()).deleteUserByIds(any());
        verifyNoInteractions(redis);
    }

    @Test
    void editorStatusChangeRevokesSessionsButOrdinaryProfileChangesDoNot()
    {
        SysUser before = new SysUser(7L);
        before.setStatus("0");
        when(mapper.selectUserById(7L)).thenReturn(before);
        when(mapper.updateUser(any())).thenReturn(1);
        SysUser update = new SysUser(7L);
        update.setStatus("1");
        service.updateUser(update);
        verify(redis).setCacheObject(eq(versionKey(7L)), anyString());
        clearInvocations(redis);
        update.setStatus("0");
        update.setNickName("new name");
        service.updateUser(update);
        service.updateUserProfile(update);
        verifyNoInteractions(redis);
    }

    @Test
    void redisFailureRollsBackAccountSecurityTransaction()
    {
        when(mapper.resetUserPwd(7L, "new-hash")).thenReturn(1);
        doThrow(new IllegalStateException("redis failed"))
            .when(redis).setCacheObject(eq(versionKey(7L)), anyString());
        RecordingTransactionManager tx = new RecordingTransactionManager();
        ProxyFactory factory = new ProxyFactory(service);
        factory.addAdvice(new TransactionInterceptor(tx, new AnnotationTransactionAttributeSource()));
        ISysUserService proxy = (ISysUserService) factory.getProxy();
        assertThrows(IllegalStateException.class, () -> proxy.resetUserPwd(7L, "new-hash"));
        assertEquals(1, tx.rollbacks);
        assertEquals(0, tx.commits);
    }

    private static String versionKey(Long userId)
    {
        return CacheConstants.LOGIN_SESSION_VERSION_KEY + userId;
    }

    private static class RecordingTransactionManager extends AbstractPlatformTransactionManager
    {
        int rollbacks;
        int commits;
        @Override protected Object doGetTransaction() { return new Object(); }
        @Override protected void doBegin(Object tx, TransactionDefinition definition) { }
        @Override protected void doCommit(DefaultTransactionStatus status) { commits++; }
        @Override protected void doRollback(DefaultTransactionStatus status) { rollbacks++; }
    }
}
