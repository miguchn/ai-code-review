package com.acr.web.controller.system;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import com.acr.common.annotation.Log;
import com.acr.common.core.controller.BaseController;
import com.acr.common.core.domain.AjaxResult;
import com.acr.common.core.page.TableDataInfo;
import com.acr.common.enums.BusinessType;
import com.acr.system.domain.SysBusinessSystem;
import com.acr.system.service.ISysBusinessSystemService;

/**
 * 业务系统管理 信息操作处理
 */
@RestController
@RequestMapping("/system/businesssystem")
public class SysBusinessSystemController extends BaseController
{
    private final ISysBusinessSystemService businessSystemService;

    public SysBusinessSystemController(ISysBusinessSystemService businessSystemService)
    {
        this.businessSystemService = businessSystemService;
    }

    /** 获取业务系统列表 */
    @PreAuthorize("@ss.hasPermi('system:businesssystem:list')")
    @GetMapping("/list")
    public TableDataInfo list(SysBusinessSystem sysBusinessSystem)
    {
        startPage();
        List<SysBusinessSystem> list = businessSystemService.selectSysBusinessSystemList(sysBusinessSystem);
        return getDataTable(list);
    }

    /** 获取业务系统详细信息 */
    @PreAuthorize("@ss.hasPermi('system:businesssystem:query')")
    @GetMapping(value = "/{systemId}")
    public AjaxResult getInfo(@PathVariable Long systemId)
    {
        return success(businessSystemService.selectSysBusinessSystemById(systemId));
    }

    /** 新增业务系统 */
    @PreAuthorize("@ss.hasPermi('system:businesssystem:add')")
    @Log(title = "业务系统管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody SysBusinessSystem sysBusinessSystem)
    {
        return toAjax(businessSystemService.insertSysBusinessSystem(sysBusinessSystem));
    }

    /** 修改业务系统 */
    @PreAuthorize("@ss.hasPermi('system:businesssystem:edit')")
    @Log(title = "业务系统管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody SysBusinessSystem sysBusinessSystem)
    {
        sysBusinessSystem.setUpdateBy(getUsername());
        return toAjax(businessSystemService.updateSysBusinessSystem(sysBusinessSystem));
    }

    /** 删除业务系统 */
    @PreAuthorize("@ss.hasPermi('system:businesssystem:remove')")
    @Log(title = "业务系统管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{systemIds}")
    public AjaxResult remove(@PathVariable Long[] systemIds)
    {
        businessSystemService.deleteSysBusinessSystemByIds(systemIds);
        return success();
    }
}
