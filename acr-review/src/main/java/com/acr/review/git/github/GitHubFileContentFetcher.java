package com.acr.review.git.github;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.acr.review.domain.ReviewPipelineConstants;
import com.acr.review.git.GitAccessContext;
import com.acr.review.git.GitFileContentFetcher;
import com.acr.review.git.GitFileContentResult;
import com.acr.review.git.GitRepositoryCoordinates;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * 通过 GitHub Contents API 拉取单文件全文（raw media type，免 base64 解码）。
 * 用于 M3.2 高影响扩展：签名/安全/配置/依赖/数据库脚本类文件在 head SHA 上的完整内容。
 * 响应体按 MAX_EXPANDED_FILE_BYTES 有界读取，超限按 FILE_TOO_LARGE 降级。
 */
@Component
public class GitHubFileContentFetcher implements GitFileContentFetcher
{
    private static final java.util.regex.Pattern SHA_PATTERN = java.util.regex.Pattern.compile("^[0-9a-fA-F]{4,64}$");

    private final HttpUrl apiBaseUrl;
    private final OkHttpClient client;

    public GitHubFileContentFetcher(
        @Value("${review.github.api-url:https://api.github.com}") String apiBaseUrl,
        @Value("${review.github.connect-timeout-ms:5000}") int connectTimeoutMs,
        @Value("${review.github.read-timeout-ms:30000}") int readTimeoutMs)
    {
        HttpUrl parsed = HttpUrl.parse(apiBaseUrl);
        if (parsed == null)
        {
            throw new IllegalArgumentException("GitHub API 地址配置无效");
        }
        this.apiBaseUrl = parsed;
        this.client = new OkHttpClient.Builder()
            .connectTimeout(connectTimeoutMs, TimeUnit.MILLISECONDS)
            .readTimeout(readTimeoutMs, TimeUnit.MILLISECONDS)
            .callTimeout(readTimeoutMs, TimeUnit.MILLISECONDS)
            .build();
    }

    @Override
    public String providerCode()
    {
        return "GITHUB";
    }

    @Override
    public GitFileContentResult fetchFileContent(GitRepositoryCoordinates repository, GitAccessContext access,
                                                 String path, String ref)
    {
        String token;
        try
        {
            token = access.requireToken();
        }
        catch (IllegalArgumentException ex)
        {
            return GitFileContentResult.fail("CREDENTIAL");
        }
        if (repository == null)
        {
            return GitFileContentResult.fail("CREDENTIAL");
        }
        if (!isValidPath(path))
        {
            return GitFileContentResult.fail("INVALID_PATH");
        }
        if (ref == null || !SHA_PATTERN.matcher(ref).matches())
        {
            return GitFileContentResult.fail("INVALID_REF");
        }

        HttpUrl.Builder urlBuilder = apiBaseUrl.newBuilder()
            .addPathSegment("repos")
            .addPathSegment(repository.owner())
            .addPathSegment(repository.repository())
            .addPathSegment("contents");
        for (String segment : path.split("/"))
        {
            urlBuilder.addPathSegment(segment);
        }
        HttpUrl url = urlBuilder.addQueryParameter("ref", ref).build();
        Request request = new Request.Builder()
            .url(url)
            .get()
            .header("Authorization", "Bearer " + token)
            .header("Accept", "application/vnd.github.v3.raw")
            .header("User-Agent", "ai-code-review")
            .build();

        try (Response response = client.newCall(request).execute())
        {
            int status = response.code();
            if (status == 200)
            {
                return readBodyCapped(response);
            }
            if (status == 401)
            {
                return GitFileContentResult.fail("CREDENTIAL");
            }
            if (isRateLimited(response))
            {
                return GitFileContentResult.fail("RATE_LIMIT");
            }
            if (status == 404)
            {
                return GitFileContentResult.fail("NOT_FOUND");
            }
            return GitFileContentResult.fail("HTTP_" + status);
        }
        catch (InterruptedIOException ex)
        {
            Thread.currentThread().interrupt();
            return GitFileContentResult.fail("TIMEOUT");
        }
        catch (IOException ex)
        {
            return GitFileContentResult.fail("IO");
        }
    }

    /** 仓库相对路径防御：拒绝绝对路径与父目录穿越。 */
    private static boolean isValidPath(String path)
    {
        return path != null && !path.isBlank() && !path.startsWith("/") && !path.contains("..");
    }

    private static boolean isRateLimited(Response response)
    {
        if (response.code() == 429)
        {
            return true;
        }
        return response.code() == 403 && "0".equals(response.header("X-RateLimit-Remaining"));
    }

    /** 有界读取：超出上限按 FILE_TOO_LARGE 处理（响应声明或实际读取超限均拦截）。 */
    private static GitFileContentResult readBodyCapped(Response response) throws IOException
    {
        okhttp3.ResponseBody body = response.body();
        if (body == null)
        {
            return GitFileContentResult.fail("EMPTY");
        }
        if (body.contentLength() > ReviewPipelineConstants.MAX_EXPANDED_FILE_BYTES)
        {
            return GitFileContentResult.fail("FILE_TOO_LARGE");
        }
        try (okio.BufferedSource source = body.source())
        {
            okio.Buffer buffer = new okio.Buffer();
            long read = source.read(buffer, ReviewPipelineConstants.MAX_EXPANDED_FILE_BYTES + 1L);
            if (read > ReviewPipelineConstants.MAX_EXPANDED_FILE_BYTES || !source.exhausted())
            {
                return GitFileContentResult.fail("FILE_TOO_LARGE");
            }
            return GitFileContentResult.ok(buffer.readString(java.nio.charset.StandardCharsets.UTF_8));
        }
    }
}
