package com.acr.review.insight;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.acr.common.core.domain.entity.SysUser;
import com.acr.review.insight.dto.InsightUserOption;
import com.acr.review.mapper.ReviewCommitFactMapper;
import com.acr.review.mapper.ReviewMemberStatsDailyMapper;
import com.acr.review.mapper.ReviewStatsDailyMapper;
import com.acr.review.mapper.ReviewStatsSourceMapper;
import com.acr.system.service.ISysConfigService;
import com.acr.system.service.ISysUserIdentityService;
import com.acr.system.service.ISysUserService;

@ExtendWith(MockitoExtension.class)
class InsightIdentityUserOptionsTest
{
    @Mock
    private InsightScopeQueries scopeQueries;
    @Mock
    private ReviewStatsDailyMapper dailyMapper;
    @Mock
    private ReviewStatsSourceMapper sourceMapper;
    @Mock
    private ReviewCommitFactMapper commitFactMapper;
    @Mock
    private ReviewMemberStatsDailyMapper memberStatsMapper;
    @Mock
    private ISysUserIdentityService userIdentityService;
    @Mock
    private ISysUserService userService;
    @Mock
    private ISysConfigService configService;

    private ReviewInsightServiceImpl service;

    @BeforeEach
    void setUp()
    {
        service = new ReviewInsightServiceImpl(scopeQueries, dailyMapper, sourceMapper, commitFactMapper,
            memberStatsMapper, userIdentityService, userService, configService);
    }

    @Test
    void listIdentityUserOptions_passesKeywordToUserNameFilter()
    {
        when(userService.selectUserList(any())).thenReturn(List.of());

        service.listIdentityUserOptions("wang");

        ArgumentCaptor<SysUser> captor = ArgumentCaptor.forClass(SysUser.class);
        verify(userService).selectUserList(captor.capture());
        assertEquals("wang", captor.getValue().getUserName());
    }

    @Test
    void listIdentityUserOptions_returnsOnlyWhitelistFields()
    {
        SysUser raw = new SysUser();
        raw.setUserId(7L);
        raw.setUserName("wangwei");
        raw.setNickName("老王");
        raw.setEmail("secret@corp.com");
        raw.setPhonenumber("13800000000");
        when(userService.selectUserList(any())).thenReturn(List.of(raw));

        List<InsightUserOption> options = service.listIdentityUserOptions(null);
        assertEquals(1, options.size());
        InsightUserOption opt = options.get(0);
        assertEquals(7L, opt.getUserId());
        assertEquals("wangwei", opt.getUserName());
        assertEquals("老王", opt.getNickName());
        // 字段白名单：DTO 不得暴露邮箱/手机号
        assertNull(methodOrNull(opt, "getEmail"));
        assertNull(methodOrNull(opt, "getPhonenumber"));
    }

    @Test
    void listIdentityUserOptions_capsAt20()
    {
        List<SysUser> many = new ArrayList<>();
        for (int i = 0; i < 25; i++)
        {
            SysUser u = new SysUser();
            u.setUserId((long) i);
            u.setUserName("u" + i);
            u.setNickName("n" + i);
            many.add(u);
        }
        when(userService.selectUserList(any())).thenReturn(many);
        assertEquals(20, service.listIdentityUserOptions("u").size());
    }

    private static Object methodOrNull(Object target, String method)
    {
        try
        {
            return target.getClass().getMethod(method);
        }
        catch (NoSuchMethodException e)
        {
            return null;
        }
    }
}
