package com.acr.review.insight.dto;

public class InsightIdentityCandidate
{
    private String authorEmail;
    private String authorName;
    private String authorKey;

    public InsightIdentityCandidate()
    {
    }

    public InsightIdentityCandidate(String authorEmail, String authorName, String authorKey)
    {
        this.authorEmail = authorEmail;
        this.authorName = authorName;
        this.authorKey = authorKey;
    }

    public String getAuthorEmail()
    {
        return authorEmail;
    }

    public void setAuthorEmail(String authorEmail)
    {
        this.authorEmail = authorEmail;
    }

    public String getAuthorName()
    {
        return authorName;
    }

    public void setAuthorName(String authorName)
    {
        this.authorName = authorName;
    }

    public String getAuthorKey()
    {
        return authorKey;
    }

    public void setAuthorKey(String authorKey)
    {
        this.authorKey = authorKey;
    }
}
