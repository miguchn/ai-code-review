package com.acr.web.controller.review;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
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
import com.acr.review.insight.dto.InsightIdentityBindRequest;
import com.acr.review.insight.dto.InsightProjectRow;
import com.acr.system.domain.SysOperLog;
import com.acr.system.service.ISysOperLogService;

/** 数据洞察 REST 接入。 */
@RestController
@RequestMapping("/insight")
public class InsightController extends BaseController
{
    private final IReviewInsightService insightService;
    private final ISysOperLogService operLogService;

    public InsightController(IReviewInsightService insightService, ISysOperLogService operLogService)
    {
        this.insightService = insightService;
        this.operLogService = operLogService;
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

    /** 本人提交邮箱候选：登录即可。 */
    @GetMapping("/identity/candidates")
    public AjaxResult identityCandidates()
    {
        return success(insightService.listIdentityCandidates());
    }

    @PreAuthorize("@ss.hasPermi('" + InsightConstants.PERM_TEAM_VIEW + "')")
    @Log(title = "数据洞察团队成员明细", businessType = BusinessType.OTHER)
    @GetMapping("/team/members")
    public AjaxResult teamMembers(@RequestParam(required = false) String beginDate,
                                  @RequestParam(required = false) String endDate,
                                  @RequestParam(required = false) Integer days,
                                  @RequestParam(required = false) Long businessSystemId,
                                  @RequestParam(required = false) Long projectId)
    {
        return success(insightService.getTeamMembers(beginDate, endDate, days, businessSystemId, projectId));
    }

    @PreAuthorize("@ss.hasPermi('" + InsightConstants.PERM_IDENTITY_MANAGE + "')")
    @Log(title = "成员提交邮箱关联清单", businessType = BusinessType.OTHER)
    @GetMapping("/team/identities")
    public AjaxResult teamIdentities()
    {
        return success(insightService.listTeamIdentities());
    }

    /** 指派弹窗用户候选：仅需 insight:identity:manage，不依赖 system:user:list。 */
    @PreAuthorize("@ss.hasPermi('" + InsightConstants.PERM_IDENTITY_MANAGE + "')")
    @GetMapping("/team/identities/userOptions")
    public AjaxResult identityUserOptions(@RequestParam(required = false) String keyword)
    {
        return success(insightService.listIdentityUserOptions(keyword));
    }

    @PreAuthorize("@ss.hasPermi('" + InsightConstants.PERM_IDENTITY_MANAGE + "')")
    @Log(title = "指派提交邮箱", businessType = BusinessType.INSERT)
    @PostMapping("/team/identities/bind")
    public AjaxResult bindTeamIdentity(@RequestBody InsightIdentityBindRequest request)
    {
        boolean reassigned = insightService.bindTeamIdentity(request);
        if (reassigned && request != null)
        {
            // 改派：解除原关联额外记一条删除日志（本方法 @Log 记新关联）
            SysOperLog unbindLog = new SysOperLog();
            unbindLog.setTitle("解除提交邮箱关联");
            unbindLog.setBusinessType(BusinessType.DELETE.ordinal());
            unbindLog.setOperName(getUsername());
            unbindLog.setStatus(0);
            unbindLog.setOperParam(request.getIdentifier());
            operLogService.insertOperlog(unbindLog);
        }
        return success();
    }

    @PreAuthorize("@ss.hasPermi('" + InsightConstants.PERM_IDENTITY_MANAGE + "')")
    @Log(title = "解除提交邮箱关联", businessType = BusinessType.DELETE)
    @DeleteMapping("/team/identities/{id}")
    public AjaxResult unbindTeamIdentity(@PathVariable Long id)
    {
        insightService.unbindTeamIdentity(id);
        return success();
    }
}
