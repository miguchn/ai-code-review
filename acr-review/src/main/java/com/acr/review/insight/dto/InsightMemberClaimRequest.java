package com.acr.review.insight.dto;

public class InsightMemberClaimRequest
{
    private String authorEmail;
    private String authorName;

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
}
