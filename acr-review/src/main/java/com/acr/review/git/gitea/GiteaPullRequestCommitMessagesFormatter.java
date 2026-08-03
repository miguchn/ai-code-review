package com.acr.review.git.gitea;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;

/** 将 Gitea PR commits API 响应格式化为审查上下文文本。 */
final class GiteaPullRequestCommitMessagesFormatter
{
    static final int MAX_COMMITS = 30;
    static final int MAX_LINE_LENGTH = 500;

    private GiteaPullRequestCommitMessagesFormatter()
    {
    }

    static String format(JSONArray commits)
    {
        if (commits == null || commits.isEmpty())
        {
            return "";
        }

        int total = commits.size();
        int limit = Math.min(total, MAX_COMMITS);
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < limit; index++)
        {
            String line = firstLine(commits.getJSONObject(index));
            if (line.isEmpty())
            {
                continue;
            }
            if (builder.length() > 0)
            {
                builder.append('\n');
            }
            builder.append(line);
        }
        if (total > MAX_COMMITS)
        {
            if (builder.length() > 0)
            {
                builder.append('\n');
            }
            builder.append("... 另有 ").append(total - MAX_COMMITS).append(" 个提交未展示");
        }
        return builder.toString();
    }

    static String firstLine(JSONObject commitNode)
    {
        if (commitNode == null)
        {
            return "";
        }
        JSONObject commit = commitNode.getJSONObject("commit");
        if (commit == null)
        {
            return "";
        }
        String message = commit.getString("message");
        if (message == null || message.isBlank())
        {
            return "";
        }
        int newline = message.indexOf('\n');
        String line = (newline >= 0 ? message.substring(0, newline) : message).trim();
        if (line.length() <= MAX_LINE_LENGTH)
        {
            return line;
        }
        return line.substring(0, MAX_LINE_LENGTH) + "...";
    }
}
