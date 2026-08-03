package com.acr.review.domain;

import java.util.List;

/** 审查任务详情：当前任务摘要 + 执行历史 + 投递摘要。 */
public class ReviewTaskDetail
{
    private ReviewTask task;
    private List<ReviewTaskRun> runs;
    /** PR 总结评论投递状态；无记录时为 null。 */
    private ReviewDeliveryRecord delivery;

    public ReviewTaskDetail()
    {
    }

    public ReviewTaskDetail(ReviewTask task, List<ReviewTaskRun> runs)
    {
        this(task, runs, null);
    }

    public ReviewTaskDetail(ReviewTask task, List<ReviewTaskRun> runs, ReviewDeliveryRecord delivery)
    {
        this.task = task;
        this.runs = runs;
        this.delivery = delivery;
    }

    public ReviewTask getTask()
    {
        return task;
    }

    public void setTask(ReviewTask task)
    {
        this.task = task;
    }

    public List<ReviewTaskRun> getRuns()
    {
        return runs;
    }

    public void setRuns(List<ReviewTaskRun> runs)
    {
        this.runs = runs;
    }

    public ReviewDeliveryRecord getDelivery()
    {
        return delivery;
    }

    public void setDelivery(ReviewDeliveryRecord delivery)
    {
        this.delivery = delivery;
    }
}
