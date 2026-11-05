package com.envmon.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class SysUser extends TenantEntity {

    private static final long serialVersionUID = 1L;

    private String username;

    /** BCrypt 口令哈希：绝不出网（SystemController 用户列表曾直出实体，脱敏兜底） */
    @JsonIgnore
    private String passwordHash;
    private String realName;
    private Long orgId;
    /** 所属区域（md_region.id），REGION 数据权限用；监管用户可空 */
    private Long regionId;
    private String phone;
    /** 运维工资质证书号 */
    private String certNo;
    /** 1 正常 0 停用 2 锁定 */
    private Integer status;
    private LocalDateTime lastLoginAt;
}
