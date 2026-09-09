package com.acr.review.service;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;
import com.acr.common.core.domain.entity.SysUser;
import com.acr.common.utils.StringUtils;
import com.acr.review.domain.ReviewIssueConstants;
import com.acr.review.domain.ReviewPipelineConstants;
import com.acr.system.domain.SysUserIdentity;
import com.acr.system.service.ISysUserIdentityService;
import com.acr.system.service.ISysUserService;

/** 按提交作者 / PR 发起人 / 项目负责人解析问题责任人。 */
@Component
public class ReviewIssueAssigneeResolver
{
    private final ISysUserIdentityService identityService;
    private final ISysUserService userService;

    public ReviewIssueAssigneeResolver(ISysUserIdentityService identityService, ISysUserService userService)
    {
        this.identityService = identityService;
        this.userService = userService;
    }

    public Optional<ReviewIssueAssignment> resolve(String origin, String eventSource, String prAuthor,
                                                  Long ownerUserId, Collection<String> commitAuthorEmails)
    {
        if (ReviewIssueConstants.ORIGIN_EXISTING.equals(origin))
        {
            return matchPrAuthor(prAuthor);
        }
        Optional<ReviewIssueAssignment> byCommit = matchSingleCommitAuthor(eventSource, commitAuthorEmails);
        if (byCommit.isPresent())
        {
            return byCommit;
        }
        Optional<ReviewIssueAssignment> byPrAuthor = matchPrAuthor(prAuthor);
        if (byPrAuthor.isPresent())
        {
            return byPrAuthor;
        }
        return matchOwner(ownerUserId);
    }

    private Optional<ReviewIssueAssignment> matchSingleCommitAuthor(String eventSource,
                                                                   Collection<String> commitAuthorEmails)
    {
        if (!ReviewPipelineConstants.EVENT_SOURCE_PUSH.equals(eventSource))
        {
            return Optional.empty();
        }
        Set<String> emails = normalizeDistinct(commitAuthorEmails);
        if (emails.size() != 1)
        {
            return Optional.empty();
        }
        String email = emails.iterator().next();
        return matchIdentity(SysUserIdentity.TYPE_GIT_COMMIT, email).map(hit -> new ReviewIssueAssignment(
            hit.userId(),
            ReviewIssueConstants.ASSIGN_SOURCE_AUTO_COMMIT,
            "按提交邮箱 " + email + " 自动指派给" + hit.displayName()));
    }

    private Optional<ReviewIssueAssignment> matchPrAuthor(String prAuthor)
    {
        String identifier = normalizeIdentifier(prAuthor);
        if (identifier == null)
        {
            return Optional.empty();
        }
        Optional<IdentityHit> hit = matchIdentity(SysUserIdentity.TYPE_GIT_COMMIT, identifier);
        if (hit.isEmpty())
        {
            hit = matchIdentity(SysUserIdentity.TYPE_GIT_PLATFORM_USER, identifier);
        }
        return hit.map(h -> new ReviewIssueAssignment(
            h.userId(),
            ReviewIssueConstants.ASSIGN_SOURCE_AUTO_PR_AUTHOR,
            "按 PR 发起人 " + identifier + " 自动指派给" + h.displayName()));
    }

    private Optional<ReviewIssueAssignment> matchOwner(Long ownerUserId)
    {
        if (ownerUserId == null)
        {
            return Optional.empty();
        }
        return Optional.of(new ReviewIssueAssignment(
            ownerUserId,
            ReviewIssueConstants.ASSIGN_SOURCE_AUTO_OWNER,
            "PR 发起人匹配失败，按项目负责人兜底指派"));
    }

    private Optional<IdentityHit> matchIdentity(String identityType, String identifier)
    {
        SysUserIdentity row = identityService.selectByTypeAndIdentifier(identityType, identifier);
        if (row == null || row.getUserId() == null)
        {
            return Optional.empty();
        }
        return Optional.of(new IdentityHit(row.getUserId(), displayName(row)));
    }

    private String displayName(SysUserIdentity row)
    {
        if (StringUtils.isNotEmpty(row.getNickName()))
        {
            return row.getNickName();
        }
        if (StringUtils.isNotEmpty(row.getUserName()))
        {
            return row.getUserName();
        }
        SysUser user = userService.selectUserById(row.getUserId());
        if (user != null && StringUtils.isNotEmpty(user.getNickName()))
        {
            return user.getNickName();
        }
        if (user != null && StringUtils.isNotEmpty(user.getUserName()))
        {
            return user.getUserName();
        }
        return "用户" + row.getUserId();
    }

    private static Set<String> normalizeDistinct(Collection<String> values)
    {
        Set<String> result = new LinkedHashSet<>();
        if (values == null)
        {
            return result;
        }
        for (String value : values)
        {
            String normalized = normalizeIdentifier(value);
            if (normalized != null)
            {
                result.add(normalized);
            }
        }
        return result;
    }

    /** 设计 §4.2：trim + 小写后精确匹配。 */
    static String normalizeIdentifier(String identifier)
    {
        if (StringUtils.isEmpty(identifier))
        {
            return null;
        }
        String normalized = identifier.trim().toLowerCase();
        return normalized.isEmpty() ? null : normalized;
    }

    private record IdentityHit(Long userId, String displayName)
    {
    }
}
