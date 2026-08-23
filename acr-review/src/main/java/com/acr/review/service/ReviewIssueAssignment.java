package com.acr.review.service;

/** 一次自动指派的解析结果。 */
public final class ReviewIssueAssignment
{
    private final Long userId;
    private final String source;
    private final String note;

    public ReviewIssueAssignment(Long userId, String source, String note)
    {
        this.userId = userId;
        this.source = source;
        this.note = note;
    }

    public Long getUserId()
    {
        return userId;
    }

    public String getSource()
    {
        return source;
    }

    public String getNote()
    {
        return note;
    }
}
