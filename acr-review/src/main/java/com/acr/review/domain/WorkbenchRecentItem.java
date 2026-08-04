package com.acr.review.domain;

/** 工作台最近动态条目。 */
public class WorkbenchRecentItem
{
    private String type;
    private String title;
    private String time;
    private String link;
    /** 审查结论（PASS/WARN/BLOCK）或伪枚举 FAILED；执行中/待执行等为 null。 */
    private String conclusion;

    public WorkbenchRecentItem()
    {
    }

    public WorkbenchRecentItem(String type, String title, String time, String link)
    {
        this(type, title, time, link, null);
    }

    public WorkbenchRecentItem(String type, String title, String time, String link, String conclusion)
    {
        this.type = type;
        this.title = title;
        this.time = time;
        this.link = link;
        this.conclusion = conclusion;
    }

    public String getType()
    {
        return type;
    }

    public void setType(String type)
    {
        this.type = type;
    }

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public String getTime()
    {
        return time;
    }

    public void setTime(String time)
    {
        this.time = time;
    }

    public String getLink()
    {
        return link;
    }

    public void setLink(String link)
    {
        this.link = link;
    }

    public String getConclusion()
    {
        return conclusion;
    }

    public void setConclusion(String conclusion)
    {
        this.conclusion = conclusion;
    }
}
