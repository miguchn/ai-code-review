package com.acr.review.git.gitlab;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.acr.review.domain.ReviewPipelineConstants;
import com.acr.review.git.GitAccessContext;
import com.acr.review.git.GitFileContentFetcher;
import com.acr.review.git.GitFileContentResult;
import com.acr.review.git.GitRepositoryCoordinates;
import com.acr.review.git.HttpResponseBodies;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/** 通过 GitLab Repository Files API 拉取单文件全文。 */
@Component
public class GitLabFileContentFetcher implements GitFileContentFetcher
{
    private static final java.util.regex.Pattern SHA_PATTERN = java.util.regex.Pattern.compile("^[0-9a-fA-F]{4,64}$");

    private final OkHttpClient client;

    public GitLabFileContentFetcher(
        @Value("${review.gitlab.connect-timeout-ms:5000}") int connectTimeoutMs,
        @Value("${review.gitlab.read-timeout-ms:30000}") int readTimeoutMs)
    {
        this.client = new OkHttpClient.Builder()
            .connectTimeout(connectTimeoutMs, TimeUnit.MILLISECONDS)
            .readTimeout(readTimeoutMs, TimeUnit.MILLISECONDS)
            .callTimeout(readTimeoutMs, TimeUnit.MILLISECONDS)
            .build();
    }

    @Override
    public String providerCode()
    {
        return "GITLAB";
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

        String encodedPath = URLEncoder.encode(path, StandardCharsets.UTF_8).replace("+", "%20");
        HttpUrl url = GitLabProvider.projectUrl(access, repository.fullPath(),
                "repository/files/" + encodedPath + "/raw")
            .newBuilder()
            .addQueryParameter("ref", ref)
            .build();
        Request request = new Request.Builder()
            .url(url)
            .get()
            .header("PRIVATE-TOKEN", token)
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
            if (status == 429)
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

    private static boolean isValidPath(String path)
    {
        return path != null && !path.isBlank() && !path.startsWith("/") && !path.contains("..");
    }

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
        return HttpResponseBodies.readWithinLimit(body, ReviewPipelineConstants.MAX_EXPANDED_FILE_BYTES)
            .map(GitFileContentResult::ok)
            .orElseGet(() -> GitFileContentResult.fail("FILE_TOO_LARGE"));
    }
}
