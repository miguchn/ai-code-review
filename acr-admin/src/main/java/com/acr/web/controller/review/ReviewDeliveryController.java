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
import com.acr.review.domain.ReviewDeliveryRecord;
import com.acr.review.service.IReviewDeliveryService;

/** 审查结果投递 REST 接入（列表、详情、按任务/记录重试）。 */
@RestController
@RequestMapping("/review/delivery")
public class ReviewDeliveryController extends BaseController
{
    private final IReviewDeliveryService deliveryService;

    public ReviewDeliveryController(IReviewDeliveryService deliveryService)
    {
        this.deliveryService = deliveryService;
    }

    @PreAuthorize("@ss.hasPermi('review:delivery:list')")
    @GetMapping("/list")
    public TableDataInfo list(ReviewDeliveryRecord query)
    {
        startPage();
        List<ReviewDeliveryRecord> list = deliveryService.selectDeliveryList(query);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('review:delivery:query')")
    @GetMapping("/{deliveryId}")
    public AjaxResult getInfo(@PathVariable Long deliveryId)
    {
        return success(deliveryService.selectDeliveryById(deliveryId));
    }

    /** 任务详情页查询该任务最近一条 IM 投递（复用任务/记录查询权限 + 部门数据范围）。 */
    @PreAuthorize("@ss.hasAnyPermi('review:record:query,review:task:query')")
    @GetMapping("/task/{taskId}/im-latest")
    public AjaxResult latestIm(@PathVariable Long taskId)
    {
        return success(deliveryService.selectLatestImDelivery(taskId));
    }

    @PreAuthorize("@ss.hasPermi('review:delivery:retry')")
    @Log(title = "审查投递", businessType = BusinessType.UPDATE)
    @PostMapping("/{taskId}/retry")
    public AjaxResult retry(@PathVariable Long taskId)
    {
        deliveryService.retryDelivery(taskId);
        return success("投递重试已完成");
    }

    @PreAuthorize("@ss.hasPermi('review:delivery:retry')")
    @Log(title = "审查投递补发", businessType = BusinessType.UPDATE)
    @PostMapping("/record/{deliveryId}/retry")
    public AjaxResult retryByRecord(@PathVariable Long deliveryId)
    {
        deliveryService.retryDeliveryById(deliveryId);
        return success("投递补发已完成");
    }
}
