package com.acr.web.controller.review;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.acr.common.annotation.Log;
import com.acr.common.core.controller.BaseController;
import com.acr.common.core.domain.AjaxResult;
import com.acr.common.core.page.TableDataInfo;
import com.acr.common.enums.BusinessType;
import com.acr.review.domain.ReviewTask;
import com.acr.review.service.IReviewTaskService;

/** 审查任务 REST 接入。 */
@RestController
@RequestMapping("/review/task")
public class ReviewTaskController extends BaseController
{
    private final IReviewTaskService taskService;

    public ReviewTaskController(IReviewTaskService taskService)
    {
        this.taskService = taskService;
    }

    @PreAuthorize("@ss.hasPermi('review:task:list')")
    @GetMapping("/list")
    public TableDataInfo list(ReviewTask task)
    {
        startPage();
        List<ReviewTask> list = taskService.selectReviewTaskList(task);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('review:task:query')")
    @GetMapping("/{taskId}")
    public AjaxResult getInfo(@PathVariable Long taskId)
    {
        return success(taskService.selectReviewTaskDetail(taskId));
    }

    @PreAuthorize("@ss.hasPermi('review:task:retry')")
    @Log(title = "审查任务", businessType = BusinessType.UPDATE)
    @PostMapping("/{taskId}/retry")
    public AjaxResult retry(@PathVariable Long taskId)
    {
        taskService.retryTask(taskId);
        return success("已提交执行");
    }
}
