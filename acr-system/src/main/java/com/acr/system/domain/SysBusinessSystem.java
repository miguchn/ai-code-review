package com.acr.system.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.acr.common.annotation.Excel;
import com.acr.common.core.domain.BaseEntity;

/**
 * 业务系统表 sys_business_system
 */
public class SysBusinessSystem extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 系统ID */
    @Excel(name = "系统ID", cellType = Excel.ColumnType.NUMERIC)
    private Long systemId;

    /** 业务系统名称 */
    @Excel(name = "业务系统名称")
    private String systemName;

    /** 业务系统编码 */
    @Excel(name = "业务系统编码")
    private String systemCode;

    /** 所属部门ID */
    @Excel(name = "所属部门ID")
    private Long deptId;

    /** 所属部门名称 */
    private String deptName;

    /** 管理用户ID列表(逗号分隔) */
    private String managerIds;

    /** 管理用户名称列表(展示用) */
    private String managerNames;

    /** 状态(0正常 1停用) */
    @Excel(name = "状态", readConverterExp = "0=正常,1=停用")
    private String status;

    public Long getSystemId()
    {
        return systemId;
    }

    public void setSystemId(Long systemId)
    {
        this.systemId = systemId;
    }

    @NotBlank(message = "业务系统名称不能为空")
    @Size(max = 100, message = "业务系统名称不能超过100个字符")
    public String getSystemName()
    {
        return systemName;
    }

    public void setSystemName(String systemName)
    {
        this.systemName = systemName;
    }

    @NotBlank(message = "业务系统编码不能为空")
    @Size(max = 64, message = "业务系统编码不能超过64个字符")
    public String getSystemCode()
    {
        return systemCode;
    }

    public void setSystemCode(String systemCode)
    {
        this.systemCode = systemCode;
    }

    public Long getDeptId()
    {
        return deptId;
    }

    public void setDeptId(Long deptId)
    {
        this.deptId = deptId;
    }

    public String getDeptName()
    {
        return deptName;
    }

    public void setDeptName(String deptName)
    {
        this.deptName = deptName;
    }

    public String getManagerIds()
    {
        return managerIds;
    }

    public void setManagerIds(String managerIds)
    {
        this.managerIds = managerIds;
    }

    public String getManagerNames()
    {
        return managerNames;
    }

    public void setManagerNames(String managerNames)
    {
        this.managerNames = managerNames;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("systemId", getSystemId())
            .append("systemName", getSystemName())
            .append("systemCode", getSystemCode())
            .append("deptId", getDeptId())
            .append("managerIds", getManagerIds())
            .append("status", getStatus())
            .append("remark", getRemark())
            .toString();
    }
}
