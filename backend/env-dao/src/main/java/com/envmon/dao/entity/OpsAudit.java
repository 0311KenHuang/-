package com.envmon.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

/** 运维审核：只追加，驳回必须填原因 */
@Data
@TableName("ops_audit")
public class OpsAudit implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;
    private Long recordId;
    private Long auditorId;
    /** PASS / REJECT */
    private String result;
    private String reason;
}
