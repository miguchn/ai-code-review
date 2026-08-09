package com.acr.review.domain;

/** 问题台账状态总览计数（与列表同口径 DataScope）。 */
public class ReviewIssueStats
{
    private int awaitingConfirm;
    private int awaitingFix;
    private int rechecking;
    /** 待人工处置 = awaitingConfirm + rechecking */
    private int pending;
    private int closed;

    public int getAwaitingConfirm()
    {
        return awaitingConfirm;
    }

    public void setAwaitingConfirm(int awaitingConfirm)
    {
        this.awaitingConfirm = awaitingConfirm;
    }

    public int getAwaitingFix()
    {
        return awaitingFix;
    }

    public void setAwaitingFix(int awaitingFix)
    {
        this.awaitingFix = awaitingFix;
    }

    public int getRechecking()
    {
        return rechecking;
    }

    public void setRechecking(int rechecking)
    {
        this.rechecking = rechecking;
    }

    public int getPending()
    {
        return pending;
    }

    public void setPending(int pending)
    {
        this.pending = pending;
    }

    public int getClosed()
    {
        return closed;
    }

    public void setClosed(int closed)
    {
        this.closed = closed;
    }
}
