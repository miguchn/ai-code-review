package com.acr.web.controller.review;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.acr.common.annotation.Log;
import com.acr.common.core.controller.BaseController;
import com.acr.common.core.domain.AjaxResult;
import com.acr.common.core.page.TableDataInfo;
import com.acr.common.enums.BusinessType;
import com.acr.review.domain.ReviewNotifyChannel;
import com.acr.review.service.IReviewNotifyChannelService;

/** 通知渠道 REST 接入。 */
@RestController
@RequestMapping("/review/notify/channel")
public class ReviewNotifyChannelController extends BaseController
{
    private final IReviewNotifyChannelService channelService;

    public ReviewNotifyChannelController(IReviewNotifyChannelService channelService)
    {
        this.channelService = channelService;
    }

    @PreAuthorize("@ss.hasPlatformPermi('review:notify:list')")
    @GetMapping("/list")
    public TableDataInfo list(ReviewNotifyChannel channel)
    {
        startPage();
        List<ReviewNotifyChannel> list = channelService.selectReviewNotifyChannelList(channel);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPlatformPermi('review:notify:query')")
    @GetMapping("/{channelId}")
    public AjaxResult getInfo(@PathVariable Long channelId)
    {
        return success(channelService.selectReviewNotifyChannelById(channelId));
    }

    @PreAuthorize("@ss.hasPlatformPermi('review:notify:add')")
    @Log(title = "通知渠道", businessType = BusinessType.INSERT,
         excludeParamNames = { "webhookUrl", "secret", "webhookUrlCiphertext", "secretCiphertext" },
         isSaveResponseData = false)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody ReviewNotifyChannel channel)
    {
        return toAjax(channelService.insertReviewNotifyChannel(channel));
    }

    @PreAuthorize("@ss.hasPlatformPermi('review:notify:edit')")
    @Log(title = "通知渠道", businessType = BusinessType.UPDATE,
         excludeParamNames = { "webhookUrl", "secret", "webhookUrlCiphertext", "secretCiphertext" },
         isSaveResponseData = false)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody ReviewNotifyChannel channel)
    {
        return toAjax(channelService.updateReviewNotifyChannel(channel));
    }

    @PreAuthorize("@ss.hasPlatformPermi('review:notify:status')")
    @Log(title = "通知渠道启停", businessType = BusinessType.UPDATE)
    @PutMapping("/changeStatus")
    public AjaxResult changeStatus(@RequestBody ReviewNotifyChannel channel)
    {
        return toAjax(channelService.changeStatus(channel.getChannelId(), channel.getStatus()));
    }

    @PreAuthorize("@ss.hasPlatformPermi('review:notify:remove')")
    @Log(title = "通知渠道", businessType = BusinessType.DELETE)
    @DeleteMapping("/{channelIds}")
    public AjaxResult remove(@PathVariable Long[] channelIds)
    {
        channelService.deleteReviewNotifyChannelByIds(channelIds);
        return success();
    }

    @PreAuthorize("@ss.hasPlatformPermi('review:notify:test')")
    @Log(title = "通知渠道测试发送", isSaveResponseData = false)
    @PostMapping("/{channelId}/test")
    public AjaxResult testSend(@PathVariable Long channelId)
    {
        return success(channelService.testSend(channelId));
    }
}
