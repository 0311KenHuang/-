package com.envmon.dao.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.envmon.dao.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    /**
     * 登录专用：此时还没有租户上下文，必须显式忽略租户插件，
     * 否则 TenantLineHandler 会因取不到 tenantId 抛错。
     *
     * 注意：不同租户允许同名用户，此方法不能单独用于登录定位，
     * 必须先用 countByUsernameIgnoreTenant 消歧或走 selectByUsernameAndTenant。
     */
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT * FROM sys_user WHERE username = #{username} AND deleted = 0 ORDER BY id LIMIT 1")
    SysUser selectByUsernameIgnoreTenant(@Param("username") String username);

    /** 登录消歧用：同名用户可能存在于多个租户 */
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT COUNT(*) FROM sys_user WHERE username = #{username} AND deleted = 0")
    int countByUsernameIgnoreTenant(@Param("username") String username);

    /** 已确定租户后的精确登录查询（登录传了租户编码时走这里） */
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT * FROM sys_user WHERE username = #{username} AND tenant_id = #{tenantId} AND deleted = 0 ORDER BY id LIMIT 1")
    SysUser selectByUsernameAndTenant(@Param("username") String username, @Param("tenantId") Long tenantId);

    /** 用户角色码，登录时一次性取出，避免 N+1 */
    @Select("SELECT r.role_code FROM sys_role r "
            + "JOIN sys_user_role ur ON ur.role_id = r.id "
            + "WHERE ur.user_id = #{userId} AND r.deleted = 0")
    List<String> selectRoleCodes(@Param("userId") Long userId);

    @Select("SELECT r.data_scope FROM sys_role r "
            + "JOIN sys_user_role ur ON ur.role_id = r.id "
            + "WHERE ur.user_id = #{userId} AND r.deleted = 0 "
            + "ORDER BY FIELD(r.data_scope, 'ALL', 'REGION', 'ORG', 'SELF') LIMIT 1")
    String selectTopDataScope(@Param("userId") Long userId);

    /**
     * 报警接收人兜底：按 ORG → MGR → SUP 的顺序找责任管理员。
     * 显式带 tenant_id 并绕开租户插件，定时任务调用时不依赖请求上下文。
     */
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT u.* FROM sys_user u "
            + "JOIN sys_user_role ur ON ur.user_id = u.id "
            + "JOIN sys_role r ON r.id = ur.role_id "
            + "WHERE u.tenant_id = #{tenantId} AND u.deleted = 0 AND u.status = 1 "
            + "AND r.role_code IN ('ORG', 'MGR', 'SUP') "
            + "ORDER BY FIELD(r.role_code, 'ORG', 'MGR', 'SUP') LIMIT 5")
    List<SysUser> selectRoleFallback(@Param("tenantId") Long tenantId);
}
