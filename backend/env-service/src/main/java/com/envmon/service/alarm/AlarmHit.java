package com.envmon.service.alarm;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 一次规则命中，直接映射成报警事件的关键字段 */
@Data
public class AlarmHit {

    private Long stationId;
    private String factorCode;
    private BigDecimal value;
    private BigDecimal threshold;
    /** 条件首次满足的时间，用于事件 begin_at */
    private LocalDateTime beginAt;
}
