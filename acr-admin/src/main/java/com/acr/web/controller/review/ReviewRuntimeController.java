package com.acr.web.controller.review;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.acr.common.core.controller.BaseController;
import com.acr.common.core.domain.AjaxResult;
import com.acr.review.runtime.IReviewRuntimeOpsService;
import com.acr.review.runtime.ReviewRuntimeBacklogItem;
import com.acr.review.runtime.ReviewRuntimeConstants;

/** 审查运行可观测与积压处置 REST 接入。 */
@RestController
@RequestMapping("/review/runtime")
public class ReviewRuntimeController extends BaseController
{
    private final IReviewRuntimeOpsService runtimeOpsService;

    public ReviewRuntimeController(IReviewRuntimeOpsService runtimeOpsService)
    {
        this.runtimeOpsService = runtimeOpsService;
    }

    @PreAuthorize("@ss.hasPermi('" + ReviewRuntimeConstants.PERM_RUNTIME_VIEW + "')")
    @GetMapping("/overview")
    public AjaxResult overview()
    {
        return success(runtimeOpsService.getOverview());
    }

    @PreAuthorize("@ss.hasPermi('" + ReviewRuntimeConstants.PERM_RUNTIME_VIEW + "')")
    @GetMapping("/backlog/overdue-pending")
    public AjaxResult overduePending(@RequestParam(required = false) Integer limit)
    {
        List<ReviewRuntimeBacklogItem> rows = runtimeOpsService.listOverduePendingTasks(limit);
        return success(rows);
    }

    @PreAuthorize("@ss.hasPermi('" + ReviewRuntimeConstants.PERM_RUNTIME_VIEW + "')")
    @GetMapping("/backlog/lease-expired")
    public AjaxResult leaseExpired(@RequestParam(required = false) Integer limit)
    {
        return success(runtimeOpsService.listLeaseExpiredTasks(limit));
    }

    @PreAuthorize("@ss.hasPermi('" + ReviewRuntimeConstants.PERM_RUNTIME_VIEW + "')")
    @GetMapping("/backlog/stuck-deliveries")
    public AjaxResult stuckDeliveries(@RequestParam(required = false) Integer limit)
    {
        return success(runtimeOpsService.listStuckDeliveries(limit));
    }
}
