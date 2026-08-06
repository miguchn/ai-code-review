package com.acr.review.domain;

import java.util.List;

/** 问题批量处置请求。 */
public class ReviewIssueBatchRequest
{
    /** CONFIRM / CLOSE / DISMISS */
    private String action;
    private List<Long> issueIds;
    private String resolveNote;
    /** IGNORED / FALSE_POSITIVE；仅 DISMISS 使用 */
    private String dismissType;

    public String getAction()
    {
        return action;
    }

    public void setAction(String action)
    {
        this.action = action;
    }

    public List<Long> getIssueIds()
    {
        return issueIds;
    }

    public void setIssueIds(List<Long> issueIds)
    {
        this.issueIds = issueIds;
    }

    public String getResolveNote()
    {
        return resolveNote;
    }

    public void setResolveNote(String resolveNote)
    {
        this.resolveNote = resolveNote;
    }

    public String getDismissType()
    {
        return dismissType;
    }

    public void setDismissType(String dismissType)
    {
        this.dismissType = dismissType;
    }
}
