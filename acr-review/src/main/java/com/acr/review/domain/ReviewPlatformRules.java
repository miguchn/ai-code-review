package com.acr.review.domain;

import java.util.ArrayList;
import java.util.List;

/** 模板管理页展示用的平台统一审查规则（不含 JSON Schema 等内部协议细节）。 */
public class ReviewPlatformRules
{
    private String protocolVersion;
    private String title;
    private String uiHint;
    private int totalMaxScore;
    private int topIssuesMax;
    private String topIssuesHint;
    private List<Dimension> dimensions = new ArrayList<>();

    public String getProtocolVersion()
    {
        return protocolVersion;
    }

    public void setProtocolVersion(String protocolVersion)
    {
        this.protocolVersion = protocolVersion;
    }

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public String getUiHint()
    {
        return uiHint;
    }

    public void setUiHint(String uiHint)
    {
        this.uiHint = uiHint;
    }

    public int getTotalMaxScore()
    {
        return totalMaxScore;
    }

    public void setTotalMaxScore(int totalMaxScore)
    {
        this.totalMaxScore = totalMaxScore;
    }

    public int getTopIssuesMax()
    {
        return topIssuesMax;
    }

    public void setTopIssuesMax(int topIssuesMax)
    {
        this.topIssuesMax = topIssuesMax;
    }

    public String getTopIssuesHint()
    {
        return topIssuesHint;
    }

    public void setTopIssuesHint(String topIssuesHint)
    {
        this.topIssuesHint = topIssuesHint;
    }

    public List<Dimension> getDimensions()
    {
        return dimensions;
    }

    public void setDimensions(List<Dimension> dimensions)
    {
        this.dimensions = dimensions;
    }

    public static class Dimension
    {
        private String code;
        private String name;
        private int maxScore;
        private String description;

        public Dimension()
        {
        }

        public Dimension(String code, String name, int maxScore, String description)
        {
            this.code = code;
            this.name = name;
            this.maxScore = maxScore;
            this.description = description;
        }

        public String getCode()
        {
            return code;
        }

        public void setCode(String code)
        {
            this.code = code;
        }

        public String getName()
        {
            return name;
        }

        public void setName(String name)
        {
            this.name = name;
        }

        public int getMaxScore()
        {
            return maxScore;
        }

        public void setMaxScore(int maxScore)
        {
            this.maxScore = maxScore;
        }

        public String getDescription()
        {
            return description;
        }

        public void setDescription(String description)
        {
            this.description = description;
        }
    }
}
