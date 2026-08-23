package com.acr.review.delivery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import com.acr.review.domain.ReviewIssue;
import com.acr.review.domain.ReviewIssueConstants;

class ReviewOverdueRemindRendererTest
{
    @Test
    void render_limitsIssueRowsByOverdueConstant()
    {
        List<ReviewIssue> issues = new ArrayList<>();
        for (int i = 1; i <= ReviewIssueConstants.MAX_ISSUES_IN_OVERDUE_REMIND + 2; i++)
        {
            ReviewIssue issue = new ReviewIssue();
            issue.setIssueId((long) i);
            issue.setSeverity("HIGH");
            issue.setTitle("问题" + i);
            issue.setAssigneeName("张三");
            issues.add(issue);
        }

        String body = ReviewOverdueRemindRenderer.render(issues, List.of(), null);

        assertTrue(body.contains("问题1"));
        assertTrue(body.contains("问题" + ReviewIssueConstants.MAX_ISSUES_IN_OVERDUE_REMIND));
        assertFalse(body.contains("问题" + (ReviewIssueConstants.MAX_ISSUES_IN_OVERDUE_REMIND + 1)));
        assertTrue(body.contains("共 " + issues.size() + " 个逾期问题，请前往问题台账处理"));
        assertEquals(5, ReviewIssueConstants.MAX_ISSUES_IN_OVERDUE_REMIND);
    }
}
