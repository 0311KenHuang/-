package com.envmon.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/** 审计日志：只追加，不提供 update/delete 的 Mapper 方法 */
@Data
@TableName("sys_audit_log")
public class SysAuditLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

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
    private LocalDateTime createdAt;
}
