package com.acr.review.insight;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;

class InsightAuthorKeysTest
{
    @Test
    void prefersLowercasedEmail()
    {
        assertEquals("alice@example.com", InsightAuthorKeys.of("Alice@Example.com", "Alice"));
        assertEquals("bob", InsightAuthorKeys.of(null, "bob"));
        assertEquals("bob", InsightAuthorKeys.of("  ", " bob "));
        assertNull(InsightAuthorKeys.of(null, null));
    }

    @Test
    void weakMatchCountsUseExactAuthorKeyEquality()
    {
        // 口径：tasks_reviewed / issues_* 经 pr_author 与 author_key 字符串相等弱匹配
        assertEquals("alice", InsightAuthorKeys.of(null, "alice"));
        assertFalse("alice".equals(InsightAuthorKeys.of("alice@x.com", "alice")));
    }
}
