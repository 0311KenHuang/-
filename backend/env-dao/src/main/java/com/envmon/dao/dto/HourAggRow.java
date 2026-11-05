package com.envmon.dao.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/** 小时数据按 (站房, 因子) 的聚合结果，供日均值计算 */
@Data
public class HourAggRow implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long tenantId;
    private Long stationId;
    private String factorCode;

    private Integer totalHourCnt;
    private Integer validHourCnt;

    /** 当天落在停运区间内的小时数，日均有效率的分母要扣掉 */
    private BigDecimal stoppedHours;

    private BigDecimal avgValid;
    private BigDecimal maxValid;
    private BigDecimal minValid;

    private BigDecimal avgAll;
    private BigDecimal maxAll;
    private BigDecimal minAll;
}
