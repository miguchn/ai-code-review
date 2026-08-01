package com.acr.review.git;

/** 经过 Provider 校验和规范化的仓库标识。 */
public record GitRepositoryCoordinates(String owner, String repository, String canonicalUrl)
{
}
