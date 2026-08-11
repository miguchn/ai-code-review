package com.acr.review.insight.dto;

public class InsightIdentityCandidateVo
{
    private String identifier;
    private String displayName;
    private String matchType;
    private String sampleProjectName;
    private String sampleMessage;
    private String sampleTime;

    public String getIdentifier()
    {
        return identifier;
    }

    public void setIdentifier(String identifier)
    {
        this.identifier = identifier;
    }

    public String getDisplayName()
    {
        return displayName;
    }

    public void setDisplayName(String displayName)
    {
        this.displayName = displayName;
    }

    public String getMatchType()
    {
        return matchType;
    }

    public void setMatchType(String matchType)
    {
        this.matchType = matchType;
    }

    public String getSampleProjectName()
    {
        return sampleProjectName;
    }

    public void setSampleProjectName(String sampleProjectName)
    {
        this.sampleProjectName = sampleProjectName;
    }

    public String getSampleMessage()
    {
        return sampleMessage;
    }

    public void setSampleMessage(String sampleMessage)
    {
        this.sampleMessage = sampleMessage;
    }

    public String getSampleTime()
    {
        return sampleTime;
    }

    public void setSampleTime(String sampleTime)
    {
        this.sampleTime = sampleTime;
    }
}
