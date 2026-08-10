package com.acr.review.insight;

import java.util.Date;
import com.acr.common.core.domain.BaseEntity;

/** review_member_stats_daily 行。 */
public class ReviewMemberStatsDaily extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long projectId;
    private String authorKey;
    private String authorName;
    private Date statDate;
    private Integer commitCount;
    private Integer tasksReviewed;
    private Integer issuesNew;
    private Integer issuesOpen;

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

    public String getAuthorKey()
    {
        return authorKey;
    }

    public void setAuthorKey(String authorKey)
    {
        this.authorKey = authorKey;
    }

    public String getAuthorName()
    {
        return authorName;
    }

    public void setAuthorName(String authorName)
    {
        this.authorName = authorName;
    }

    public Date getStatDate()
    {
        return statDate;
    }

    public void setStatDate(Date statDate)
    {
        this.statDate = statDate;
    }

    public Integer getCommitCount()
    {
        return commitCount;
    }

    public void setCommitCount(Integer commitCount)
    {
        this.commitCount = commitCount;
    }

    public Integer getTasksReviewed()
    {
        return tasksReviewed;
    }

    public void setTasksReviewed(Integer tasksReviewed)
    {
        this.tasksReviewed = tasksReviewed;
    }

    public Integer getIssuesNew()
    {
        return issuesNew;
    }

    public void setIssuesNew(Integer issuesNew)
    {
        this.issuesNew = issuesNew;
    }

    public Integer getIssuesOpen()
    {
        return issuesOpen;
    }

    public void setIssuesOpen(Integer issuesOpen)
    {
        this.issuesOpen = issuesOpen;
    }
}
