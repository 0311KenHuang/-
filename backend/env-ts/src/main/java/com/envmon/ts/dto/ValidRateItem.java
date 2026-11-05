package com.envmon.ts.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/** 单因子有效率（附录 G） */
@Data
public class ValidRateItem implements Serializable {

    private static final long serialVersionUID = 1L;

    private String factorCode;
    private String unit;
    /** 有效小时数（小时值 is_valid = 1） */
    private Integer validHourCnt;
    /** 已有小时值条数 */
    private Integer totalHourCnt;
    /** 有效小时数 / 应有时段小时数 */
    private BigDecimal validRate;
    /** 是否达到 ≥75% 有效小时 */
    private Boolean valid;
    /** 有效小时均值 */
    private BigDecimal avgValue;
}
