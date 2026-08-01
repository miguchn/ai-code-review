package com.acr.review.git;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;

/** 统一的 Git 连接检测结果。 */
public class GitConnectionResult
{
    private boolean success;
    private GitConnectionFailure failure;
    private String message;
    private String defaultBranch;
    private String repositoryUrl;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date checkedAt;

    public static GitConnectionResult success(String message)
    {
        return success(message, null, null);
    }

    public static GitConnectionResult success(String message, String defaultBranch, String repositoryUrl)
    {
        GitConnectionResult result = new GitConnectionResult();
        result.success = true;
        result.failure = GitConnectionFailure.NONE;
        result.message = message;
        result.defaultBranch = defaultBranch;
        result.repositoryUrl = repositoryUrl;
        result.checkedAt = new Date();
        return result;
    }

    public static GitConnectionResult failure(GitConnectionFailure failure, String message)
    {
        GitConnectionResult result = new GitConnectionResult();
        result.success = false;
        result.failure = failure;
        result.message = message;
        result.checkedAt = new Date();
        return result;
    }

    public boolean isSuccess()
    {
        return success;
    }

    public GitConnectionFailure getFailure()
    {
        return failure;
    }

    public String getMessage()
    {
        return message;
    }

    public String getDefaultBranch()
    {
        return defaultBranch;
    }

    public String getRepositoryUrl()
    {
        return repositoryUrl;
    }

    public Date getCheckedAt()
    {
        return checkedAt;
    }
}
