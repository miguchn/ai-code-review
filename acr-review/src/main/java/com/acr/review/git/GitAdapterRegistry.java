package com.acr.review.git;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;
import com.acr.common.exception.ServiceException;

/** 按 providerCode 解析 Git 适配实现，避免业务直接注入单一平台 Bean。 */
@Component
public class GitAdapterRegistry
{
    private final Map<String, GitProvider> providers = new HashMap<>();
    private final Map<String, GitWebhookAdapter> webhookAdapters = new HashMap<>();
    private final Map<String, GitPullRequestDiffFetcher> diffFetchers = new HashMap<>();
    private final Map<String, GitPullRequestMetadataFetcher> metadataFetchers = new HashMap<>();
    private final Map<String, GitFileContentFetcher> fileContentFetchers = new HashMap<>();
    private final Map<String, GitPullRequestWorkspacePreparer> workspacePreparers = new HashMap<>();
    private final Map<String, GitPullRequestCommentClient> commentClients = new HashMap<>();

    public GitAdapterRegistry(List<GitProvider> providers,
                              List<GitWebhookAdapter> webhookAdapters,
                              List<GitPullRequestDiffFetcher> diffFetchers,
                              List<GitPullRequestMetadataFetcher> metadataFetchers,
                              List<GitFileContentFetcher> fileContentFetchers,
                              List<GitPullRequestWorkspacePreparer> workspacePreparers,
                              List<GitPullRequestCommentClient> commentClients)
    {
        index(providers, this.providers, "GitProvider");
        index(webhookAdapters, this.webhookAdapters, "GitWebhookAdapter");
        index(diffFetchers, this.diffFetchers, "GitPullRequestDiffFetcher");
        index(metadataFetchers, this.metadataFetchers, "GitPullRequestMetadataFetcher");
        index(fileContentFetchers, this.fileContentFetchers, "GitFileContentFetcher");
        index(workspacePreparers, this.workspacePreparers, "GitPullRequestWorkspacePreparer");
        index(commentClients, this.commentClients, "GitPullRequestCommentClient");
    }

    public GitProvider requireProvider(String providerCode)
    {
        return require(providers, providerCode, "GitProvider");
    }

    public GitWebhookAdapter requireWebhookAdapter(String providerCode)
    {
        return require(webhookAdapters, providerCode, "GitWebhookAdapter");
    }

    public GitPullRequestDiffFetcher requireDiffFetcher(String providerCode)
    {
        return require(diffFetchers, providerCode, "GitPullRequestDiffFetcher");
    }

    public GitPullRequestMetadataFetcher requireMetadataFetcher(String providerCode)
    {
        return require(metadataFetchers, providerCode, "GitPullRequestMetadataFetcher");
    }

    public GitFileContentFetcher requireFileContentFetcher(String providerCode)
    {
        return require(fileContentFetchers, providerCode, "GitFileContentFetcher");
    }

    public GitPullRequestWorkspacePreparer requireWorkspacePreparer(String providerCode)
    {
        return require(workspacePreparers, providerCode, "GitPullRequestWorkspacePreparer");
    }

    public GitPullRequestCommentClient requireCommentClient(String providerCode)
    {
        return require(commentClients, providerCode, "GitPullRequestCommentClient");
    }

    private static <T> void index(List<T> beans, Map<String, T> target, String label)
    {
        if (beans == null)
        {
            return;
        }
        for (T bean : beans)
        {
            String code = providerCodeOf(bean);
            if (code == null || code.isBlank())
            {
                throw new IllegalStateException(label + " 缺少 providerCode");
            }
            String key = code.toUpperCase(Locale.ROOT);
            if (target.containsKey(key))
            {
                throw new IllegalStateException("重复的 " + label + ": " + key);
            }
            target.put(key, bean);
        }
    }

    private static String providerCodeOf(Object bean)
    {
        if (bean instanceof GitProvider provider)
        {
            return provider.providerCode();
        }
        if (bean instanceof GitWebhookAdapter adapter)
        {
            return adapter.providerCode();
        }
        if (bean instanceof GitPullRequestDiffFetcher fetcher)
        {
            return fetcher.providerCode();
        }
        if (bean instanceof GitPullRequestMetadataFetcher fetcher)
        {
            return fetcher.providerCode();
        }
        if (bean instanceof GitFileContentFetcher fetcher)
        {
            return fetcher.providerCode();
        }
        if (bean instanceof GitPullRequestWorkspacePreparer preparer)
        {
            return preparer.providerCode();
        }
        if (bean instanceof GitPullRequestCommentClient client)
        {
            return client.providerCode();
        }
        return null;
    }

    private static <T> T require(Map<String, T> map, String providerCode, String label)
    {
        if (providerCode == null || providerCode.isBlank())
        {
            throw new ServiceException("Git Provider 不能为空");
        }
        T bean = map.get(providerCode.toUpperCase(Locale.ROOT));
        if (bean == null)
        {
            throw new ServiceException("暂不支持的 Git Provider：" + providerCode + "（缺少 " + label + "）");
        }
        return bean;
    }
}
