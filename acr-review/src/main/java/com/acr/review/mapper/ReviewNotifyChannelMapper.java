package com.acr.review.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.acr.review.domain.ReviewNotifyChannel;

/** 审查通知渠道数据访问。 */
public interface ReviewNotifyChannelMapper
{
    ReviewNotifyChannel selectReviewNotifyChannelById(Long channelId);

    /** 含密文，仅服务端发送/测试使用。 */
    ReviewNotifyChannel selectReviewNotifyChannelSecretById(Long channelId);

    List<ReviewNotifyChannel> selectReviewNotifyChannelList(ReviewNotifyChannel channel);

    ReviewNotifyChannel selectByTypeAndName(@Param("channelType") String channelType,
                                            @Param("channelName") String channelName,
                                            @Param("excludeChannelId") Long excludeChannelId);

    int insertReviewNotifyChannel(ReviewNotifyChannel channel);

    int updateReviewNotifyChannel(ReviewNotifyChannel channel);

    int updateConnectionCheck(ReviewNotifyChannel channel);

    int resetConnectionCheck(Long channelId);

    int deleteReviewNotifyChannelByIds(Long[] channelIds);

    int countProjectsByChannelId(Long channelId);
}
