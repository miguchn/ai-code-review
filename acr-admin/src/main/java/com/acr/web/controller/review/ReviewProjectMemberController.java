package com.acr.web.controller.review;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.acr.common.annotation.Log;
import com.acr.common.core.controller.BaseController;
import com.acr.common.core.domain.AjaxResult;
import com.acr.common.enums.BusinessType;
import com.acr.review.domain.ReviewProjectMember;
import com.acr.review.service.IReviewProjectMemberService;

/** 项目成员 REST 接入。 */
@RestController
@RequestMapping("/review/project")
public class ReviewProjectMemberController extends BaseController
{
    private final IReviewProjectMemberService memberService;

    public ReviewProjectMemberController(IReviewProjectMemberService memberService)
    {
        this.memberService = memberService;
    }

    @PreAuthorize("@ss.hasPermi('review:project:query')")
    @GetMapping("/{projectId}/members")
    public AjaxResult list(@PathVariable Long projectId)
    {
        return success(memberService.selectProjectMembers(projectId));
    }

    @PreAuthorize("@ss.hasPermi('review:project:edit')")
    @Log(title = "项目成员授权", businessType = BusinessType.GRANT)
    @PutMapping("/{projectId}/members")
    public AjaxResult save(@PathVariable Long projectId, @RequestBody ReviewProjectMember member)
    {
        return toAjax(memberService.saveProjectMember(projectId, member));
    }

    @PreAuthorize("@ss.hasPermi('review:project:edit')")
    @Log(title = "项目成员移除", businessType = BusinessType.GRANT)
    @DeleteMapping("/{projectId}/members/{memberId}")
    public AjaxResult remove(@PathVariable Long projectId, @PathVariable Long memberId)
    {
        return toAjax(memberService.deleteProjectMember(projectId, memberId));
    }
}
