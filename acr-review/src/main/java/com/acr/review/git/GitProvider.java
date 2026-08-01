package com.acr.review.git;

/** 当前项目接入功能所需的最小 Git Provider 契约。 */
public interface GitProvider
{
    String providerCode();

    GitRepositoryCoordinates parseRepository(String repositoryUrl);

    GitConnectionResult testCredential(String token);

    GitConnectionResult testRepository(GitRepositoryCoordinates repository, String token);

    GitRepositoryInfoResult readRepository(GitRepositoryCoordinates repository, String token);
}
