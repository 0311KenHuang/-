package com.envmon.dao.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 带租户的业务表基类：查询由 MyBatis-Plus 租户插件自动补 tenant_id */
@Data
@EqualsAndHashCode(callSuper = true)
public abstract class TenantEntity extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 租户 ID 是内部隔离字段，不出网（前端已确认不消费）；
     * 代码内赋值（新增后回填、审计写入）不受序列化注解影响
     */
    @JsonIgnore
    private Long tenantId;
}
