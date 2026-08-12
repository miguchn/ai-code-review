package com.acr.system.service.impl;

import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.acr.common.utils.SecurityUtils;
import com.acr.common.utils.StringUtils;
import com.acr.system.domain.SysBusinessAudit;
import com.acr.system.mapper.SysBusinessAuditMapper;
import com.acr.system.service.ISysBusinessAuditService;

@Service
public class SysBusinessAuditServiceImpl implements ISysBusinessAuditService
{
    @Autowired
    private SysBusinessAuditMapper auditMapper;

    @Override
    public void record(SysBusinessAudit audit)
    {
        if (audit == null)
        {
            throw new IllegalArgumentException("业务审计对象不能为空");
        }
        if (StringUtils.isEmpty(audit.getEventKey()))
        {
            audit.setEventKey(java.util.UUID.randomUUID().toString());
        }
        if (StringUtils.isEmpty(audit.getOperator()))
        {
            audit.setOperator(currentOperator());
        }
        if (audit.getAuditTime() == null)
        {
            audit.setAuditTime(new Date());
        }
        if (StringUtils.isEmpty(audit.getSource()))
        {
            audit.setSource("system");
        }
        auditMapper.insertBusinessAudit(audit);
    }

    @Override
    public List<SysBusinessAudit> selectBusinessAuditList(SysBusinessAudit audit)
    {
        return auditMapper.selectBusinessAuditList(audit);
    }

    private String currentOperator()
    {
        try
        {
            return SecurityUtils.getUsername();
        }
        catch (Exception ignored)
        {
            return "system";
        }
    }
}
