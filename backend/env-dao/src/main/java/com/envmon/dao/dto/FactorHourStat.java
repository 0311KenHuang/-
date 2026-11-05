package com.envmon.dao.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/** 某因子在一段时间内的小时有效率统计 */
@Data
public class FactorHourStat implements Serializable {

    private static final long serialVersionUID = 1L;

    private String factorCode;
    /** 已有小时值条数 */
    private Integer totalHourCnt;
    /** 其中有效的小时数 */
    private Integer validHourCnt;
    /** 有效小时的均值 */
    private BigDecimal avgValue;
}
