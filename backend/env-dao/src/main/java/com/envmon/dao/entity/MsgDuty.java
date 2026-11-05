package com.envmon.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;

/** 值班表：接收人解析的第二优先级 */
@Data
@TableName("msg_duty")
public class MsgDuty implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;
    private Long orgId;
    private LocalDate dutyDate;
    /** DAY / NIGHT */
    private String shift;
    private Long userId;
    private String phone;
}
