package com.envmon.common.context;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/** 请求级上下文：一次请求内所有租户/用户判断都从这里取 */
@Data
public class TenantContext implements Serializable {

    private static final long serialVersionUID = 1L;

    private String traceId;
    private Long tenantId;
    private Long userId;
    private String username;
    private Long orgId;
    /** 用户所属区域，REGION 数据权限用；未绑定为 null */
    private Long regionId;
    private List<String> roles;
    /** ALL / REGION / ORG / SELF */
    private String dataScope;
}
