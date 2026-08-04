package com.acr.review.domain;

/** 工作台审查结论趋势单日点。 */
public class WorkbenchTrendPoint
{
    private String date;
    private int pass;
    private int warn;
    private int block;
    /** 当日执行失败任务数（非审查结论，仅供 tooltip 附带）。 */
    private int failed;

    public WorkbenchTrendPoint()
    {
    }

    public WorkbenchTrendPoint(String date, int pass, int warn, int block, int failed)
    {
        this.date = date;
        this.pass = pass;
        this.warn = warn;
        this.block = block;
        this.failed = failed;
    }

    public String getDate()
    {
        return date;
    }

    public void setDate(String date)
    {
        this.date = date;
    }

    public int getPass()
    {
        return pass;
    }

    public void setPass(int pass)
    {
        this.pass = pass;
    }

    public int getWarn()
    {
        return warn;
    }

    public void setWarn(int warn)
    {
        this.warn = warn;
    }

    public int getBlock()
    {
        return block;
    }

    public void setBlock(int block)
    {
        this.block = block;
    }

    public int getFailed()
    {
        return failed;
    }

    public void setFailed(int failed)
    {
        this.failed = failed;
    }
}
