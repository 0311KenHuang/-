package com.envmon.dao.dto;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 分钟数据按 (站房, 因子) 的聚合结果，供小时值计算。
 * 同时给出"仅有效分钟"和"全部分钟"两套统计：
 * 附录 G 规定时段内无有效数据时降级用全部数据参与计算。
 */
@Data
public class MinuteAggRow implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long tenantId;
    private Long stationId;
    private String factorCode;

    /** 所属小时的起点，仅按小时聚合时有值 */
    private LocalDateTime hourTs;

    /** 实际收到的分钟条数 */
    private Integer totalCnt;
    /** quality = 1 的分钟条数 */
    private Integer validCnt;

    private BigDecimal avgValid;
    private BigDecimal maxValid;
    private BigDecimal minValid;

    private BigDecimal avgAll;
    private BigDecimal maxAll;
    private BigDecimal minAll;

    /** 各设备标记出现的次数，用于决定小时值的 flag */
    private Integer dCnt;
    private Integer cCnt;
    private Integer mCnt;
    private Integer tCnt;
    private Integer tdCnt;
    private Integer noFlagCnt;
}
