package com.acr.system.service;

import java.util.List;
import com.acr.system.domain.SysBusinessAudit;

public interface ISysBusinessAuditService
{
    /** 追加一条不可变业务审计事实。 */
    void record(SysBusinessAudit audit);

    List<SysBusinessAudit> selectBusinessAuditList(SysBusinessAudit audit);
}
