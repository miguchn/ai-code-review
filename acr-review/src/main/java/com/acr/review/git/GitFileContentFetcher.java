package com.acr.review.git;

/** 按提交引用拉取仓库内单文件内容（M3.2 高影响扩展全文拉取）。 */
public interface GitFileContentFetcher
{
    String providerCode();

    /**
     * 拉取指定 ref 下的文件全文。
     * 返回结构化结果，永不抛异常；文件过大、路径非法、凭据失效、限流等均体现为 success=false。
     */
    GitFileContentResult fetchFileContent(GitRepositoryCoordinates repository, GitAccessContext access,
                                          String path, String ref);
}
