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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.acr.common.annotation.Log;
import com.acr.common.core.controller.BaseController;
import com.acr.common.core.domain.AjaxResult;
import com.acr.common.core.page.TableDataInfo;
import com.acr.common.enums.BusinessType;
import com.acr.review.domain.ReviewProject;
import com.acr.review.domain.GitRepositoryReadRequest;
import com.acr.review.service.IReviewProjectService;

/** 代码项目 REST 接入。 */
@RestController
@RequestMapping("/review/project")
public class ReviewProjectController extends BaseController
{
    private final IReviewProjectService projectService;

    public ReviewProjectController(IReviewProjectService projectService)
    {
        this.projectService = projectService;
    }

    @PreAuthorize("@ss.hasPermi('review:project:list')")
    @GetMapping("/list")
    public TableDataInfo list(ReviewProject project)
    {
        startPage();
        List<ReviewProject> list = projectService.selectReviewProjectList(project);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('review:project:list')")
    @GetMapping("/options")
    public AjaxResult options()
    {
        return success(projectService.getFormOptions());
    }

    @PreAuthorize("@ss.hasPermi('review:project:test')")
    @Log(title = "读取仓库信息")
    @PostMapping("/repository-info")
    public AjaxResult readRepositoryInfo(@Validated @RequestBody GitRepositoryReadRequest request)
    {
        return success(projectService.readRepositoryInfo(request));
    }

    @PreAuthorize("@ss.hasPermi('review:project:query')")
    @GetMapping("/{projectId}")
    public AjaxResult getInfo(@PathVariable Long projectId)
    {
        return success(projectService.selectReviewProjectById(projectId));
    }

    @PreAuthorize("@ss.hasPermi('review:project:add')")
    @Log(title = "代码项目", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody ReviewProject project)
    {
        return toAjax(projectService.insertReviewProject(project));
    }

    @PreAuthorize("@ss.hasPermi('review:project:edit')")
    @Log(title = "代码项目", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody ReviewProject project)
    {
        return toAjax(projectService.updateReviewProject(project));
    }

    @PreAuthorize("@ss.hasPermi('review:project:remove')")
    @Log(title = "代码项目", businessType = BusinessType.DELETE)
    @DeleteMapping("/{projectIds}")
    public AjaxResult remove(@PathVariable Long[] projectIds)
    {
        projectService.deleteReviewProjectByIds(projectIds);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('review:project:status')")
    @Log(title = "代码项目启停", businessType = BusinessType.UPDATE)
    @PutMapping("/{projectId}/status")
    public AjaxResult changeStatus(@PathVariable Long projectId, @RequestParam String status)
    {
        return toAjax(projectService.updateProjectStatus(projectId, status));
    }

    @PreAuthorize("@ss.hasPermi('review:project:test')")
    @Log(title = "代码项目连接测试")
    @PostMapping("/{projectId}/test")
    public AjaxResult testConnection(@PathVariable Long projectId)
    {
        return success(projectService.testConnection(projectId));
    }
}
