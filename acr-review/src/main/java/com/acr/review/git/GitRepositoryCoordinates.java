package com.acr.review.git;

/** 经过 Provider 校验和规范化的仓库标识。匹配与唯一约束只使用 fullPath。 */
public record GitRepositoryCoordinates(String owner, String repository, String fullPath, String canonicalUrl)
{
    public GitRepositoryCoordinates
    {
        if (owner == null || owner.isBlank())
        {
            throw new IllegalArgumentException("仓库命名空间不能为空");
        }
        if (repository == null || repository.isBlank())
        {
            throw new IllegalArgumentException("仓库名不能为空");
        }
        if (fullPath == null || fullPath.isBlank())
        {
            throw new IllegalArgumentException("仓库全路径不能为空");
        }
        if (canonicalUrl == null || canonicalUrl.isBlank())
        {
            throw new IllegalArgumentException("仓库地址不能为空");
        }
    }

    /** 兼容两段式构造：fullPath = owner/repository。 */
    public GitRepositoryCoordinates(String owner, String repository, String canonicalUrl)
    {
        this(owner, repository, owner + "/" + repository, canonicalUrl);
    }
}
