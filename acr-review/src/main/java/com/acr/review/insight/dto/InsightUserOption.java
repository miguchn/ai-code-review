package com.acr.review.insight.dto;

/** 身份指派用户候选（字段白名单，不含手机号/邮箱）。 */
public class InsightUserOption
{
    private Long userId;
    private String userName;
    private String nickName;

    public InsightUserOption()
    {
    }

    public InsightUserOption(Long userId, String userName, String nickName)
    {
        this.userId = userId;
        this.userName = userName;
        this.nickName = nickName;
    }

    public Long getUserId()
    {
        return userId;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    public String getUserName()
    {
        return userName;
    }

    public void setUserName(String userName)
    {
        this.userName = userName;
    }

    public String getNickName()
    {
        return nickName;
    }

    public void setNickName(String nickName)
    {
        this.nickName = nickName;
    }
}
