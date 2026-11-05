package com.envmon.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 日数据：附录 G —— 有效小时数 ≥ 75%（18 小时）的日才有效 */
@Data
@TableName("ts_data_day")
public class TsDataDay implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;
    private Long stationId;
    private String factorCode;

    /** 日起点 00:00 */
    private LocalDateTime ts;

    private BigDecimal avgValue;
    private BigDecimal maxValue;
    private BigDecimal minValue;
    /** 日排放量，预留 */
    private BigDecimal emission;

    private Integer validHourCnt;
    private Integer totalHourCnt;
    /** 应统计小时数 = 24 - 停运小时数；有效率的分母 */
    private Integer expectedHourCnt;
    /** 有效小时数 / 应统计小时数 */
    private BigDecimal validRate;
    private Integer isValid;
}
