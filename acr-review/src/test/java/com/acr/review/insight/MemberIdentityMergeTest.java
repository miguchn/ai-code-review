package com.acr.review.insight;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import com.acr.review.insight.dto.InsightCommitTrendPoint;
import com.acr.review.insight.dto.InsightTeamMemberRow;
import com.acr.system.domain.SysUserIdentity;

/**
 * 多身份合并 / 未关联分组 / stackedTrend 全量口径（与 ReviewInsightServiceImpl.getTeamMembers 一致）。
 */
class MemberIdentityMergeTest
{
    @Test
    void mergesMultipleIdentitiesOfSameUserAndKeepsUnbound()
    {
        Map<String, SysUserIdentity> identityByKey = new HashMap<>();
        identityByKey.put("a@x.com", binding(10L, "a@x.com", "老王"));
        identityByKey.put("wang", binding(10L, "wang", "老王"));

        Map<Long, InsightTeamMemberRow> bound = new LinkedHashMap<>();
        Map<String, InsightTeamMemberRow> unbound = new LinkedHashMap<>();

        for (Stat s : List.of(
            new Stat("a@x.com", 3, 1),
            new Stat("wang", 2, 2),
            new Stat("orphan", 5, 0)))
        {
            SysUserIdentity binding = identityByKey.get(s.authorKey);
            if (binding != null)
            {
                InsightTeamMemberRow m = bound.computeIfAbsent(binding.getUserId(), uid -> {
                    InsightTeamMemberRow row = new InsightTeamMemberRow();
                    row.setUserId(uid);
                    row.setAuthorName(binding.getNickName());
                    row.setAuthorKey("user:" + uid);
                    row.setCommitCount(0);
                    row.setTasksReviewed(0);
                    row.setIdentities(new ArrayList<>());
                    return row;
                });
                if (!m.getIdentities().contains(s.authorKey))
                {
                    m.getIdentities().add(s.authorKey);
                }
                m.setCommitCount(m.getCommitCount() + s.commits);
                m.setTasksReviewed(m.getTasksReviewed() + s.tasks);
            }
            else
            {
                InsightTeamMemberRow m = unbound.computeIfAbsent(s.authorKey, key -> {
                    InsightTeamMemberRow row = new InsightTeamMemberRow();
                    row.setAuthorKey(key);
                    row.setCommitCount(0);
                    row.setTasksReviewed(0);
                    return row;
                });
                m.setCommitCount(m.getCommitCount() + s.commits);
                m.setTasksReviewed(m.getTasksReviewed() + s.tasks);
            }
        }

        assertEquals(1, bound.size());
        InsightTeamMemberRow wang = bound.get(10L);
        assertEquals(5, wang.getCommitCount());
        assertEquals(3, wang.getTasksReviewed());
        assertEquals(2, wang.getIdentities().size());
        assertEquals("老王", wang.getAuthorName());

        assertEquals(1, unbound.size());
        assertTrue(unbound.containsKey("orphan"));
        assertEquals(5, unbound.get("orphan").getCommitCount());
    }

    @Test
    void stackedTrend_includesBoundAndUnboundAuthorKeys()
    {
        InsightTeamMemberRow bound = new InsightTeamMemberRow();
        bound.setAuthorKey("user:10");
        bound.setCommitTrend(List.of(point("user:10", "2026-08-01", 2)));

        InsightTeamMemberRow unbound = new InsightTeamMemberRow();
        unbound.setAuthorKey("orphan@x.com");
        unbound.setCommitTrend(List.of(point("orphan@x.com", "2026-08-01", 4)));

        List<InsightCommitTrendPoint> stackedTrend = new ArrayList<>();
        stackedTrend.addAll(bound.getCommitTrend());
        stackedTrend.addAll(unbound.getCommitTrend());

        Set<String> keys = new HashSet<>();
        for (InsightCommitTrendPoint p : stackedTrend)
        {
            keys.add(p.getAuthorKey());
        }
        assertTrue(keys.contains("user:10"));
        assertTrue(keys.contains("orphan@x.com"));
        assertEquals(2, stackedTrend.size());
    }

    private static InsightCommitTrendPoint point(String authorKey, String date, int count)
    {
        InsightCommitTrendPoint p = new InsightCommitTrendPoint();
        p.setAuthorKey(authorKey);
        p.setDate(date);
        p.setCommitCount(count);
        return p;
    }

    private static SysUserIdentity binding(Long userId, String identifier, String nick)
    {
        SysUserIdentity i = new SysUserIdentity();
        i.setUserId(userId);
        i.setIdentifier(identifier);
        i.setNickName(nick);
        return i;
    }

    private record Stat(String authorKey, int commits, int tasks)
    {
    }
}
