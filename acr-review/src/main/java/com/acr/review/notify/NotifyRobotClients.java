package com.acr.review.notify;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import com.acr.common.exception.ServiceException;

/** 按渠道类型解析机器人客户端。 */
@Component
public class NotifyRobotClients
{
    private final Map<String, NotifyRobotClient> byType;

    public NotifyRobotClients(List<NotifyRobotClient> clients)
    {
        this.byType = clients.stream()
            .collect(Collectors.toMap(NotifyRobotClient::channelType, Function.identity(), (a, b) -> a));
    }

    public NotifyRobotClient require(String channelType)
    {
        NotifyRobotClient client = byType.get(channelType);
        if (client == null)
        {
            throw new ServiceException("不支持的通知渠道类型：" + channelType);
        }
        return client;
    }
}
