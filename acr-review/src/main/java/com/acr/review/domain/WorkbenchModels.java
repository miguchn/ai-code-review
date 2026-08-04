package com.acr.review.domain;

import java.util.ArrayList;
import java.util.List;

/** 工作台模型运行健康摘要（登录可调；仅启用模型，字段白名单脱敏）。 */
public class WorkbenchModels
{
    private int enabledCount;
    /** 启用且最近检测成功（checkStatus=SUCCESS）的模型数。 */
    private int onlineCount;
    /** 启用集合中最近一次检测时间（yyyy-MM-dd HH:mm），无则 null。 */
    private String lastCheckTime;
    private List<WorkbenchModelItem> items = new ArrayList<>();

    public int getEnabledCount()
    {
        return enabledCount;
    }

    public void setEnabledCount(int enabledCount)
    {
        this.enabledCount = enabledCount;
    }

    public int getOnlineCount()
    {
        return onlineCount;
    }

    public void setOnlineCount(int onlineCount)
    {
        this.onlineCount = onlineCount;
    }

    public String getLastCheckTime()
    {
        return lastCheckTime;
    }

    public void setLastCheckTime(String lastCheckTime)
    {
        this.lastCheckTime = lastCheckTime;
    }

    public List<WorkbenchModelItem> getItems()
    {
        return items;
    }

    public void setItems(List<WorkbenchModelItem> items)
    {
        this.items = items;
    }
}
