package com.acr.web.controller.review;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.acr.common.core.controller.BaseController;
import com.acr.common.core.domain.AjaxResult;
import com.acr.review.domain.WorkbenchConstants;
import com.acr.review.service.IWorkbenchService;

/**
 * 首页工作台。登录即可访问，不新增权限串；卡片可见性由后端按既有 list 权限裁剪。
 */
@RestController
@RequestMapping("/workbench")
public class WorkbenchController extends BaseController
{
    private final IWorkbenchService workbenchService;

    public WorkbenchController(IWorkbenchService workbenchService)
    {
        this.workbenchService = workbenchService;
    }

    @GetMapping("/summary")
    public AjaxResult summary()
    {
        return success(workbenchService.getSummary());
    }

    /** 审查结论按天趋势；无 review:record:list 权限时 data 为 null，前端隐藏区块。 */
    @GetMapping("/trend")
    public AjaxResult trend(@RequestParam(defaultValue = "14") Integer days)
    {
        int window = days == null ? WorkbenchConstants.TREND_DEFAULT_DAYS : days;
        return success(workbenchService.getTrend(window));
    }

    /** 启用模型健康摘要（登录可调，字段白名单脱敏）。 */
    @GetMapping("/models")
    public AjaxResult models()
    {
        return success(workbenchService.getModelHealth());
    }
}
