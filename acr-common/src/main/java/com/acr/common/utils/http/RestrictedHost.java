package com.acr.common.utils.http;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Locale;
import java.util.Set;

/**
 * 拦截链路本地与云元数据地址。不拦截 RFC1918 / 回环，以便自建 Git 与本地联调。
 */
public final class RestrictedHost
{
    private static final Set<String> METADATA_HOSTS = Set.of(
        "metadata.google.internal",
        "metadata.tencentyun.com",
        "metadata.aliyuncs.com");

    private RestrictedHost()
    {
    }

    public static void requireAllowed(String host)
    {
        if (host == null || host.isBlank())
        {
            throw new IllegalArgumentException("主机地址无效");
        }
        String normalized = stripBrackets(host.trim().toLowerCase(Locale.ROOT));
        if (METADATA_HOSTS.contains(normalized))
        {
            throw new IllegalArgumentException("禁止访问链路本地或云元数据地址");
        }
        try
        {
            for (InetAddress address : InetAddress.getAllByName(normalized))
            {
                if (isBlocked(address))
                {
                    throw new IllegalArgumentException("禁止访问链路本地或云元数据地址");
                }
            }
        }
        catch (UnknownHostException ex)
        {
            // 无法解析时由后续连接失败，避免把正常公网域名误判为攻击。
        }
    }

    static boolean isBlocked(InetAddress address)
    {
        if (address == null)
        {
            return false;
        }
        if (address.isLinkLocalAddress() || address.isMulticastAddress())
        {
            return true;
        }
        if (address instanceof Inet4Address)
        {
            byte[] bytes = address.getAddress();
            return bytes.length == 4
                && (bytes[0] & 0xff) == 100
                && (bytes[1] & 0xff) == 100
                && (bytes[2] & 0xff) == 100
                && (bytes[3] & 0xff) == 200;
        }
        return false;
    }

    private static String stripBrackets(String host)
    {
        if (host.startsWith("[") && host.endsWith("]") && host.length() > 2)
        {
            return host.substring(1, host.length() - 1);
        }
        return host;
    }
}
