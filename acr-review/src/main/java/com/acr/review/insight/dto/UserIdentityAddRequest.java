package com.acr.review.insight.dto;

public class UserIdentityAddRequest
{
    private String identifier;
    private String displayName;
    /** SELF / AUTO（本人确认建议时传 AUTO） */
    private String origin;

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

    public String getOrigin()
    {
        return origin;
    }

    public void setOrigin(String origin)
    {
        this.origin = origin;
    }
}
