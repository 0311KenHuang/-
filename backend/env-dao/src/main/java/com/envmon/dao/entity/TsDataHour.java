package com.envmon.dao.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 小时数据：附录 G —— 有效分钟数 ≥ 45 的小时才有效 */
@Data
@TableName("ts_data_hour")
public class TsDataHour implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long tenantId;
    private Long stationId;
    private String factorCode;

    /** 小时起点 */
    private LocalDateTime ts;

    private BigDecimal avgValue;
    private BigDecimal maxValue;
    private BigDecimal minValue;

    /** 有效分钟数 */
    private Integer validCnt;
    /** 实际收到的分钟数 */
    private Integer totalCnt;
    /** validCnt / 60 */
    private BigDecimal validRate;
    private Integer isValid;

    /** 有效=N；无效取最严重的设备标记；单纯缺数为 U */
    private String flag;
}
