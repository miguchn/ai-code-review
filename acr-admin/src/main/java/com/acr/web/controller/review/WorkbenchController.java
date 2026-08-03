package com.acr.web.controller.review;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.acr.common.core.controller.BaseController;
import com.acr.common.core.domain.AjaxResult;
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
}
