package com.acr.review.service;

import java.util.List;
import java.util.Map;
import com.acr.review.domain.ReviewNotifyChannel;

/** 审查通知渠道管理。 */
public interface IReviewNotifyChannelService
{
    ReviewNotifyChannel selectReviewNotifyChannelById(Long channelId);

    List<ReviewNotifyChannel> selectReviewNotifyChannelList(ReviewNotifyChannel channel);

    int insertReviewNotifyChannel(ReviewNotifyChannel channel);

    int updateReviewNotifyChannel(ReviewNotifyChannel channel);

    int changeStatus(Long channelId, String status);

    void deleteReviewNotifyChannelByIds(Long[] channelIds);

    /** 测试发送固定文案，更新 last_check_*。 */
    Map<String, Object> testSend(Long channelId);

    /** 解密后的 URL/Secret，供投递使用；requireEnabled=true 时停用则失败。 */
    DecryptedNotifyChannel getDecryptedChannel(Long channelId, boolean requireEnabled);

    record DecryptedNotifyChannel(Long channelId, String channelName, String channelType,
                                  String webhookUrl, String secret)
    {
    }
}
