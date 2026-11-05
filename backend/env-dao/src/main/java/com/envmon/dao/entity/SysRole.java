package com.envmon.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 角色：SUP / REG / ORG / MGR / ENG / ENT */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_role")
public class SysRole extends TenantEntity {

    private static final long serialVersionUID = 1L;

    private String roleCode;
    private String name;
    /** ALL / REGION / ORG / SELF */
    private String dataScope;
    private String remark;
}
