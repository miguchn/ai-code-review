package com.acr.review.engine;

import java.util.Map;

/** 标准审查引擎请求。 */
public class ReviewEngineRequest
{
    private String projectKey;
    private String repositoryKey;
    private String baseSha;
    private String headSha;
    private String diffContent;
    private String workingDirectory;
    private Map<String, String> modelEnvironment;
    private int timeoutSeconds;
    private ReviewEngineInvocationType invocationType = ReviewEngineInvocationType.REVIEW;

    public String getProjectKey()
    {
        return projectKey;
    }

    public void setProjectKey(String projectKey)
    {
        this.projectKey = projectKey;
    }

    public String getRepositoryKey()
    {
        return repositoryKey;
    }

    public void setRepositoryKey(String repositoryKey)
    {
        this.repositoryKey = repositoryKey;
    }

    public String getBaseSha()
    {
        return baseSha;
    }

    public void setBaseSha(String baseSha)
    {
        this.baseSha = baseSha;
    }

    public String getHeadSha()
    {
        return headSha;
    }

    public void setHeadSha(String headSha)
    {
        this.headSha = headSha;
    }

    public String getDiffContent()
    {
        return diffContent;
    }

    public void setDiffContent(String diffContent)
    {
        this.diffContent = diffContent;
    }

    public String getWorkingDirectory()
    {
        return workingDirectory;
    }

    public void setWorkingDirectory(String workingDirectory)
    {
        this.workingDirectory = workingDirectory;
    }

    public Map<String, String> getModelEnvironment()
    {
        return modelEnvironment;
    }

    public void setModelEnvironment(Map<String, String> modelEnvironment)
    {
        this.modelEnvironment = modelEnvironment;
    }

    public int getTimeoutSeconds()
    {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds)
    {
        this.timeoutSeconds = timeoutSeconds;
    }

    public ReviewEngineInvocationType getInvocationType()
    {
        return invocationType;
    }

    public void setInvocationType(ReviewEngineInvocationType invocationType)
    {
        this.invocationType = invocationType;
    }
}
