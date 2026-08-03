package com.acr.review.domain;

import java.util.LinkedHashMap;
import java.util.Map;

/** 工作台待办卡片。 */
public class WorkbenchCard
{
    private String type;
    private String title;
    private int count;
    private String link;
    private Map<String, String> query = new LinkedHashMap<>();

    public WorkbenchCard()
    {
    }

    public WorkbenchCard(String type, String title, int count, String link, Map<String, String> query)
    {
        this.type = type;
        this.title = title;
        this.count = count;
        this.link = link;
        if (query != null)
        {
            this.query = query;
        }
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

    public int getCount()
    {
        return count;
    }

    public void setCount(int count)
    {
        this.count = count;
    }

    public String getLink()
    {
        return link;
    }

    public void setLink(String link)
    {
        this.link = link;
    }

    public Map<String, String> getQuery()
    {
        return query;
    }

    public void setQuery(Map<String, String> query)
    {
        this.query = query;
    }
}
