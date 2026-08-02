package com.acr.review.domain.result;

/** Top 重点问题（最多 3 条）。 */
public class ReviewTopIssue
{
    private Integer rank;
    private String severity;
    private String category;
    private String title;
    private String description;
    private String filePath;
    private Integer startLine;
    private Integer endLine;
    private String evidence;
    private String suggestion;
    /** 归属（协议 v1.1）：NEW=本次变更引入，EXISTING=存量。由后端行号映射覆写，v1.0 结果缺省视为 NEW。 */
    private String origin;

    public Integer getRank()
    {
        return rank;
    }

    public void setRank(Integer rank)
    {
        this.rank = rank;
    }

    public String getSeverity()
    {
        return severity;
    }

    public void setSeverity(String severity)
    {
        this.severity = severity;
    }

    public String getCategory()
    {
        return category;
    }

    public void setCategory(String category)
    {
        this.category = category;
    }

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public String getFilePath()
    {
        return filePath;
    }

    public void setFilePath(String filePath)
    {
        this.filePath = filePath;
    }

    public Integer getStartLine()
    {
        return startLine;
    }

    public void setStartLine(Integer startLine)
    {
        this.startLine = startLine;
    }

    public Integer getEndLine()
    {
        return endLine;
    }

    public void setEndLine(Integer endLine)
    {
        this.endLine = endLine;
    }

    public String getEvidence()
    {
        return evidence;
    }

    public void setEvidence(String evidence)
    {
        this.evidence = evidence;
    }

    public String getSuggestion()
    {
        return suggestion;
    }

    public void setSuggestion(String suggestion)
    {
        this.suggestion = suggestion;
    }

    public String getOrigin()
    {
        return origin;
    }

    public void setOrigin(String origin)
    {
        this.origin = origin;
    }
}
