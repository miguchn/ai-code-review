package com.acr.review.insight;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import com.acr.common.utils.StringUtils;

/**
 * 身份候选匹配（只产建议不产绑定）。
 * 精确：author_email = user.email（忽略大小写）；
 * 名称：author_name = user_name / nick_name。
 */
public final class IdentityCandidateMatcher
{
    public static final String MATCH_EMAIL = "EMAIL";
    public static final String MATCH_NAME = "NAME";

    private IdentityCandidateMatcher()
    {
    }

    public static final class CommitIdentity
    {
        public final String authorEmail;
        public final String authorName;
        public final String authorKey;

        public CommitIdentity(String authorEmail, String authorName, String authorKey)
        {
            this.authorEmail = authorEmail;
            this.authorName = authorName;
            this.authorKey = authorKey;
        }
    }

    public static final class Match
    {
        public final String authorKey;
        public final String authorEmail;
        public final String authorName;
        public final String matchType;

        public Match(String authorKey, String authorEmail, String authorName, String matchType)
        {
            this.authorKey = authorKey;
            this.authorEmail = authorEmail;
            this.authorName = authorName;
            this.matchType = matchType;
        }
    }

    /**
     * @param userEmail 平台用户邮箱；空则不产生 EMAIL 候选
     * @param userName  登录名
     * @param nickName  昵称
     * @param excludeKeys 已关联到本人的 identifier，排除
     */
    public static List<Match> match(String userEmail, String userName, String nickName,
                                    List<CommitIdentity> commits, Set<String> excludeKeys)
    {
        Map<String, Match> byKey = new LinkedHashMap<>();
        String email = blankToNull(userEmail);
        if (email != null)
        {
            email = email.toLowerCase(Locale.ROOT);
        }
        String name = blankToNull(userName);
        String nick = blankToNull(nickName);
        Set<String> exclude = excludeKeys == null ? Set.of() : excludeKeys;

        for (CommitIdentity c : commits)
        {
            if (c == null || StringUtils.isEmpty(c.authorKey) || exclude.contains(c.authorKey))
            {
                continue;
            }
            if (email != null && StringUtils.isNotEmpty(c.authorEmail)
                && email.equals(c.authorEmail.trim().toLowerCase(Locale.ROOT)))
            {
                byKey.putIfAbsent(c.authorKey, new Match(c.authorKey, c.authorEmail, c.authorName, MATCH_EMAIL));
                continue;
            }
            if (StringUtils.isNotEmpty(c.authorName))
            {
                String an = c.authorName.trim();
                if ((name != null && name.equals(an)) || (nick != null && nick.equals(an)))
                {
                    byKey.putIfAbsent(c.authorKey, new Match(c.authorKey, c.authorEmail, c.authorName, MATCH_NAME));
                }
            }
        }
        return new ArrayList<>(byKey.values());
    }

    private static String blankToNull(String value)
    {
        if (StringUtils.isEmpty(value))
        {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
