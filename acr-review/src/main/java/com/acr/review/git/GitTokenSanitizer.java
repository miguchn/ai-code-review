package com.acr.review.git;

import java.util.regex.Pattern;
import com.acr.review.delivery.ReviewDeliveryConstants;

/**
 * 平台中立的失败消息脱敏：掩掉明文 Token 与各平台 Token 特征片段。
 * 供投递、工作区准备等跨平台路径复用，避免耦合单一平台的实现类。
 */
public final class GitTokenSanitizer
{
    /** 覆盖 GitHub/GitLab/Gitea/Gitee 的 Token 特征；最后一段兜底掩掉长字母数字串（Gitee 明文令牌）。 */
    private static final Pattern TOKEN_PATTERN = Pattern.compile(
        "(?i)(ghp_|gho_|ghu_|ghs_|ghr_|github_pat_)[A-Za-z0-9_]{10,}"
            + "|glpat-[A-Za-z0-9_-]{10,}"
            + "|gitea_[A-Za-z0-9_]{10,}"
            + "|[A-Za-z0-9_]{16,}");

    private GitTokenSanitizer()
    {
    }

    public static String sanitize(String message, String token)
    {
        if (message == null)
        {
            return "Git 操作失败";
        }
        String sanitized = message;
        if (token != null && !token.isBlank())
        {
            sanitized = sanitized.replace(token, "***");
        }
        sanitized = TOKEN_PATTERN.matcher(sanitized).replaceAll("***");
        return sanitized.length() > ReviewDeliveryConstants.MAX_FAILURE_MESSAGE_CHARS
            ? sanitized.substring(0, ReviewDeliveryConstants.MAX_FAILURE_MESSAGE_CHARS)
            : sanitized;
    }
}
