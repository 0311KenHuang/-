package com.envmon.ts.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 某站房某因子的最新值 */
@Data
public class RealTimeValue implements Serializable {

    private static final long serialVersionUID = 1L;

    private String factorCode;
    private String unit;
    private BigDecimal value;
    /** 数据时间（现场机上报的 DataTime，不是入库时间） */
    private LocalDateTime ts;
    private String devFlag;
    /** 1 有效 / 2 无效 / 3 未标记 */
    private Integer quality;
    private BigDecimal stdLimit;
}
