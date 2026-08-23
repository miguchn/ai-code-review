package com.acr.review.insight.dto;

public class UserIdentityAddRequest
{
    private String identifier;
    private String displayName;
    /** SELF / AUTO（本人确认建议时传 AUTO） */
    private String origin;
    /** IM 身份类型：IM_WECOM / IM_DINGTALK / IM_FEISHU */
    private String identityType;

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

    public String getIdentityType()
    {
        return identityType;
    }

    public void setIdentityType(String identityType)
    {
        this.identityType = identityType;
    }
}
