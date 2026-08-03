package com.acr.review.domain;

import java.util.ArrayList;
import java.util.List;

/** 工作台汇总响应体。 */
public class WorkbenchSummary
{
    private WorkbenchScope scope = new WorkbenchScope();
    private List<WorkbenchCard> cards = new ArrayList<>();
    private WorkbenchToday today = new WorkbenchToday();
    private List<WorkbenchRecentItem> recent = new ArrayList<>();

    public WorkbenchScope getScope()
    {
        return scope;
    }

    public void setScope(WorkbenchScope scope)
    {
        this.scope = scope;
    }

    public List<WorkbenchCard> getCards()
    {
        return cards;
    }

    public void setCards(List<WorkbenchCard> cards)
    {
        this.cards = cards;
    }

    public WorkbenchToday getToday()
    {
        return today;
    }

    public void setToday(WorkbenchToday today)
    {
        this.today = today;
    }

    public List<WorkbenchRecentItem> getRecent()
    {
        return recent;
    }

    public void setRecent(List<WorkbenchRecentItem> recent)
    {
        this.recent = recent;
    }
}
