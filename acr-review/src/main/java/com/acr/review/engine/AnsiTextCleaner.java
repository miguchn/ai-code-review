package com.acr.review.engine;

import java.util.regex.Pattern;

/** 清理 CLI 输出中的 ANSI 转义序列，避免前端展示乱码。 */
final class AnsiTextCleaner
{
    /** CSI / OSC 等常见终端控制序列。 */
    private static final Pattern ANSI_ESCAPE = Pattern.compile(
        "\\u001B\\[[0-9;?]*[ -/]*[@-~]"
            + "|\\u001B\\][^\\u0007\\u001B]*(?:\\u0007|\\u001B\\\\)"
            + "|\\u001B[@-Z\\\\-_]");

    private AnsiTextCleaner()
    {
    }

    static String strip(String text)
    {
        if (text == null || text.isEmpty())
        {
            return text;
        }
        return ANSI_ESCAPE.matcher(text).replaceAll("");
    }
}
