package com.acr.review.scheduling;

import java.lang.management.ManagementFactory;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** 单个应用进程的稳定 worker 标识，写入数据库租约用于跨实例 fencing。 */
@Component
public class ReviewTaskWorkerIdentity
{
    private final String owner = createOwner();

    public String owner()
    {
        return owner;
    }

    private static String createOwner()
    {
        String host = System.getenv("HOSTNAME");
        if (host == null || host.isBlank())
        {
            host = "acr";
        }
        String process = ManagementFactory.getRuntimeMXBean().getName();
        String value = host + ":" + process + ":" + UUID.randomUUID().toString().substring(0, 8);
        return value.length() <= 128 ? value : value.substring(value.length() - 128);
    }
}
