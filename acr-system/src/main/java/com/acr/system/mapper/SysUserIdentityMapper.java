package com.acr.system.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import com.acr.system.domain.SysUserIdentity;

public interface SysUserIdentityMapper
{
    SysUserIdentity selectById(@Param("id") Long id);

    List<SysUserIdentity> selectByUserId(@Param("userId") Long userId,
                                         @Param("identityType") String identityType);

    SysUserIdentity selectByTypeAndIdentifier(@Param("identityType") String identityType,
                                              @Param("identifier") String identifier);

    List<SysUserIdentity> selectByType(@Param("identityType") String identityType);

    /** 带用户/部门联查；支持 DataScope（别名 u/d）。 */
    List<SysUserIdentity> selectScopedList(SysUserIdentity query);

    int insert(SysUserIdentity row);

    int deleteById(@Param("id") Long id);

    int deleteByTypeAndIdentifier(@Param("identityType") String identityType,
                                  @Param("identifier") String identifier);

    @Update("UPDATE sys_user_identity SET user_id = #{userId}, origin = #{origin} WHERE id = #{id}")
    int updateUserIdAndOrigin(@Param("id") Long id, @Param("userId") Long userId, @Param("origin") String origin);
}
