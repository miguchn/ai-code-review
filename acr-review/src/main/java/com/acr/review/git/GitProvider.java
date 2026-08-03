package com.acr.review.git;

/** 当前项目接入功能所需的最小 Git Provider 契约。 */
public interface GitProvider
{
    String providerCode();

    /**
     * 解析仓库地址。自建实例时使用 access.serverUrl 校验 host 一致性。
     * GitHub/Gitee 可忽略 serverUrl 差异，仅使用官方 host。
     */
    GitRepositoryCoordinates parseRepository(String repositoryUrl, GitAccessContext access);

    /** 兼容仅传 URL 的调用：使用平台默认 serverUrl（无默认则失败）。 */
    default GitRepositoryCoordinates parseRepository(String repositoryUrl)
    {
        String server = GitProviderCodes.defaultServerUrl(providerCode());
        if (server == null)
        {
            throw new IllegalArgumentException(providerCode() + " 解析仓库地址需要服务地址");
        }
        return parseRepository(repositoryUrl, GitAccessContext.forParse(server));
    }

    GitConnectionResult testCredential(GitAccessContext access);

    GitConnectionResult testRepository(GitRepositoryCoordinates repository, GitAccessContext access);

    GitRepositoryInfoResult readRepository(GitRepositoryCoordinates repository, GitAccessContext access);
}
