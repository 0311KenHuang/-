package com.envmon.common.audit;

import lombok.Data;

import java.io.Serializable;

/** 审计条目，由各模块的 AuditWriter 落库 */
@Data
public class AuditLogEntry implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long tenantId;
    private Long userId;
    private String userName;
    private String module;
    private String action;
    private String bizId;
    private String beforeJson;
    private String afterJson;
    private String ip;
    private String ua;
}
