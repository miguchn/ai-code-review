package com.acr.review.insight;

import java.util.Date;

/** review_commit_fact 行。 */
public class ReviewCommitFact
{
    private Long id;
    private Long projectId;
    private String commitSha;
    private Date commitTime;
    private String authorName;
    private String authorEmail;
    private String messageFirstLine;
    private Long sourceEventId;
    private Date createTime;

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public Long getProjectId()
    {
        return projectId;
    }

    public void setProjectId(Long projectId)
    {
        this.projectId = projectId;
    }

    public String getCommitSha()
    {
        return commitSha;
    }

    public void setCommitSha(String commitSha)
    {
        this.commitSha = commitSha;
    }

    public Date getCommitTime()
    {
        return commitTime;
    }

    public void setCommitTime(Date commitTime)
    {
        this.commitTime = commitTime;
    }

    public String getAuthorName()
    {
        return authorName;
    }

    public void setAuthorName(String authorName)
    {
        this.authorName = authorName;
    }

    public String getAuthorEmail()
    {
        return authorEmail;
    }

    public void setAuthorEmail(String authorEmail)
    {
        this.authorEmail = authorEmail;
    }

    public String getMessageFirstLine()
    {
        return messageFirstLine;
    }

    public void setMessageFirstLine(String messageFirstLine)
    {
        this.messageFirstLine = messageFirstLine;
    }

    public Long getSourceEventId()
    {
        return sourceEventId;
    }

    public void setSourceEventId(Long sourceEventId)
    {
        this.sourceEventId = sourceEventId;
    }

    public Date getCreateTime()
    {
        return createTime;
    }

    public void setCreateTime(Date createTime)
    {
        this.createTime = createTime;
    }
}
