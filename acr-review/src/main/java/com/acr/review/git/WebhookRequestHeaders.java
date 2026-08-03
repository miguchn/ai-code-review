package com.acr.review.git;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** 平台无关的 Webhook 请求头视图（大小写不敏感）。 */
public final class WebhookRequestHeaders
{
    private final Map<String, String> values;

    private WebhookRequestHeaders(Map<String, String> values)
    {
        this.values = values;
    }

    public static WebhookRequestHeaders of(Map<String, String> headers)
    {
        Map<String, String> normalized = new LinkedHashMap<>();
        if (headers != null)
        {
            for (Map.Entry<String, String> entry : headers.entrySet())
            {
                if (entry.getKey() == null || entry.getKey().isBlank())
                {
                    continue;
                }
                normalized.put(entry.getKey().toLowerCase(Locale.ROOT), entry.getValue());
            }
        }
        return new WebhookRequestHeaders(Collections.unmodifiableMap(normalized));
    }

    public static WebhookRequestHeaders empty()
    {
        return of(Map.of());
    }

    public String get(String name)
    {
        if (name == null || name.isBlank())
        {
            return null;
        }
        return values.get(name.toLowerCase(Locale.ROOT));
    }

    public String require(String name)
    {
        String value = get(name);
        if (value == null || value.isBlank())
        {
            throw new IllegalArgumentException("缺少请求头 " + name);
        }
        return value;
    }

    public Map<String, String> asMap()
    {
        return values;
    }
}
