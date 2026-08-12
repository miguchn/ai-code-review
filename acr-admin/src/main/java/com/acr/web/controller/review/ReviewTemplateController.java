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
import com.acr.review.domain.ReviewTemplate;
import com.acr.review.service.IReviewTemplateService;

/** 项目审查模板 REST 接入。 */
@RestController
@RequestMapping("/review/template")
public class ReviewTemplateController extends BaseController
{
    private final IReviewTemplateService templateService;

    public ReviewTemplateController(IReviewTemplateService templateService)
    {
        this.templateService = templateService;
    }

    @PreAuthorize("@ss.hasPlatformPermi('review:template:list')")
    @GetMapping("/list")
    public TableDataInfo list(ReviewTemplate template)
    {
        startPage();
        List<ReviewTemplate> list = templateService.selectReviewTemplateList(template);
        return getDataTable(list);
    }

    /** 平台统一审查规则（只读；与执行 Prompt 共用评分数据源）。须放在 /{templateId} 之前。 */
    @PreAuthorize("@ss.hasPlatformPermi('review:template:list')")
    @GetMapping("/platform-rules")
    public AjaxResult platformRules()
    {
        return success(templateService.getPlatformRules());
    }

    @PreAuthorize("@ss.hasPlatformPermi('review:template:query')")
    @GetMapping("/{templateId}")
    public AjaxResult getInfo(@PathVariable Long templateId)
    {
        return success(templateService.selectReviewTemplateById(templateId));
    }

    @PreAuthorize("@ss.hasPlatformPermi('review:template:add')")
    @Log(title = "审查模板", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody ReviewTemplate template)
    {
        return toAjax(templateService.insertReviewTemplate(template));
    }

    @PreAuthorize("@ss.hasPlatformPermi('review:template:edit')")
    @Log(title = "审查模板", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody ReviewTemplate template)
    {
        return toAjax(templateService.updateReviewTemplate(template));
    }

    @PreAuthorize("@ss.hasPlatformPermi('review:template:remove')")
    @Log(title = "审查模板", businessType = BusinessType.DELETE)
    @DeleteMapping("/{templateIds}")
    public AjaxResult remove(@PathVariable Long[] templateIds)
    {
        templateService.deleteReviewTemplateByIds(templateIds);
        return success();
    }
}
