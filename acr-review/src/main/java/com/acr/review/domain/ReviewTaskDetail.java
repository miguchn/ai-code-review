package com.acr.review.domain;

import java.util.List;

/** 审查任务详情：当前任务摘要 + 执行历史。 */
public class ReviewTaskDetail
{
    private ReviewTask task;
    private List<ReviewTaskRun> runs;

    public ReviewTaskDetail()
    {
    }

    public ReviewTaskDetail(ReviewTask task, List<ReviewTaskRun> runs)
    {
        this.task = task;
        this.runs = runs;
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
}
