package com.acr.system.mapper;

import java.util.List;
import com.acr.system.domain.SysBusinessAudit;

public interface SysBusinessAuditMapper
{
    int insertBusinessAudit(SysBusinessAudit audit);

    List<SysBusinessAudit> selectBusinessAuditList(SysBusinessAudit audit);
}
