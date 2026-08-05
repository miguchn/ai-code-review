package com.acr.review.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import org.junit.jupiter.api.Test;
import com.acr.review.domain.result.ReviewTopIssue;

class ReviewIssueFingerprintTest
{
    @Test
    void sameCategoryPathTitleProduceSameFingerprint()
    {
        ReviewTopIssue a = issue("SEC", "a/B.java", "  Password  Leak ");
        ReviewTopIssue b = issue("SEC", "a/B.java", "password leak");
        assertEquals(ReviewIssueFingerprint.of(a), ReviewIssueFingerprint.of(b));
    }

    @Test
    void lineNumbersDoNotAffectFingerprint()
    {
        ReviewTopIssue a = issue("SEC", "a/B.java", "x");
        a.setStartLine(10);
        ReviewTopIssue b = issue("SEC", "a/B.java", "x");
        b.setStartLine(99);
        assertEquals(ReviewIssueFingerprint.of(a), ReviewIssueFingerprint.of(b));
    }

    @Test
    void differentTitleProducesDifferentFingerprint()
    {
        assertNotEquals(
            ReviewIssueFingerprint.of("SEC", "a.java", "one"),
            ReviewIssueFingerprint.of("SEC", "a.java", "two"));
    }

    @Test
    void batchSuffixAppendsIndex()
    {
        String base = ReviewIssueFingerprint.of("C", "f", "t");
        assertEquals(base, ReviewIssueFingerprint.withBatchSuffix(base, 0));
        assertEquals(base + ":1", ReviewIssueFingerprint.withBatchSuffix(base, 1));
    }

    @Test
    void nullTitleUsesDefault()
    {
        assertEquals(
            ReviewIssueFingerprint.of("C", "f", "未命名问题"),
            ReviewIssueFingerprint.of("C", "f", null));
    }

    @Test
    void familyKeyIgnoresTitleAndUsesFileThenCategory()
    {
        assertEquals(
            ReviewIssueFingerprint.familyKey("a.java", "SEC"),
            ReviewIssueFingerprint.familyKey("a.java", "SEC"));
        assertEquals(
            ReviewIssueFingerprint.familyKey(issue("SEC", "a.java", "one")),
            ReviewIssueFingerprint.familyKey(issue("SEC", "a.java", "two")));
        assertNotEquals(
            ReviewIssueFingerprint.familyKey("a.java", "SEC"),
            ReviewIssueFingerprint.familyKey("b.java", "SEC"));
        assertNotEquals(
            ReviewIssueFingerprint.of("SEC", "a.java", "t"),
            ReviewIssueFingerprint.familyKey("a.java", "SEC"));
    }

    private static ReviewTopIssue issue(String category, String path, String title)
    {
        ReviewTopIssue issue = new ReviewTopIssue();
        issue.setCategory(category);
        issue.setFilePath(path);
        issue.setTitle(title);
        return issue;
    }
}
