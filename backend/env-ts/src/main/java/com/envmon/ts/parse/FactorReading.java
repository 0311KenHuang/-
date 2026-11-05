package com.envmon.ts.parse;

import lombok.Data;

import java.math.BigDecimal;

/** 单个因子在一次采集里的读数 */
@Data
public class FactorReading {

    /** {因子}-Rtd，解析失败为 null */
    private BigDecimal value;

    /** {因子}-Flag，规范化为 N/D/C/M/T/Td；未上报为 null */
    private String devFlag;
}
