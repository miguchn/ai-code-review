package com.acr.web.controller.review;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.acr.common.core.controller.BaseController;
import com.acr.common.core.domain.AjaxResult;
import com.acr.system.domain.SysUserIdentity;
import com.acr.system.service.ISysUserIdentityService;

/** Git 平台用户身份映射管理（自动发现 + 管理员绑定）。 */
@RestController
@RequestMapping("/review/identity-mapping")
public class ReviewIdentityMappingController extends BaseController
{
    @Autowired
    private ISysUserIdentityService identityService;

    /** 列出所有自动发现的 Git 平台用户（含已映射/未映射）。 */
    @PreAuthorize("@ss.hasPermi('review:project:edit')")
    @GetMapping
    public AjaxResult listPlatformUsers()
    {
        List<SysUserIdentity> list = identityService.listByType(SysUserIdentity.TYPE_GIT_PLATFORM_USER);
        return AjaxResult.success(list);
    }

    /** 管理员将自动发现的身份绑定到系统用户。 */
    @PreAuthorize("@ss.hasPermi('review:project:edit')")
    @PutMapping("/{id}")
    public AjaxResult bindIdentity(@PathVariable Long id, @RequestBody java.util.Map<String, Object> body)
    {
        Object userIdObj = body.get("userId");
        if (userIdObj == null)
        {
            return AjaxResult.error("请选择系统用户");
        }
        Long userId = Long.valueOf(userIdObj.toString());
        identityService.mapIdentity(id, userId);
        return AjaxResult.success();
    }
}
