package com.acr.review.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.acr.review.domain.ReviewIssueConstants;
import com.acr.review.domain.ReviewPipelineConstants;
import com.acr.system.domain.SysUserIdentity;
import com.acr.system.service.ISysUserIdentityService;
import com.acr.system.service.ISysUserService;

@ExtendWith(MockitoExtension.class)
class ReviewIssueAssigneeResolverTest
{
    @Mock
    private ISysUserIdentityService identityService;
    @Mock
    private ISysUserService userService;

    private ReviewIssueAssigneeResolver resolver;

    @BeforeEach
    void setUp()
    {
        resolver = new ReviewIssueAssigneeResolver(identityService, userService);
    }

    @Test
    void newPush_singleCommitAuthorHit_assignsAutoCommit()
    {
        when(identityService.selectByTypeAndIdentifier(SysUserIdentity.TYPE_GIT_COMMIT, "dev@corp.cn"))
            .thenReturn(identity(11L, "张三"));

        Optional<ReviewIssueAssignment> result = resolver.resolve(
            ReviewIssueConstants.ORIGIN_NEW, ReviewPipelineConstants.EVENT_SOURCE_PUSH,
            "octocat", 99L, Set.of(" Dev@Corp.CN "));

        assertTrue(result.isPresent());
        assertEquals(11L, result.get().getUserId());
        assertEquals(ReviewIssueConstants.ASSIGN_SOURCE_AUTO_COMMIT, result.get().getSource());
        assertEquals("按提交邮箱 dev@corp.cn 自动指派给张三", result.get().getNote());
        verify(identityService, never()).selectByTypeAndIdentifier(eq(SysUserIdentity.TYPE_GIT_COMMIT), eq("octocat"));
    }

    @Test
    void newPush_multipleAuthors_skipsCommitAndUsesPrAuthor()
    {
        when(identityService.selectByTypeAndIdentifier(SysUserIdentity.TYPE_GIT_COMMIT, "octocat"))
            .thenReturn(identity(22L, "李四"));

        Optional<ReviewIssueAssignment> result = resolver.resolve(
            ReviewIssueConstants.ORIGIN_NEW, ReviewPipelineConstants.EVENT_SOURCE_PUSH,
            "Octocat", 99L, Set.of("a@corp.cn", "b@corp.cn"));

        assertTrue(result.isPresent());
        assertEquals(22L, result.get().getUserId());
        assertEquals(ReviewIssueConstants.ASSIGN_SOURCE_AUTO_PR_AUTHOR, result.get().getSource());
        assertEquals("按 PR 发起人 octocat 自动指派给李四", result.get().getNote());
    }

    @Test
    void newIssue_prAuthorHit_assignsAutoPrAuthor()
    {
        when(identityService.selectByTypeAndIdentifier(SysUserIdentity.TYPE_GIT_COMMIT, "octocat"))
            .thenReturn(identity(22L, "李四"));

        Optional<ReviewIssueAssignment> result = resolver.resolve(
            ReviewIssueConstants.ORIGIN_NEW, ReviewPipelineConstants.EVENT_SOURCE_PR,
            "octocat", 99L, Set.of("dev@corp.cn"));

        assertTrue(result.isPresent());
        assertEquals(ReviewIssueConstants.ASSIGN_SOURCE_AUTO_PR_AUTHOR, result.get().getSource());
        assertEquals(22L, result.get().getUserId());
    }

    @Test
    void newIssue_identityMiss_fallsBackToOwner()
    {
        when(identityService.selectByTypeAndIdentifier(any(), any())).thenReturn(null);

        Optional<ReviewIssueAssignment> result = resolver.resolve(
            ReviewIssueConstants.ORIGIN_NEW, ReviewPipelineConstants.EVENT_SOURCE_PR,
            "ghost", 99L, List.of());

        assertTrue(result.isPresent());
        assertEquals(99L, result.get().getUserId());
        assertEquals(ReviewIssueConstants.ASSIGN_SOURCE_AUTO_OWNER, result.get().getSource());
        assertEquals("PR 发起人匹配失败，按项目负责人兜底指派", result.get().getNote());
    }

    @Test
    void existing_prAuthorHit_assigns()
    {
        when(identityService.selectByTypeAndIdentifier(SysUserIdentity.TYPE_GIT_COMMIT, "octocat"))
            .thenReturn(identity(22L, "李四"));

        Optional<ReviewIssueAssignment> result = resolver.resolve(
            ReviewIssueConstants.ORIGIN_EXISTING, ReviewPipelineConstants.EVENT_SOURCE_PR,
            "octocat", 99L, Set.of("dev@corp.cn"));

        assertTrue(result.isPresent());
        assertEquals(ReviewIssueConstants.ASSIGN_SOURCE_AUTO_PR_AUTHOR, result.get().getSource());
        verify(userService, never()).selectUserById(any());
    }

    @Test
    void existing_prAuthorMiss_doesNotFallbackToOwner()
    {
        when(identityService.selectByTypeAndIdentifier(any(), any())).thenReturn(null);

        Optional<ReviewIssueAssignment> result = resolver.resolve(
            ReviewIssueConstants.ORIGIN_EXISTING, ReviewPipelineConstants.EVENT_SOURCE_PR,
            "ghost", 99L, Set.of("dev@corp.cn"));

        assertTrue(result.isEmpty());
        verify(userService, never()).selectUserById(any());
    }

    @Test
    void newPush_singleAuthorIdentityMiss_fallsThroughToPrAuthorThenOwner()
    {
        when(identityService.selectByTypeAndIdentifier(SysUserIdentity.TYPE_GIT_COMMIT, "nobody@corp.cn"))
            .thenReturn(null);
        when(identityService.selectByTypeAndIdentifier(SysUserIdentity.TYPE_GIT_COMMIT, "ghost"))
            .thenReturn(null);

        Optional<ReviewIssueAssignment> result = resolver.resolve(
            ReviewIssueConstants.ORIGIN_NEW, ReviewPipelineConstants.EVENT_SOURCE_PUSH,
            "ghost", 7L, Set.of("nobody@corp.cn"));

        assertTrue(result.isPresent());
        assertEquals(ReviewIssueConstants.ASSIGN_SOURCE_AUTO_OWNER, result.get().getSource());
        assertEquals(7L, result.get().getUserId());
    }

    @Test
    void newIssue_ownerMissing_returnsEmpty()
    {
        when(identityService.selectByTypeAndIdentifier(any(), any())).thenReturn(null);

        Optional<ReviewIssueAssignment> result = resolver.resolve(
            ReviewIssueConstants.ORIGIN_NEW, ReviewPipelineConstants.EVENT_SOURCE_PR,
            "ghost", null, List.of());

        assertTrue(result.isEmpty());
    }

    private static SysUserIdentity identity(Long userId, String nickName)
    {
        SysUserIdentity row = new SysUserIdentity();
        row.setUserId(userId);
        row.setNickName(nickName);
        return row;
    }

}
