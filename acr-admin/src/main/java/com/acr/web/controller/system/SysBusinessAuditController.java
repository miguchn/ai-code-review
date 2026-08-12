package com.acr.web.controller.system;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.acr.common.core.controller.BaseController;
import com.acr.common.core.page.TableDataInfo;
import com.acr.system.domain.SysBusinessAudit;
import com.acr.system.service.ISysBusinessAuditService;

/** 业务审计事实查询；无更新、删除接口。 */
@RestController
@RequestMapping("/system/business-audit")
public class SysBusinessAuditController extends BaseController
{
    @Autowired
    private ISysBusinessAuditService auditService;

    @PreAuthorize("@ss.hasPermi('system:audit:list')")
    @GetMapping("/list")
    public TableDataInfo list(SysBusinessAudit audit)
    {
        startPage();
        List<SysBusinessAudit> list = auditService.selectBusinessAuditList(audit);
        return getDataTable(list);
    }
}
