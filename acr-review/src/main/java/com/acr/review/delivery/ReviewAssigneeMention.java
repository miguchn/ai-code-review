package com.acr.review.delivery;

/** 审查总结 / 逾期提醒中的责任人 @ 目标。 */
public final class ReviewAssigneeMention
{
    private final Long userId;
    private final String displayName;
    private final String dingTalkMobile;
    private final String wecomUserId;
    private final String feishuOpenId;

    public ReviewAssigneeMention(Long userId, String displayName,
                                 String dingTalkMobile, String wecomUserId, String feishuOpenId)
    {
        this.userId = userId;
        this.displayName = displayName;
        this.dingTalkMobile = dingTalkMobile;
        this.wecomUserId = wecomUserId;
        this.feishuOpenId = feishuOpenId;
    }

    public Long getUserId()
    {
        return userId;
    }

    public String getDisplayName()
    {
        return displayName;
    }

    public String getDingTalkMobile()
    {
        return dingTalkMobile;
    }

    public String getWecomUserId()
    {
        return wecomUserId;
    }

    public String getFeishuOpenId()
    {
        return feishuOpenId;
    }

    public String channelIdentifier(String channelType)
    {
        if (ReviewDeliveryConstants.CHANNEL_DINGTALK_ROBOT.equals(channelType))
        {
            return dingTalkMobile;
        }
        if (ReviewDeliveryConstants.CHANNEL_WECOM_ROBOT.equals(channelType))
        {
            return wecomUserId;
        }
        if (ReviewDeliveryConstants.CHANNEL_FEISHU_BOT.equals(channelType))
        {
            return feishuOpenId;
        }
        return null;
    }
}
