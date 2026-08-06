package com.acr.review.git;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.BufferedSource;

/**
 * HTTP 响应体有界读取：先 {@code source.request(cap)} 阻塞至缓冲满或 EOF，
 * 再按 cap 精确截取。避免 {@code BufferedSource.read(sink, byteCount)} 单次上游读
 * 在 TCP/TLS 分包下静默截尾。
 */
public final class HttpResponseBodies
{
    private HttpResponseBodies()
    {
    }

    /**
     * Diff 语义：超出上限的部分静默丢弃。空 body 返回空串。
     */
    public static String readCapped(Response response, long cap) throws IOException
    {
        ResponseBody body = response.body();
        if (body == null)
        {
            return "";
        }
        try (BufferedSource source = body.source())
        {
            source.request(cap);
            long take = Math.min(source.getBuffer().size(), cap);
            return source.getBuffer().readByteString(take).string(StandardCharsets.UTF_8);
        }
    }

    /**
     * 文件语义：body 不超过 {@code cap} 字节则返回全文；超出则 empty（由调用方映射 FILE_TOO_LARGE）。
     * 调用方负责 null body 与 contentLength 预判。
     */
    public static Optional<String> readWithinLimit(ResponseBody body, long cap) throws IOException
    {
        try (BufferedSource source = body.source())
        {
            source.request(cap + 1L);
            long size = source.getBuffer().size();
            if (size > cap)
            {
                return Optional.empty();
            }
            return Optional.of(source.getBuffer().readByteString(size).string(StandardCharsets.UTF_8));
        }
    }
}
