package com.acr.web.controller.review;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.acr.common.annotation.Log;
import com.acr.common.core.controller.BaseController;
import com.acr.common.core.domain.AjaxResult;
import com.acr.review.domain.ReviewEngineTestRequest;
import com.acr.review.service.IReviewEngineService;

/** 审查引擎 REST 接入。 */
@RestController
@RequestMapping("/review/engine")
public class ReviewEngineController extends BaseController
{
    private final IReviewEngineService reviewEngineService;

    public ReviewEngineController(IReviewEngineService reviewEngineService)
    {
        this.reviewEngineService = reviewEngineService;
    }

    @PreAuthorize("@ss.hasPlatformPermi('review:engine:query')")
    @GetMapping("/info")
    public AjaxResult info()
    {
        return success(reviewEngineService.getEngineInfo());
    }

    @PreAuthorize("@ss.hasPlatformPermi('review:engine:detect')")
    @Log(title = "审查引擎环境检测")
    @PostMapping("/detect")
    public AjaxResult detect()
    {
        return success(reviewEngineService.detectEnvironment());
    }

    @PreAuthorize("@ss.hasPlatformPermi('review:engine:test')")
    @Log(title = "审查引擎测试调用")
    @PostMapping("/test")
    public AjaxResult test(@RequestBody(required = false) ReviewEngineTestRequest request)
    {
        return success(reviewEngineService.testInvoke(request));
    }
}
