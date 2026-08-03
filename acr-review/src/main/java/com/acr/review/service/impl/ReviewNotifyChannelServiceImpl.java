package com.acr.review.service.impl;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.acr.common.exception.ServiceException;
import com.acr.common.utils.SecurityUtils;
import com.acr.common.utils.StringUtils;
import com.acr.review.delivery.ReviewDeliveryConstants;
import com.acr.review.domain.ReviewNotifyChannel;
import com.acr.review.mapper.ReviewNotifyChannelMapper;
import com.acr.review.notify.NotifyRobotClients;
import com.acr.review.notify.NotifyRobotException;
import com.acr.review.security.CredentialCryptoService;
import com.acr.review.service.IReviewNotifyChannelService;

/** 通知渠道 CRUD、启停与测试发送。 */
@Service
public class ReviewNotifyChannelServiceImpl implements IReviewNotifyChannelService
{
    private final ReviewNotifyChannelMapper channelMapper;
    private final CredentialCryptoService cryptoService;
    private final NotifyRobotClients robotClients;

    public ReviewNotifyChannelServiceImpl(ReviewNotifyChannelMapper channelMapper,
                                          CredentialCryptoService cryptoService,
                                          NotifyRobotClients robotClients)
    {
        this.channelMapper = channelMapper;
        this.cryptoService = cryptoService;
        this.robotClients = robotClients;
    }

    @Override
    public ReviewNotifyChannel selectReviewNotifyChannelById(Long channelId)
    {
        ReviewNotifyChannel channel = channelMapper.selectReviewNotifyChannelById(channelId);
        if (channel == null)
        {
            throw new ServiceException("通知渠道不存在");
        }
        return channel;
    }

    @Override
    public List<ReviewNotifyChannel> selectReviewNotifyChannelList(ReviewNotifyChannel channel)
    {
        return channelMapper.selectReviewNotifyChannelList(channel);
    }

    @Override
    public int insertReviewNotifyChannel(ReviewNotifyChannel channel)
    {
        normalize(channel);
        if (StringUtils.isEmpty(channel.getWebhookUrl()))
        {
            throw new ServiceException("新增通知渠道时必须输入 Webhook URL");
        }
        checkNameUnique(channel);
        channel.setWebhookUrlCiphertext(cryptoService.encryptNotifyWebhookUrl(channel.getWebhookUrl().trim()));
        if (StringUtils.isNotEmpty(channel.getSecret()))
        {
            channel.setSecretCiphertext(cryptoService.encryptNotifyWebhookSecret(channel.getSecret().trim()));
        }
        channel.setCreateBy(SecurityUtils.getUsername());
        return channelMapper.insertReviewNotifyChannel(channel);
    }

    @Override
    @Transactional
    public int updateReviewNotifyChannel(ReviewNotifyChannel channel)
    {
        if (channel.getChannelId() == null)
        {
            throw new ServiceException("渠道 ID 不能为空");
        }
        selectReviewNotifyChannelById(channel.getChannelId());
        normalize(channel);
        checkNameUnique(channel);
        boolean urlChanged = StringUtils.isNotEmpty(channel.getWebhookUrl());
        boolean secretProvided = channel.getSecret() != null;
        if (urlChanged)
        {
            channel.setWebhookUrlCiphertext(cryptoService.encryptNotifyWebhookUrl(channel.getWebhookUrl().trim()));
        }
        if (secretProvided)
        {
            if (StringUtils.isEmpty(channel.getSecret()))
            {
                channel.setSecretCiphertext("");
            }
            else
            {
                channel.setSecretCiphertext(cryptoService.encryptNotifyWebhookSecret(channel.getSecret().trim()));
            }
        }
        channel.setUpdateBy(SecurityUtils.getUsername());
        int rows = channelMapper.updateReviewNotifyChannel(channel);
        if (urlChanged || secretProvided)
        {
            channelMapper.resetConnectionCheck(channel.getChannelId());
        }
        return rows;
    }

    @Override
    public int changeStatus(Long channelId, String status)
    {
        selectReviewNotifyChannelById(channelId);
        if (!"0".equals(status) && !"1".equals(status))
        {
            throw new ServiceException("渠道状态仅支持启用或停用");
        }
        ReviewNotifyChannel update = new ReviewNotifyChannel();
        update.setChannelId(channelId);
        update.setStatus(status);
        update.setUpdateBy(SecurityUtils.getUsername());
        return channelMapper.updateReviewNotifyChannel(update);
    }

    @Override
    @Transactional
    public void deleteReviewNotifyChannelByIds(Long[] channelIds)
    {
        for (Long channelId : channelIds)
        {
            ReviewNotifyChannel channel = selectReviewNotifyChannelById(channelId);
            int references = channelMapper.countProjectsByChannelId(channelId);
            if (references > 0)
            {
                throw new ServiceException("渠道“" + channel.getChannelName() + "”已被 "
                    + references + " 个项目引用，不能删除");
            }
        }
        channelMapper.deleteReviewNotifyChannelByIds(channelIds);
    }

    @Override
    public Map<String, Object> testSend(Long channelId)
    {
        DecryptedNotifyChannel decrypted = getDecryptedChannel(channelId, false);
        boolean success = false;
        String message;
        try
        {
            robotClients.require(decrypted.channelType()).send(
                decrypted.webhookUrl(),
                decrypted.secret(),
                ReviewDeliveryConstants.TEST_MESSAGE_TITLE,
                ReviewDeliveryConstants.TEST_MESSAGE_BODY);
            success = true;
            message = "测试发送成功";
        }
        catch (NotifyRobotException | ServiceException ex)
        {
            message = StringUtils.defaultIfEmpty(ex.getMessage(), "测试发送失败");
        }
        catch (Exception ex)
        {
            message = "测试发送异常";
        }
        message = truncate(message, 255);
        ReviewNotifyChannel update = new ReviewNotifyChannel();
        update.setChannelId(channelId);
        update.setLastCheckStatus(success ? "SUCCESS" : "FAILED");
        update.setLastCheckMessage(message);
        update.setLastCheckTime(new Date());
        update.setUpdateBy(SecurityUtils.getUsername());
        channelMapper.updateConnectionCheck(update);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", success);
        result.put("message", message);
        result.put("checkedAt", update.getLastCheckTime());
        return result;
    }

    @Override
    public DecryptedNotifyChannel getDecryptedChannel(Long channelId, boolean requireEnabled)
    {
        ReviewNotifyChannel channel = channelMapper.selectReviewNotifyChannelSecretById(channelId);
        if (channel == null)
        {
            throw new ServiceException("通知渠道不存在");
        }
        if (requireEnabled && !"0".equals(channel.getStatus()))
        {
            throw new ServiceException("通知渠道已停用");
        }
        if (!ReviewDeliveryConstants.isSupportedNotifyChannelType(channel.getChannelType()))
        {
            throw new ServiceException("不支持的通知渠道类型");
        }
        String url = cryptoService.decryptNotifyWebhookUrl(channel.getWebhookUrlCiphertext());
        String secret = null;
        if (StringUtils.isNotEmpty(channel.getSecretCiphertext()))
        {
            secret = cryptoService.decryptNotifyWebhookSecret(channel.getSecretCiphertext());
        }
        return new DecryptedNotifyChannel(channel.getChannelId(), channel.getChannelName(),
            channel.getChannelType(), url, secret);
    }

    private void normalize(ReviewNotifyChannel channel)
    {
        channel.setChannelName(channel.getChannelName().trim());
        channel.setChannelType(channel.getChannelType().trim().toUpperCase());
        if (!ReviewDeliveryConstants.isSupportedNotifyChannelType(channel.getChannelType()))
        {
            throw new ServiceException("渠道类型仅支持钉钉/企微/飞书群机器人");
        }
        if (!"0".equals(channel.getStatus()) && !"1".equals(channel.getStatus()))
        {
            channel.setStatus("0");
        }
    }

    private void checkNameUnique(ReviewNotifyChannel channel)
    {
        if (channelMapper.selectByTypeAndName(channel.getChannelType(), channel.getChannelName(),
            channel.getChannelId()) != null)
        {
            throw new ServiceException("同类型下渠道名称已存在");
        }
    }

    private static String truncate(String message, int max)
    {
        if (message == null)
        {
            return null;
        }
        return message.length() > max ? message.substring(0, max) : message;
    }
}
