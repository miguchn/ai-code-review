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

/** 个人设置：我的 IM 账号。登录即可。 */
@RestController
@RequestMapping("/system/userprofile/imIdentities")
public class SysUserProfileImIdentityController extends BaseController
{
    private final ISysUserIdentityService userIdentityService;

    public SysUserProfileImIdentityController(ISysUserIdentityService userIdentityService)
    {
        this.userIdentityService = userIdentityService;
    }

    @GetMapping
    public AjaxResult list()
    {
        List<SysUserIdentity> list = userIdentityService.listMineIm(getUserId());
        return success(list);
    }

    @Log(title = "我的 IM 账号", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody UserIdentityAddRequest request)
    {
        String identityType = request == null ? null : request.getIdentityType();
        String identifier = request == null ? null : request.getIdentifier();
        SysUserIdentity row = userIdentityService.addMineIm(
            getUserId(), identityType, identifier, getUsername());
        return success(row);
    }

    @Log(title = "我的 IM 账号", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public AjaxResult remove(@PathVariable Long id)
    {
        userIdentityService.deleteMineIm(getUserId(), id);
        return success();
    }
}
