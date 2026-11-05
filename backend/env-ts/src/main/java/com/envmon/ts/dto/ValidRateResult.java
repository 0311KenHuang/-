package com.envmon.ts.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** 某站房某天的有效率汇总 */
@Data
public class ValidRateResult implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long stationId;
    private LocalDate date;
    /** 分母：过去日期为 24 扣停运小时，当天为已过小时数扣停运小时 */
    private Integer expectedHours;
    /** 统计窗口内的停运小时数 */
    private Integer stoppedHours;
    /** 当前是否仍在停运：前端可显示"停运"而不是"有效率 0%" */
    private Boolean stopped;
    private List<ValidRateItem> factors = new ArrayList<ValidRateItem>();
}
