package com.acr.review.domain;

import com.acr.common.exception.ServiceException;

/** 问题转派请求。 */
public class ReviewIssueTransferRequest
{
    private Long assigneeUserId;
    private String note;

    public Long getAssigneeUserId()
    {
        return assigneeUserId;
    }

    public void setAssigneeUserId(Object raw)
    {
        this.assigneeUserId = parseAssigneeUserId(raw);
    }

    public String getNote()
    {
        return note;
    }

    public void setNote(String note)
    {
        this.note = note;
    }

    static Long parseAssigneeUserId(Object raw)
    {
        if (raw == null)
        {
            return null;
        }
        if (raw instanceof Number number)
        {
            return number.longValue();
        }
        String text = String.valueOf(raw).trim();
        if (text.isEmpty())
        {
            return null;
        }
        try
        {
            return Long.valueOf(text);
        }
        catch (NumberFormatException ex)
        {
            throw new ServiceException("责任人 ID 无效");
        }
    }
}
