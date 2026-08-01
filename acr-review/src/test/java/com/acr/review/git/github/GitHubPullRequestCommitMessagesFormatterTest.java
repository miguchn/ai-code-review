package com.acr.review.git.github;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;

class GitHubPullRequestCommitMessagesFormatterTest
{
    @Test
    void usesFirstLineAndJoinsMessages()
    {
        JSONArray commits = JSON.parseArray("""
            [
              {"commit":{"message":"Fix parser bug\\n\\nDetailed explanation"}},
              {"commit":{"message":"Add tests"}}
            ]
            """);

        assertEquals("Fix parser bug\nAdd tests", GitHubPullRequestCommitMessagesFormatter.format(commits));
    }

    @Test
    void truncatesLongFirstLine()
    {
        String longSubject = "x".repeat(GitHubPullRequestCommitMessagesFormatter.MAX_LINE_LENGTH + 10);
        JSONArray commits = JSON.parseArray("""
            [{"commit":{"message":"%s"}}]
            """.formatted(longSubject));

        String formatted = GitHubPullRequestCommitMessagesFormatter.format(commits);
        assertTrue(formatted.endsWith("..."));
        assertEquals(GitHubPullRequestCommitMessagesFormatter.MAX_LINE_LENGTH + 3, formatted.length());
    }

    @Test
    void limitsCommitCountAndNotesRemainder()
    {
        StringBuilder body = new StringBuilder("[");
        for (int index = 0; index < 35; index++)
        {
            if (index > 0)
            {
                body.append(',');
            }
            body.append("{\"commit\":{\"message\":\"commit-").append(index).append("\"}}");
        }
        body.append(']');

        String formatted = GitHubPullRequestCommitMessagesFormatter.format(JSON.parseArray(body.toString()));
        assertTrue(formatted.contains("commit-0"));
        assertTrue(formatted.contains("commit-29"));
        assertFalse(formatted.contains("commit-30"));
        assertTrue(formatted.contains("... 另有 5 个提交未展示"));
    }

    @Test
    void returnsEmptyForMissingPayload()
    {
        assertEquals("", GitHubPullRequestCommitMessagesFormatter.format(null));
        assertEquals("", GitHubPullRequestCommitMessagesFormatter.format(JSON.parseArray("[]")));
        assertEquals("", GitHubPullRequestCommitMessagesFormatter.firstLine(null));
    }
}
