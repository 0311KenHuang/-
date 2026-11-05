package com.envmon.dao.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 报警事件：OPEN → ACK → PROCESSING → CLOSED / FALSE_ALARM */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("alm_event")
public class AlmEvent extends TenantEntity {

    private static final long serialVersionUID = 1L;

    private String eventNo;
    private Long ruleId;
    private Long stationId;
    private String factorCode;
    /** P1 / P2 / P3 */
    private String level;
    private BigDecimal value;
    private BigDecimal threshold;
    private LocalDateTime beginAt;
    private LocalDateTime endAt;
    private String status;
    /** 0 首报 / 1 T+15min / 2 T+30min，防止升级消息重复推 */
    private Integer escalateStage;
    private Long ownerId;
    private Long relatedTaskId;
}
