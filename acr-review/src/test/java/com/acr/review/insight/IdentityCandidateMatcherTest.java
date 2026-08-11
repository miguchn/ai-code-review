package com.acr.review.insight;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class IdentityCandidateMatcherTest
{
    @Test
    void emailExactMatch_ignoresCase()
    {
        List<IdentityCandidateMatcher.Match> matches = IdentityCandidateMatcher.match(
            "Wang@Corp.com", "wangwei", "老王",
            List.of(
                new IdentityCandidateMatcher.CommitIdentity("wang@corp.com", "Wang Wei", "wang@corp.com"),
                new IdentityCandidateMatcher.CommitIdentity("other@corp.com", "Other", "other@corp.com")),
            Set.of());
        assertEquals(1, matches.size());
        assertEquals(IdentityCandidateMatcher.MATCH_EMAIL, matches.get(0).matchType);
        assertEquals("wang@corp.com", matches.get(0).authorKey);
    }

    @Test
    void nameMatch_usesUserNameOrNickName()
    {
        List<IdentityCandidateMatcher.Match> matches = IdentityCandidateMatcher.match(
            null, "zhangwei", "张伟",
            List.of(
                new IdentityCandidateMatcher.CommitIdentity(null, "张伟", "张伟"),
                new IdentityCandidateMatcher.CommitIdentity(null, "zhangwei", "zhangwei"),
                new IdentityCandidateMatcher.CommitIdentity(null, "李四", "李四")),
            Set.of());
        assertEquals(2, matches.size());
        assertTrue(matches.stream().allMatch(m -> IdentityCandidateMatcher.MATCH_NAME.equals(m.matchType)));
    }

    @Test
    void noEmailUser_hasNoEmailCandidates()
    {
        List<IdentityCandidateMatcher.Match> matches = IdentityCandidateMatcher.match(
            "  ", "alice", "Alice",
            List.of(new IdentityCandidateMatcher.CommitIdentity("alice@x.com", "Alice", "alice@x.com")),
            Set.of());
        // 无平台邮箱：不走 EMAIL；名称 Alice == nick 可命中 NAME
        assertEquals(1, matches.size());
        assertEquals(IdentityCandidateMatcher.MATCH_NAME, matches.get(0).matchType);
    }

    @Test
    void excludesAlreadyBoundKeys()
    {
        List<IdentityCandidateMatcher.Match> matches = IdentityCandidateMatcher.match(
            "a@x.com", "a", "A",
            List.of(new IdentityCandidateMatcher.CommitIdentity("a@x.com", "A", "a@x.com")),
            Set.of("a@x.com"));
        assertTrue(matches.isEmpty());
    }
}
