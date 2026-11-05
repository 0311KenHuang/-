package com.envmon.dao.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 报警判定用的数据点：站房 + 因子 + 时间 + 值（超标时带限值） */
@Data
public class AlarmSampleRow implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long stationId;
    private String factorCode;
    private LocalDateTime ts;
    private BigDecimal value;
    /** 超标规则用：该站该因子的执行限值 */
    private BigDecimal threshold;
}
