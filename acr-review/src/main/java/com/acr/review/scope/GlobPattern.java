package com.acr.review.scope;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * 路径 glob 匹配（无第三方依赖）。
 * 支持 `**`（任意层级，含零层）、`*`（段内任意字符）、`?`（段内单字符）；路径统一以 `/` 分隔、无前导斜杠。
 */
public final class GlobPattern
{
    private static final Map<String, Pattern> CACHE = new ConcurrentHashMap<>();

    private GlobPattern()
    {
    }

    public static boolean matches(String glob, String path)
    {
        if (glob == null || glob.isBlank() || path == null)
        {
            return false;
        }
        String normalizedPath = normalize(path);
        return CACHE.computeIfAbsent(normalize(glob), GlobPattern::compile).matcher(normalizedPath).matches();
    }

    private static String normalize(String value)
    {
        String result = value.trim().replace('\\', '/');
        while (result.startsWith("/"))
        {
            result = result.substring(1);
        }
        return result;
    }

    private static Pattern compile(String glob)
    {
        StringBuilder regex = new StringBuilder("^");
        int i = 0;
        while (i < glob.length())
        {
            char c = glob.charAt(i);
            if (c == '*')
            {
                boolean doubleStar = i + 1 < glob.length() && glob.charAt(i + 1) == '*';
                if (doubleStar)
                {
                    boolean followedBySlash = i + 2 < glob.length() && glob.charAt(i + 2) == '/';
                    if (followedBySlash)
                    {
                        // `**/` 匹配任意层级目录（含零层）
                        regex.append("(?:.*/)?");
                        i += 3;
                    }
                    else
                    {
                        regex.append(".*");
                        i += 2;
                    }
                }
                else
                {
                    regex.append("[^/]*");
                    i++;
                }
            }
            else if (c == '?')
            {
                regex.append("[^/]");
                i++;
            }
            else
            {
                if ("\\.[]{}()+-^$|".indexOf(c) >= 0)
                {
                    regex.append('\\');
                }
                regex.append(c);
                i++;
            }
        }
        regex.append('$');
        return Pattern.compile(regex.toString());
    }
}
