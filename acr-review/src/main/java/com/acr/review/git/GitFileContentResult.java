package com.acr.review.git;

/**
 * 单文件内容拉取结果。任何失败（限流/凭据/过大/网络）都结构化返回，
 * 不抛异常：调用方按失败原因降级，不阻断审查。
 */
public record GitFileContentResult(boolean success, String content, String failureReason)
{
    public static GitFileContentResult ok(String content)
    {
        return new GitFileContentResult(true, content, null);
    }

    public static GitFileContentResult fail(String failureReason)
    {
        return new GitFileContentResult(false, null, failureReason);
    }
}
