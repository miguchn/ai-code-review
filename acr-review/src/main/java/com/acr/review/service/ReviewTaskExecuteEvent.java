package com.acr.review.service;

import org.springframework.context.ApplicationEvent;

/** 审查任务可执行事件（事务提交后再异步消费）。 */
public class ReviewTaskExecuteEvent extends ApplicationEvent
{
    private final Long taskId;

    public ReviewTaskExecuteEvent(Object source, Long taskId)
    {
        super(source);
        this.taskId = taskId;
    }

    public Long getTaskId()
    {
        return taskId;
    }
}
