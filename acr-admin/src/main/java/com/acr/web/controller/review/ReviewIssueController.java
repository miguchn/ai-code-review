package com.acr.web.controller.review;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.acr.common.annotation.Log;
import com.acr.common.core.controller.BaseController;
import com.acr.common.core.domain.AjaxResult;
import com.acr.common.core.page.TableDataInfo;
import com.acr.common.enums.BusinessType;
import com.acr.review.delivery.ReviewDeliveryConstants;
import com.acr.review.domain.ReviewCommentSyncResult;
import com.acr.review.domain.ReviewIssue;
import com.acr.review.domain.ReviewIssueDetail;
import com.acr.review.service.IReviewIssueService;

/** 问题台账 REST。 */
@RestController
@RequestMapping("/review/issue")
public class ReviewIssueController extends BaseController
{
    private final IReviewIssueService issueService;

    public ReviewIssueController(IReviewIssueService issueService)
    {
        this.issueService = issueService;
    }

    @PreAuthorize("@ss.hasPermi('review:issue:list')")
    @GetMapping("/list")
    public TableDataInfo list(ReviewIssue query)
    {
        startPage();
        List<ReviewIssue> list = issueService.selectIssueList(query);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('review:issue:query')")
    @GetMapping("/{issueId}")
    public AjaxResult getInfo(@PathVariable Long issueId)
    {
        return success(issueService.selectIssueDetail(issueId));
    }

    @PreAuthorize("@ss.hasPermi('review:issue:confirm')")
    @Log(title = "问题确认", businessType = BusinessType.UPDATE)
    @PutMapping("/{issueId}/confirm")
    public AjaxResult confirm(@PathVariable Long issueId)
    {
        return success(toCommentSyncData(issueService.confirm(issueId)));
    }

    @PreAuthorize("@ss.hasPermi('review:issue:close')")
    @Log(title = "问题关闭", businessType = BusinessType.UPDATE)
    @PutMapping("/{issueId}/close")
    public AjaxResult close(@PathVariable Long issueId, @RequestBody(required = false) Map<String, String> body)
    {
        String note = body == null ? null : body.get("resolveNote");
        return success(toCommentSyncData(issueService.close(issueId, note)));
    }

    @PreAuthorize("@ss.hasPermi('review:issue:close')")
    @Log(title = "问题忽略误报", businessType = BusinessType.UPDATE)
    @PutMapping("/{issueId}/dismiss")
    public AjaxResult dismiss(@PathVariable Long issueId, @RequestBody Map<String, String> body)
    {
        if (body == null)
        {
            body = Map.of();
        }
        return success(toCommentSyncData(issueService.dismiss(issueId, body.get("dismissType"), body.get("resolveNote"))));
    }

    private static Map<String, Object> toCommentSyncData(ReviewCommentSyncResult sync)
    {
        Map<String, Object> data = new LinkedHashMap<>();
        String status = sync == null || sync.getStatus() == null
            ? ReviewDeliveryConstants.STATUS_SKIPPED
            : sync.getStatus();
        data.put("commentSyncStatus", status);
        if (ReviewDeliveryConstants.STATUS_FAILED.equals(status))
        {
            if (sync.getFailureMessage() != null)
            {
                data.put("commentSyncFailureMessage", sync.getFailureMessage());
            }
            if (sync.getDeliveryId() != null)
            {
                data.put("deliveryId", sync.getDeliveryId());
            }
        }
        return data;
    }
}
