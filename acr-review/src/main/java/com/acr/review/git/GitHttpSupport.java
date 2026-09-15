package com.acr.review.git;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import com.acr.common.utils.http.RestrictedHost;
import okhttp3.OkHttpClient;

/** Git HTTP 客户端公共约束：禁止跟随重定向，拒绝链路本地与云元数据。 */
public final class GitHttpSupport
{
    private GitHttpSupport()
    {
    }

    public static OkHttpClient.Builder clientBuilder(int connectTimeoutMs, int readTimeoutMs)
    {
        return new OkHttpClient.Builder()
            .followRedirects(false)
            .followSslRedirects(false)
            .addInterceptor(chain -> {
                try
                {
                    RestrictedHost.requireAllowed(chain.request().url().host());
                }
                catch (IllegalArgumentException ex)
                {
                    throw new IOException(ex.getMessage());
                }
                return chain.proceed(chain.request());
            })
            .connectTimeout(connectTimeoutMs, TimeUnit.MILLISECONDS)
            .readTimeout(readTimeoutMs, TimeUnit.MILLISECONDS)
            .callTimeout(readTimeoutMs, TimeUnit.MILLISECONDS);
    }
}
