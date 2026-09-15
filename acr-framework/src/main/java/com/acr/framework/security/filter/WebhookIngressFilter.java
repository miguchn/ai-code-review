package com.acr.framework.security.filter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import com.acr.common.constant.CacheConstants;
import com.acr.common.utils.ip.IpUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ReadListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletInputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Webhook 入口约束：按 Content-Length 与读取上限拒绝超大载荷，并按 IP 限流。
 * Redis 不可用时限流失败开放，避免误伤合法 Git 回调。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class WebhookIngressFilter extends OncePerRequestFilter
{
    private static final Logger log = LoggerFactory.getLogger(WebhookIngressFilter.class);

    private final RedisTemplate<Object, Object> redisTemplate;
    private final RedisScript<Long> limitScript;
    private final int maxPayloadBytes;
    private final int rateLimitCount;
    private final int rateLimitSeconds;

    public WebhookIngressFilter(RedisTemplate<Object, Object> redisTemplate,
                                RedisScript<Long> limitScript,
                                @Value("${review.webhook.max-payload-bytes:262144}") int maxPayloadBytes,
                                @Value("${review.webhook.rate-limit-count:120}") int rateLimitCount,
                                @Value("${review.webhook.rate-limit-seconds:60}") int rateLimitSeconds)
    {
        this.redisTemplate = redisTemplate;
        this.limitScript = limitScript;
        this.maxPayloadBytes = Math.max(1024, maxPayloadBytes);
        this.rateLimitCount = Math.max(1, rateLimitCount);
        this.rateLimitSeconds = Math.max(1, rateLimitSeconds);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request)
    {
        String path = request.getServletPath();
        return path == null || !path.startsWith("/webhook/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
        throws ServletException, IOException
    {
        long declared = request.getContentLengthLong();
        if (declared > maxPayloadBytes)
        {
            writeJson(response, HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE, "Webhook 载荷超过大小限制");
            return;
        }
        if (!allowByRate(request))
        {
            writeJson(response, 429, "请求过于频繁，请稍后重试");
            return;
        }
        try
        {
            chain.doFilter(new BoundedPayloadRequest(request, maxPayloadBytes), response);
        }
        catch (PayloadTooLargeException ex)
        {
            if (!response.isCommitted())
            {
                writeJson(response, HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE, "Webhook 载荷超过大小限制");
            }
        }
    }

    private boolean allowByRate(HttpServletRequest request)
    {
        try
        {
            String key = CacheConstants.RATE_LIMIT_KEY + "webhook:" + IpUtils.getIpAddr(request);
            List<Object> keys = Collections.singletonList(key);
            Long number = redisTemplate.execute(limitScript, keys, rateLimitCount, rateLimitSeconds);
            return number != null && number.intValue() <= rateLimitCount;
        }
        catch (RuntimeException ex)
        {
            log.warn("Webhook 限流暂不可用，本次放行, reason={}", ex.getMessage());
            return true;
        }
    }

    private static void writeJson(HttpServletResponse response, int status, String message) throws IOException
    {
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write("{\"message\":\"" + message + "\"}");
    }

    static final class PayloadTooLargeException extends IOException
    {
        PayloadTooLargeException()
        {
            super("payload too large");
        }
    }

    static final class BoundedPayloadRequest extends HttpServletRequestWrapper
    {
        private final int maxBytes;

        BoundedPayloadRequest(HttpServletRequest request, int maxBytes)
        {
            super(request);
            this.maxBytes = maxBytes;
        }

        @Override
        public ServletInputStream getInputStream() throws IOException
        {
            return new BoundedServletInputStream(super.getInputStream(), maxBytes);
        }
    }

    static final class BoundedServletInputStream extends ServletInputStream
    {
        private final ServletInputStream delegate;
        private int remaining;

        BoundedServletInputStream(ServletInputStream delegate, int maxBytes)
        {
            this.delegate = delegate;
            this.remaining = maxBytes;
        }

        @Override
        public int read() throws IOException
        {
            if (remaining < 0)
            {
                throw new PayloadTooLargeException();
            }
            int value = delegate.read();
            if (value >= 0)
            {
                remaining--;
                if (remaining < 0)
                {
                    throw new PayloadTooLargeException();
                }
            }
            return value;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException
        {
            if (remaining < 0)
            {
                throw new PayloadTooLargeException();
            }
            int allowed = Math.min(len, remaining + 1);
            int n = delegate.read(b, off, allowed);
            if (n > 0)
            {
                remaining -= n;
                if (remaining < 0)
                {
                    throw new PayloadTooLargeException();
                }
            }
            return n;
        }

        @Override
        public boolean isFinished()
        {
            return delegate.isFinished();
        }

        @Override
        public boolean isReady()
        {
            return delegate.isReady();
        }

        @Override
        public void setReadListener(ReadListener readListener)
        {
            delegate.setReadListener(readListener);
        }
    }
}
