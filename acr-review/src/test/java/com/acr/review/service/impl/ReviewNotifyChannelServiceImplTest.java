package com.acr.review.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import com.acr.common.exception.ServiceException;
import com.acr.common.utils.SecurityUtils;
import com.acr.review.domain.ReviewNotifyChannel;
import com.acr.review.mapper.ReviewNotifyChannelMapper;
import com.acr.review.notify.NotifyRobotClients;
import com.acr.review.security.CredentialCryptoService;

@ExtendWith(MockitoExtension.class)
class ReviewNotifyChannelServiceImplTest
{
    @Mock private ReviewNotifyChannelMapper channelMapper;
    @Mock private CredentialCryptoService cryptoService;
    @Mock private NotifyRobotClients robotClients;

    private ReviewNotifyChannelServiceImpl service;

    @BeforeEach
    void setUp()
    {
        service = new ReviewNotifyChannelServiceImpl(channelMapper, cryptoService, robotClients);
    }

    @Test
    void insertRequiresWebhookUrl()
    {
        ReviewNotifyChannel channel = newChannel(null, "DINGTALK_ROBOT");

        ServiceException ex = assertThrows(ServiceException.class, () -> service.insertReviewNotifyChannel(channel));
        assertEquals("新增通知渠道时必须输入 Webhook URL", ex.getMessage());
        verify(channelMapper, never()).insertReviewNotifyChannel(any());
    }

    @Test
    void updateKeepsCiphertextWhenWebhookAndSecretBlank()
    {
        ReviewNotifyChannel existing = newChannel(1L, "DINGTALK_ROBOT");
        existing.setWebhookUrlCiphertext("stored-url-cipher");
        existing.setSecretCiphertext("stored-secret-cipher");
        when(channelMapper.selectReviewNotifyChannelById(1L)).thenReturn(existing);
        when(channelMapper.updateReviewNotifyChannel(any())).thenReturn(1);

        ReviewNotifyChannel update = newChannel(1L, "DINGTALK_ROBOT");
        update.setChannelName("Renamed");
        update.setWebhookUrl(null);
        update.setSecret(null);

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class))
        {
            securityUtils.when(SecurityUtils::getUsername).thenReturn("tester");
            service.updateReviewNotifyChannel(update);
        }

        verify(cryptoService, never()).encryptNotifyWebhookUrl(any());
        verify(cryptoService, never()).encryptNotifyWebhookSecret(any());
        ArgumentCaptor<ReviewNotifyChannel> captor = ArgumentCaptor.forClass(ReviewNotifyChannel.class);
        verify(channelMapper).updateReviewNotifyChannel(captor.capture());
        assertEquals("Renamed", captor.getValue().getChannelName());
    }

    @Test
    void deleteBlockedWhenReferenced()
    {
        ReviewNotifyChannel channel = newChannel(2L, "WECOM_ROBOT");
        when(channelMapper.selectReviewNotifyChannelById(2L)).thenReturn(channel);
        when(channelMapper.countProjectsByChannelId(2L)).thenReturn(3);

        ServiceException ex = assertThrows(ServiceException.class,
            () -> service.deleteReviewNotifyChannelByIds(new Long[] { 2L }));
        assertEquals("渠道“Test Channel”已被 3 个项目引用，不能删除", ex.getMessage());
        verify(channelMapper, never()).deleteReviewNotifyChannelByIds(any());
    }

    private static ReviewNotifyChannel newChannel(Long channelId, String channelType)
    {
        ReviewNotifyChannel channel = new ReviewNotifyChannel();
        channel.setChannelId(channelId);
        channel.setChannelName("Test Channel");
        channel.setChannelType(channelType);
        channel.setStatus("0");
        return channel;
    }
}
