package com.acr.review.service;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import com.acr.review.domain.ReviewTask;

class ReviewPromptRendererTest
{
    @Test
    void replacesKnownPlaceholders()
    {
        ReviewTask task = new ReviewTask();
        task.setPrTitle("Fix NPE");
        task.setSourceBranch("feature/a");
        task.setTargetBranch("dev");
        task.setBaseSha("aaa");
        task.setHeadSha("bbb");

        String rendered = new ReviewPromptRenderer().render(
            "title={{pr_title}};desc={{pr_description}};commits={{commit_messages}};src={{source_branch}};dst={{target_branch}};diff={{diff}}",
            task, "@@ -1 +1 @@", "fix null pointer", "fix: guard NPE\nchore: tidy");

        assertTrue(rendered.contains("title=Fix NPE"));
        assertTrue(rendered.contains("desc=fix null pointer"));
        assertTrue(rendered.contains("commits=fix: guard NPE"));
        assertTrue(rendered.contains("src=feature/a"));
        assertTrue(rendered.contains("dst=dev"));
        assertTrue(rendered.contains("diff=@@ -1 +1 @@"));
    }

    @Test
    void placeholderInsideValueIsNotExpandedAgain()
    {
        // PR 标题里含 {{diff}} 字面量时，不得被后续 diff 替换覆盖（单趟替换）
        ReviewTask task = new ReviewTask();
        task.setPrTitle("修复 {{diff}} 渲染问题");

        String rendered = new ReviewPromptRenderer().render(
            "title={{pr_title}};diff={{diff}}", task, "ACTUAL_DIFF");

        assertTrue(rendered.contains("title=修复 {{diff}} 渲染问题"));
        assertTrue(rendered.contains("diff=ACTUAL_DIFF"));
    }

    @Test
    void unknownPlaceholderIsPreserved()
    {
        String rendered = new ReviewPromptRenderer().render(
            "a={{unknown_key}};b={{pr_title}}", new ReviewTask(), "");
        assertTrue(rendered.contains("a={{unknown_key}}"));
    }
}
