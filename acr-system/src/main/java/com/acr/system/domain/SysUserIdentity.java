package com.acr.system.domain;

import java.util.Date;
import com.acr.common.core.domain.BaseEntity;

/** 用户身份关联 sys_user_identity。 */
public class SysUserIdentity extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    public static final String TYPE_GIT_COMMIT = "GIT_COMMIT";
    public static final String TYPE_IM_WECOM = "IM_WECOM";
    public static final String TYPE_IM_DINGTALK = "IM_DINGTALK";
    public static final String TYPE_IM_FEISHU = "IM_FEISHU";

    public static final String ORIGIN_SELF = "SELF";
    public static final String ORIGIN_AUTO = "AUTO";
    public static final String ORIGIN_ADMIN = "ADMIN";

    private Long id;
    private Long userId;
    private String identityType;
    private String identifier;
    private String displayName;
    private String origin;

    /** 联查展示：用户昵称 */
    private String nickName;
    /** 联查展示：用户名 */
    private String userName;
    /** 联查展示：部门名 */
    private String deptName;

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public Long getUserId()
    {
        return userId;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    public String getIdentityType()
    {
        return identityType;
    }

    public void setIdentityType(String identityType)
    {
        this.identityType = identityType;
    }

    public String getIdentifier()
    {
        return identifier;
    }

    public void setIdentifier(String identifier)
    {
        this.identifier = identifier;
    }

    public String getDisplayName()
    {
        return displayName;
    }

    public void setDisplayName(String displayName)
    {
        this.displayName = displayName;
    }

    public String getOrigin()
    {
        return origin;
    }

    public void setOrigin(String origin)
    {
        this.origin = origin;
    }

    public String getNickName()
    {
        return nickName;
    }

    public void setNickName(String nickName)
    {
        this.nickName = nickName;
    }

    public String getUserName()
    {
        return userName;
    }

    public void setUserName(String userName)
    {
        this.userName = userName;
    }

    public String getDeptName()
    {
        return deptName;
    }

    public void setDeptName(String deptName)
    {
        this.deptName = deptName;
    }

    public Date getCreateTime()
    {
        return super.getCreateTime();
    }

    public void setCreateTime(Date createTime)
    {
        super.setCreateTime(createTime);
    }
}
