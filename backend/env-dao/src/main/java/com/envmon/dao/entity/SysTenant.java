package com.envmon.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 租户：REG 监管方 / OPS 运维单位 / ENT 企业 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_tenant")
public class SysTenant extends BaseEntity {

    private static final long serialVersionUID = 1L;

    private String tenantCode;
    private String name;
    /** REG / OPS / ENT */
    private String tenantType;
    private Integer status;
}
