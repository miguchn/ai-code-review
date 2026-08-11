package com.acr.web.controller.system;

import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.acr.common.annotation.Log;
import com.acr.common.core.controller.BaseController;
import com.acr.common.core.domain.AjaxResult;
import com.acr.common.enums.BusinessType;
import com.acr.review.insight.dto.UserIdentityAddRequest;
import com.acr.system.domain.SysUserIdentity;
import com.acr.system.service.ISysUserIdentityService;

/** 个人设置：我的提交邮箱。登录即可。 */
@RestController
@RequestMapping("/system/userprofile/identities")
public class SysUserProfileIdentityController extends BaseController
{
    private final ISysUserIdentityService userIdentityService;

    public SysUserProfileIdentityController(ISysUserIdentityService userIdentityService)
    {
        this.userIdentityService = userIdentityService;
    }

    @GetMapping
    public AjaxResult list()
    {
        List<SysUserIdentity> list = userIdentityService.listMineGit(getUserId());
        return success(list);
    }

    @Log(title = "我的提交邮箱", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody UserIdentityAddRequest request)
    {
        String identifier = request == null ? null : request.getIdentifier();
        String displayName = request == null ? null : request.getDisplayName();
        String origin = request == null ? null : request.getOrigin();
        SysUserIdentity row = userIdentityService.addMineGit(
            getUserId(), identifier, displayName, getUsername(), origin);
        return success(row);
    }

    @Log(title = "我的提交邮箱", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public AjaxResult remove(@PathVariable Long id)
    {
        userIdentityService.deleteMine(getUserId(), id);
        return success();
    }
}
