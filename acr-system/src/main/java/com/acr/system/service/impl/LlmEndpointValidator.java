package com.acr.system.service.impl;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.acr.common.exception.ServiceException;

/**
 * LLM 端点安全校验，默认拒绝明文 HTTP 与内网目标，避免测试接口被用作 SSRF 代理。
 */
@Component
public class LlmEndpointValidator
{
    private final boolean allowHttp;
    private final boolean allowPrivateEndpoints;

    public LlmEndpointValidator(@Value("${llm.endpoint.allow-http:false}") boolean allowHttp,
        @Value("${llm.endpoint.allow-private:false}") boolean allowPrivateEndpoints)
    {
        this.allowHttp = allowHttp;
        this.allowPrivateEndpoints = allowPrivateEndpoints;
    }

    public void validate(String endpoint)
    {
        try
        {
            URI uri = URI.create(endpoint);
            String scheme = uri.getScheme();
            if (!"https".equalsIgnoreCase(scheme) && !("http".equalsIgnoreCase(scheme) && allowHttp))
            {
                throw new ServiceException("模型地址必须使用 HTTPS");
            }
            if (uri.getHost() == null || uri.getUserInfo() != null)
            {
                throw new ServiceException("模型地址格式无效");
            }
            if (!allowPrivateEndpoints)
            {
                for (InetAddress address : InetAddress.getAllByName(uri.getHost()))
                {
                    if (isPrivate(address))
                    {
                        throw new ServiceException("模型地址不能指向本机或内网");
                    }
                }
            }
        }
        catch (IllegalArgumentException | UnknownHostException e)
        {
            throw new ServiceException("模型地址无法解析");
        }
    }

    private boolean isPrivate(InetAddress address)
    {
        if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress()
            || address.isSiteLocalAddress() || address.isMulticastAddress())
        {
            return true;
        }
        if (address instanceof Inet6Address)
        {
            byte[] bytes = address.getAddress();
            return bytes.length == 16 && (bytes[0] & 0xfe) == 0xfc;
        }
        return false;
    }
}
