package com.acr.review.domain.result;

/** 单一评分维度。 */
public class ReviewScoreDimension
{
    private String dimension;
    private Integer score;
    private Integer maxScore;
    private String reason;

    public String getDimension()
    {
        return dimension;
    }

    public void setDimension(String dimension)
    {
        this.dimension = dimension;
    }

    public Integer getScore()
    {
        return score;
    }

    public void setScore(Integer score)
    {
        this.score = score;
    }

    public Integer getMaxScore()
    {
        return maxScore;
    }

    public void setMaxScore(Integer maxScore)
    {
        this.maxScore = maxScore;
    }

    public String getReason()
    {
        return reason;
    }

    public void setReason(String reason)
    {
        this.reason = reason;
    }
}
