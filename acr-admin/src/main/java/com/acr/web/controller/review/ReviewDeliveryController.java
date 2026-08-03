package com.acr.web.controller.review;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.acr.common.annotation.Log;
import com.acr.common.core.controller.BaseController;
import com.acr.common.core.domain.AjaxResult;
import com.acr.common.enums.BusinessType;
import com.acr.review.service.IReviewDeliveryService;

/** 审查结果投递 REST 接入（GitHub PR 总结评论重试）。 */
@RestController
@RequestMapping("/review/delivery")
public class ReviewDeliveryController extends BaseController
{
    private final IReviewDeliveryService deliveryService;

    public ReviewDeliveryController(IReviewDeliveryService deliveryService)
    {
        this.deliveryService = deliveryService;
    }

    @PreAuthorize("@ss.hasPermi('review:delivery:retry')")
    @Log(title = "审查投递", businessType = BusinessType.UPDATE)
    @PostMapping("/{taskId}/retry")
    public AjaxResult retry(@PathVariable Long taskId)
    {
        deliveryService.retryDelivery(taskId);
        return success("投递重试已完成");
    }
}
