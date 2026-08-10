package com.acr.review.git;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;

/**
 * 四平台 push 载荷 commits[] 解析。缺数组返回空列表；单条关键字段缺失则跳过该条。
 */
public final class GitPushCommitParser
{
    private GitPushCommitParser()
    {
    }

    public static List<GitPushCommit> parseCommits(JSONObject root)
    {
        if (root == null)
        {
            return List.of();
        }
        JSONArray commits = root.getJSONArray("commits");
        if (commits == null || commits.isEmpty())
        {
            return List.of();
        }
        List<GitPushCommit> result = new ArrayList<>(commits.size());
        for (int i = 0; i < commits.size(); i++)
        {
            JSONObject item = commits.getJSONObject(i);
            GitPushCommit parsed = parseOne(item);
            if (parsed != null)
            {
                result.add(parsed);
            }
        }
        return Collections.unmodifiableList(result);
    }

    static GitPushCommit parseOne(JSONObject item)
    {
        if (item == null)
        {
            return null;
        }
        String sha = firstNonBlank(item.getString("id"), item.getString("sha"));
        if (sha == null || sha.isBlank())
        {
            return null;
        }
        JSONObject author = item.getJSONObject("author");
        String authorName = author == null ? null : trimToNull(author.getString("name"));
        String authorEmail = author == null ? null : trimToNull(author.getString("email"));
        Date timestamp = parseTimestamp(item.get("timestamp"));
        if (timestamp == null)
        {
            timestamp = parseTimestamp(item.get("authored_date"));
        }
        String message = item.getString("message");
        String firstLine = firstLine(message);
        return new GitPushCommit(sha.trim(), authorName, authorEmail, timestamp, firstLine);
    }

    static Date parseTimestamp(Object raw)
    {
        if (raw == null)
        {
            return null;
        }
        if (raw instanceof Date date)
        {
            return date;
        }
        if (raw instanceof Number number)
        {
            long value = number.longValue();
            // 秒级时间戳（10 位量级）转毫秒
            if (value > 0 && value < 100_000_000_000L)
            {
                value = value * 1000L;
            }
            return new Date(value);
        }
        String text = String.valueOf(raw).trim();
        if (text.isEmpty())
        {
            return null;
        }
        try
        {
            if (text.chars().allMatch(Character::isDigit))
            {
                return parseTimestamp(Long.parseLong(text));
            }
            return Date.from(OffsetDateTime.parse(text).toInstant());
        }
        catch (DateTimeParseException | NumberFormatException ignored)
        {
            try
            {
                return Date.from(Instant.parse(text));
            }
            catch (DateTimeParseException ex)
            {
                return null;
            }
        }
    }

    static String firstLine(String message)
    {
        if (message == null)
        {
            return null;
        }
        String trimmed = message.trim();
        if (trimmed.isEmpty())
        {
            return null;
        }
        int newline = trimmed.indexOf('\n');
        String line = newline >= 0 ? trimmed.substring(0, newline).trim() : trimmed;
        if (line.length() > 255)
        {
            return line.substring(0, 255);
        }
        return line.isEmpty() ? null : line;
    }

    private static String firstNonBlank(String a, String b)
    {
        if (a != null && !a.isBlank())
        {
            return a;
        }
        if (b != null && !b.isBlank())
        {
            return b;
        }
        return null;
    }

    private static String trimToNull(String value)
    {
        if (value == null)
        {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
