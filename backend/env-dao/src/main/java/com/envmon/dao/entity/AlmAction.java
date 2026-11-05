package com.envmon.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/** 报警处置：只追加，不改不删，和审计日志一个思路 */
@Data
@TableName("alm_action")
public class AlmAction implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;
    private Long eventId;
    /** ACK / CLOSE / FALSE_ALARM / AUTO_CLOSE */
    private String action;
    /** 操作人；自动关闭为 0 */
    private Long operator;
    private String remark;
    private LocalDateTime createdAt;
}
