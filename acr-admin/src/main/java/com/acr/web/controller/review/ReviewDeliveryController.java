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

    /** 任务详情：该任务全部行内评论投递记录。 */
    @PreAuthorize("@ss.hasAnyPermi('review:record:query,review:task:query')")
    @GetMapping("/task/{taskId}/inline")
    public AjaxResult inlineByTask(@PathVariable Long taskId)
    {
        return success(deliveryService.selectInlineDeliveriesByTaskId(taskId));
    }

    /** 问题详情：按 issueId 反查行内评论投递状态。 */
    @PreAuthorize("@ss.hasAnyPermi('review:issue:query,review:record:query,review:task:query')")
    @GetMapping("/issue/{issueId}/inline")
    public AjaxResult inlineByIssue(@PathVariable Long issueId)
    {
        return success(deliveryService.selectInlineDeliveryByIssueId(issueId));
    }

    @PreAuthorize("@ss.hasPermi('review:delivery:retry')")
    @Log(title = "审查投递", businessType = BusinessType.UPDATE)
    @PostMapping("/{taskId}/retry")
    public AjaxResult retry(@PathVariable Long taskId)
    {
        deliveryService.retryDelivery(taskId);
        return success("已进入投递队列");
    }

    @PreAuthorize("@ss.hasPermi('review:delivery:retry')")
    @Log(title = "审查投递补发", businessType = BusinessType.UPDATE)
    @PostMapping("/record/{deliveryId}/retry")
    public AjaxResult retryByRecord(@PathVariable Long deliveryId)
    {
        deliveryService.retryDeliveryById(deliveryId);
        return success("已进入投递队列");
    }

    @PreAuthorize("@ss.hasPermi('review:task:handle')")
    @Log(title = "投递标记人工已处理", businessType = BusinessType.UPDATE)
    @PostMapping("/record/{deliveryId}/mark-handled")
    public AjaxResult markHandled(@PathVariable Long deliveryId)
    {
        deliveryService.markManualHandled(deliveryId);
        return success("已标记人工处理");
    }

    /** 查看实际发出的正文快照（复用列表权限 + 部门数据范围）。 */
    @PreAuthorize("@ss.hasPermi('review:delivery:list')")
    @GetMapping("/record/{deliveryId}/content")
    public AjaxResult content(@PathVariable Long deliveryId)
    {
        return success(deliveryService.selectDeliveryContent(deliveryId));
    }
}
