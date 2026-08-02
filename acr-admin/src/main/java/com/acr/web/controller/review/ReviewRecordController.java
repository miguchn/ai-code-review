package com.acr.web.controller.review;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.acr.common.core.controller.BaseController;
import com.acr.common.core.domain.AjaxResult;
import com.acr.common.core.page.TableDataInfo;
import com.acr.review.domain.ReviewTask;
import com.acr.review.service.IReviewRecordService;

/** 审查记录 REST 接入（已完成审查结果历史）。 */
@RestController
@RequestMapping("/review/record")
public class ReviewRecordController extends BaseController
{
    private final IReviewRecordService recordService;

    public ReviewRecordController(IReviewRecordService recordService)
    {
        this.recordService = recordService;
    }

    @PreAuthorize("@ss.hasPermi('review:record:list')")
    @GetMapping("/list")
    public TableDataInfo list(ReviewTask query)
    {
        startPage();
        List<ReviewTask> list = recordService.selectReviewRecordList(query);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('review:record:query')")
    @GetMapping("/{taskId}")
    public AjaxResult getInfo(@PathVariable Long taskId)
    {
        return success(recordService.selectReviewRecordDetail(taskId));
    }
}
