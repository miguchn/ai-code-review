package com.acr.framework.web.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import com.acr.common.constant.CacheConstants;
import com.acr.common.core.domain.entity.SysUser;
import com.acr.common.core.domain.model.LoginUser;
import com.acr.common.core.redis.RedisCache;
import com.acr.common.exception.ServiceException;
import com.acr.system.service.ISysUserService;
import com.acr.framework.config.FastJson2JsonRedisSerializer;

class TokenServiceSessionTest
{
    private final RedisCache redis = mock(RedisCache.class);
    private final ISysUserService users = mock(ISysUserService.class);
    private final Map<String, Object> cache = new HashMap<>();
    private final TokenService tokens = spy(new TokenService());
    private SysUser current;
    private LoginUser session;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp()
    {
        ReflectionTestUtils.setField(tokens, "redisCache", redis);
        ReflectionTestUtils.setField(tokens, "userService", users);
        ReflectionTestUtils.setField(tokens, "header", "Authorization");
        ReflectionTestUtils.setField(tokens, "expireTime", 30);
        ReflectionTestUtils.setField(tokens, "secret", Base64.getEncoder().encodeToString(new byte[64]));
        doNothing().when(tokens).setUserAgent(any());
        when(redis.getCacheObject(anyString())).thenAnswer(a -> cache.get(a.getArgument(0)));
        doAnswer(a -> { cache.put(a.getArgument(0), a.getArgument(1)); return null; })
            .when(redis).setCacheObject(anyString(), any(), anyInt(), any());
        when(redis.deleteObject(anyString())).thenAnswer(a -> cache.remove(a.getArgument(0)) != null);
        current = user(7L, "hash-before");
        when(users.selectUserById(7L)).thenAnswer(a -> current);
        session = new LoginUser(7L, 3L, user(7L, "hash-before"), Set.of("review:task:list"));
        request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + tokens.createToken(session));
    }

    @Test
    void redisSerializerRoundTripHidesPasswordAndPreservesSessionVersion()
    {
        cache.put(CacheConstants.LOGIN_SESSION_VERSION_KEY + 7L, "current-version");
        LoginUser fresh = new LoginUser(7L, 3L, user(7L, "hash-before"), Set.of());
        MockHttpServletRequest freshRequest = new MockHttpServletRequest();
        freshRequest.addHeader("Authorization", "Bearer " + tokens.createToken(fresh));
        FastJson2JsonRedisSerializer<Object> serializer = new FastJson2JsonRedisSerializer<>(Object.class);
        LoginUser restored = (LoginUser) serializer.deserialize(serializer.serialize(fresh));
        assertNull(restored.getUser().getPassword(), "Redis 会话不应保存密码哈希");
        assertEquals("current-version", restored.getSessionVersion());
        cache.put(CacheConstants.LOGIN_TOKEN_KEY + restored.getToken(), restored);
        assertSame(restored, tokens.getLoginUser(freshRequest));
    }

    @Test
    void unchangedAccountKeepsItsSessionWithoutScanningKeys()
    {
        assertSame(session, tokens.getLoginUser(request));
        verify(redis, never()).keys(anyString());
    }

    @Test
    void disabledOrDeletedAccountCannotUseCachedIdentity()
    {
        current.setStatus("1");
        assertNull(tokens.getLoginUser(request));
        assertFalse(cache.containsKey(CacheConstants.LOGIN_TOKEN_KEY + session.getToken()));
        cache.put(CacheConstants.LOGIN_TOKEN_KEY + session.getToken(), session);
        current.setStatus("0");
        current.setDelFlag("2");
        assertNull(tokens.getLoginUser(request));
    }

    @Test
    void removedOrRecreatedAccountCannotUseOldPasswordSnapshot()
    {
        current = null;
        assertNull(tokens.getLoginUser(request));
        cache.put(CacheConstants.LOGIN_TOKEN_KEY + session.getToken(), session);
        current = user(7L, "new-account-hash");
        cache.put(CacheConstants.LOGIN_SESSION_VERSION_KEY + 7L, "after-recreation");
        assertNull(tokens.getLoginUser(request));
    }

    @Test
    void resetPasswordInvalidatesOldSessionThroughAccountVersion()
    {
        current.setPassword("hash-after");
        cache.put(CacheConstants.LOGIN_SESSION_VERSION_KEY + 7L, "after-reset");
        assertNull(tokens.getLoginUser(request));
    }

    @Test
    void reenabledAccountAndRecreatedAccountWithSamePasswordCannotReviveOldSession()
    {
        cache.put(CacheConstants.LOGIN_SESSION_VERSION_KEY + 7L, "after-security-change");
        assertNull(tokens.getLoginUser(request));
        // An in-flight refresh can rewrite the old cache entry, but cannot update its generation.
        tokens.refreshToken(session);
        assertNull(tokens.getLoginUser(request));
        current = user(7L, "hash-before");
        tokens.refreshToken(session);
        assertNull(tokens.getLoginUser(request));
    }

    @Test
    void freshLoginAfterResetCapturesCurrentVersion()
    {
        current.setPassword("hash-after");
        cache.put(CacheConstants.LOGIN_SESSION_VERSION_KEY + 7L, "current-version");
        LoginUser fresh = new LoginUser(7L, 3L, user(7L, "hash-after"), Set.of());
        MockHttpServletRequest freshRequest = new MockHttpServletRequest();
        freshRequest.addHeader("Authorization", "Bearer " + tokens.createToken(fresh));
        assertSame(fresh, tokens.getLoginUser(freshRequest));
        assertEquals("current-version", fresh.getSessionVersion());
        assertNull(tokens.getLoginUser(request));
    }

    @Test
    void passwordCacheRefreshDoesNotMakeExistingTokenValidAgain()
    {
        current.setPassword("hash-after");
        cache.put(CacheConstants.LOGIN_SESSION_VERSION_KEY + 7L, "current-version");
        session.getUser().setPassword("hash-after");
        tokens.setLoginUser(session);
        assertNull(tokens.getLoginUser(request));
    }

    @Test
    void unavailableDatabaseOrRedisRejectsCachedAuthentication()
    {
        when(users.selectUserById(7L)).thenThrow(new IllegalStateException("database unavailable"));
        assertNull(tokens.getLoginUser(request));
        doReturn(current).when(users).selectUserById(7L);
        when(redis.getCacheObject(CacheConstants.LOGIN_SESSION_VERSION_KEY + 7L))
            .thenThrow(new IllegalStateException("redis unavailable"));
        assertNull(tokens.getLoginUser(request));
    }

    @Test
    void accountChangedDuringLoginCannotReceiveNewToken()
    {
        current.setPassword("hash-after");
        assertThrows(ServiceException.class, () -> tokens.createToken(session));
    }

    private static SysUser user(Long id, String password)
    {
        SysUser user = new SysUser(id);
        user.setUserName("operator");
        user.setStatus("0");
        user.setDelFlag("0");
        user.setPassword(password);
        return user;
    }
}
