package com.acr.web.controller.review;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.acr.common.annotation.Log;
import com.acr.common.core.controller.BaseController;
import com.acr.common.core.domain.AjaxResult;
import com.acr.common.enums.BusinessType;
import com.acr.review.insight.IReviewInsightService;
import com.acr.review.insight.InsightConstants;
import com.acr.review.insight.dto.InsightMemberClaimRequest;
import com.acr.review.insight.dto.InsightProjectRow;

/** 数据洞察 REST 接入。 */
@RestController
@RequestMapping("/insight")
public class InsightController extends BaseController
{
    private final IReviewInsightService insightService;

    public InsightController(IReviewInsightService insightService)
    {
        this.insightService = insightService;
    }

    @PreAuthorize("@ss.hasPermi('" + InsightConstants.PERM_OVERVIEW_VIEW + "')")
    @GetMapping("/overview")
    public AjaxResult overview(@RequestParam(required = false) String beginDate,
                               @RequestParam(required = false) String endDate,
                               @RequestParam(required = false) Integer days,
                               @RequestParam(required = false) Long businessSystemId)
    {
        return success(insightService.getOverview(beginDate, endDate, days, businessSystemId));
    }

    @PreAuthorize("@ss.hasPermi('" + InsightConstants.PERM_PROJECT_VIEW + "')")
    @GetMapping("/projects")
    public AjaxResult projects(@RequestParam(required = false) String beginDate,
                               @RequestParam(required = false) String endDate,
                               @RequestParam(required = false) Integer days,
                               @RequestParam(required = false) Long businessSystemId,
                               @RequestParam(required = false) Long projectId,
                               @RequestParam(required = false) String orderBy)
    {
        List<InsightProjectRow> rows = insightService.listProjects(beginDate, endDate, days,
            businessSystemId, projectId, orderBy);
        return success(rows);
    }

    @PreAuthorize("@ss.hasPermi('" + InsightConstants.PERM_PROJECT_VIEW + "')")
    @GetMapping("/project/{projectId}")
    public AjaxResult projectDetail(@PathVariable Long projectId,
                                    @RequestParam(required = false) String beginDate,
                                    @RequestParam(required = false) String endDate,
                                    @RequestParam(required = false) Integer days)
    {
        return success(insightService.getProjectDetail(projectId, beginDate, endDate, days));
    }

    @PreAuthorize("@ss.hasPermi('" + InsightConstants.PERM_OVERVIEW_VIEW + "')")
    @GetMapping("/metrics-dict")
    public AjaxResult metricsDict()
    {
        return success(insightService.getMetricsDictionary());
    }

    /** 本人视图：登录即可。 */
    @GetMapping("/member/mine")
    public AjaxResult memberMine(@RequestParam(required = false) String beginDate,
                                 @RequestParam(required = false) String endDate,
                                 @RequestParam(required = false) Integer days)
    {
        return success(insightService.getMemberMine(beginDate, endDate, days));
    }

    @Log(title = "数据洞察身份认领", businessType = BusinessType.INSERT)
    @PostMapping("/member/claim")
    public AjaxResult memberClaim(@RequestBody InsightMemberClaimRequest request)
    {
        insightService.claimMemberIdentity(request);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('" + InsightConstants.PERM_TEAM_VIEW + "')")
    @Log(title = "数据洞察团队成员明细", businessType = BusinessType.OTHER)
    @GetMapping("/team/members")
    public AjaxResult teamMembers(@RequestParam(required = false) String beginDate,
                                  @RequestParam(required = false) String endDate,
                                  @RequestParam(required = false) Integer days,
                                  @RequestParam(required = false) Long businessSystemId)
    {
        return success(insightService.getTeamMembers(beginDate, endDate, days, businessSystemId));
    }
}
