package com.acr.review.git.gitee;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;

class GiteePullRequestCommitMessagesFormatterTest
{
    @Test
    void formatsFirstLineOfEachCommit()
    {
        JSONArray commits = new JSONArray();
        JSONObject first = new JSONObject();
        JSONObject firstCommit = new JSONObject();
        firstCommit.put("message", "feat: one\nbody");
        first.put("commit", firstCommit);
        commits.add(first);

        JSONObject second = new JSONObject();
        JSONObject secondCommit = new JSONObject();
        secondCommit.put("message", "fix: two");
        second.put("commit", secondCommit);
        commits.add(second);

        String formatted = GiteePullRequestCommitMessagesFormatter.format(commits);

        assertTrue(formatted.contains("feat: one"));
        assertTrue(formatted.contains("fix: two"));
    }

    @Test
    void truncatesLongFirstLine()
    {
        JSONArray commits = new JSONArray();
        JSONObject node = new JSONObject();
        JSONObject commit = new JSONObject();
        commit.put("message", "x".repeat(GiteePullRequestCommitMessagesFormatter.MAX_LINE_LENGTH + 10));
        node.put("commit", commit);
        commits.add(node);

        String line = GiteePullRequestCommitMessagesFormatter.firstLine(node);
        assertEquals(GiteePullRequestCommitMessagesFormatter.MAX_LINE_LENGTH + 3, line.length());
        assertTrue(line.endsWith("..."));
    }
}
