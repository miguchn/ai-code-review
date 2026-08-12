package com.acr.review.domain;

import com.acr.common.core.domain.BaseEntity;

/** 代码审查项目成员。 */
public class ReviewProjectMember extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    public static final String ROLE_OWNER = "OWNER";
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_REVIEWER = "REVIEWER";
    public static final String ROLE_VIEWER = "VIEWER";

    private Long memberId;
    private Long projectId;
    private Long userId;
    private String userName;
    private Long deptId;
    private String deptName;
    private String projectRole;
    private String status;
    private boolean projectOwner;

    public Long getMemberId()
    {
        return memberId;
    }

    public void setMemberId(Long memberId)
    {
        this.memberId = memberId;
    }

    public Long getProjectId()
    {
        return projectId;
    }

    public void setProjectId(Long projectId)
    {
        this.projectId = projectId;
    }

    public Long getUserId()
    {
        return userId;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    public String getUserName()
    {
        return userName;
    }

    public void setUserName(String userName)
    {
        this.userName = userName;
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

    public String getProjectRole()
    {
        return projectRole;
    }

    public void setProjectRole(String projectRole)
    {
        this.projectRole = projectRole;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public boolean isProjectOwner()
    {
        return projectOwner;
    }

    public void setProjectOwner(boolean projectOwner)
    {
        this.projectOwner = projectOwner;
    }
}
