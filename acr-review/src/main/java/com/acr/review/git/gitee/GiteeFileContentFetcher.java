package com.acr.review.git.gitee;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.acr.review.domain.ReviewPipelineConstants;
import com.acr.review.git.GitAccessContext;
import com.acr.review.git.GitFileContentFetcher;
import com.acr.review.git.GitFileContentResult;
import com.acr.review.git.GitProviderCodes;
import com.acr.review.git.GitRepositoryCoordinates;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * 通过 Gitee Contents API 拉取单文件全文。
 * 响应体按 MAX_EXPANDED_FILE_BYTES 有界读取，超限按 FILE_TOO_LARGE 降级。
 */
@Component
public class GiteeFileContentFetcher implements GitFileContentFetcher
{
    private static final java.util.regex.Pattern SHA_PATTERN = java.util.regex.Pattern.compile("^[0-9a-fA-F]{4,64}$");

    private final HttpUrl apiBaseUrl;
    private final OkHttpClient client;

    @Autowired
    public GiteeFileContentFetcher(
        @Value("${review.gitee.server-url:https://gitee.com}") String serverUrl,
        @Value("${review.gitee.connect-timeout-ms:5000}") int connectTimeoutMs,
        @Value("${review.gitee.read-timeout-ms:30000}") int readTimeoutMs)
    {
        this(GiteeApiUrls.apiBaseFromServer(serverUrl), connectTimeoutMs, readTimeoutMs);
    }

    GiteeFileContentFetcher(HttpUrl apiBaseUrl, int connectTimeoutMs, int readTimeoutMs)
    {
        this.apiBaseUrl = apiBaseUrl;
        this.client = new OkHttpClient.Builder()
            .connectTimeout(connectTimeoutMs, TimeUnit.MILLISECONDS)
            .readTimeout(readTimeoutMs, TimeUnit.MILLISECONDS)
            .callTimeout(readTimeoutMs, TimeUnit.MILLISECONDS)
            .build();
    }

    @Override
    public String providerCode()
    {
        return GitProviderCodes.GITEE;
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
        HttpUrl url = urlBuilder
            .addQueryParameter("ref", ref)
            .addQueryParameter("access_token", token)
            .build();
        Request request = new Request.Builder()
            .url(url)
            .get()
            .header("User-Agent", "ai-code-review")
            .build();

        try (Response response = client.newCall(request).execute())
        {
            int status = response.code();
            if (status == 200)
            {
                return parseContentResponse(response);
            }
            if (status == 401)
            {
                return GitFileContentResult.fail("CREDENTIAL");
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

    private static GitFileContentResult parseContentResponse(Response response) throws IOException
    {
        String body = response.body() == null ? "" : response.body().string();
        if (body.isBlank())
        {
            return GitFileContentResult.fail("EMPTY");
        }
        try
        {
            JSONObject json = JSON.parseObject(body);
            if (json == null)
            {
                return GitFileContentResult.fail("EMPTY");
            }
            String encoding = json.getString("encoding");
            String content = json.getString("content");
            if (content == null)
            {
                return GitFileContentResult.fail("EMPTY");
            }
            if ("base64".equalsIgnoreCase(encoding))
            {
                content = content.replace("\n", "");
                byte[] decoded = Base64.getDecoder().decode(content);
                if (decoded.length > ReviewPipelineConstants.MAX_EXPANDED_FILE_BYTES)
                {
                    return GitFileContentResult.fail("FILE_TOO_LARGE");
                }
                return GitFileContentResult.ok(new String(decoded, StandardCharsets.UTF_8));
            }
            if (content.length() > ReviewPipelineConstants.MAX_EXPANDED_FILE_BYTES)
            {
                return GitFileContentResult.fail("FILE_TOO_LARGE");
            }
            return GitFileContentResult.ok(content);
        }
        catch (RuntimeException ex)
        {
            return GitFileContentResult.fail("EMPTY");
        }
    }

    private static boolean isValidPath(String path)
    {
        return path != null && !path.isBlank() && !path.startsWith("/") && !path.contains("..");
    }
}
