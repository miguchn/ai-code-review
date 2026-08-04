package com.acr.review.domain.result;

/** 审查结论按天聚合结果（Mapper 行映射；列别名驼峰，未开启全局驼峰映射）。 */
public class ReviewConclusionDailyStat
{
    private String statDate;
    private int passCount;
    private int warnCount;
    private int blockCount;
    private int failedCount;

    public String getStatDate()
    {
        return statDate;
    }

    public void setStatDate(String statDate)
    {
        this.statDate = statDate;
    }

    public int getPassCount()
    {
        return passCount;
    }

    public void setPassCount(int passCount)
    {
        this.passCount = passCount;
    }

    public int getWarnCount()
    {
        return warnCount;
    }

    public void setWarnCount(int warnCount)
    {
        this.warnCount = warnCount;
    }

    public int getBlockCount()
    {
        return blockCount;
    }

    public void setBlockCount(int blockCount)
    {
        this.blockCount = blockCount;
    }

    public int getFailedCount()
    {
        return failedCount;
    }

    public void setFailedCount(int failedCount)
    {
        this.failedCount = failedCount;
    }
}
