package com.acr.review.git.gitlab;

import java.io.InterruptedIOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.acr.review.git.GitAccessContext;
import com.acr.review.git.GitPullRequestBaseRefResolver;
import com.acr.review.git.GitRepositoryCoordinates;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import com.acr.review.git.GitHttpSupport;

/**
 * 经 GitLab MR API 的 diff_refs 补全审查基线。
 *
 * GitLab 19.x Webhook 载荷已移除 diff_refs / target_branch_sha，
 * 但 GET /projects/:id/merge_requests/:iid 仍返回 diff_refs.base_sha。
 * 注意 GitLab 在 MR 创建后异步计算 diff_refs，open 事件触达时可能尚未就绪，故短重试。
 */
@Component
public class GitLabPullRequestBaseRefResolver implements GitPullRequestBaseRefResolver
{
    private static final Logger log = LoggerFactory.getLogger(GitLabPullRequestBaseRefResolver.class);

    /** diff_refs 异步就绪窗口内的小重试（次 × 毫秒）。 */
    private static final int MAX_ATTEMPTS = 3;
    private static final long RETRY_DELAY_MS = 1000L;

    private final OkHttpClient client;

    public GitLabPullRequestBaseRefResolver(
        @Value("${review.gitlab.connect-timeout-ms:5000}") int connectTimeoutMs,
        @Value("${review.gitlab.read-timeout-ms:30000}") int readTimeoutMs)
    {
        this.client = GitHttpSupport.clientBuilder(connectTimeoutMs, readTimeoutMs).build();
    }

    @Override
    public String providerCode()
    {
        return "GITLAB";
    }

    @Override
    public String resolveBaseRef(GitRepositoryCoordinates repository, GitAccessContext access, int prNumber)
    {
        if (repository == null || prNumber <= 0)
        {
            return null;
        }
        String token;
        try
        {
            token = access.requireToken();
        }
        catch (IllegalArgumentException ex)
        {
            log.warn("GitLab 凭据不可用，无法补全 base SHA, repo={}", repository.fullPath());
            return null;
        }
        HttpUrl url = GitLabProvider.projectUrl(access, repository.fullPath(), "merge_requests/" + prNumber);
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++)
        {
            try (Response response = client.newCall(buildRequest(url, token)).execute())
            {
                if (response.code() == 401 || response.code() == 403)
                {
                    log.warn("GitLab Token 权限不足({}), 无法补全 base SHA, repo={}, mr=!{}",
                        response.code(), repository.fullPath(), prNumber);
                    return null;
                }
                if (!response.isSuccessful() || response.body() == null)
                {
                    log.warn("GitLab MR API 非 2xx({}), 补全 base SHA 失败, repo={}, mr=!{}, attempt={}/{}",
                        response.code(), repository.fullPath(), prNumber, attempt, MAX_ATTEMPTS);
                }
                else
                {
                    JSONObject body = JSON.parseObject(response.body().string());
                    JSONObject diffRefs = body == null ? null : body.getJSONObject("diff_refs");
                    String baseSha = diffRefs == null ? null : diffRefs.getString("base_sha");
                    if (baseSha != null && !baseSha.isBlank())
                    {
                        return baseSha;
                    }
                    // diff_refs 尚未就绪（GitLab 异步计算），短暂等待后重试
                    log.info("GitLab diff_refs 未就绪, repo={}, mr=!{}, attempt={}/{}",
                        repository.fullPath(), prNumber, attempt, MAX_ATTEMPTS);
                }
            }
            catch (InterruptedIOException ex)
            {
                Thread.currentThread().interrupt();
                log.warn("GitLab MR API 调用被中断, repo={}, mr=!{}", repository.fullPath(), prNumber);
                return null;
            }
            catch (Exception ex)
            {
                log.warn("GitLab MR API 调用异常, repo={}, mr=!{}, attempt={}/{}",
                    repository.fullPath(), prNumber, attempt, MAX_ATTEMPTS, ex);
            }
            if (attempt < MAX_ATTEMPTS)
            {
                try
                {
                    Thread.sleep(RETRY_DELAY_MS);
                }
                catch (InterruptedException ie)
                {
                    Thread.currentThread().interrupt();
                    return null;
                }
            }
        }
        return null;
    }

    private static Request buildRequest(HttpUrl url, String token)
    {
        return new Request.Builder()
            .url(url)
            .get()
            .header("PRIVATE-TOKEN", token)
            .header("User-Agent", "ai-code-review")
            .build();
    }
}
