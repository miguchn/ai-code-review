package com.acr.review.domain.result;

/**
 * 范围统计（协议 v1.1 scopeStats）。
 * 由后端在范围决策与归属打标后注入，模型无需输出；决策降级时整体缺省。
 */
public class ReviewScopeStats
{
    /** 纳入审查的文件数（scoped diff 中的文件）。 */
    private Integer includedFiles;
    /** 被排除的文件数（平台默认 + 项目规则 + 测试文件）。 */
    private Integer excludedFiles;
    /** 高影响扩展文件数。 */
    private Integer expandedFiles;
    /** scoped diff 是否发生预算截断。 */
    private Boolean truncated;
    /** 归属判定为本次变更引入的问题数（含不可判定按 NEW 计的部分）。 */
    private Integer newCount;
    /** 归属判定为存量的问题数。 */
    private Integer existingCount;
    /** 无法判定归属（文件不在 Diff 或行号缺失）按 NEW 计的问题数。 */
    private Integer originUnverifiable;

    public Integer getIncludedFiles()
    {
        return includedFiles;
    }

    public void setIncludedFiles(Integer includedFiles)
    {
        this.includedFiles = includedFiles;
    }

    public Integer getExcludedFiles()
    {
        return excludedFiles;
    }

    public void setExcludedFiles(Integer excludedFiles)
    {
        this.excludedFiles = excludedFiles;
    }

    public Integer getExpandedFiles()
    {
        return expandedFiles;
    }

    public void setExpandedFiles(Integer expandedFiles)
    {
        this.expandedFiles = expandedFiles;
    }

    public Boolean getTruncated()
    {
        return truncated;
    }

    public void setTruncated(Boolean truncated)
    {
        this.truncated = truncated;
    }

    public Integer getNewCount()
    {
        return newCount;
    }

    public void setNewCount(Integer newCount)
    {
        this.newCount = newCount;
    }

    public Integer getExistingCount()
    {
        return existingCount;
    }

    public void setExistingCount(Integer existingCount)
    {
        this.existingCount = existingCount;
    }

    public Integer getOriginUnverifiable()
    {
        return originUnverifiable;
    }

    public void setOriginUnverifiable(Integer originUnverifiable)
    {
        this.originUnverifiable = originUnverifiable;
    }
}
